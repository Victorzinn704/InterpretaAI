package br.gov.interpretaai.domain

import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.time.Instant

enum class StoryNodeType { COMIC, PUZZLE, WORD_BUILDER, GROUP_HANDOFF, END }
enum class StorySupportLevel { VOICE_REPEAT, VISUAL_CUE, CHOICE_REVEALED }
enum class StoryAssetKind { SCENE_IMAGE, OBJECT_IMAGE, CHARACTER_IMAGE, NARRATION_AUDIO }
enum class StoryAssetRole { PHONE, TABLET, THUMBNAIL, AUDIO }

data class StorySupport(
    val level: StorySupportLevel,
    val spokenHint: String,
    val visualTargetId: String?
)

data class StoryDialogueLine(val speaker: String, val text: String, val audioAssetId: String?)

sealed interface StoryNode {
    val id: String
    val type: StoryNodeType
    val objectiveIds: List<String>
    val supports: List<StorySupport>
    val nextNodeId: String?
}

data class ComicStoryNode(
    override val id: String,
    override val objectiveIds: List<String>,
    override val supports: List<StorySupport>,
    val visualAssetId: String,
    val altText: String,
    val dialogue: List<StoryDialogueLine>,
    val prompt: String?,
    override val nextNodeId: String
) : StoryNode {
    override val type = StoryNodeType.COMIC
}

data class PuzzleStoryNode(
    override val id: String,
    override val objectiveIds: List<String>,
    override val supports: List<StorySupport>,
    val imageAssetId: String,
    val grid: String,
    val interactionModes: Set<String>,
    val instruction: String,
    val completionSpeech: String,
    override val nextNodeId: String
) : StoryNode {
    override val type = StoryNodeType.PUZZLE
}

data class WordBuilderStoryNode(
    override val id: String,
    override val objectiveIds: List<String>,
    override val supports: List<StorySupport>,
    val imageAssetId: String,
    val targetWord: String,
    val letterTiles: List<String>,
    val instruction: String,
    val completionSpeech: String,
    override val nextNodeId: String
) : StoryNode {
    override val type = StoryNodeType.WORD_BUILDER
}

data class GroupHandoffStoryNode(
    override val id: String,
    override val objectiveIds: List<String>,
    override val supports: List<StorySupport>,
    val instruction: String,
    override val nextNodeId: String
) : StoryNode {
    override val type = StoryNodeType.GROUP_HANDOFF
}

data class EndStoryNode(
    override val id: String,
    override val objectiveIds: List<String>,
    override val supports: List<StorySupport>,
    val closingSpeech: String
) : StoryNode {
    override val type = StoryNodeType.END
    override val nextNodeId: String? = null
}

data class StoryAssetVariant(
    val role: StoryAssetRole,
    val path: String,
    val mediaType: String,
    val bytes: Long,
    val sha256: String
)

data class StoryAsset(
    val id: String,
    val kind: StoryAssetKind,
    val required: Boolean,
    val variants: List<StoryAssetVariant>
)

data class StoryAssetOrigin(val assetId: String, val reviewedByTeacher: Boolean)

data class LearningStoryPack(
    val schemaVersion: String,
    val packId: String,
    val storyId: String,
    val version: Int,
    val minAppVersion: Int,
    val title: String,
    val methodology: String,
    val objectiveIds: List<String>,
    val startNodeId: String,
    val nodes: List<StoryNode>,
    val assets: List<StoryAsset>,
    val minTouchTargetDp: Int,
    val reducedStimuliSupported: Boolean,
    val spokenInstructions: Boolean,
    val noRequiredScroll: Boolean,
    val assetOrigins: List<StoryAssetOrigin>
)

data class StoryPackIssue(val code: String, val path: String, val message: String)

sealed interface StoryPackLoadResult {
    data class Accepted(val pack: LearningStoryPack) : StoryPackLoadResult
    data class Blocked(val issues: List<StoryPackIssue>) : StoryPackLoadResult
}

