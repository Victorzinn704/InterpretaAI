package br.gov.interpretaai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File
import org.junit.Test

class LearningStoryPackParserTest {
    @Test
    fun acceptsAnApprovedOfflinePackWithTheExistingComponents() {
        val result = LearningStoryPackParser.parse(validPack(), appVersion = 21)

        assertTrue(result is StoryPackLoadResult.Accepted)
        val pack = (result as StoryPackLoadResult.Accepted).pack
        assertEquals("pack_maca_001", pack.packId)
        assertEquals(4, pack.nodes.size)
        assertEquals(StoryNodeType.COMIC, pack.nodes.first().type)
        assertEquals(StoryNodeType.END, pack.nodes.last().type)
    }

    @Test
    fun acceptsTheVersionedExampleUsedByTheBackendContract() {
        val workingDirectory = File(requireNotNull(System.getProperty("user.dir")))
        val projectRoot = workingDirectory.takeUnless { it.name == "app" }
            ?: requireNotNull(workingDirectory.parentFile)
        val contractExample = File(projectRoot,
            "docs/v2/contracts/example-apple-story-pack.json")
        assertTrue("Contract example missing: ${contractExample.absolutePath}", contractExample.isFile)

        val result = LearningStoryPackParser.parse(contractExample.readText(), appVersion = 21)
        assertTrue("Contract example was blocked: $result", result is StoryPackLoadResult.Accepted)
        assertEquals("pack_maca_001", (result as StoryPackLoadResult.Accepted).pack.packId)
    }

    @Test
    fun blocksUnsupportedAppMissingAssetsBrokenWordsAndPunitiveLanguage() {
        assertIssue(validPack().replace("\"minAppVersion\": 20", "\"minAppVersion\": 22"),
            appVersion = 21, code = "app_version_unsupported")
        assertIssue(validPack().replace("\"imageAssetId\": \"maca_objeto\"", "\"imageAssetId\": \"ausente\""),
            code = "asset_not_found")
        assertIssue(validPack().replace("\"letterTiles\": [\"M\", \"A\", \"Ç\", \"Ã\", \"B\"]",
            "\"letterTiles\": [\"M\", \"A\", \"Ç\", \"B\"]"), code = "word_cannot_be_built")
        assertIssue(validPack().replace("Você montou MAÇÃ!", "Você errou a palavra."), code = "punitive_error")
    }

    @Test
    fun rejectsUnsafeStructureAndNonProgressiveSupport() {
        assertIssue(validPack().replace("\"type\": \"COMIC\"", "\"type\": \"NEW_GAME\""),
            code = "invalid_structure")
        assertIssue(validPack().replace("\"level\":\"VOICE_REPEAT\"", "\"level\":\"CHOICE_REVEALED\""),
            code = "support_order")
    }

    @Test
    fun blocksAnActivityWhoseResponsiveAssetsWouldExhaustATabletCache() {
        val hashA = "a".repeat(64)
        val huge = 8_388_608
        val original = """{"role":"PHONE","path":"images/comic.webp","mediaType":"image/webp","bytes":1200,"sha256":"$hashA"}"""
        val expanded = """{"role":"PHONE","path":"images/comic.webp","mediaType":"image/webp","bytes":$huge,"sha256":"$hashA"},{"role":"TABLET","path":"images/comic_tablet.webp","mediaType":"image/webp","bytes":$huge,"sha256":"${"c".repeat(64)}"},{"role":"THUMBNAIL","path":"images/comic_thumb.webp","mediaType":"image/webp","bytes":$huge,"sha256":"${"d".repeat(64)}"}"""
        val oversized = validPack().replace(original, expanded)
            .replace("\"bytes\":800", "\"bytes\":$huge")

        assertIssue(oversized, code = "asset_total_too_large")
    }

