package br.gov.interpretaai.server.authoring;

import br.gov.interpretaai.server.api.AuthoringJobModels.CreateAuthoringJobRequest;
import br.gov.interpretaai.server.api.AuthoringJobModels.YearRange;
import br.gov.interpretaai.server.authoring.AuthoringPlanContract.DraftPlan;
import br.gov.interpretaai.server.authoring.GuidanceSourceCatalog.Evidence;
import br.gov.interpretaai.server.authoring.GuidanceSourceCatalog.Query;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Teacher-side planning boundary. No child data, image bytes or publication capability. */
public final class AuthoringPlanService {
    @FunctionalInterface
    public interface Provider {
        String propose(ChatRequest request);
    }

    public static Provider langChain4j(ChatModel model) {
        return request -> model.chat(request).aiMessage().text();
    }

    public static final class NoApprovedGuidance extends RuntimeException {
        public NoApprovedGuidance() {
            super("Nenhuma orientação aprovada atende a este objetivo e faixa. "
                    + "A professora pode planejar manualmente enquanto a curadoria é concluída.");
        }
    }

    private final GuidanceSourceCatalog guidance;
    private final ObjectMapper mapper;
    private final Provider provider;

    public AuthoringPlanService(GuidanceSourceCatalog guidance, ObjectMapper mapper, Provider provider) {
        this.guidance = guidance;
        this.mapper = mapper;
        this.provider = provider;
    }

    public DraftPlan plan(CreateAuthoringJobRequest input, String schoolId, String userId) {
        if (input == null || input.confirmedWord() == null || input.confirmedWord().isBlank()
                || input.requestedComponents() == null || input.requestedComponents().isEmpty()
                || input.objectiveIds() == null || input.objectiveIds().isEmpty()
                || input.yearRange() == null || input.source() == null) {
            throw new IllegalArgumentException("authoring_input_incomplete");
        }
        String subject = input.source().theme() == null ? input.confirmedWord()
                : input.confirmedWord() + " " + input.source().theme();
        List<Evidence> found = guidance.retrieve(new Query(subject,
                Set.copyOf(input.objectiveIds()), year(input.yearRange()), Set.of(),
                Map.of("SCHOOL", schoolId, "TEACHER", userId), 5));
        if (found.isEmpty()) throw new NoApprovedGuidance();
        String proposal = provider.propose(AuthoringPlanContract.request(mapper, input, found));
        return AuthoringPlanContract.decode(mapper, proposal, input, found);
    }

    private static String year(YearRange range) {
        return switch (range) {
            case YEAR_1 -> "1_YEAR";
            case YEAR_2 -> "2_YEAR";
            case YEAR_3 -> "3_YEAR";
            case YEAR_4 -> "4_YEAR";
            case YEAR_5 -> "5_YEAR";
            case MIXED -> "MIXED";
        };
    }
}
