package br.gov.interpretaai.platform.storycache

import java.io.File
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class StoryPackFileStore(private val root: File) {
    init {
        require(root.exists() || root.mkdirs()) { "story_cache_root_unavailable" }
    }

    fun install(
        expectedSha256: String,
        expectedBytes: Long,
        input: InputStream
    ): CacheWriteResult {
        if (expectedBytes !in 1..MAX_ASSET_BYTES) return CacheWriteResult.Rejected("asset_size_invalid")
        val destination = assetFile(expectedSha256)
        if (isVerified(destination, expectedSha256, expectedBytes)) return CacheWriteResult.AlreadyPresent(destination)
        val temporary = File(root, ".${expectedSha256}.${System.nanoTime()}.partial")
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            var written = 0L
            input.use { source -> temporary.outputStream().buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = source.read(buffer)
                    if (read < 0) break
                    written += read
                    if (written > expectedBytes || written > MAX_ASSET_BYTES) {
                        return@use
                    }
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                }
            } }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
            if (written != expectedBytes) {
                return CacheWriteResult.Rejected("asset_bytes_mismatch")
            }
            if (!actualHash.equals(expectedSha256, ignoreCase = false)) {
                return CacheWriteResult.Rejected("asset_hash_mismatch")
            }
            try {
                moveAtomically(temporary, destination)
                CacheWriteResult.Stored(destination)
            } catch (_: FileAlreadyExistsException) {
                if (isVerified(destination, expectedSha256, expectedBytes)) {
                    CacheWriteResult.AlreadyPresent(destination)
                } else {
                    CacheWriteResult.Rejected("asset_storage_unavailable")
                }
            }
        } catch (_: Exception) {
            CacheWriteResult.Rejected("asset_storage_unavailable")
        } finally {
            deleteQuietly(temporary)
        }
    }

    fun isVerified(expectedSha256: String, expectedBytes: Long): Boolean =
        isVerified(assetFile(expectedSha256), expectedSha256, expectedBytes)

    fun verifiedFile(expectedSha256: String, expectedBytes: Long): File? =
        assetFile(expectedSha256).takeIf { isVerified(it, expectedSha256, expectedBytes) }

    fun deletePartialFiles() {
        root.listFiles { file -> file.name.endsWith(".partial") }?.forEach(::deleteQuietly)
    }

    private fun assetFile(sha256: String): File = File(root, sha256)

    private fun isVerified(file: File, expectedSha256: String, expectedBytes: Long): Boolean {
        if (!file.isFile || file.length() != expectedBytes) return false
        return runCatching {
            file.inputStream().use { stream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
                digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) } == expectedSha256
            }
        }.getOrDefault(false)
    }

    private fun moveAtomically(temporary: File, destination: File) {
        try {
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), destination.toPath())
        }
    }

    private fun deleteQuietly(file: File) {
        runCatching { Files.deleteIfExists(file.toPath()) }
    }

    sealed interface CacheWriteResult {
        data class Stored(val file: File) : CacheWriteResult
        data class AlreadyPresent(val file: File) : CacheWriteResult
        data class Rejected(val code: String) : CacheWriteResult
    }

    private companion object {
        const val MAX_ASSET_BYTES = 8_388_608L
    }
}