/** Local guard: remote content is data, never executable UI behavior. */
object LearningStoryPackParser {
    private const val MAX_PACK_CHARS = 262_144
    private const val MAX_TOTAL_ASSET_BYTES = 25_165_824L
    private val id = Regex("[a-z0-9][a-z0-9_-]{2,63}")
    private val hash = Regex("[a-f0-9]{64}")
    private val safeAssetPath = Regex("[a-zA-Z0-9_./-]+")

    fun parse(raw: String, appVersion: Int): StoryPackLoadResult {
        if (raw.length > MAX_PACK_CHARS) {
            return StoryPackLoadResult.Blocked(listOf(issue(
                "pack_too_large", "$", "O pacote excede o limite local."
            )))
        }
        val pack = try {
            decode(JSONObject(raw))
        } catch (error: Exception) {
            return StoryPackLoadResult.Blocked(listOf(issue(
                "invalid_structure", "$", "O pacote não possui uma estrutura compatível."
            )))
        }
        val issues = validate(pack, appVersion)
        return if (issues.isEmpty()) StoryPackLoadResult.Accepted(pack)
        else StoryPackLoadResult.Blocked(issues)
    }

    private fun decode(json: JSONObject): LearningStoryPack {
        requireOnly(json, setOf(
            "schemaVersion", "packId", "storyId", "version", "minAppVersion", "title",
            "methodology", "objectiveIds", "estimatedMinutes", "startNodeId", "nodes", "assets",
            "accessibility", "provenance", "publishedAt"
        ))
        val accessibility = json.objectValue("accessibility")
        val provenance = json.objectValue("provenance")
        if (json.has("estimatedMinutes")) require(json.getInt("estimatedMinutes") in 3..40)
        return LearningStoryPack(
            schemaVersion = json.text("schemaVersion", 1, 8),
            packId = json.identifier("packId"),
            storyId = json.identifier("storyId"),
            version = json.positiveInt("version"),
            minAppVersion = json.positiveInt("minAppVersion"),
            title = json.text("title", 1, 120),
            methodology = json.text("methodology", 1, 16),
            objectiveIds = json.identifiers("objectiveIds", 1, 8).also { values -> require(values.size == values.toSet().size) },
            startNodeId = json.identifier("startNodeId"),
            nodes = json.arrayValue("nodes").objects(2, 24).map(::decodeNode),
            assets = json.arrayValue("assets").objects(0, 100).map(::decodeAsset),
            minTouchTargetDp = accessibility.positiveInt("minTouchTargetDp"),
            reducedStimuliSupported = accessibility.requiredBoolean("reducedStimuliSupported"),
            spokenInstructions = accessibility.requiredBoolean("spokenInstructions"),
            noRequiredScroll = accessibility.requiredBoolean("noRequiredScroll"),
            assetOrigins = decodeProvenance(provenance)
        )
    }

    private fun decodeProvenance(json: JSONObject): List<StoryAssetOrigin> {
        requireOnly(json, setOf("createdBy", "approvedBy", "approvedAt", "sourceRefs", "assetOrigins"))
        require(json.text("createdBy", 1, 16) in setOf("TEACHER", "ASSISTED"))
        json.identifier("approvedBy")
        Instant.parse(json.text("approvedAt", 20, 40))
        json.arrayValue("sourceRefs").objects(0, 30).forEach { source ->
            requireOnly(source, setOf("sourceId", "sourceVersion"))
            source.identifier("sourceId")
            source.identifier("sourceVersion")
        }
        return json.arrayValue("assetOrigins").objects(0, 100).map {
            requireOnly(it, setOf("assetId", "origin", "provider", "model", "generationId", "reviewedByTeacher"))
            require(it.text("origin", 1, 32) in setOf("TEACHER_UPLOAD", "APPROVED_LIBRARY", "AI_GENERATED", "AI_EDITED"))
            StoryAssetOrigin(it.identifier("assetId"), it.requiredBoolean("reviewedByTeacher"))
        }
    }

