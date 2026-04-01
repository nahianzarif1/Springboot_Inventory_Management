package com.example.inventory_management.controller;

import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.service.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
public class ViewController {

    private final AuthService authService;
    private final ProductRepository productRepository;

    public ViewController(AuthService authService, ProductRepository productRepository) {
        this.authService = authService;
        this.productRepository = productRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("lowStockProducts", productRepository.findLowStockGlobal(10, PageRequest.of(0, 8)));
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(@Valid @ModelAttribute("form") RegisterForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        authService.registerBuyer(form.getUsername().trim(), form.getEmail().trim(), form.getPassword());
        redirectAttributes.addFlashAttribute("message", "Account created. Please sign in.");
        return "redirect:/login";
    }

    @Getter
    @Setter
    public static class RegisterForm {
        @NotBlank @Size(min = 3, max = 60)
        private String username;
        @NotBlank @Email @Size(max = 120)
        private String email;
        @NotBlank @Size(min = 6, max = 100)
        private String password;
    }
}
