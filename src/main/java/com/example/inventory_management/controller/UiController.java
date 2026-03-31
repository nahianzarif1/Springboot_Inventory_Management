package com.example.inventory_management.controller;

import com.example.inventory_management.dto.cart.CartItemDTO;
import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.dto.product.ProductUpdateRequest;
import com.example.inventory_management.dto.review.CreateProductReviewRequest;
import com.example.inventory_management.dto.user.AdminUserCreateRequest;
import com.example.inventory_management.dto.user.AdminUserUpdateRequest;
import com.example.inventory_management.entity.OrderStatus;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.repository.ProductReviewRepository;
import com.example.inventory_management.repository.CategoryRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.CartService;
import com.example.inventory_management.service.OrderService;
import com.example.inventory_management.service.ProductService;
import com.example.inventory_management.service.ProductReviewService;
import com.example.inventory_management.service.ReportService;
import com.example.inventory_management.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ui")
public class UiController {

    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;
    private final ReportService reportService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductReviewService productReviewService;
    private final ProductReviewRepository productReviewRepository;
    private final UserRepository userRepository;

    public UiController(
            ProductService productService,
            CartService cartService,
            OrderService orderService,
            UserService userService,
            ReportService reportService,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductReviewService productReviewService,
            ProductReviewRepository productReviewRepository,
            UserRepository userRepository) {
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.userService = userService;
        this.reportService = reportService;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productReviewService = productReviewService;
        this.productReviewRepository = productReviewRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String q,
                           @RequestParam(required = false) Long categoryId,
                           Model model) {
        var page = productService.searchProducts(q, categoryId, PageRequest.of(0, 100));
        model.addAttribute("products", page.getContent());
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("createForm", new ProductForm());
        return "products";
    }

    @GetMapping("/products/{id}/reviews")
    public String productReviews(@PathVariable long id, Authentication authentication, Model model) {
        var product = productRepository.findById(id)
                .orElseThrow(() -> new com.example.inventory_management.exception.ResourceNotFoundException("Product not found"));

        Double avg = productReviewRepository.averageRating(product.getId());
        long count = productReviewRepository.reviewCount(product.getId());
        model.addAttribute("summary", new ReviewSummary(avg != null ? avg : 0.0, count));

        var reviews = productReviewService.listForProduct(id);
        model.addAttribute("reviews", reviews);
        model.addAttribute("product", product);

        String me = authentication != null ? authentication.getName() : null;
        boolean admin = authentication != null && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        model.addAttribute("me", me);
        model.addAttribute("admin", admin);

        if (me != null) {
            var buyer = userRepository.findByUsernameIgnoreCase(me).orElse(null);
            if (buyer != null) {
                var my = reviews.stream()
                        .filter(r -> r.buyerUsername() != null && r.buyerUsername().equalsIgnoreCase(me))
                        .findFirst()
                        .orElse(null);
                model.addAttribute("myReview", my);
            }
        }

        return "product_reviews";
    }

    @PostMapping("/products/{id}/reviews")
    @PreAuthorize("hasRole('BUYER')")
    public String saveProductReview(@PathVariable long id,
                                    @RequestParam int rating,
                                    @RequestParam(required = false) String comment,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        productReviewService.createOrUpdate(id, authentication.getName(), new CreateProductReviewRequest(rating, comment));
        redirectAttributes.addFlashAttribute("message", "Review saved");
        return "redirect:/ui/products/" + id + "/reviews";
    }

    @PostMapping("/products/{productId}/reviews/{reviewId}/delete")
    public String deleteProductReview(@PathVariable long productId,
                                      @PathVariable long reviewId,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        boolean admin = authentication != null && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        productReviewService.delete(productId, reviewId, authentication.getName(), admin);
        redirectAttributes.addFlashAttribute("message", "Review deleted");
        return "redirect:/ui/products/" + productId + "/reviews";
    }

    public record ReviewSummary(double averageRating, long reviewCount) {}

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

