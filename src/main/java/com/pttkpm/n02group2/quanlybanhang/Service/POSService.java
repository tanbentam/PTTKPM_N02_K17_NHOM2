package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Controller.POSController;
import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class POSService {
private ProductService productService;
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private VipRequestRepository vipRequestRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    // Cache để theo dõi tồn kho real-time
    private final Map<Long, Integer> inventoryCache = new ConcurrentHashMap<>();

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> processOrder(OrderRequest orderRequest) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            System.out.println("=== BẮT ĐẦU XỬ LÝ ĐỚN HÀNG POS ===");
            System.out.println("Khách hàng: " + orderRequest.getCustomer().getName());
            System.out.println("Tổng tiền: " + orderRequest.getTotalAmount());
            System.out.println("Yêu cầu VIP: " + orderRequest.isCreateVipRequest());
            
            // 1. Validate stock trước khi xử lý
            validateAndReserveStock(orderRequest.getItems());
            
            // 2. Xử lý khách hàng (tạo mới hoặc cập nhật)
            Customer customer = handleCustomer(orderRequest.getCustomer(), orderRequest.isCreateVipRequest());
            
            // 3. Kiểm tra và áp dụng VIP discount nếu khách hàng đã là VIP
            if (customer.isVip() && customer.getVipDiscountPercent() > 0) {
                double discountAmount = orderRequest.getTotalAmount() * (customer.getVipDiscountPercent() / 100);
                orderRequest.setVipDiscountAmount(discountAmount);
                orderRequest.setFinalAmount(orderRequest.getTotalAmount() - discountAmount);
                orderRequest.setVipOrder(true);
                System.out.println("Áp dụng VIP discount: " + customer.getVipDiscountPercent() + "% = " + 
                                 String.format("%,.0f", discountAmount) + " VND");
            }
            
            // 4. Tạo đơn hàng
            Order order = createOrder(orderRequest, customer);
            
            // 5. CẬP NHẬT TỒN KHO TRONG DATABASE (QUAN TRỌNG)
            updateProductStockInDatabase(orderRequest.getItems());
            
            // 6. Tạo VIP request PENDING nếu có yêu cầu
            boolean vipRequestCreated = false;
            if (orderRequest.isCreateVipRequest() && !customer.isVip() && !customer.isPendingVip()) {
                vipRequestCreated = createVipRequestPending(customer, order, orderRequest.getTotalAmount());
                if (vipRequestCreated) {
                    // Cập nhật trạng thái pending cho customer
                    customer.setPendingVip(true);
                    customerRepository.save(customer);
                }
            }
            
            // 7. Cập nhật thống kê khách hàng
            updateCustomerStats(customer, orderRequest.getFinalAmount());
            
            // 8. Force flush để đảm bảo dữ liệu được lưu
            customerRepository.flush();
            orderRepository.flush();
            productRepository.flush();
            
            result.put("success", true);
            result.put("orderId", order.getId());
            result.put("orderNumber", order.getOrderNumber());
            result.put("isVipOrder", orderRequest.isVipOrder());
            result.put("vipRequestCreated", vipRequestCreated);
            result.put("vipDiscount", orderRequest.getVipDiscountAmount());
            result.put("finalAmount", orderRequest.getFinalAmount());
            result.put("message", "Đơn hàng đã được tạo và lưu thành công");
            
            System.out.println("✅ ĐƠN HÀNG HOÀN THÀNH - ID: " + order.getId());
            System.out.println("✅ VIP Request tạo: " + vipRequestCreated);
            
        } catch (Exception e) {
            System.err.println("❌ LỖI XỬ LÝ ĐỚN HÀNG: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("message", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        
        return result;
    }

    private void validateAndReserveStock(List<OrderRequest.OrderItemInfo> items) {
        System.out.println("--- KIỂM TRA TỒN KHO ---");
        
        for (OrderRequest.OrderItemInfo item : items) {
            Product product = productRepository.findById(item.getId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: ID " + item.getId()));
            
            System.out.println("Kiểm tra " + product.getName() + ": Tồn kho=" + product.getQuantity() + 
                             ", Cần=" + item.getQuantity());
            
            if (product.getQuantity() < item.getQuantity()) {
                throw new RuntimeException(
                    String.format("❌ KHÔNG ĐỦ HÀNG!\nSản phẩm: %s\nTồn kho: %d\nYêu cầu: %d", 
                                product.getName(), product.getQuantity(), item.getQuantity())
                );
            }
        }
        
        System.out.println("✅ Tồn kho đủ cho tất cả sản phẩm");
    }

    private Customer handleCustomer(OrderRequest.CustomerInfo customerInfo, boolean isVipCandidate) {
        Customer customer;
        
        System.out.println("--- XỬ LÝ KHÁCH HÀNG ---");
        System.out.println("VIP Candidate: " + isVipCandidate);
        
        if (customerInfo.getId() == null) {
            // Kiểm tra khách hàng đã tồn tại qua SĐT
            if (customerInfo.getPhone() != null && !customerInfo.getPhone().trim().isEmpty()) {
                Optional<Customer> existingOpt = customerRepository.findByPhone(customerInfo.getPhone());
                if (existingOpt.isPresent()) {
                    customer = existingOpt.get();
                    System.out.println("Tìm thấy khách hàng: " + customer.getName() + 
                                     " (VIP: " + customer.isVip() + ", Pending: " + customer.isPendingVip() + ")");
                    return customer;
                }
            }
            
            // Tạo khách hàng mới
            customer = new Customer();
            customer.setName(customerInfo.getName() != null ? customerInfo.getName() : "Khách lẻ");
            customer.setPhone(customerInfo.getPhone());
            customer.setAddress(customerInfo.getAddress());
            customer.setDateOfBirth(customerInfo.getDateOfBirth());
            customer.setProvince(customerInfo.getProvince());
            customer.setDistrict(customerInfo.getDistrict());
            customer.setWard(customerInfo.getWard());
            customer.setVip(false);
            customer.setPendingVip(false);  // Sẽ set true sau khi tạo VIP request
            customer.setVipDiscountPercent(0.0);
            customer.setCreatedAt(LocalDateTime.now());
            customer.setTotalSpent(0.0);
            customer.setOrderCount(0);
            
            customer = customerRepository.save(customer);
            System.out.println("✅ Tạo khách hàng mới: " + customer.getName() + " (ID: " + customer.getId() + ")");
        } else {
            // Lấy khách hàng có sẵn
            customer = customerRepository.findById(customerInfo.getId())
                .orElseThrow(() -> new RuntimeException("Khách hàng không tồn tại với ID: " + customerInfo.getId()));
            
            System.out.println("✅ Sử dụng khách hàng: " + customer.getName() + 
                             " (VIP: " + customer.isVip() + ", Pending: " + customer.isPendingVip() + ")");
        }
        
        return customer;
    }
public void updateInventory(List<POSController.CartItem> cartItems) {
        for (POSController.CartItem item : cartItems) {
            try {
                Optional<Product> productOpt = productService.getProductById(item.getProductId());
                if (productOpt.isPresent()) {
                    Product product = productOpt.get();
                    int newQuantity = product.getQuantity() - item.getQuantity();
                    if (newQuantity < 0) {
                        throw new RuntimeException("Không đủ tồn kho cho sản phẩm: " + product.getName());
                    }
                    product.setQuantity(newQuantity);
                    // Giả sử ProductService có method updateProduct(Long, Product)
                    productService.updateProduct(product.getId(), product);
                } else {
                    throw new RuntimeException("Sản phẩm không tồn tại: " + item.getProductId());
                }
            } catch (Exception e) {
                System.err.println("Lỗi cập nhật tồn kho cho sản phẩm ID " + item.getProductId() + ": " + e.getMessage());
                throw e;
            }
        }
    }

    private Order createOrder(OrderRequest orderRequest, Customer customer) {
        System.out.println("--- TẠO ĐỚN HÀNG ---");
        
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderNumber(generateOrderNumber());
        order.setOrderDate(new Date());
        order.setTotalAmount(orderRequest.getTotalAmount());
        order.setVipDiscountAmount(orderRequest.getVipDiscountAmount());
        order.setFinalAmount(orderRequest.getFinalAmount());
        order.setVipOrder(orderRequest.isVipOrder());
        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setCreatedAt(LocalDateTime.now());
        
        // Lưu order trước
        order = orderRepository.save(order);
        
        // Tạo order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderRequest.OrderItemInfo itemInfo : orderRequest.getItems()) {
            Product product = productRepository.findById(itemInfo.getId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + itemInfo.getId()));
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemInfo.getQuantity());
            orderItem.setUnitPrice(itemInfo.getPrice());
            orderItem.setTotalPrice(itemInfo.getTotal());
            
            orderItems.add(orderItem);
        }
        
        // Lưu tất cả order items
        orderItemRepository.saveAll(orderItems);
        order.setItems(orderItems);
        
        System.out.println("✅ Tạo đơn hàng: " + order.getOrderNumber() + 
                         " - Tổng: " + String.format("%,.0f", order.getFinalAmount()) + " VND");
        
        return order;
    }

    private void updateProductStockInDatabase(List<OrderRequest.OrderItemInfo> items) {
        System.out.println("--- CẬP NHẬT TỒN KHO TRONG DATABASE ---");
        
        for (OrderRequest.OrderItemInfo item : items) {
            // Lấy lại sản phẩm từ DB để đảm bảo dữ liệu mới nhất
            Product product = productRepository.findById(item.getId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + item.getId()));
            
            int currentQuantity = product.getQuantity();
            int newQuantity = currentQuantity - item.getQuantity();
            
            if (newQuantity < 0) {
                throw new RuntimeException("Tồn kho không đủ cho sản phẩm: " + product.getName() +
                    ". Hiện tại: " + currentQuantity + ", cần: " + item.getQuantity());
            }
            
            product.setQuantity(newQuantity);
            product.setUpdatedDate(LocalDateTime.now());
            
            // Lưu ngay vào database
            productRepository.save(product);
            
            System.out.println("✅ Cập nhật " + product.getName() + ": " + 
                             currentQuantity + " -> " + newQuantity);
        }
        
        // Force flush để đảm bảo dữ liệu được commit
        productRepository.flush();
        System.out.println("✅ ĐÃ LƯU TỒN KHO VÀO DATABASE");
    }

    private boolean createVipRequestPending(Customer customer, Order order, Double orderAmount) {
        try {
            System.out.println("--- TẠO YÊU CẦU VIP CHỜ DUYỆT ---");
            System.out.println("Customer: " + customer.getName());
            System.out.println("Order Amount: " + String.format("%,.0f", orderAmount) + " VND");
            
            // Kiểm tra điều kiện: đơn hàng >= 2 triệu và chưa là VIP
            if (orderAmount >= 2000000 && !customer.isVip() && !customer.isPendingVip()) {
                
                // Kiểm tra chưa có VIP request PENDING
                boolean hasPendingRequest = vipRequestRepository.existsByCustomerIdAndStatus(
                    customer.getId(), VipRequest.RequestStatus.PENDING);
                
                if (!hasPendingRequest) {
                    VipRequest vipRequest = new VipRequest();
                    vipRequest.setCustomer(customer);
                    vipRequest.setOrder(order);
                    vipRequest.setRequestDate(LocalDateTime.now());
                    vipRequest.setStatus(VipRequest.RequestStatus.PENDING);  // CHỜ ADMIN DUYỆT
                    vipRequest.setReason("Yêu cầu VIP từ đơn hàng POS >= 2,000,000 VND - " +
                                       "Đơn hàng: " + order.getOrderNumber() + 
                                       " - Tổng tiền: " + String.format("%,.0f", orderAmount) + " VND");
                    
                    vipRequestRepository.save(vipRequest);
                    vipRequestRepository.flush();  // Đảm bảo lưu ngay
                    
                    System.out.println("✅ Đã tạo VIP request CHỜ DUYỆT cho khách hàng: " + customer.getName());
                    return true;
                } else {
                    System.out.println("⚠️ VIP request đã tồn tại cho khách hàng: " + customer.getName());
                    return false;
                }
            } else {
                System.out.println("❌ Không đủ điều kiện VIP - Amount: " + String.format("%,.0f", orderAmount) + 
                                 ", IsVIP: " + customer.isVip() + ", PendingVIP: " + customer.isPendingVip());
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi tạo VIP request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void updateCustomerStats(Customer customer, Double orderAmount) {
        try {
            System.out.println("--- CẬP NHẬT THỐNG KÊ KHÁCH HÀNG ---");
            
            int currentOrderCount = customer.getOrderCount() != null ? customer.getOrderCount() : 0;
            double currentTotalSpent = customer.getTotalSpent() != null ? customer.getTotalSpent() : 0.0;
            
            customer.setOrderCount(currentOrderCount + 1);
            customer.setTotalSpent(currentTotalSpent + orderAmount);
            customer.setLastOrderDate(LocalDateTime.now());
            customer.setUpdatedAt(LocalDateTime.now());
            
            customerRepository.save(customer);
            
            System.out.println("✅ Cập nhật khách hàng " + customer.getName() + ":");
            System.out.println("  - Số đơn hàng: " + customer.getOrderCount());
            System.out.println("  - Tổng chi tiêu: " + String.format("%,.0f", customer.getTotalSpent()) + " VND");
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi cập nhật thống kê: " + e.getMessage());
        }
    }

    private String generateOrderNumber() {
        String prefix = "POS";
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(6);
        return prefix + timestamp;
    }

    // ==================== ADDITIONAL UTILITY METHODS ====================

    public boolean canCreateVipRequest(Long customerId) {
        try {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            if (customer == null || customer.isVip() || customer.isPendingVip()) {
                return false;
            }
            
            boolean hasPendingRequest = vipRequestRepository.existsByCustomerIdAndStatus(
                customerId, VipRequest.RequestStatus.PENDING);
            
            return !hasPendingRequest;
            
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> getOrderById(Long orderId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));
            
            result.put("success", true);
            result.put("order", order);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }

    public List<Order> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public Map<String, Object> getDailySalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatus(
                startDate, endDate, Order.OrderStatus.COMPLETED);
            
            double totalRevenue = orders.stream()
                .mapToDouble(Order::getFinalAmount)
                .sum();
            
            int totalOrders = orders.size();
            
            result.put("success", true);
            result.put("totalRevenue", totalRevenue);
            result.put("totalOrders", totalOrders);
            result.put("orders", orders);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        
        return result;
    }
}