package com.example.inventory_management.controller;

import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.InsufficientStockException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@ControllerAdvice(assignableTypes = {UiController.class, ViewController.class})
public class UiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UiExceptionHandler.class);

    @ExceptionHandler({ConflictException.class, ResourceNotFoundException.class, InsufficientStockException.class})
    public String handleUiExceptions(Exception ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        String ctx = request.getContextPath();
        if (ctx == null) {
            ctx = "";
        }
        String reqUri = request.getRequestURI();
        if (reqUri != null) {
            if (reqUri.startsWith(ctx + "/ui/cart")) {
                return "redirect:" + ctx + "/ui/cart";
            }
            if (reqUri.startsWith(ctx + "/ui/admin/orders")) {
                return "redirect:" + ctx + "/ui/admin/orders";
            }
            if (reqUri.startsWith(ctx + "/ui/orders")) {
                return "redirect:" + ctx + "/ui/orders";
            }
        }
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
        return "redirect:" + ctx + "/";
    }

    /** Avoid bare 500 on checkout; log root cause for debugging. */
    @ExceptionHandler(Exception.class)
    public String handleUnexpectedOnCheckout(Exception ex, RedirectAttributes redirectAttributes, HttpServletRequest request) throws Exception {
        String ctx = request.getContextPath();
        if (ctx == null) {
            ctx = "";
        }
        String uri = request.getRequestURI();
        if (uri != null && uri.contains("/ui/cart/checkout")) {
            log.warn("Checkout failed", ex);
            redirectAttributes.addFlashAttribute("error", "Checkout could not complete. Please try again.");
            return "redirect:" + ctx + "/ui/cart";
        }
        throw ex;
    }
}
