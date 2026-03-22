package com.example.inventory_management.controller;

import com.example.inventory_management.dto.cart.CartItemDTO;
import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.dto.product.ProductUpdateRequest;
import com.example.inventory_management.entity.OrderStatus;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.service.CartService;
import com.example.inventory_management.service.OrderService;
import com.example.inventory_management.service.ProductService;
import com.example.inventory_management.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/ui")
@RequiredArgsConstructor
public class UiController {

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String q, Model model) {
        var page = productService.searchProducts(q, PageRequest.of(0, 50));
        model.addAttribute("products", page.getContent());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("createForm", new ProductForm());
        return "products";
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public String createProduct(@Valid @ModelAttribute("createForm") ProductForm form,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        productService.createProduct(form.toCreateRequest(), authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Product created");
        return "redirect:/ui/products";
    }

    @PostMapping("/products/{id}/update")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public String updateProduct(@PathVariable long id,
                                @Valid @ModelAttribute ProductForm form,
                                Authentication authentication) {
        productService.updateProduct(id, form.toUpdateRequest(), authentication.getName());
        return "redirect:/ui/products";
    }

    @PostMapping("/products/{id}/delete")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public String deleteProduct(@PathVariable long id, Authentication authentication) {
        productService.deleteProduct(id, authentication.getName());
        return "redirect:/ui/products";
    }

    @GetMapping("/cart")
    @Transactional(readOnly = true)
    public String cart(Authentication authentication, Model model) {
        var cartItems = cartService.getCart(authentication.getName()).stream()
                .map(ci -> new CartItemDTO(
                        ci.getProduct().getId(),
                        ci.getProduct().getName(),
                        ci.getProduct().getPrice(),
                        ci.getQuantity()))
                .toList();
        model.addAttribute("cartItems", cartItems);
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam long productId, @RequestParam int quantity, Authentication authentication) {
        cartService.addToCart(authentication.getName(), productId, quantity);
        return "redirect:/ui/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam long productId, Authentication authentication) {
        cartService.removeFromCart(authentication.getName(), productId);
        return "redirect:/ui/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkout(Authentication authentication) {
        orderService.createOrderFromCart(authentication.getName());
        return "redirect:/ui/orders";
    }

    @GetMapping("/orders")
    public String orders(Authentication authentication, Model model) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        model.addAttribute("orders", orderService.listOrders(authentication.getName(), admin));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("admin", admin);
        return "orders";
    }

    @PostMapping("/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateOrderStatus(@PathVariable long id, @RequestParam OrderStatus status) {
        orderService.updateStatus(id, status);
        return "redirect:/ui/orders";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String admin(Model model) {
        model.addAttribute("users", userService.listUsers());
        model.addAttribute("roles", Role.values());
        return "admin";
    }

    @PostMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUserRole(@PathVariable long id, @RequestParam Role role) {
        userService.updateUserRole(id, role);
        return "redirect:/ui/admin";
    }

    @PostMapping("/admin/users/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable long id) {
        userService.deleteUser(id);
        return "redirect:/ui/admin";
    }

    public static class ProductForm {
        private String sku;
        private String name;
        private BigDecimal price;
        private Integer stockQuantity;
        private Long categoryId;

        public String getSku() {
            return sku;
        }

        public void setSku(String sku) {
            this.sku = sku;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public Integer getStockQuantity() {
            return stockQuantity;
        }

        public void setStockQuantity(Integer stockQuantity) {
            this.stockQuantity = stockQuantity;
        }

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        ProductCreateRequest toCreateRequest() {
            return new ProductCreateRequest(
                    sku,
                    name,
                    price,
                    stockQuantity == null ? 0 : stockQuantity,
                    categoryId
            );
        }

        ProductUpdateRequest toUpdateRequest() {
            return new ProductUpdateRequest(
                    name,
                    price,
                    stockQuantity == null ? 0 : stockQuantity,
                    categoryId
            );
        }
    }
}
