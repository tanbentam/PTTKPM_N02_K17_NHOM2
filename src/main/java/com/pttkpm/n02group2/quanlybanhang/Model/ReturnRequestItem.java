package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;

@Entity
public class ReturnRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Đơn đổi trả liên kết
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    // Sản phẩm muốn nhận
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;

    private int unitPrice;

    // Constructors
    public ReturnRequestItem() {}

    public ReturnRequestItem(Order order, Product product, int quantity, int unitPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }
}