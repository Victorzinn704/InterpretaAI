package br.gov.interpretaai.server.media;

import br.gov.interpretaai.server.api.MediaUploadModels.CreateUploadRequest;
import br.gov.interpretaai.server.api.MediaUploadModels.UploadReceipt;
import br.gov.interpretaai.server.identity.InstitutionAction;
import br.gov.interpretaai.server.identity.InstitutionAuditStore;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaUploadService {
    private static final Pattern IDEMPOTENCY_KEY =
            Pattern.compile("[A-Za-z0-9._:-]{16,128}");
    private static final Set<String> MEDIA_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final InstitutionalAccessService access;
    private final InstitutionAuditStore audit;
    private final MediaUploadStore uploads;
    private final PrivateObjectStore objects;
    private final Clock clock;
    private final long maxBytes;
    private final Duration ttl;

    public MediaUploadService(
            InstitutionalAccessService access,
            InstitutionAuditStore audit,
            MediaUploadStore uploads,
            PrivateObjectStore objects,
            Clock clock,
            @Value("${interpretaai.media.max-bytes:10485760}") long maxBytes,
            @Value("${interpretaai.media.upload-ttl-minutes:15}") long ttlMinutes) {
        this.access = access;
        this.audit = audit;
        this.uploads = uploads;
        this.objects = objects;
        this.clock = clock;
        this.maxBytes = maxBytes;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Transactional
    public MediaUploadStore.Upload create(
            String oidcSubject,
            String schoolId,
            String idempotencyKey,
            CreateUploadRequest request) {
        validateCreate(idempotencyKey, request);
        var grant = access.requireSchoolAction(
                oidcSubject, schoolId, InstitutionAction.UPLOAD_MEDIA);
        String requestFingerprint = fingerprint(request);
        var existing = uploads.findByIdempotency(grant.userId(), schoolId, idempotencyKey);
        if (existing.isPresent()) return matching(existing.orElseThrow(), requestFingerprint);

        Instant now = clock.instant();
        String mediaId = "media_" + UUID.randomUUID().toString().replace("-", "");
        var upload = new MediaUploadStore.Upload(
                mediaId,
                schoolId,
                grant.userId(),
                idempotencyKey,
                requestFingerprint,
                request.fileName(),
                request.mediaType(),
                request.bytes(),
                "PENDING_UPLOAD",
                "pending/" + mediaId,
                null,
                null,
                now.plus(ttl),
                now);
        try {
            uploads.insert(upload, now);
        } catch (DataIntegrityViolationException race) {
            return matching(uploads.findByIdempotency(
                    grant.userId(), schoolId, idempotencyKey).orElseThrow(() -> race), requestFingerprint);
        }
        audit.append(grant.userId(), schoolId, "MEDIA_UPLOAD_CREATED", "MEDIA", mediaId, now);
        return upload;
    }

    @Transactional
    public synchronized UploadReceipt receive(
            String oidcSubject,
            String schoolId,
            String mediaId,
            String contentType,
            InputStream input) {
        var grant = access.requireSchoolAction(
                oidcSubject, schoolId, InstitutionAction.UPLOAD_MEDIA);
        var upload = uploads.findOwned(mediaId, grant.userId(), schoolId)
                .orElseThrow(InstitutionalAccessService.AccessDeniedException::new);
        String canonicalContentType = contentType.split(";", 2)[0].trim().toLowerCase();
        if (!upload.mediaType().equals(canonicalContentType)) {
            throw new MediaUploadException(
                    415, "media_type_mismatch", "O tipo enviado difere da sessão de upload.");
        }
        Instant now = clock.instant();
        if (upload.expiresAt().isBefore(now)) {
            uploads.markExpired(mediaId, now);
            throw new MediaUploadException(
                    410, "media_upload_expired", "A sessão de upload expirou.");
        }

        PrivateObjectStore.StagedObject staged;
        try {
            staged = objects.stage(mediaId, input, Math.min(maxBytes, upload.declaredBytes()));
        } catch (IOException error) {
            throw new MediaUploadException(
                    503, "media_storage_unavailable", "Não foi possível guardar a imagem agora.");
        }
        if (staged.bytes() != upload.declaredBytes()) {
            objects.discard(staged);
            throw new MediaUploadException(
                    422, "media_size_mismatch", "O tamanho enviado difere da sessão de upload.");
        }
        if ("UPLOADED".equals(upload.status())) {
            objects.discard(staged);
            if (staged.bytes() == upload.actualBytes() && staged.sha256().equals(upload.sha256())) {
                return receipt(upload);
            }
            throw new MediaUploadException(
                    409, "media_already_uploaded", "A sessão já recebeu outro conteúdo.");
        }
        if (!"PENDING_UPLOAD".equals(upload.status())) {
            objects.discard(staged);
            throw new MediaUploadException(
                    409, "media_upload_not_pending", "A sessão não aceita novos dados.");
        }

        String objectKey = "raw/" + schoolId + "/" + mediaId + "/" + staged.sha256();
        try {
            objects.commit(staged, objectKey);
        } catch (IOException error) {
            objects.discard(staged);
            throw new MediaUploadException(
                    503, "media_storage_unavailable", "Não foi possível guardar a imagem agora.");
        }
        if (!uploads.markUploaded(mediaId, objectKey, staged.bytes(), staged.sha256(), now)) {
            throw new MediaUploadException(
                    409, "media_upload_race", "O upload foi atualizado por outra solicitação.");
        }
        audit.append(grant.userId(), schoolId, "MEDIA_UPLOAD_RECEIVED", "MEDIA", mediaId, now);
        return new UploadReceipt(mediaId, "UPLOADED", staged.bytes(), staged.sha256());
    }

    private MediaUploadStore.Upload matching(
            MediaUploadStore.Upload existing, String requestFingerprint) {
        if (!existing.requestFingerprint().equals(requestFingerprint)) {
            throw new MediaUploadException(
                    409, "idempotency_conflict", "A chave já pertence a outra requisição.");
        }
        return existing;
    }

    private void validateCreate(String idempotencyKey, CreateUploadRequest request) {
        if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new MediaUploadException(
                    400, "invalid_idempotency_key", "Use uma chave idempotente de 16 a 128 caracteres.");
        }
        if (request.fileName() == null
                || request.fileName().isBlank()
                || request.fileName().length() > 160
                || request.fileName().chars().anyMatch(character -> character < 32)
                || request.fileName().contains("/")
                || request.fileName().contains("\\")) {
            throw new MediaUploadException(
                    400, "invalid_file_name", "O nome do arquivo não é válido.");
        }
        if (!MEDIA_TYPES.contains(request.mediaType())) {
            throw new MediaUploadException(
                    415, "unsupported_media_type", "Envie uma imagem JPEG, PNG ou WebP.");
        }
        if (!"STORY_SOURCE".equals(request.purpose())) {
            throw new MediaUploadException(
                    400, "invalid_media_purpose", "A finalidade do upload não é aceita.");
        }
        if (request.bytes() < 1 || request.bytes() > maxBytes) {
            throw new MediaUploadException(
                    413, "media_too_large", "A imagem ultrapassa o limite permitido.");
        }
    }

    private String fingerprint(CreateUploadRequest request) {
        return sha256(String.join("\u0000",
                request.fileName(), request.mediaType(), Long.toString(request.bytes()), request.purpose()));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private UploadReceipt receipt(MediaUploadStore.Upload upload) {
        return new UploadReceipt(
                upload.mediaId(), upload.status(), upload.actualBytes(), upload.sha256());
    }
}
