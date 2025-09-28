package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vip_requests")
public class VipRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RequestStatus status;

    @Column(name = "reason", length = 500)
    private String reason;

    // Enum định nghĩa trạng thái VIP request
    public static enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    // ==================== CONSTRUCTORS ====================

    public VipRequest() {}

    public VipRequest(Customer customer, Order order, RequestStatus status, String reason) {
        this.customer = customer;
        this.order = order;
        this.status = status;
        this.reason = reason;
        this.requestDate = LocalDateTime.now();
    }

    // ==================== GETTERS & SETTERS ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    // ==================== UTILITY METHODS ====================

    @Override
    public String toString() {
        return "VipRequest{" +
                "id=" + id +
                ", customer=" + (customer != null ? customer.getName() : "null") +
                ", order=" + (order != null ? order.getOrderNumber() : "null") +
                ", status=" + status +
                ", requestDate=" + requestDate +
                '}';
    }
}