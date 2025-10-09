package com.pttkpm.n02group2.quanlybanhang.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

public class OrderRequest {
    private CustomerInfo customer;
    private List<OrderItemInfo> items;
    private Double totalAmount;
    private Double vipDiscountAmount;
    private Double finalAmount;
    private boolean isVipOrder;
    private boolean createVipRequest; // Để tạo yêu cầu VIP cho admin

    // Inner class cho thông tin khách hàng
    public static class CustomerInfo {
        private Long id;
        private String name;
        private String phone;
        private String email; // THÊM FIELD NÀY
        private String address;
        
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dateOfBirth;
        
        private String province;
        private String district;
        private String ward;
        private boolean isVip;
        private boolean isPendingVip;
        private boolean isVipCandidate; // Để đánh dấu có thể tạo yêu cầu VIP
        private int vipDiscountPercent;

        // Constructors
        public CustomerInfo() {}

        public CustomerInfo(Long id, String name, String phone) {
            this.id = id;
            this.name = name;
            this.phone = phone;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        // THÊM GETTER VÀ SETTER CHO EMAIL
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }

        public boolean isVip() { return isVip; }
        public void setVip(boolean vip) { this.isVip = vip; }

        public boolean isPendingVip() { return isPendingVip; }
        public void setPendingVip(boolean pendingVip) { this.isPendingVip = pendingVip; }

        public boolean isVipCandidate() { return isVipCandidate; }
        public void setVipCandidate(boolean vipCandidate) { this.isVipCandidate = vipCandidate; }

        public int getVipDiscountPercent() { return vipDiscountPercent; }
        public void setVipDiscountPercent(int vipDiscountPercent) { this.vipDiscountPercent = vipDiscountPercent; }
    }

    // Inner class cho thông tin sản phẩm trong đơn hàng
    public static class OrderItemInfo {
        private Long id; // Product ID
        private String name;
        private Double price;
        private Integer quantity;
        private Double total;
        private Integer stock; // Tồn kho hiện tại

        // Constructors
        public OrderItemInfo() {}

        public OrderItemInfo(Long id, String name, Double price, Integer quantity, Double total) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.total = total;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public Double getTotal() { return total; }
        public void setTotal(Double total) { this.total = total; }

        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }
    }

    private String paymentMethod;

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    // Constructors
    public OrderRequest() {}

    public OrderRequest(CustomerInfo customer, List<OrderItemInfo> items, Double totalAmount, 
                       Double vipDiscountAmount, Double finalAmount, boolean isVipOrder) {
        this.customer = customer;
        this.items = items;
        this.totalAmount = totalAmount;
        this.vipDiscountAmount = vipDiscountAmount;
        this.finalAmount = finalAmount;
        this.isVipOrder = isVipOrder;
        this.createVipRequest = false; // Mặc định không tạo yêu cầu VIP
    }

    // Getters and Setters
    public CustomerInfo getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerInfo customer) {
        this.customer = customer;
    }

    public List<OrderItemInfo> getItems() {
        return items;
    }

    public void setItems(List<OrderItemInfo> items) {
        this.items = items;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getVipDiscountAmount() {
        return vipDiscountAmount;
    }

    public void setVipDiscountAmount(Double vipDiscountAmount) {
        this.vipDiscountAmount = vipDiscountAmount;
    }

    public Double getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
    }

    public boolean isVipOrder() {
        return isVipOrder;
    }

    public void setVipOrder(boolean vipOrder) {
        this.isVipOrder = vipOrder;
    }

    public boolean isCreateVipRequest() {
        return createVipRequest;
    }

    public void setCreateVipRequest(boolean createVipRequest) {
        this.createVipRequest = createVipRequest;
    }

    // ==================== BUSINESS METHODS ====================

    // Kiểm tra đơn hàng có đủ điều kiện tạo yêu cầu VIP không
    public boolean isEligibleForVipRequest() {
        return this.totalAmount != null && this.totalAmount >= 2000000 && 
               this.customer != null && !this.customer.isVip() && !this.customer.isPendingVip();
    }

    // Tính tổng số lượng sản phẩm trong đơn hàng
    public int getTotalItemCount() {
        if (items == null) return 0;
        return items.stream().mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 0).sum();
    }

    // Kiểm tra tồn kho đủ không
    public boolean hasStockAvailable() {
        if (items == null) return true;
        return items.stream().allMatch(item -> 
            item.getStock() != null && item.getQuantity() != null && 
            item.getStock() >= item.getQuantity()
        );
    }

    // Lấy danh sách sản phẩm hết hàng
    public List<OrderItemInfo> getOutOfStockItems() {
        if (items == null) return List.of();
        return items.stream()
            .filter(item -> item.getStock() != null && item.getQuantity() != null && 
                           item.getStock() < item.getQuantity())
            .toList();
    }

    @Override
    public String toString() {
        return "OrderRequest{" +
                "customer=" + (customer != null ? customer.getName() : "null") +
                ", itemsCount=" + (items != null ? items.size() : 0) +
                ", totalAmount=" + totalAmount +
                ", vipDiscountAmount=" + vipDiscountAmount +
                ", finalAmount=" + finalAmount +
                ", isVipOrder=" + isVipOrder +
                ", createVipRequest=" + createVipRequest +
                '}';
    }
}