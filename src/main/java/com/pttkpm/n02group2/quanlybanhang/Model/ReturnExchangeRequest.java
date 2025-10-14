package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "return_exchange_requests")
public class ReturnExchangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order originalOrder;

    @ManyToOne
    @JoinColumn(name = "exchange_order_id")
    private Order exchangeOrder;

    @Enumerated(EnumType.STRING)
    private ReturnExchangeType type;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    private ReturnExchangeStatus status;

    @OneToMany(mappedBy = "returnExchangeRequest", cascade = CascadeType.ALL)
    private List<ReturnExchangeItem> items;

    @Column
    private LocalDateTime createdAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public enum ReturnExchangeType {
        RETURN,
        EXCHANGE
    }

    public enum ReturnExchangeStatus {
        PENDING,     // Chờ duyệt
        APPROVED,    // Đã duyệt
        REJECTED,    // Đã từ chối
        PROCESSING,  // Đang xử lý
        COMPLETED,   // Hoàn thành
        CANCELLED    // Đã hủy
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOriginalOrder() {
        return originalOrder;
    }

    public void setOriginalOrder(Order originalOrder) {
        this.originalOrder = originalOrder;
    }

    public Order getExchangeOrder() {
        return exchangeOrder;
    }

    public void setExchangeOrder(Order exchangeOrder) {
        this.exchangeOrder = exchangeOrder;
    }

    public ReturnExchangeType getType() {
        return type;
    }

    public void setType(ReturnExchangeType type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ReturnExchangeStatus getStatus() {
        return status;
    }

    public void setStatus(ReturnExchangeStatus status) {
        this.status = status;
    }

    public List<ReturnExchangeItem> getItems() {
        return items;
    }

    public void setItems(List<ReturnExchangeItem> items) {
        this.items = items;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}