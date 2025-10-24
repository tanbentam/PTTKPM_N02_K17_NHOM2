package com.pttkpm.n02group2.quanlybanhang.Model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String name;

    @Column(unique = true)
    private String phone;

    // Địa chỉ chi tiết
    private String address;    // Số nhà, tên đường
    private String ward;       // Phường/Xã
    private String district;   // Quận/Huyện
    private String province;   // Tỉnh/Thành phố

    // Ngày sinh - THÊM NULLABLE = TRUE EXPLICITLY
    @Column(name = "date_of_birth", nullable = true)
    private LocalDate dateOfBirth;

    // Email (nếu cần)
    private String email;

    // Thông tin mua hàng cho VIP
@Column(name = "total_spent", columnDefinition = "DECIMAL(20,5) DEFAULT 0")
private Double totalSpent = 0.0; 



    @Column(name = "order_count", columnDefinition = "INT DEFAULT 0")
    private Integer orderCount = 0;

    @Column(name = "is_vip", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isVip = false;

    @Column(name = "vip_discount_percent", columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private Double vipDiscountPercent = 0.0;

    @Column(name = "pending_vip", columnDefinition = "BOOLEAN DEFAULT FALSE")  
    private Boolean pendingVip = false;

    @Column(name = "last_order_date")
    private LocalDateTime lastOrderDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Customer() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Customer(String name, String phone, String address, String ward, String district, String province, LocalDate dateOfBirth, String email) {
        this();
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.ward = ward;
        this.district = district;
        this.province = province;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    
    // ENHANCED SETTER VỚI LOGGING
    public void setDateOfBirth(LocalDate dateOfBirth) { 
        System.out.println("=== Customer.setDateOfBirth() ===");
        System.out.println("Setting dateOfBirth from: " + this.dateOfBirth + " to: " + dateOfBirth);
        this.dateOfBirth = dateOfBirth; 
        System.out.println("dateOfBirth after set: " + this.dateOfBirth);
        System.out.println("=== End setDateOfBirth() ===");
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }

    public Boolean getIsVip() { return isVip; }
    public void setIsVip(Boolean isVip) { this.isVip = isVip; }

    public Double getVipDiscountPercent() { return vipDiscountPercent; }
    public void setVipDiscountPercent(Double vipDiscountPercent) { this.vipDiscountPercent = vipDiscountPercent; }

    public Boolean getPendingVip() { return pendingVip; }
    public void setPendingVip(Boolean pendingVip) { this.pendingVip = pendingVip; }

    public LocalDateTime getLastOrderDate() { return lastOrderDate; }
    public void setLastOrderDate(LocalDateTime lastOrderDate) { this.lastOrderDate = lastOrderDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ==================== BUSINESS METHODS ====================

    // Thêm đơn hàng và cập nhật thông tin VIP
    public void addOrder(double orderAmount) {
        this.totalSpent = (this.totalSpent != null ? this.totalSpent : 0.0) + orderAmount;
        this.orderCount = (this.orderCount != null ? this.orderCount : 0) + 1;
        this.lastOrderDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        updateVipStatus();
    }
    
    // Cập nhật trạng thái VIP dựa trên tổng chi tiêu
    public void updateVipStatus() {
        double total = this.totalSpent != null ? this.totalSpent : 0.0;
        if (total >= 5000000) {
            this.isVip = true;
            this.vipDiscountPercent = 10.0;
        } else if (total >= 2000000) {
            this.isVip = true;
            this.vipDiscountPercent = 7.0;
        } else if (total >= 1000000) {
            this.isVip = true;
            this.vipDiscountPercent = 5.0;
        } else {
            this.isVip = false;
            this.vipDiscountPercent = 0.0;
        }
    }

    public boolean canGetVipDiscount() {
        return this.isVip != null && this.isVip && this.vipDiscountPercent != null && this.vipDiscountPercent > 0;
    }



    // Helper method để lấy địa chỉ đầy đủ
    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();
        if (address != null && !address.trim().isEmpty()) {
            fullAddress.append(address);
        }
        if (ward != null && !ward.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(ward);
        }
        if (district != null && !district.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(district);
        }
        if (province != null && !province.trim().isEmpty()) {
            if (fullAddress.length() > 0) fullAddress.append(", ");
            fullAddress.append(province);
        }
        return fullAddress.toString();
    }

    // Getter cho VIP đúng chuẩn Java Bean
    public boolean isVip() {
        return isVip != null && isVip;
    }
    public void setVip(Boolean vip) {
        this.isVip = vip;
    }

    public boolean isPendingVip() {
        return pendingVip != null && pendingVip;
    }

    @PrePersist
    protected void onCreate() {
        System.out.println("=== @PrePersist onCreate() ===");
        System.out.println("dateOfBirth before onCreate: " + this.dateOfBirth);
        
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalSpent == null) totalSpent = 0.0;
        if (orderCount == null) orderCount = 0;
        if (isVip == null) isVip = false;
        if (vipDiscountPercent == null) vipDiscountPercent = 0.0;
        if (pendingVip == null) pendingVip = false;
        
        // KHÔNG SET dateOfBirth = null nếu nó đã có giá trị
        System.out.println("dateOfBirth after onCreate: " + this.dateOfBirth);
        System.out.println("=== End onCreate() ===");
    }

    @PreUpdate
    protected void onUpdate() {
        System.out.println("=== @PreUpdate onUpdate() ===");
        System.out.println("dateOfBirth in onUpdate: " + this.dateOfBirth);
        updatedAt = LocalDateTime.now();
        System.out.println("=== End onUpdate() ===");
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", fullAddress='" + getFullAddress() + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", email=" + email +
                ", totalSpent=" + totalSpent +
                ", orderCount=" + orderCount +
                ", pendingVip=" + pendingVip +
                '}';
    }
}