package com.example.warningsystem;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping(value = {"/", "/login", "/strategy", "/console"})
    public String index() {
        return "forward:/index.html";
    }
}
