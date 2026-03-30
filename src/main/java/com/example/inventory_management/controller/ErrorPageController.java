package com.example.inventory_management.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Provides a friendly fallback for UI errors so users don't see the Whitelabel page.
 * REST APIs still return ProblemDetail via GlobalExceptionHandler.
 */
@Controller
public class ErrorPageController implements ErrorController {

    @RequestMapping("/error")
    public String handleError() {
        return "redirect:/";
    }
}
