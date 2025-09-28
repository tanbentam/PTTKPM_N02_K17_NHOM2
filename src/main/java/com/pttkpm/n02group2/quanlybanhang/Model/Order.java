package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
    
    @Column(name = "order_date", nullable = false)
    private Date orderDate;
    
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;
    
    @Column(name = "vip_discount_amount")
    private Double vipDiscountAmount = 0.0;
    
    @Column(name = "final_amount", nullable = false)
    private Double finalAmount;
    
    @Column(name = "vip_order", nullable = false)
    private Boolean vipOrder = false;
    
    @Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false, length = 20)
private OrderStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;
    
    // Enum cho trạng thái đơn hàng
    public enum OrderStatus {
        PENDING,    // Đang chờ xử lý
        COMPLETED,  // Đã hoàn thành
        CANCELLED   // Đã hủy
    }
    
    // Constructors
    public Order() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    
    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
    
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    
    public Double getVipDiscountAmount() { return vipDiscountAmount; }
    public void setVipDiscountAmount(Double vipDiscountAmount) { this.vipDiscountAmount = vipDiscountAmount; }
    
    public Double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(Double finalAmount) { this.finalAmount = finalAmount; }
    
    public Boolean getVipOrder() { return vipOrder; }
    public void setVipOrder(Boolean vipOrder) { this.vipOrder = vipOrder; }
    
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", orderNumber='" + orderNumber + '\'' +
                ", customer=" + (customer != null ? customer.getName() : "null") +
                ", totalAmount=" + totalAmount +
                ", finalAmount=" + finalAmount +
                ", status=" + status +
                '}';
    }
}