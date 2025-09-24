package com.pttkpm.n02group2.quanlybanhang.Model;

import java.util.List;

public class Order {
    private Long id;
    private Long customerId;
    private List<OrderItem> orderItems;
    private double totalAmount;
    private String paymentStatus;

    public Order() {
    }

    public Order(Long id, Long customerId, List<OrderItem> orderItems, double totalAmount, String paymentStatus) {
        this.id = id;
        this.customerId = customerId;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.paymentStatus = paymentStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }
}