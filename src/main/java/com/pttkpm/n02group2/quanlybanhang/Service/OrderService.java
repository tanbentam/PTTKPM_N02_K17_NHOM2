package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CustomerService customerService;

    // Cache để theo dõi tồn kho real-time
    private final Map<Long, Integer> inventoryCache = new ConcurrentHashMap<>();

    // ==================== MAIN CREATE ORDER METHOD ====================
    
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(Customer customer, List<OrderItem> orderItems) {
        try {
            System.out.println("=== BẮT ĐẦU TẠO ĐỚN HÀNG ===");
            System.out.println("Khách hàng: " + customer.getName());
            System.out.println("Số lượng items: " + orderItems.size());
            
            // 1. Kiểm tra và lock tồn kho với synchronization
            synchronized (this) {
                validateAndLockInventory(orderItems);
            }

            // 2. Tạo Order
            Order order = new Order();
            order.setCustomer(customer);
            order.setOrderDate(new Date());
            order.setStatus(Order.OrderStatus.PENDING);
            order.setOrderNumber(generateOrderNumber());

            // 3. Tính toán chi tiết
            double totalAmount = calculateOrderTotal(orderItems, order);
            
            // 4. Áp dụng giảm giá VIP
            applyVipDiscount(customer, order, totalAmount);

            // 5. Set items và lưu order
            order.setItems(orderItems);
            Order savedOrder = orderRepository.save(order);
            System.out.println("✅ Đã lưu order ID: " + savedOrder.getId());

            // 6. Thực thi trừ tồn kho
            executeInventoryReduction(orderItems);
            
            // 7. Cập nhật thông tin khách hàng VIP
            updateCustomerStats(customer, savedOrder.getFinalAmount());

            System.out.println("=== ĐƠN HÀNG HOÀN THÀNH ===");
            System.out.println("Mã đơn: " + savedOrder.getOrderNumber());
            System.out.println("Tổng tiền: " + savedOrder.getFinalAmount() + " VND");
            
            return savedOrder;

        } catch (Exception e) {
            System.err.println("❌ LỖI TẠO ĐƠN HÀNG: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể tạo đơn hàng: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================
    
    private void validateAndLockInventory(List<OrderItem> orderItems) {
        System.out.println("--- KIỂM TRA TỒN KHO ---");
        
        // Group by product để tính tổng quantity cần
        Map<Long, Integer> totalNeeded = new HashMap<>();
        for (OrderItem item : orderItems) {
            Long productId = item.getProduct().getId();
            totalNeeded.merge(productId, item.getQuantity(), Integer::sum);
        }
        
        // Kiểm tra từng product
        for (Map.Entry<Long, Integer> entry : totalNeeded.entrySet()) {
            Long productId = entry.getKey();
            Integer quantityNeeded = entry.getValue();
            
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + productId));
            
            int currentStock = product.getQuantity();
            
            System.out.println("Sản phẩm: " + product.getName());
            System.out.println("  - Tồn kho: " + currentStock);
            System.out.println("  - Cần bán: " + quantityNeeded);
            
            if (currentStock < quantityNeeded) {
                throw new RuntimeException(
                    String.format("❌ KHÔNG ĐỦ HÀNG!\nSản phẩm: %s\nTồn kho: %d\nYêu cầu: %d", 
                                product.getName(), currentStock, quantityNeeded)
                );
            }
            
            // Update product reference trong items
            for (OrderItem item : orderItems) {
                if (item.getProduct().getId().equals(productId)) {
                    item.setProduct(product);
                }
            }
        }
        
        System.out.println("✅ Tồn kho đủ cho tất cả sản phẩm");
    }
    
    private double calculateOrderTotal(List<OrderItem> orderItems, Order order) {
        double totalAmount = 0;
        System.out.println("--- TÍNH TỔNG TIỀN ---");
        
        for (OrderItem item : orderItems) {
            double itemTotal = item.getQuantity() * item.getUnitPrice();
            item.setTotalPrice(itemTotal);
            item.setOrder(order);
            totalAmount += itemTotal;
            
            System.out.println(item.getProduct().getName() + 
                             " x" + item.getQuantity() + 
                             " = " + String.format("%,.0f", itemTotal) + " VND");
        }
        
        order.setTotalAmount(totalAmount);
        System.out.println("Tổng cộng: " + String.format("%,.0f", totalAmount) + " VND");
        return totalAmount;
    }
    
    private void applyVipDiscount(Customer customer, Order order, double totalAmount) {
        double vipDiscountAmount = 0;
        
        if (customer.isVip() && customer.getVipDiscountPercent() > 0) {
            vipDiscountAmount = totalAmount * (customer.getVipDiscountPercent() / 100);
            order.setVipDiscountAmount(vipDiscountAmount);
            order.setVipOrder(true);
            
            System.out.println("🌟 GIẢM GIÁ VIP: " + customer.getVipDiscountPercent() + 
                             "% = " + String.format("%,.0f", vipDiscountAmount) + " VND");
        } else {
            order.setVipDiscountAmount(0.0);
            order.setVipOrder(false);
        }
        
        double finalAmount = totalAmount - vipDiscountAmount;
        order.setFinalAmount(finalAmount);
        
        System.out.println("Thành tiền: " + String.format("%,.0f", finalAmount) + " VND");
    }
    
    private void executeInventoryReduction(List<OrderItem> orderItems) {
        System.out.println("--- TRỪ TỒN KHO ---");
        
        // Group by product để trừ một lần
        Map<Long, Integer> reductionMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            Long productId = item.getProduct().getId();
            reductionMap.merge(productId, item.getQuantity(), Integer::sum);
        }
        
        for (Map.Entry<Long, Integer> entry : reductionMap.entrySet()) {
            Long productId = entry.getKey();
            Integer totalReduction = entry.getValue();
            
            Product product = productRepository.findById(productId).get();
            int oldQuantity = product.getQuantity();
            int newQuantity = oldQuantity - totalReduction;
            
            // Đảm bảo không âm
            if (newQuantity < 0) {
                newQuantity = 0;
            }
            
            product.setQuantity(newQuantity);
            productRepository.save(product);
            
            System.out.println("📦 " + product.getName() + 
                             ": " + oldQuantity + " → " + newQuantity + 
                             " (đã bán " + totalReduction + ")");
        }
    }
    
    private void updateCustomerStats(Customer customer, double orderAmount) {
        try {
            customer.addOrder(orderAmount); // Cập nhật VIP status
            customerService.saveCustomer(customer);
            System.out.println("👤 Cập nhật thông tin khách hàng: " + customer.getVipLevel());
        } catch (Exception e) {
            System.err.println("Lỗi cập nhật khách hàng: " + e.getMessage());
        }
    }

    // ==================== UTILITY METHODS ====================
    
    public String generateOrderNumber() {
        try {
            long orderCount = orderRepository.count();
            LocalDateTime now = LocalDateTime.now();
            String dateFormat = String.format("%04d%02d%02d", 
                                             now.getYear(), 
                                             now.getMonthValue(), 
                                             now.getDayOfMonth());
            String orderSequence = String.format("%04d", orderCount + 1);
            return "ORD" + dateFormat + orderSequence;
        } catch (Exception e) {
            return "ORD" + System.currentTimeMillis();
        }
    }
    
    // Method để check tồn kho real-time
    public boolean checkProductAvailability(Long productId, int requestedQuantity) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return false;
        
        return product.getQuantity() >= requestedQuantity;
    }
    
    // Method để lấy tồn kho hiện tại
    public int getCurrentStock(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        return product != null ? product.getQuantity() : 0;
    }

    // ==================== EXISTING METHODS (giữ nguyên) ====================
    
    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    // ...existing code...

    @Transactional
    public Order createOrderWithInventoryReduction(OrderRequest request) {
        try {
            // Chuyển đổi CustomerInfo thành Customer entity
            Customer customer = convertCustomerInfoToCustomer(request.getCustomer());
            
            // Chuyển đổi OrderItemInfo thành OrderItem
            List<OrderItem> items = new ArrayList<>();
            for (OrderRequest.OrderItemInfo itemInfo : request.getItems()) {
                Product product = productRepository.findById(itemInfo.getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại: " + itemInfo.getId()));
                
                OrderItem item = new OrderItem();
                item.setProduct(product);
                item.setQuantity(itemInfo.getQuantity());
                item.setUnitPrice(itemInfo.getPrice());
                items.add(item);
            }
            
            return createOrder(customer, items);
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo đơn hàng từ OrderRequest: " + e.getMessage());
        }
    }

    // Helper method để chuyển đổi CustomerInfo thành Customer
    private Customer convertCustomerInfoToCustomer(OrderRequest.CustomerInfo customerInfo) {
        Customer customer;
        
        if (customerInfo.getId() != null) {
            // Tìm customer có sẵn
            customer = customerService.findById(customerInfo.getId());
            if (customer == null) {
                throw new RuntimeException("Không tìm thấy khách hàng với ID: " + customerInfo.getId());
            }
        } else {
            // Kiểm tra khách hàng đã tồn tại qua SĐT
            if (customerInfo.getPhone() != null && !customerInfo.getPhone().trim().isEmpty()) {
                customer = customerService.findByPhone(customerInfo.getPhone());
                if (customer != null) {
                    return customer; // Trả về customer có sẵn
                }
            }
            
            // Tạo customer mới
            customer = new Customer();
            customer.setName(customerInfo.getName() != null ? customerInfo.getName() : "Khách lẻ");
            customer.setPhone(customerInfo.getPhone());
            customer.setAddress(customerInfo.getAddress());
            customer.setDateOfBirth(customerInfo.getDateOfBirth());
            customer.setProvince(customerInfo.getProvince());
            customer.setDistrict(customerInfo.getDistrict());
            customer.setWard(customerInfo.getWard());
            customer.setVip(customerInfo.isVip());
            customer.setPendingVip(customerInfo.isPendingVip());
            customer.setVipDiscountPercent((double) customerInfo.getVipDiscountPercent());
            customer.setCreatedAt(LocalDateTime.now());
            customer.setTotalSpent(0.0);
            customer.setOrderCount(0);
            
            customer = customerService.saveCustomer(customer);
        }
        
        return customer;
    }

// ...existing code...

    @Transactional
    public Order createOrderWithItems(Order order, List<OrderItem> orderItems) {
        return createOrder(order.getCustomer(), orderItems);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isPresent()) {
                return orderOpt.get().getItems();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Lỗi lấy order items: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Order> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    // ...existing code...

    @Transactional
    public Optional<Order> updateOrder(Long id, Order orderDetails) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order existingOrder = orderOpt.get();
            existingOrder.setCustomer(orderDetails.getCustomer());
            existingOrder.setTotalAmount(orderDetails.getTotalAmount());
            existingOrder.setVipDiscountAmount(orderDetails.getVipDiscountAmount());
            existingOrder.setFinalAmount(orderDetails.getFinalAmount());
            existingOrder.setVipOrder(orderDetails.getVipOrder());  // Sử dụng getVipOrder()
            existingOrder.setStatus(orderDetails.getStatus());
            return Optional.of(orderRepository.save(existingOrder));
        }
        return Optional.empty();
    }

// ...existing code...

    @Transactional
    public Optional<Order> updateOrderStatus(Long id, Order.OrderStatus status) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isPresent()) {
            Order existingOrder = orderOpt.get();
            existingOrder.setStatus(status);
            System.out.println("Cập nhật trạng thái đơn hàng " + id + " thành: " + status);
            return Optional.of(orderRepository.save(existingOrder));
        }
        return Optional.empty();
    }

    @Transactional
    public boolean deleteOrder(Long id) {
        if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id);
            System.out.println("Đã xóa đơn hàng ID: " + id);
            return true;
        }
        return false;
    }

    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public Order findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).orElse(null);
    }
}