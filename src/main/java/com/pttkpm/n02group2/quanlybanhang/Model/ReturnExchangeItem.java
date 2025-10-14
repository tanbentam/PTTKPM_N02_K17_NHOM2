package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "return_exchange_items")
public class ReturnExchangeItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "return_exchange_request_id", nullable = false)
    private ReturnExchangeRequest returnExchangeRequest;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    private String note;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReturnExchangeRequest getReturnExchangeRequest() {
        return returnExchangeRequest;
    }

    public void setReturnExchangeRequest(ReturnExchangeRequest returnExchangeRequest) {
        this.returnExchangeRequest = returnExchangeRequest;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}