    private fun decodeNode(json: JSONObject): StoryNode {
        val type = StoryNodeType.valueOf(json.text("type", 1, 32))
        val base = baseNode(json, type)
        return when (type) {
            StoryNodeType.COMIC -> {
                requireOnly(json, base + setOf("visualAssetId", "altText", "dialogue", "prompt", "nextNodeId"))
                ComicStoryNode(
                    json.identifier("id"), json.uniqueIdentifiers("objectiveIds", 1, 4), supports(json),
                    json.identifier("visualAssetId"), json.text("altText", 1, 240),
                    json.arrayValue("dialogue").objects(1, 6).map(::decodeDialogue),
                    json.optionalText("prompt", 1, 280), json.identifier("nextNodeId")
                )
            }
            StoryNodeType.PUZZLE -> {
                requireOnly(json, base + setOf("imageAssetId", "grid", "interactionModes", "instruction", "completionSpeech", "nextNodeId"))
                val grid = json.text("grid", 1, 4)
                require(grid in setOf("2x2", "3x2"))
                val modes = json.texts("interactionModes", 1, 2, 3, 16).toSet()
                require(modes.all { it in setOf("TAP_SWAP", "DRAG") })
                PuzzleStoryNode(
                    json.identifier("id"), json.uniqueIdentifiers("objectiveIds", 1, 4), supports(json),
                    json.identifier("imageAssetId"), grid, modes,
                    json.text("instruction", 1, 280), json.text("completionSpeech", 1, 280),
                    json.identifier("nextNodeId")
                )
            }
            StoryNodeType.WORD_BUILDER -> {
                requireOnly(json, base + setOf("imageAssetId", "targetWord", "letterTiles", "syllables", "initialLetterName", "initialPhonemeCue", "instruction", "completionSpeech", "nextNodeId"))
                WordBuilderStoryNode(
                    json.identifier("id"), json.uniqueIdentifiers("objectiveIds", 1, 4), supports(json),
                    json.identifier("imageAssetId"), json.text("targetWord", 2, 24),
                    json.texts("letterTiles", 2, 32, 1, 2),
                    json.text("instruction", 1, 280), json.text("completionSpeech", 1, 280),
                    json.identifier("nextNodeId")
                )
            }
            StoryNodeType.GROUP_HANDOFF -> {
                requireOnly(json, base + setOf("instruction", "nextNodeId"))
                GroupHandoffStoryNode(
                    json.identifier("id"), json.uniqueIdentifiers("objectiveIds", 1, 4), supports(json),
                    json.text("instruction", 1, 280), json.identifier("nextNodeId")
                )
            }
            StoryNodeType.END -> {
                requireOnly(json, base + setOf("closingSpeech"))
                EndStoryNode(
                    json.identifier("id"), json.uniqueIdentifiers("objectiveIds", 1, 4), supports(json),
                    json.text("closingSpeech", 1, 280)
                )
            }
        }
    }

    private fun baseNode(json: JSONObject, type: StoryNodeType): Set<String> {
        require(json.text("type", 1, 32) == type.name)
        return setOf("id", "type", "objectiveIds", "supports")
    }

    private fun supports(json: JSONObject): List<StorySupport> {
        if (!json.has("supports")) return emptyList()
        return json.arrayValue("supports").objects(0, 3).map {
            requireOnly(it, setOf("level", "spokenHint", "visualTargetId"))
            StorySupport(
                StorySupportLevel.valueOf(it.text("level", 1, 32)),
                it.text("spokenHint", 1, 280),
                if (it.has("visualTargetId")) it.identifier("visualTargetId") else null
            )
        }
    }

