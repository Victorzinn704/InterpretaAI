package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;
import br.gov.interpretaai.server.core.SpeechProvider;
import br.gov.interpretaai.server.core.SpeechProvider.SpeechAudio;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "interpretaai.speech.provider", havingValue = "google")
public class GoogleCloudSpeechProvider implements SpeechProvider {
    @Override
    public SpeechAudio synthesize(String text, Speaker speaker) {
        String voice = speaker == Speaker.DAVI_MALE
                ? "pt-BR-Chirp3-HD-Puck" : "pt-BR-Chirp3-HD-Aoede";
        try (TextToSpeechClient client = TextToSpeechClient.create()) {
            SynthesizeSpeechResponse response = client.synthesizeSpeech(
                    SynthesisInput.newBuilder().setText(text).build(),
                    VoiceSelectionParams.newBuilder().setLanguageCode("pt-BR").setName(voice).build(),
                    AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.OGG_OPUS).build());
            return new SpeechAudio(response.getAudioContent().toByteArray(), "audio/ogg; codecs=opus");
        } catch (IOException error) {
            throw new IllegalStateException("Cloud TTS indisponível", error);
        }
    }
}
