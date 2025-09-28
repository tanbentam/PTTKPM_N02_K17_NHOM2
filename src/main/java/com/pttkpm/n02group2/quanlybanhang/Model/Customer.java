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

    @Column(name = "full_name", nullable = false)  // Map với cột 'full_name' trong database
    private String name;  // Java field vẫn là 'name' nhưng map với DB column 'full_name'

    @Column(unique = true)  // Bỏ nullable = false, chỉ giữ unique
    private String phone;

    // THÊM: Thông tin địa chỉ
    private String province; // Tỉnh/Thành phố
    private String district; // Quận/Huyện  
    private String ward;     // Phường/Xã
    private String address;  // Số nhà, tên đường

    // THÊM: Ngày sinh
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // THÊM: Thông tin mua hàng cho VIP
    @Column(name = "total_spent", columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private Double totalSpent = 0.0;

    @Column(name = "order_count", columnDefinition = "INT DEFAULT 0")
    private Integer orderCount = 0;

    @Column(name = "is_vip", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isVip = false;

    @Column(name = "vip_discount_percent", columnDefinition = "DECIMAL(5,2) DEFAULT 0")
    private Double vipDiscountPercent = 0.0;

    // THÊM: Trường cho POS Service
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

    public Customer(String name, String phone, String province, String district, String ward, String address, LocalDate dateOfBirth) {
        this();
        this.name = name;
        this.phone = phone;
        this.province = province;
        this.district = district;
        this.ward = ward;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

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

        // Tự động cập nhật trạng thái VIP
        updateVipStatus();
    }

    // Cập nhật trạng thái VIP dựa trên tổng chi tiêu
    public void updateVipStatus() {
        double total = this.totalSpent != null ? this.totalSpent : 0.0;

        if (total >= 5000000) { // VIP Diamond: >= 5 triệu
            this.isVip = true;
            this.vipDiscountPercent = 10.0;
        } else if (total >= 2000000) { // VIP Gold: >= 2 triệu
            this.isVip = true;
            this.vipDiscountPercent = 7.0;
        } else if (total >= 1000000) { // VIP Silver: >= 1 triệu
            this.isVip = true;
            this.vipDiscountPercent = 5.0;
        } else {
            this.isVip = false;
            this.vipDiscountPercent = 0.0;
        }
    }

    // Kiểm tra có thể nhận giảm giá VIP không
    public boolean canGetVipDiscount() {
        return this.isVip != null && this.isVip && this.vipDiscountPercent != null && this.vipDiscountPercent > 0;
    }

    // Lấy cấp độ VIP
    public String getVipLevel() {
        if (!canGetVipDiscount()) {
            return "Regular";
        }

        double discount = this.vipDiscountPercent != null ? this.vipDiscountPercent : 0.0;
        if (discount >= 10.0) {
            return "Diamond";
        } else if (discount >= 7.0) {
            return "Gold";
        } else if (discount >= 5.0) {
            return "Silver";
        }
        return "Regular";
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

    // Getter cho VIP đúng chuẩn Java Bean (để dùng với isVip() trong Service)
    public boolean isVip() {
        return isVip != null && isVip;
    }

    public void setVip(Boolean vip) {
        this.isVip = vip;
    }

    // Getter cho pendingVip đúng chuẩn Java Bean
    public boolean isPendingVip() {
        return pendingVip != null && pendingVip;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Đảm bảo các trường số có giá trị mặc định
        if (totalSpent == null) totalSpent = 0.0;
        if (orderCount == null) orderCount = 0;
        if (isVip == null) isVip = false;
        if (vipDiscountPercent == null) vipDiscountPercent = 0.0;
        if (pendingVip == null) pendingVip = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", fullAddress='" + getFullAddress() + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", totalSpent=" + totalSpent +
                ", orderCount=" + orderCount +
                ", vipLevel='" + getVipLevel() + '\'' +
                ", pendingVip=" + pendingVip +
                '}';
    }
}