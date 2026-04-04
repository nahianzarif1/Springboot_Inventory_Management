package com.example.inventory_management.controller;

import com.example.inventory_management.repository.OrderRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.service.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
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
    private final OrderRepository orderRepository;

    public ViewController(AuthService authService, ProductRepository productRepository, OrderRepository orderRepository) {
        this.authService = authService;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        model.addAttribute("lowStockProducts", productRepository.findLowStockGlobal(10, PageRequest.of(0, 8)));
        boolean roleAdmin = hasRole(authentication, "ADMIN");
        boolean roleSeller = hasRole(authentication, "SELLER");
        boolean roleBuyer = hasRole(authentication, "BUYER");
        model.addAttribute("roleAdmin", roleAdmin);
        model.addAttribute("roleSeller", roleSeller);
        model.addAttribute("roleBuyer", roleBuyer);
        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalOrders", orderRepository.count());
        if (authentication != null) {
            model.addAttribute("dashboardUser", authentication.getName());
        }
        return "index";
    }

    private static boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> ("ROLE_" + role).equals(a.getAuthority()));
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
