package com.dondeanime.backend.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class OpenApiDocsController {

    @GetMapping("/api/v1/docs")
    public RedirectView docs() {
        return new RedirectView("/swagger-ui/index.html?urls.primaryName=public-v1", false);
    }
}
