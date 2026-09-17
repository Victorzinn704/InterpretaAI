package br.gov.interpretaai.server.studio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** A stable post-login landing route for the teacher's static Studio. */
@Controller
@ConditionalOnProperty(prefix = "interpretaai.studio", name = "enabled", havingValue = "true")
public class StudioPageController {
    @GetMapping({"/studio", "/studio/"})
    public String index() {
        return "redirect:/studio/index.html";
    }
}
