package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "return_request_item")
public class ReturnRequestItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Loại yêu cầu: "RETURN" (trả lại) hoặc "RECEIVE" (muốn nhận)
   @Column(name = "type", nullable = false, length = 20)
    private String type;


    // Đơn hàng liên kết
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Sản phẩm liên kết
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Số lượng sản phẩm
    @Column(nullable = false)
    private int quantity;

    // Đơn giá tại thời điểm yêu cầu (nên dùng kiểu double cho tiền)
    @Column(nullable = false)
    private double unitPrice;

    // Constructors
    public ReturnRequestItem() {}

    public ReturnRequestItem(Order order, Product product, int quantity, double unitPrice, String type) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.type = type;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
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

    public double getUnitPrice() {
        return unitPrice;
    }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }
}