    private fun decodeDialogue(json: JSONObject): StoryDialogueLine {
        requireOnly(json, setOf("speaker", "characterId", "text", "audioAssetId"))
        val speaker = json.text("speaker", 1, 32)
        require(speaker in setOf("LEIA_TEACHER", "NARRATOR", "CHILD_CHARACTER", "DOG"))
        return StoryDialogueLine(
            speaker, json.text("text", 1, 280),
            if (json.has("audioAssetId")) json.identifier("audioAssetId") else null
        )
    }

    private fun decodeAsset(json: JSONObject): StoryAsset {
        requireOnly(json, setOf("id", "kind", "required", "variants"))
        val kind = StoryAssetKind.valueOf(json.text("kind", 1, 32))
        return StoryAsset(
            json.identifier("id"), kind, json.requiredBoolean("required"),
            json.arrayValue("variants").objects(1, 4).map(::decodeVariant)
        )
    }

    private fun decodeVariant(json: JSONObject): StoryAssetVariant {
        requireOnly(json, setOf("role", "path", "mediaType", "width", "height", "bytes", "sha256"))
        val role = StoryAssetRole.valueOf(json.text("role", 1, 16))
        val path = json.text("path", 1, 255)
        val mediaType = json.text("mediaType", 1, 32)
        require(safeAssetPath.matches(path) && path.split('/').all { it.isNotBlank() && it !in setOf(".", "..") })
        require(mediaType in setOf("image/jpeg", "image/png", "image/webp", "audio/ogg", "audio/wav"))
        return StoryAssetVariant(role, path, mediaType, json.positiveLong("bytes", 8_388_608),
            json.text("sha256", 64, 64).also { require(hash.matches(it)) })
    }

    private fun validate(pack: LearningStoryPack, appVersion: Int): List<StoryPackIssue> = buildList {
        fun add(code: String, path: String, message: String) = add(issue(code, path, message))
        if (pack.schemaVersion != "1.0") add("schema_version_unsupported", "$.schemaVersion", "Versão de pacote não suportada.")
        if (pack.methodology != "LEIA") add("methodology_invalid", "$.methodology", "O pacote precisa usar o método LEIA.")
        if (pack.minAppVersion > appVersion) add("app_version_unsupported", "$.minAppVersion", "Atualize o aplicativo para abrir esta atividade.")
        if (pack.objectiveIds.size != pack.objectiveIds.toSet().size) add("duplicate_objective", "$.objectiveIds", "Objetivos não podem se repetir.")

        val nodes = pack.nodes.associateBy { it.id }
        val assets = pack.assets.associateBy { it.id }
        if (nodes.size != pack.nodes.size) add("duplicate_node_id", "$.nodes", "Identificadores de cena devem ser únicos.")
        if (assets.size != pack.assets.size) add("duplicate_asset_id", "$.assets", "Identificadores de mídia devem ser únicos.")
        if (pack.startNodeId !in nodes) add("start_not_found", "$.startNodeId", "A cena inicial não existe.")

        pack.nodes.forEachIndexed { index, node ->
            val nodePath = "$.nodes[$index]"
            if (!pack.objectiveIds.containsAll(node.objectiveIds)) add("objective_not_declared", "$nodePath.objectiveIds", "A cena usa um objetivo não declarado.")
            node.nextNodeId?.let { if (it !in nodes) add("next_not_found", "$nodePath.nextNodeId", "A próxima cena não existe.") }
            val referenced = when (node) {
                is ComicStoryNode -> listOf(node.visualAssetId) + node.dialogue.mapNotNull { it.audioAssetId }
                is PuzzleStoryNode -> listOf(node.imageAssetId)
                is WordBuilderStoryNode -> listOf(node.imageAssetId)
                else -> emptyList()
            }
            referenced.filter { it !in assets }.forEach { add("asset_not_found", nodePath, "A mídia referenciada não existe.") }
            val levels = node.supports.map { it.level }
            if (levels.size != levels.toSet().size) add("duplicate_support", "$nodePath.supports", "Um apoio foi repetido.")
            if (levels != levels.sorted()) add("support_order", "$nodePath.supports", "Apoios devem avançar progressivamente.")
            node.supports.filter { it.visualTargetId != null && it.visualTargetId !in nodes && it.visualTargetId !in assets }
                .forEach { add("visual_target_not_found", "$nodePath.supports", "O alvo visual não existe.") }
            if (node is WordBuilderStoryNode && !canBuild(node.targetWord, node.letterTiles)) {
                add("word_cannot_be_built", "$nodePath.letterTiles", "Faltam letras para formar a palavra.")
            }
        }
        checkGraph(pack, nodes, ::add)
        if (pack.minTouchTargetDp < 48) add("touch_target_too_small", "$.accessibility.minTouchTargetDp", "Mínimo: 48dp.")
        if (!pack.reducedStimuliSupported || !pack.spokenInstructions || !pack.noRequiredScroll) {
            add("accessibility_required", "$.accessibility", "Acessibilidade infantil obrigatória ausente.")
        }
        pack.assets.forEachIndexed { index, asset ->
            if (asset.variants.map { it.role }.size != asset.variants.map { it.role }.toSet().size) {
                add("duplicate_asset_variant", "$.assets[$index].variants", "A mídia repete a mesma variante.")
            }
        }
        if (pack.assets.sumOf { asset -> asset.variants.sumOf(StoryAssetVariant::bytes) } > MAX_TOTAL_ASSET_BYTES) {
            add("asset_total_too_large", "$.assets", "A atividade ultrapassa o limite de mídia deste aparelho.")
        }
        if (pack.assetOrigins.map { it.assetId }.sorted() != pack.assets.map { it.id }.sorted()) {
            add("asset_provenance_mismatch", "$.provenance.assetOrigins", "Cada mídia precisa de uma origem.")
        }
        pack.assetOrigins.filterNot { it.reviewedByTeacher }.forEach {
            add("asset_not_reviewed", "$.provenance.assetOrigins", "Mídia não revisada pela professora.")
        }
        textValues(pack).forEach { (path, text) -> banned.entries.firstOrNull { it.value.containsMatchIn(text) }
            ?.let { add(it.key, path, "Linguagem bloqueada para a jornada infantil.") } }
    }.sortedWith(compareBy(StoryPackIssue::path, StoryPackIssue::code))

