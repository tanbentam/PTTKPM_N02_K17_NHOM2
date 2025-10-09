package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "promotion_products")
public class PromotionProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;
    
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false)
    private double discountPercent;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Promotion getPromotion() {
        return promotion;
    }
    
    public void setPromotion(Promotion promotion) {
        this.promotion = promotion;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public double getDiscountPercent() {
        return discountPercent;
    }
    
    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }
}