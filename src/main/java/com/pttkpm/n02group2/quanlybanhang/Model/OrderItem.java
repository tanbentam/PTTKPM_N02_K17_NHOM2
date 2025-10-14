package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double unitPrice; // Giá tại thời điểm mua

    @Column(nullable = false)
    private Double totalPrice; // quantity * unitPrice

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    // ==================== CONSTRUCTORS ====================

    public OrderItem() {}

    public OrderItem(Order order, Product product, Integer quantity, Double unitPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = quantity * unitPrice;
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
    }

    // Constructor cho JSON mapping (không có relationship objects)
    public OrderItem(Long productId, String productName, Integer quantity, Double unitPrice, Double totalPrice) {
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
        this.createdDate = LocalDateTime.now();
        this.updatedDate = LocalDateTime.now();
        
        // Tạo product object tạm thời với ID
        this.product = new Product();
        this.product.setId(productId);
        this.product.setName(productName);
    }

    // ==================== LIFECYCLE HOOKS ====================
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
        if (totalPrice == null && quantity != null && unitPrice != null) {
            totalPrice = quantity * unitPrice;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
        if (totalPrice == null && quantity != null && unitPrice != null) {
            totalPrice = quantity * unitPrice;
        }
    }

    // ==================== BUSINESS METHODS ====================

    public void updateTotalPrice() {
        if (quantity != null && unitPrice != null) {
            this.totalPrice = quantity * unitPrice;
        }
    }

    public boolean isValid() {
        return order != null && product != null && quantity != null && 
               quantity > 0 && unitPrice != null && unitPrice > 0;
    }

    // Method để lấy productId - cần thiết cho OrderService
    public Long getProductId() {
        return product != null ? product.getId() : null;
    }

    // Method để lấy tên sản phẩm - tiện lợi cho frontend
    public String getName() {
        return product != null ? product.getName() : null;
    }

    // Method để lấy giá - alias cho unitPrice
    public Double getPrice() {
        return unitPrice;
    }

    public void setPrice(Double price) {
        this.unitPrice = price;
        updateTotalPrice();
    }

    // Method để lấy tổng tiền - alias cho totalPrice
    public Double getTotal() {
        return totalPrice;
    }

    public void setTotal(Double total) {
        this.totalPrice = total;
    }

    // ==================== GETTERS AND SETTERS ====================
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { 
        this.quantity = quantity;
        updateTotalPrice();
    }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { 
        this.unitPrice = unitPrice;
        updateTotalPrice();
    }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }

    public LocalDateTime getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(LocalDateTime updatedDate) { this.updatedDate = updatedDate; }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", product=" + (product != null ? product.getName() : "null") +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                ", totalPrice=" + totalPrice +
                '}';
    }
}