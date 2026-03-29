package com.example.inventory_management.service.impl;

import com.example.inventory_management.dto.InventoryLogDTO;
import com.example.inventory_management.dto.ProductDTO;
import com.example.inventory_management.dto.product.ProductCreateRequest;
import com.example.inventory_management.dto.product.ProductUpdateRequest;
import com.example.inventory_management.entity.Category;
import com.example.inventory_management.entity.Product;
import com.example.inventory_management.entity.Role;
import com.example.inventory_management.entity.User;
import com.example.inventory_management.exception.ConflictException;
import com.example.inventory_management.exception.ResourceNotFoundException;
import com.example.inventory_management.repository.CategoryRepository;
import com.example.inventory_management.repository.InventoryLogRepository;
import com.example.inventory_management.repository.ProductRepository;
import com.example.inventory_management.repository.UserRepository;
import com.example.inventory_management.service.InventoryLogService;
import com.example.inventory_management.service.ProductService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    @Value("${app.upload-dir:uploads/products}")
    private String uploadDir;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final InventoryLogService inventoryLogService;
    private final InventoryLogRepository inventoryLogRepository;

    public ProductServiceImpl(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            InventoryLogService inventoryLogService,
            InventoryLogRepository inventoryLogRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.inventoryLogService = inventoryLogService;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    private static String descOrEmpty(String d) {
        return d == null ? "" : d.trim();
    }

    @Override
    @Transactional
    public ProductDTO createProduct(ProductCreateRequest request, String sellerUsername) {
        if (request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Product price cannot be negative");
        }
        if (request.stockQuantity() < 0) {
            throw new ConflictException("Product quantity cannot be negative");
        }
        if (productRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new ConflictException("Duplicate SKU");
        }

        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        if (productRepository.existsBySeller_IdAndNameIgnoreCase(seller.getId(), request.name().trim())) {
            throw new ConflictException("Duplicate product name for this seller");
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Product p = Product.builder()
                .sku(request.sku().trim())
                .name(request.name().trim())
                .description(descOrEmpty(request.description()))
                .price(request.price())
                .stockQuantity(request.stockQuantity())
                .category(category)
                .seller(seller)
                .build();

        try {
            p = productRepository.save(p);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Duplicate product");
        }

        inventoryLogService.record(p, 0, p.getStockQuantity(), seller);
        return toDto(p);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(long productId, ProductUpdateRequest request, String sellerUsername) {
        if (request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("Product price cannot be negative");
        }
        if (request.stockQuantity() < 0) {
            throw new ConflictException("Product quantity cannot be negative");
        }

        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        Product p = productRepository.findByIdAndSeller(productId, seller)
                .orElseThrow(() -> new ConflictException("Seller cannot edit another seller's product"));

        String newName = request.name().trim();
        if (productRepository.existsBySeller_IdAndNameIgnoreCaseAndIdNot(seller.getId(), newName, productId)) {
            throw new ConflictException("Duplicate product name for this seller");
        }

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        int oldStock = p.getStockQuantity();
        p.setName(newName);
        p.setDescription(descOrEmpty(request.description()));
        p.setPrice(request.price());
        p.setStockQuantity(request.stockQuantity());
        p.setCategory(category);

        inventoryLogService.record(p, oldStock, p.getStockQuantity(), seller);
        return toDto(p);
    }

    @Override
    @Transactional
    public void deleteProduct(long productId, String sellerUsername) {
        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        Product p = productRepository.findByIdAndSeller(productId, seller)
                .orElseThrow(() -> new ConflictException("Seller cannot delete another seller's product"));
        productRepository.delete(p);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO findProductById(long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return toDto(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String q, Long categoryId, Pageable pageable) {
        String term = (q == null || q.isBlank()) ? null : q.trim();
        return productRepository.search(term, categoryId, pageable).map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> listAllProducts() {
        return productRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public void deleteProductByAdmin(long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productRepository.delete(p);
    }

    @Override
    @Transactional
    public void adjustStockByAdmin(long productId, int newQuantity, String adminUsername) {
        if (newQuantity < 0) {
            throw new ConflictException("Product quantity cannot be negative");
        }
        User admin = userRepository.findByUsernameIgnoreCase(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        int old = p.getStockQuantity();
        p.setStockQuantity(newQuantity);
        inventoryLogService.record(p, old, newQuantity, admin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> lowStockForSeller(String sellerUsername, int threshold) {
        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        return productRepository.findBySellerAndStockQuantityLessThanEqual(seller, threshold).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void saveProductImage(long productId, MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw new ConflictException("Empty file");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ConflictException("Only image uploads are allowed");
        }
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        boolean admin = user.getRoles().contains(Role.ADMIN);
        if (!admin && (p.getSeller() == null || !p.getSeller().getId().equals(user.getId()))) {
            throw new ConflictException("Seller cannot edit another seller's product");
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String original = file.getOriginalFilename();
            String ext = "jpg";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase();
                if (ext.length() > 8) {
                    ext = "jpg";
                }
            }
            String filename = productId + "-" + UUID.randomUUID() + "." + ext;
            Path dest = dir.resolve(filename);
            file.transferTo(dest);
            p.setImageUrl("/uploads/products/" + filename);
        } catch (IOException e) {
            throw new ConflictException("Could not store image");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLogDTO> inventoryHistoryForSeller(String sellerUsername) {
        User seller = userRepository.findByUsernameIgnoreCase(sellerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        return inventoryLogRepository.findBySellerId(seller.getId()).stream().map(l -> new InventoryLogDTO(
                l.getId(),
                l.getProduct().getId(),
                l.getProduct().getName(),
                l.getOldQuantity(),
                l.getNewQuantity(),
                l.getChangedBy() != null ? l.getChangedBy().getUsername() : "-",
                l.getCreatedAt()
        )).toList();
    }

    private ProductDTO toDto(Product p) {
        Long categoryId = p.getCategory() != null ? p.getCategory().getId() : null;
        String categoryName = p.getCategory() != null ? p.getCategory().getName() : null;
        Long sellerId = p.getSeller() != null ? p.getSeller().getId() : null;
        String sellerUsername = p.getSeller() != null ? p.getSeller().getUsername() : null;

        return new ProductDTO(
                p.getId(),
                p.getSku(),
                p.getName(),
                p.getDescription() != null ? p.getDescription() : "",
                p.getImageUrl(),
                p.getPrice(),
                p.getStockQuantity(),
                categoryId,
                categoryName,
                sellerId,
                sellerUsername
        );
    }
}
