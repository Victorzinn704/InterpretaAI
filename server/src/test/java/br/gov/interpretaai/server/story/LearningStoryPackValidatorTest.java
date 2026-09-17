package br.gov.interpretaai.server.story;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class LearningStoryPackValidatorTest {
    private final LearningStoryPackValidator validator = new LearningStoryPackValidator(new ObjectMapper());

    @Test
    void acceptsTheVersionedContractExample() throws Exception {
        var result = validator.validate(example());

        assertThat(result.issues()).isEmpty();
        assertThat(result.storyId()).isEqualTo("historia_lanche_leia");
        assertThat(result.version()).isEqualTo(1);
    }

    @Test
    void keepsRepeatedLetterTilesAndRejectsOnlyAnImpossibleWord() throws Exception {
        String repeatedLetters = example()
                .replace("\"targetWord\": \"MAÇÃ\"", "\"targetWord\": \"MAMÃ\"")
                .replace("\"letterTiles\": [\"M\", \"A\", \"Ç\", \"Ã\", \"B\", \"O\"]",
                        "\"letterTiles\": [\"M\", \"A\", \"M\", \"Ã\"]");
        assertThat(validator.validate(repeatedLetters).issues())
                .noneMatch(issue -> issue.code().equals("word_cannot_be_built"));

        String impossible = repeatedLetters.replace("\"letterTiles\": [\"M\", \"A\", \"M\", \"Ã\"]",
                "\"letterTiles\": [\"M\", \"A\", \"Ã\"]");
        assertThat(validator.validate(impossible).issues())
                .anyMatch(issue -> issue.code().equals("word_cannot_be_built"));
    }

    @Test
    void blocksUnknownComponentsBrokenReferencesAndPunitiveLanguage() throws Exception {
        assertThat(validator.validate(example().replace("\"visualAssetId\": \"quadrinho_lanche\"",
                "\"visualAssetId\": \"imagem_inexistente\"")).issues())
                .anyMatch(issue -> issue.code().equals("asset_not_found"));
        assertThat(validator.validate(example().replace(
                "Meu lanche sumiu da cesta.", "Você errou. Meu lanche sumiu da cesta.")).issues())
                .anyMatch(issue -> issue.code().equals("punitive_error"));
        assertThat(validator.validate(example().replace("\"methodology\": \"LEIA\"",
                "\"methodology\": \"LEIA\", \"remoteAction\": \"run\"")).issues())
                .anyMatch(issue -> issue.code().equals("unknown_field"));
    }

    @Test
    void blocksAStoryWhoseDeclaredVariantsExceedTheTabletBudget() throws Exception {
        String oversized = example()
                .replace("\"bytes\": 184320", "\"bytes\": 8388608")
                .replace("\"bytes\": 245760", "\"bytes\": 8388608")
                .replace("\"bytes\": 122880", "\"bytes\": 8388608")
                .replace("\"bytes\": 176128", "\"bytes\": 8388608");

        assertThat(validator.validate(oversized).issues())
                .anyMatch(issue -> issue.code().equals("asset_total_too_large"));
    }

    @Test
    void blocksWordTilesThatWouldRequireScrollingOnASmallPhone() throws Exception {
        String tooMany = example().replace(
                "\"letterTiles\": [\"M\", \"A\", \"Ç\", \"Ã\", \"B\", \"O\"]",
                "\"letterTiles\": [\"M\", \"A\", \"Ç\", \"Ã\", \"B\", \"O\", \"P\", \"U\", \"X\"]");
        assertThat(validator.validate(tooMany).issues())
                .anyMatch(issue -> issue.path().contains("letterTiles"));
        String syllableTile = example().replace("\"M\", \"A\", \"Ç\"",
                "\"MA\", \"A\", \"Ç\"");
        assertThat(validator.validate(syllableTile).issues())
                .anyMatch(issue -> issue.path().contains("letterTiles"));
    }

    private String example() throws Exception {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path root = Files.exists(current.resolve("docs")) ? current : current.getParent();
        return Files.readString(root.resolve("docs/v2/contracts/example-apple-story-pack.json"));
    }
}
