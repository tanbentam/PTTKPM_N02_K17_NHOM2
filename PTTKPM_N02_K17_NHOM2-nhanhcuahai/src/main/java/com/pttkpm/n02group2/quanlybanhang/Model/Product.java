package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 100)
    private String category;

    @Column(name = "image_path")
    private String imagePath;
    
    // Nếu bạn muốn có imageUrl riêng biệt
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    // ==================== CONSTRUCTORS ====================

    public Product() {}

    public Product(String name, String description, Double price, Integer quantity, String category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    // ==================== LIFECYCLE HOOKS ====================
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }

    // ==================== GETTERS AND SETTERS ====================
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    // ==================== BUSINESS METHODS ====================

    public boolean isInStock() {
        return quantity != null && quantity > 0;
    }

    public boolean isLowStock() {
        return quantity != null && quantity > 0 && quantity <= 10;
    }

    public boolean isOutOfStock() {
        return quantity == null || quantity == 0;
    }
    
    public String getStockStatus() {
        if (isOutOfStock()) return "out-of-stock";
        if (isLowStock()) return "low-stock";
        return "in-stock";
    }
    
    public String getStockStatusText() {
        if (isOutOfStock()) return "Hết hàng";
        if (isLowStock()) return "Sắp hết";
        return "Còn hàng";
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", category='" + category + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", createdDate=" + createdDate +
                ", updatedDate=" + updatedDate +
                '}';
    }
}