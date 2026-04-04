package com.example.inventory_management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Friendly URL aliases for browser navigation. Canonical UI routes live under {@code /ui/...}
 * (see {@link UiController}). REST APIs use {@code /products}, {@code /cart}, {@code /orders}
 * and must not be shadowed by duplicate MVC mappings on those paths.
 */
@Controller
public class PageAliasController {

    /** Tutorial-style “home” → same page as {@code /} ({@code index.html}). */
    @GetMapping("/home")
    public String home() {
        return "redirect:/";
    }

    /** Optional alias for the public catalog ({@code products.html} via {@code /ui/products}). */
    @GetMapping("/catalog")
    public String catalogAlias() {
        return "redirect:/ui/products";
    }

    /** Cart UI lives at {@code /ui/cart} ({@code cart.html}); REST cart API stays at {@code /cart}. */
    @GetMapping("/shopping-cart")
    public String cartUiAlias() {
        return "redirect:/ui/cart";
    }

    /** Short alias → buyer order list ({@code buyer_orders.html} via {@code /ui/orders}). */
    @GetMapping("/my-orders")
    public String myOrdersAlias() {
        return "redirect:/ui/orders";
    }
}