    @PostMapping("/products/{id}/image")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public String uploadProductImage(@PathVariable long id,
                                     @RequestParam("file") MultipartFile file,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        productService.saveProductImage(id, file, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Image updated");
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
    @PreAuthorize("hasRole('BUYER')")
    public String addToCart(@RequestParam long productId, @RequestParam int quantity, Authentication authentication) {
        cartService.addToCart(authentication.getName(), productId, quantity);
        return "redirect:/ui/cart";
    }

    @PostMapping("/cart/update")
    @PreAuthorize("hasRole('BUYER')")
    public String updateCartQty(@RequestParam long productId, @RequestParam int quantity, Authentication authentication) {
        cartService.updateQuantity(authentication.getName(), productId, quantity);
        return "redirect:/ui/cart";
    }

    @PostMapping("/cart/remove")
    @PreAuthorize("hasRole('BUYER')")
    public String removeFromCart(@RequestParam long productId, Authentication authentication) {
        cartService.removeFromCart(authentication.getName(), productId);
        return "redirect:/ui/cart";
    }

    @PostMapping("/cart/checkout")
    @PreAuthorize("hasRole('BUYER')")
    public String checkout(Authentication authentication, RedirectAttributes redirectAttributes) {
        orderService.createOrderFromCart(authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Order placed");
        return "redirect:/ui/orders";
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('BUYER','ADMIN')")
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

    @PostMapping("/orders/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public String cancelOrder(@PathVariable long id, Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        orderService.cancelOrder(id, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Order canceled");
        return "redirect:/ui/orders";
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    @Transactional(readOnly = true)
    public String sellerDashboard(Authentication authentication, Model model) {
        String name = authentication.getName();
        model.addAttribute("lowStock", productService.lowStockForSeller(name, 10));
        model.addAttribute("history", productService.inventoryHistoryForSeller(name).stream().limit(40).toList());
        model.addAttribute("orders", orderService.listOrdersForSeller(name));
        model.addAttribute("statuses", OrderStatus.values());
        return "seller_dashboard";
    }

    @PostMapping("/seller/orders/{id}/status")
    @PreAuthorize("hasRole('SELLER')")
    public String sellerOrderStatus(@PathVariable long id, @RequestParam OrderStatus status, Authentication authentication) {
        orderService.updateStatusForSeller(id, status, authentication.getName());
        return "redirect:/ui/seller";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String admin(Model model) {
        model.addAttribute("users", userService.listUsers());
        model.addAttribute("roles", Role.values());
        model.addAttribute("createUser", new AdminUserForm());
        return "admin";
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminCreateUser(@Valid @ModelAttribute("createUser") AdminUserForm form,
                                  RedirectAttributes redirectAttributes) {
        Set<Role> roles = form.parseRoles();
        userService.createUser(new AdminUserCreateRequest(form.getUsername(), form.getEmail(), form.getPassword(), roles));
        redirectAttributes.addFlashAttribute("message", "User created");
        return "redirect:/ui/admin";
    }

    @PostMapping("/admin/users/{id}/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminUpdateUser(@PathVariable long id, @RequestParam(required = false) String username,
                                  @RequestParam(required = false) String email) {
        userService.updateUser(id, new AdminUserUpdateRequest(
                username != null && !username.isBlank() ? username.trim() : null,
                email != null && !email.isBlank() ? email.trim() : null));
        return "redirect:/ui/admin";
    }

    @PostMapping("/admin/users/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminEnableUser(@PathVariable long id, @RequestParam boolean enabled) {
        userService.setEnabled(id, enabled);
        return "redirect:/ui/admin";
    }

    @PostMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUserRole(@PathVariable long id, @RequestParam Role role) {
        userService.updateUserRole(id, role);
        return "redirect:/ui/admin";
    }

    @PostMapping("/admin/users/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUserRoles(@PathVariable long id, @RequestParam(value = "roles", required = false) String[] roles) {
        if (roles == null || roles.length == 0) {
            throw new com.example.inventory_management.exception.ConflictException("Select at least one role");
        }
        Set<Role> set = Arrays.stream(roles).map(Role::valueOf).collect(Collectors.toSet());
        userService.updateUserRoles(id, set);
        return "redirect:/ui/admin";
    }

    @PostMapping("/admin/users/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable long id) {
        userService.deleteUser(id);
        return "redirect:/ui/admin";
    }

    @GetMapping("/admin/products")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminProducts(Model model) {
        model.addAttribute("products", productService.listAllProducts());
        return "admin_products";
    }

    @PostMapping("/admin/products/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAdjustStock(@PathVariable long id, @RequestParam int stock,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        productService.adjustStockByAdmin(id, stock, authentication.getName());
        redirectAttributes.addFlashAttribute("message", "Stock updated");
        return "redirect:/ui/admin/products";
    }

    @PostMapping("/admin/products/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDeleteProduct(@PathVariable long id, RedirectAttributes redirectAttributes) {
        productService.deleteProductByAdmin(id);
        redirectAttributes.addFlashAttribute("message", "Product removed");
        return "redirect:/ui/admin/products";
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public String reports(Model model) {
        model.addAttribute("totalSales", reportService.totalSales());
        model.addAttribute("topProducts", reportService.topSellingProducts(10));
        model.addAttribute("sellerPerformance", reportService.sellerPerformance());
        return "reports";
    }

    public static class ProductForm {
        private String sku;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer stockQuantity;
        /** Request param as string so empty option maps to no category */
        private String categoryId;

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
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

        public String getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(String categoryId) {
            this.categoryId = categoryId;
        }

        private static Long parseCategoryId(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return Long.parseLong(raw.trim());
        }

        ProductCreateRequest toCreateRequest() {
            return new ProductCreateRequest(
                    sku,
                    name,
                    description,
                    price,
                    stockQuantity == null ? 0 : stockQuantity,
                    parseCategoryId(categoryId)
            );
        }

        ProductUpdateRequest toUpdateRequest() {
            return new ProductUpdateRequest(
                    name,
                    description,
                    price,
                    stockQuantity == null ? 0 : stockQuantity,
                    parseCategoryId(categoryId)
            );
        }
    }

    @Getter
    @Setter
    public static class AdminUserForm {
        @NotBlank @Size(min = 3, max = 60)
        private String username;
        @NotBlank @Email @Size(max = 120)
        private String email;
        @NotBlank @Size(min = 6, max = 100)
        private String password;
        /** Selected role checkboxes: BUYER, SELLER, ADMIN */
        private String[] roleChecks;

        Set<Role> parseRoles() {
            if (roleChecks == null || roleChecks.length == 0) {
                return new HashSet<>(Set.of(Role.BUYER));
            }
            return Arrays.stream(roleChecks).map(Role::valueOf).collect(Collectors.toSet());
        }
    }
}