    private fun checkGraph(
        pack: LearningStoryPack,
        nodes: Map<String, StoryNode>,
        add: (String, String, String) -> Unit
    ) {
        if (pack.startNodeId !in nodes) return
        val visited = mutableSetOf<String>()
        val active = mutableSetOf<String>()
        var endReached = false
        fun walk(id: String) {
            if (id in active) { add("cycle_detected", "$.nodes", "A história contém um ciclo."); return }
            if (!visited.add(id)) return
            active += id
            val node = nodes.getValue(id)
            if (node.type == StoryNodeType.END) endReached = true
            else node.nextNodeId?.takeIf { it in nodes }?.let(::walk)
                ?: add("dead_end", "$.nodes[$id]", "O caminho termina sem encerramento.")
            active -= id
        }
        walk(pack.startNodeId)
        if (visited.size != nodes.size) add("unreachable_nodes", "$.nodes", "Há cenas inalcançáveis.")
        if (!endReached) add("end_not_reached", "$.nodes", "Não existe caminho até o encerramento.")
    }

    private fun canBuild(word: String, tiles: List<String>): Boolean {
        val required = letters(word).groupingBy { it }.eachCount()
        val available = tiles.flatMap(::letters).groupingBy { it }.eachCount()
        return required.all { (letter, count) -> (available[letter] ?: 0) >= count }
    }

    private fun letters(value: String): List<String> = Normalizer.normalize(value, Normalizer.Form.NFC)
        .uppercase().filterNot(Char::isWhitespace).map(Char::toString)

