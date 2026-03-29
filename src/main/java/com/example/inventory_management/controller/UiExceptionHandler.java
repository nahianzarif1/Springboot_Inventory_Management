package com.example.inventory_management.controller;

import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.InsufficientStockException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@ControllerAdvice(assignableTypes = {UiController.class, ViewController.class})
public class UiExceptionHandler {

    @ExceptionHandler({ConflictException.class, ResourceNotFoundException.class, InsufficientStockException.class})
    public String handleUiExceptions(Exception ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        String ref = request.getHeader("Referer");
        if (ref != null && !ref.isBlank()) {
            try {
                URI uri = URI.create(ref);
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    String q = uri.getQuery();
                    return "redirect:" + path + (q != null ? "?" + q : "");
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        return "redirect:/";
    }
}
