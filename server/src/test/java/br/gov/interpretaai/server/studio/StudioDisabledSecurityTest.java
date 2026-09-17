package br.gov.interpretaai.server.studio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "interpretaai.studio.enabled=false",
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:studio-disabled;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class StudioDisabledSecurityTest {
    @Autowired MockMvc mvc;

    @Test
    void doesNotExposeTheTeacherPageOrApiWhenStudioIsDisabled() throws Exception {
        mvc.perform(get("/studio/index.html")).andExpect(status().isForbidden());
        mvc.perform(get("/studio/api/me")).andExpect(status().isForbidden());
    }
}