    private fun textValues(pack: LearningStoryPack): List<Pair<String, String>> = buildList {
        add("$.title" to pack.title)
        pack.nodes.forEachIndexed { index, node ->
            node.supports.forEachIndexed { supportIndex, support -> add("$.nodes[$index].supports[$supportIndex].spokenHint" to support.spokenHint) }
            when (node) {
                is ComicStoryNode -> {
                    add("$.nodes[$index].altText" to node.altText)
                    node.prompt?.let { add("$.nodes[$index].prompt" to it) }
                    node.dialogue.forEachIndexed { dialogueIndex, line -> add("$.nodes[$index].dialogue[$dialogueIndex].text" to line.text) }
                }
                is PuzzleStoryNode -> { add("$.nodes[$index].instruction" to node.instruction); add("$.nodes[$index].completionSpeech" to node.completionSpeech) }
                is WordBuilderStoryNode -> { add("$.nodes[$index].instruction" to node.instruction); add("$.nodes[$index].completionSpeech" to node.completionSpeech) }
                is GroupHandoffStoryNode -> add("$.nodes[$index].instruction" to node.instruction)
                is EndStoryNode -> add("$.nodes[$index].closingSpeech" to node.closingSpeech)
            }
        }
    }

    private val banned = mapOf(
        "punitive_error" to Regex("\\b(voc[eê]\\s+errou|resposta\\s+errada|est[aá]\\s+errado)\\b", RegexOption.IGNORE_CASE),
        "diagnosis" to Regex("\\b(diagn[oó]stic|transtorno|d[eé]ficit|dislexia)\\w*\\b", RegexOption.IGNORE_CASE),
        "guilt" to Regex("\\b(n[aã]o\\s+me\\s+abandone|voc[eê]\\s+est[aá]\\s+demorando|preste\\s+aten[cç][aã]o)\\b", RegexOption.IGNORE_CASE),
        "grading" to Regex("\\b(nota|ranking|reprovad[oa])\\b", RegexOption.IGNORE_CASE)
    )

    private fun issue(code: String, path: String, message: String) = StoryPackIssue(code, path, message)

    private fun requireOnly(json: JSONObject, allowed: Set<String>) {
        require(json.keys().asSequence().all { it in allowed })
    }

    private fun JSONObject.identifier(key: String): String = text(key, 3, 64).also { require(id.matches(it)) }
    private fun JSONObject.text(key: String, min: Int, max: Int): String = getString(key).also { require(it.length in min..max) }
    private fun JSONObject.optionalText(key: String, min: Int, max: Int): String? = if (has(key)) text(key, min, max) else null
    private fun JSONObject.positiveInt(key: String): Int = getInt(key).also { require(it >= 1) }
    private fun JSONObject.positiveLong(key: String, max: Long): Long = getLong(key).also { require(it in 1..max) }
    private fun JSONObject.requiredBoolean(key: String): Boolean = getBoolean(key)
    private fun JSONObject.objectValue(key: String): JSONObject = getJSONObject(key)
    private fun JSONObject.arrayValue(key: String): JSONArray = getJSONArray(key)
    private fun JSONObject.identifiers(key: String, min: Int, max: Int): List<String> = arrayValue(key).texts(min, max, 3, 64).also { values -> require(values.all(id::matches)) }
    private fun JSONObject.uniqueIdentifiers(key: String, min: Int, max: Int): List<String> = identifiers(key, min, max).also { values -> require(values.size == values.toSet().size) }
    private fun JSONObject.texts(key: String, minItems: Int, maxItems: Int, minLength: Int, maxLength: Int): List<String> = arrayValue(key).texts(minItems, maxItems, minLength, maxLength)
    private fun JSONArray.objects(min: Int, max: Int): List<JSONObject> {
        require(length() in min..max)
        return List(length()) { getJSONObject(it) }
    }
    private fun JSONArray.texts(minItems: Int, maxItems: Int, minLength: Int, maxLength: Int): List<String> {
        require(length() in minItems..maxItems)
        return List(length()) { getString(it).also { value -> require(value.length in minLength..maxLength) } }
    }
}