    @Test
    fun blocksWordTilesThatDoNotFitOnePhoneViewport() {
        assertIssue(validPack().replace(
            "\"letterTiles\": [\"M\", \"A\", \"Ç\", \"Ã\", \"B\"]",
            "\"letterTiles\": [\"M\", \"A\", \"Ç\", \"Ã\", \"B\", \"O\", \"P\", \"U\", \"X\"]"
        ), code = "invalid_structure")
        assertIssue(validPack().replace("\"M\", \"A\", \"Ç\"", "\"MA\", \"A\", \"Ç\""),
            code = "invalid_structure")
    }

    private fun assertIssue(raw: String, appVersion: Int = 21, code: String) {
        val result = LearningStoryPackParser.parse(raw, appVersion)
        assertTrue("Expected $code but result was $result", result is StoryPackLoadResult.Blocked)
        assertTrue((result as StoryPackLoadResult.Blocked).issues.any { it.code == code })
    }

    private fun validPack() = """
        {
          "schemaVersion": "1.0",
          "packId": "pack_maca_001",
          "storyId": "historia_maca_001",
          "version": 1,
          "minAppVersion": 20,
          "title": "A maçã da LÉIA",
          "methodology": "LEIA",
          "objectiveIds": ["reconhecer_maca", "formar_palavra_maca"],
          "startNodeId": "cena_inicio",
          "nodes": [
            {
              "id": "cena_inicio",
              "type": "COMIC",
              "objectiveIds": ["reconhecer_maca"],
              "visualAssetId": "quadrinho_maca",
              "altText": "LÉIA e o cachorro procuram uma maçã.",
              "dialogue": [{"speaker":"LEIA_TEACHER","text":"Vamos procurar uma fruta redonda?"}],
              "supports": [
                {"level":"VOICE_REPEAT","spokenHint":"Ouça a pista de novo."},
                {"level":"VISUAL_CUE","spokenHint":"Olhe a cesta.","visualTargetId":"quadrinho_maca"}
              ],
              "nextNodeId": "montar_maca"
            },
            {
              "id": "montar_maca",
              "type": "PUZZLE",
              "objectiveIds": ["reconhecer_maca"],
              "imageAssetId": "maca_objeto",
              "grid": "2x2",
              "interactionModes": ["TAP_SWAP", "DRAG"],
              "instruction": "Monte a maçã.",
              "completionSpeech": "Você montou MAÇÃ!",
              "nextNodeId": "formar_maca"
            },
            {
              "id": "formar_maca",
              "type": "WORD_BUILDER",
              "objectiveIds": ["formar_palavra_maca"],
              "imageAssetId": "maca_objeto",
              "targetWord": "MAÇÃ",
              "letterTiles": ["M", "A", "Ç", "Ã", "B"],
              "instruction": "Forme MAÇÃ.",
              "completionSpeech": "A palavra está pronta.",
              "nextNodeId": "fim"
            },
            {
              "id": "fim",
              "type": "END",
              "objectiveIds": ["reconhecer_maca", "formar_palavra_maca"],
              "closingSpeech": "Vocês encontraram a maçã."
            }
          ],
          "assets": [
            {"id":"quadrinho_maca","kind":"SCENE_IMAGE","required":true,"variants":[{"role":"PHONE","path":"images/comic.webp","mediaType":"image/webp","bytes":1200,"sha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"}]},
            {"id":"maca_objeto","kind":"OBJECT_IMAGE","required":true,"variants":[{"role":"PHONE","path":"images/apple.webp","mediaType":"image/webp","bytes":800,"sha256":"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}]}
          ],
          "accessibility": {"minTouchTargetDp":48,"reducedStimuliSupported":true,"spokenInstructions":true,"noRequiredScroll":true},
          "provenance": {
            "createdBy":"ASSISTED",
            "approvedBy":"professora_demo",
            "approvedAt":"2026-09-16T12:00:00Z",
            "sourceRefs":[],
            "assetOrigins":[
              {"assetId":"quadrinho_maca","origin":"AI_GENERATED","reviewedByTeacher":true},
              {"assetId":"maca_objeto","origin":"TEACHER_UPLOAD","reviewedByTeacher":true}
            ]
          }
        }
    """.trimIndent()
}
