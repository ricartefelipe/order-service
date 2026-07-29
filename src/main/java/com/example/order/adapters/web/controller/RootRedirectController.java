package com.example.order.adapters.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootRedirectController {

    @GetMapping("/")
    public String root() {
        return "redirect:/swagger-ui/index.html";
    }
}
