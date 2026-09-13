package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;
import br.gov.interpretaai.server.core.SpeechProvider;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleCloudSpeechProvider implements SpeechProvider {
    private final boolean enabled;

    public GoogleCloudSpeechProvider(@Value("${interpretaai.tts.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public byte[] synthesize(String text, Speaker speaker) {
        if (!enabled) return new byte[0];
        String voice = speaker == Speaker.DAVI_MALE
                ? "pt-BR-Chirp3-HD-Puck" : "pt-BR-Chirp3-HD-Aoede";
        try (TextToSpeechClient client = TextToSpeechClient.create()) {
            SynthesizeSpeechResponse response = client.synthesizeSpeech(
                    SynthesisInput.newBuilder().setText(text).build(),
                    VoiceSelectionParams.newBuilder().setLanguageCode("pt-BR").setName(voice).build(),
                    AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.OGG_OPUS).build());
            return response.getAudioContent().toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("Cloud TTS indisponível", error);
        }
    }
}
