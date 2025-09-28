package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/user/pos")
@SessionAttributes({"cart", "selectedCustomer"})
public class POSController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private POSService posService;

    // ==================== POS MAIN INTERFACE ====================

    @GetMapping
    public String posInterface(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        try {
            List<Product> availableProducts = productService.getAllProducts().stream()
                .filter(p -> p.getQuantity() > 0)
                .toList();

            long totalProducts = availableProducts.size();
            long lowStockProducts = availableProducts.stream()
                .mapToLong(p -> p.getQuantity() < 10 ? 1 : 0)
                .sum();

            List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cartItems");
            if (cartItems == null) {
                cartItems = new ArrayList<>();
                session.setAttribute("cartItems", cartItems);
            }

            double cartTotal = cartItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();

            model.addAttribute("products", availableProducts);
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("cartTotal", cartTotal);
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("lowStockProducts", lowStockProducts);
            model.addAttribute("staffName", username);

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
        }

        return "user/pos/interface";
    }

    // ==================== PRODUCT SEARCH (Tìm kiếm theo tên, mô tả, danh mục) ====================

    @GetMapping("/search")
@ResponseBody
public List<Product> searchProducts(@RequestParam String query) {
    String lowerQuery = query.toLowerCase();
    return productService.getAllProducts().stream()
        .filter(p -> p.getQuantity() > 0 &&
            (
                (p.getName() != null && p.getName().toLowerCase().contains(lowerQuery)) ||
                (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerQuery)) ||
                (p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerQuery))
            )
        )
        .limit(10)
        .toList();
}

@GetMapping("/products")
@ResponseBody
public List<Product> getProducts() {
    return productService.getAllProducts().stream()
        .filter(p -> p.getQuantity() > 0)
        .toList();
}

@GetMapping("/categories")
@ResponseBody
public List<String> getCategories() {
    return productService.getAllProducts().stream()
        .map(Product::getCategory)
        .filter(c -> c != null && !c.trim().isEmpty())
        .distinct()
        .toList();
}

@GetMapping("/search-by-category")
@ResponseBody
public List<Product> searchProductsByCategory(@RequestParam String category) {
    String lowerCategory = category.toLowerCase();
    return productService.getAllProducts().stream()
        .filter(p -> p.getQuantity() > 0 &&
            p.getCategory() != null &&
            p.getCategory().toLowerCase().equals(lowerCategory)
        )
        .toList();
}
    // ==================== CART MANAGEMENT ====================

    @PostMapping("/cart/add")
    @ResponseBody
    public CartResponse addToCart(@RequestParam Long productId, 
                                @RequestParam Integer quantity,
                                HttpSession session) {
        try {
            Optional<Product> productOpt = productService.getProductById(productId);
            if (!productOpt.isPresent()) {
                return new CartResponse(false, "Sản phẩm không tồn tại");
            }
            
            Product product = productOpt.get();
            if (product.getQuantity() < quantity) {
                return new CartResponse(false, "Không đủ hàng trong kho");
            }

            List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cartItems");
            if (cartItems == null) {
                cartItems = new ArrayList<>();
            }

            Optional<CartItem> existingItem = cartItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

            if (existingItem.isPresent()) {
                CartItem item = existingItem.get();
                int newQuantity = item.getQuantity() + quantity;
                if (product.getQuantity() < newQuantity) {
                    return new CartResponse(false, "Tổng số lượng vượt quá kho");
                }
                item.setQuantity(newQuantity);
            } else {
                CartItem newItem = new CartItem();
                newItem.setProductId(productId);
                newItem.setProductName(product.getName());
                newItem.setPrice(product.getPrice());
                newItem.setQuantity(quantity);
                cartItems.add(newItem);
            }

            session.setAttribute("cartItems", cartItems);
            
            double cartTotal = cartItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();

            return new CartResponse(true, "Đã thêm vào giỏ hàng", cartItems.size(), cartTotal);

        } catch (Exception e) {
            return new CartResponse(false, "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    @PostMapping("/cart/update")
    @ResponseBody
    public CartResponse updateCartItem(@RequestParam Long productId, 
                                     @RequestParam Integer quantity,
                                     HttpSession session) {
        try {
            List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cartItems");
            if (cartItems == null) {
                return new CartResponse(false, "Giỏ hàng trống");
            }

            Optional<CartItem> itemOpt = cartItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

            if (!itemOpt.isPresent()) {
                return new CartResponse(false, "Sản phẩm không có trong giỏ");
            }

            CartItem item = itemOpt.get();
            
            Optional<Product> productOpt = productService.getProductById(productId);
            if (productOpt.isPresent() && productOpt.get().getQuantity() < quantity) {
                return new CartResponse(false, "Không đủ hàng trong kho");
            }

            if (quantity <= 0) {
                cartItems.remove(item);
            } else {
                item.setQuantity(quantity);
            }

            session.setAttribute("cartItems", cartItems);
            
            double cartTotal = cartItems.stream()
                .mapToDouble(i -> i.getQuantity() * i.getPrice())
                .sum();

            return new CartResponse(true, "Đã cập nhật giỏ hàng", cartItems.size(), cartTotal);

        } catch (Exception e) {
            return new CartResponse(false, "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    @PostMapping("/cart/remove")
    @ResponseBody
    public CartResponse removeFromCart(@RequestParam Long productId, HttpSession session) {
        try {
            List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cartItems");
            if (cartItems != null) {
                cartItems.removeIf(item -> item.getProductId().equals(productId));
                session.setAttribute("cartItems", cartItems);
                
                double cartTotal = cartItems.stream()
                    .mapToDouble(item -> item.getQuantity() * item.getPrice())
                    .sum();

                return new CartResponse(true, "Đã xóa khỏi giỏ hàng", cartItems.size(), cartTotal);
            }
            return new CartResponse(false, "Giỏ hàng trống");
        } catch (Exception e) {
            return new CartResponse(false, "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    @PostMapping("/cart/clear")
    @ResponseBody
    public CartResponse clearCart(HttpSession session) {
        session.removeAttribute("cartItems");
        return new CartResponse(true, "Đã xóa toàn bộ giỏ hàng", 0, 0.0);
    }

    // ==================== CUSTOMER MANAGEMENT ====================

    // ...existing code...

// ==================== CUSTOMER MANAGEMENT ====================

@GetMapping("/customer/all")
@ResponseBody
public List<Customer> getAllCustomers() {
    try {
        return customerService.getAllCustomers();
    } catch (Exception e) {
        // Trả về danh sách rỗng nếu có lỗi để tránh crash
        return new ArrayList<>();
    }
}

@GetMapping("/customer/search")
@ResponseBody
public List<Customer> searchCustomers(@RequestParam String query) {
    try {
        if (query == null || query.length() < 3) {
            return new ArrayList<>();
        }
        String lowerQuery = query.toLowerCase();
        return customerService.getAllCustomers().stream()
            .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(lowerQuery)) ||
                        (c.getPhone() != null && c.getPhone().contains(query)))
            .limit(10)
            .toList();
    } catch (Exception e) {
        // Trả về danh sách rỗng nếu có lỗi
        return new ArrayList<>();
    }
}

// ...existing code...
    @PostMapping("/customer/create")
    @ResponseBody
    public CustomerResponse createCustomer(@RequestParam String fullName,
                                         @RequestParam String phoneNumber,
                                         @RequestParam(required = false) String email) {
        try {
            if (customerService.existsByPhone(phoneNumber)) {
                return new CustomerResponse(false, "Số điện thoại đã tồn tại", null);
            }

            Customer customer = new Customer();
            customer.setName(fullName);
            customer.setPhone(phoneNumber);
            
            Customer savedCustomer = customerService.createCustomer(customer);
            return new CustomerResponse(true, "Đã tạo khách hàng thành công", savedCustomer);

        } catch (Exception e) {
            return new CustomerResponse(false, "Có lỗi xảy ra: " + e.getMessage(), null);
        }
    }

    // ==================== PAYMENT PAGE ====================

    @GetMapping("/payment")
    public String showPaymentPage(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        try {
            List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cartItems");
            if (cartItems == null || cartItems.isEmpty()) {
                model.addAttribute("error", "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
                return "redirect:/user/pos";
            }

            Customer selectedCustomer = (Customer) session.getAttribute("selectedCustomer");

            double subtotal = cartItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();
            double vipDiscount = 0.0;
            double finalAmount = subtotal;

            if (selectedCustomer != null && selectedCustomer.isVip()) {
                vipDiscount = subtotal * (selectedCustomer.getVipDiscountPercent() / 100.0);
                finalAmount = subtotal - vipDiscount;
            }

            model.addAttribute("cartItems", cartItems);
            model.addAttribute("selectedCustomer", selectedCustomer);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("vipDiscount", vipDiscount);
            model.addAttribute("finalAmount", finalAmount);
            model.addAttribute("staffName", username);

            return "user/pos/payment";

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải trang thanh toán: " + e.getMessage());
            return "redirect:/user/pos";
        }
    }

    
    // ==================== PROCESS PAYMENT ====================

@GetMapping("/get-current-order-id")
@ResponseBody
public Map<String, Object> getCurrentOrderId(HttpSession session) {
    Object orderId = session.getAttribute("orderId");
    if (orderId != null) {
        return Map.of("orderId", orderId);
    } else {
        return Map.of("orderId", null);
    }
}
    @PostMapping("/process-payment")
public String processPayment(@RequestParam String paymentMethod,
                           @RequestParam(required = false) String notes,
                           @RequestParam(defaultValue = "false") boolean createVipRequest,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
    try {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập hết hạn");
            return "redirect:/login";
        }

        List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cartItems");
        if (cartItems == null || cartItems.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
            return "redirect:/user/pos/payment";
        }

        Customer selectedCustomer = (Customer) session.getAttribute("selectedCustomer");

        // Loại bỏ kiểm tra bắt buộc selectedCustomer != null để hỗ trợ khách lẻ
        // if (selectedCustomer == null) {
        //     redirectAttributes.addFlashAttribute("error", "Vui lòng chọn khách hàng trước khi thanh toán");
        //     return "redirect:/user/pos/payment";
        // }

        if (selectedCustomer != null && selectedCustomer.isVip()) {
            if (selectedCustomer.getAddress() == null || selectedCustomer.getAddress().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Khách VIP phải nhập đầy đủ địa chỉ");
                return "redirect:/user/pos/payment";
            }
        }

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setCustomer(selectedCustomer != null ? convertCustomerToCustomerInfo(selectedCustomer) : null); // null cho khách lẻ
        orderRequest.setItems(convertCartItemsToOrderItems(cartItems));
        orderRequest.setCreateVipRequest(createVipRequest);

        double subtotal = cartItems.stream()
            .mapToDouble(item -> item.getQuantity() * item.getPrice())
            .sum();
        double vipDiscountAmount = (selectedCustomer != null && selectedCustomer.isVip()) ? subtotal * (selectedCustomer.getVipDiscountPercent() / 100.0) : 0.0;
        double finalAmount = subtotal - vipDiscountAmount;

        orderRequest.setTotalAmount(subtotal);
        orderRequest.setVipDiscountAmount(vipDiscountAmount);
        orderRequest.setFinalAmount(finalAmount);
        orderRequest.setVipOrder(selectedCustomer != null && selectedCustomer.isVip());

        Map<String, Object> result;
        try {
            result = posService.processOrder(orderRequest);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không tải được dữ liệu từ database: " + ex.getMessage());
            return "redirect:/user/pos/payment";
        }

        if ((Boolean) result.get("success")) {
            // Cập nhật tồn kho
            try {
                posService.updateInventory(cartItems);
            } catch (Exception ex) {
                System.out.println("Lỗi cập nhật tồn kho: " + ex.getMessage());
            }

            // Lưu orderNumber vào session để hiển thị trong modal
            Long orderId = (Long) result.get("orderId");
            String orderNumber = (String) result.get("orderNumber");
            session.setAttribute("orderNumber", orderNumber);
            session.setAttribute("orderId", orderId);

            Boolean vipRequestCreated = (Boolean) result.get("vipRequestCreated");
            redirectAttributes.addFlashAttribute("success", 
                "Đơn hàng " + orderNumber + " đã được tạo thành công!");
            
            if (vipRequestCreated) {
                redirectAttributes.addFlashAttribute("vipMessage", 
                    "Yêu cầu VIP đã được tạo và đang chờ admin xác nhận!");
            }

            return "redirect:/user/pos/payment";  // Redirect về payment để hiển thị modal
        } else {
            redirectAttributes.addFlashAttribute("error", result.get("message"));
            return "redirect:/user/pos/payment";
        }

    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", 
            "Có lỗi xảy ra khi xử lý thanh toán: " + e.getMessage());
        return "redirect:/user/pos/payment";
    }
}

    // ==================== POST PAYMENT (From Interface) ====================

    @PostMapping("/payment")
    public String showPaymentPagePost(
        @RequestParam("customerId") String customerId,
        @RequestParam("customerName") String customerName,
        @RequestParam("customerPhone") String customerPhone,
        @RequestParam("cartData") String cartDataJson,
        @RequestParam("customerData") String customerDataJson,
        Model model, HttpSession session) {
        
        try {
            // Khởi tạo ObjectMapper để parse JSON
            ObjectMapper objectMapper = new ObjectMapper();
            
            // Parse cart data từ JSON
            List<Map<String, Object>> cart = objectMapper.readValue(cartDataJson, List.class);
            
            // Parse customer data từ JSON
            Map<String, Object> customer = objectMapper.readValue(customerDataJson, Map.class);
            
            // Convert cart (Map) thành List<CartItem>
            List<CartItem> cartItems = new ArrayList<>();
            for (Map<String, Object> item : cart) {
                CartItem ci = new CartItem();
                ci.setProductId(Long.valueOf(item.get("id").toString()));
                ci.setProductName((String) item.get("name"));
                ci.setPrice(Double.valueOf(item.get("price").toString()));
                ci.setQuantity(Integer.valueOf(item.get("quantity").toString()));
                cartItems.add(ci);
            }
            
            // Convert customer (Map) thành Customer object
            Customer selectedCustomer = new Customer();
            selectedCustomer.setId(customer.get("id") != null ? Long.valueOf(customer.get("id").toString()) : null);
            selectedCustomer.setName((String) customer.get("name"));
            selectedCustomer.setPhone((String) customer.get("phone"));
            selectedCustomer.setVip(Boolean.parseBoolean(customer.getOrDefault("vip", false).toString()));
            selectedCustomer.setVipDiscountPercent(Double.valueOf(customer.getOrDefault("vipDiscountPercent", 0).toString()));
            selectedCustomer.setPendingVip(Boolean.parseBoolean(customer.getOrDefault("pendingVip", false).toString()));
            
            // Tính toán tổng tiền (dùng cartItems)
            double subtotal = cartItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();
            double vipDiscountAmount = 0.0;
            if (selectedCustomer.isVip()) {
                vipDiscountAmount = subtotal * (selectedCustomer.getVipDiscountPercent() / 100);
            }
            double finalAmount = subtotal - vipDiscountAmount;
            
            // Lưu vào session với tên match GET method
            session.setAttribute("cartItems", cartItems);
            session.setAttribute("selectedCustomer", selectedCustomer);
            session.setAttribute("subtotal", subtotal);
            session.setAttribute("vipDiscountAmount", vipDiscountAmount);
            session.setAttribute("finalAmount", finalAmount);
            
            // Thêm vào model
            model.addAttribute("cartItems", cartItems);
            model.addAttribute("selectedCustomer", selectedCustomer);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("vipDiscountAmount", vipDiscountAmount);
            model.addAttribute("finalAmount", finalAmount);
            
            // Log để debug
            System.out.println("Converted CartItems: " + cartItems);
            System.out.println("Converted Customer: " + selectedCustomer);
            System.out.println("Subtotal: " + subtotal + ", VIP Discount: " + vipDiscountAmount + ", Final: " + finalAmount);
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi xử lý dữ liệu thanh toán. Vui lòng thử lại!");
            return "user/pos/interface";
        }
        
        return "user/pos/payment";
    }



// Thêm endpoint này
// ...existing code...

@GetMapping("/receipt-data/{orderId}")
@ResponseBody
public Map<String, Object> getReceiptData(@PathVariable Long orderId) {
    try {
        Map<String, Object> orderResult = posService.getOrderById(orderId);
        if ((Boolean) orderResult.get("success")) {
            Order order = (Order) orderResult.get("order");
            Map<String, Object> data = new HashMap<>();
            data.put("orderNumber", order.getOrderNumber());
            data.put("createdAt", order.getCreatedAt());
            data.put("status", order.getStatus());
            // Xử lý customer có thể null cho khách lẻ
            if (order.getCustomer() != null) {
                Map<String, Object> customerMap = new HashMap<>();
                customerMap.put("name", order.getCustomer().getName());
                customerMap.put("phone", order.getCustomer().getPhone());
                customerMap.put("address", order.getCustomer().getAddress());
                customerMap.put("vip", order.getCustomer().isVip());
                data.put("customer", customerMap);
            } else {
                Map<String, Object> customerMap = new HashMap<>();
                customerMap.put("name", "Khách lẻ");
                customerMap.put("phone", null);
                customerMap.put("address", null);
                customerMap.put("vip", false);
                data.put("customer", customerMap);
            }
            data.put("items", order.getItems().stream().map(item -> {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("name", item.getProduct().getName());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPrice());
                itemMap.put("totalPrice", item.getTotalPrice());
                return itemMap;
            }).toList());
            data.put("totalAmount", order.getTotalAmount());
            data.put("vipDiscountAmount", order.getVipDiscountAmount());
            data.put("finalAmount", order.getFinalAmount());
            return Map.of("success", true, "data", data);
        } else {
            return Map.of("success", false, "message", orderResult.get("message"));
        }
    } catch (Exception e) {
        return Map.of("success", false, "message", e.getMessage());
    }
}

// ...existing code...
// ...existing code...
    // ==================== CLEAR SESSION ====================

    @PostMapping("/clear-session")
    public String clearSession(HttpSession session) {
        session.removeAttribute("cartItems");
        session.removeAttribute("selectedCustomer");
        session.removeAttribute("subtotal");
        session.removeAttribute("vipDiscountAmount");
        session.removeAttribute("finalAmount");
        session.removeAttribute("orderNumber");
        return "redirect:/user/pos";
    }

    // ==================== CHECKOUT (Updated to use POSService) ====================

    @PostMapping("/checkout")
    public String checkout(@RequestParam(required = false) Long customerId,
                         @RequestParam(required = false) String customerName,
                         @RequestParam(required = false) String customerPhone,
                         @RequestParam(defaultValue = "false") boolean createVipRequest,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        try {
            List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cartItems");
            if (cartItems == null || cartItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
                return "redirect:/user/pos";
            }

            OrderRequest orderRequest = convertCartToOrderRequest(cartItems, customerId, customerName, customerPhone, createVipRequest);

            Map<String, Object> result;
            try {
                result = posService.processOrder(orderRequest);
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("error", "Không tải được dữ liệu từ database: " + ex.getMessage());
                return "redirect:/user/pos";
            }

            if ((Boolean) result.get("success")) {
                session.removeAttribute("cartItems");

                Long orderId = (Long) result.get("orderId");
                String orderNumber = (String) result.get("orderNumber");
                Boolean vipRequestCreated = (Boolean) result.get("vipRequestCreated");

                redirectAttributes.addFlashAttribute("success", 
                    "Đơn hàng " + orderNumber + " đã được tạo thành công!");
                
                if (vipRequestCreated) {
                    redirectAttributes.addFlashAttribute("vipMessage", 
                        "Yêu cầu VIP đã được tạo và đang chờ duyệt!");
                }

                return "redirect:/user/pos/receipt/" + orderId;
            } else {
                redirectAttributes.addFlashAttribute("error", result.get("message"));
                return "redirect:/user/pos";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Có lỗi xảy ra khi tạo đơn hàng: " + e.getMessage());
            return "redirect:/user/pos";
        }
    }

    @PostMapping("/process-order")
    @ResponseBody
    public Map<String, Object> processOrder(@RequestBody Map<String, Object> orderData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                response.put("success", false);
                response.put("message", "Phiên đăng nhập hết hạn");
                return response;
            }
            
            OrderRequest orderRequest = convertOrderDataToOrderRequest(orderData);

            if (orderRequest.getCustomer() == null) {
                response.put("success", false);
                response.put("message", "Thông tin khách hàng không được để trống!");
                return response;
            }
            
            Map<String, Object> result;
            try {
                result = posService.processOrder(orderRequest);
            } catch (Exception ex) {
                response.put("success", false);
                response.put("message", "Không tải được dữ liệu từ database: " + ex.getMessage());
                return response;
            }
            
            if ((Boolean) result.get("success")) {
                session.removeAttribute("cartItems");
                
                response.put("success", true);
                response.put("orderId", result.get("orderId"));
                response.put("orderNumber", result.get("orderNumber"));
                response.put("finalAmount", result.get("finalAmount"));
                response.put("vipRequestCreated", result.get("vipRequestCreated"));
                response.put("isVipOrder", result.get("isVipOrder"));
            } else {
                response.put("success", false);
                response.put("message", result.get("message"));
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return response;
    }

    // Helper method để chuyển đổi dữ liệu từ frontend
    private OrderRequest convertOrderDataToOrderRequest(Map<String, Object> orderData) {
        OrderRequest request = new OrderRequest();
        
        Map<String, Object> customerData = (Map<String, Object>) orderData.get("customer");
        if (customerData != null && customerData.get("id") != null) {
            OrderRequest.CustomerInfo customerInfo = new OrderRequest.CustomerInfo();
            customerInfo.setId(Long.valueOf(customerData.get("id").toString()));
            customerInfo.setName((String) customerData.get("name"));
            customerInfo.setPhone((String) customerData.get("phone"));
            request.setCustomer(customerInfo);
        }
        
        List<Map<String, Object>> itemsData = (List<Map<String, Object>>) orderData.get("items");
        List<OrderRequest.OrderItemInfo> items = new ArrayList<>();
        for (Map<String, Object> itemData : itemsData) {
            OrderRequest.OrderItemInfo item = new OrderRequest.OrderItemInfo();
            item.setId(Long.valueOf(itemData.get("id").toString()));
            item.setQuantity(Integer.valueOf(itemData.get("quantity").toString()));
            item.setPrice(Double.valueOf(itemData.get("price").toString()));
            item.setTotal(Double.valueOf(itemData.get("total").toString()));
            items.add(item);
        }
        request.setItems(items);
        
        request.setTotalAmount(Double.valueOf(orderData.get("totalAmount").toString()));
        request.setVipDiscountAmount(Double.valueOf(orderData.get("vipDiscountAmount").toString()));
        request.setFinalAmount(Double.valueOf(orderData.get("finalAmount").toString()));
        request.setVipOrder((Boolean) orderData.get("vipOrder"));
        request.setCreateVipRequest((Boolean) orderData.get("createVipRequest"));
        
        return request;
    }

    // ==================== RECEIPT ====================

    @GetMapping("/receipt/{orderId}")
    public String showReceipt(@PathVariable Long orderId, Model model) {
        try {
            Map<String, Object> orderResult = posService.getOrderById(orderId);
            
            if ((Boolean) orderResult.get("success")) {
                Order order = (Order) orderResult.get("order");
                
                model.addAttribute("order", order);
                model.addAttribute("orderItems", order.getItems());
                
                return "user/pos/receipt";
            } else {
                model.addAttribute("error", "Không tìm thấy đơn hàng");
                return "redirect:/user/pos";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/user/pos";
        }
    }

    // ==================== HELPER METHODS ====================

    private OrderRequest convertCartToOrderRequest(List<CartItem> cartItems, Long customerId, 
                                                  String customerName, String customerPhone, 
                                                  boolean createVipRequest) {
        OrderRequest orderRequest = new OrderRequest();

        OrderRequest.CustomerInfo customerInfo = new OrderRequest.CustomerInfo();
        customerInfo.setId(customerId);
        customerInfo.setName(customerName);
        customerInfo.setPhone(customerPhone);
        orderRequest.setCustomer(customerInfo);

        List<OrderRequest.OrderItemInfo> orderItems = new ArrayList<>();
        double totalAmount = 0.0;

        for (CartItem cartItem : cartItems) {
            OrderRequest.OrderItemInfo itemInfo = new OrderRequest.OrderItemInfo();
            itemInfo.setId(cartItem.getProductId());
            itemInfo.setName(cartItem.getProductName());
            itemInfo.setPrice(cartItem.getPrice());
            itemInfo.setQuantity(cartItem.getQuantity());
            itemInfo.setTotal(cartItem.getPrice() * cartItem.getQuantity());

            orderItems.add(itemInfo);
            totalAmount += itemInfo.getTotal();
        }

        orderRequest.setItems(orderItems);
        orderRequest.setTotalAmount(totalAmount);
        orderRequest.setVipDiscountAmount(0.0);
        orderRequest.setFinalAmount(totalAmount);
        orderRequest.setVipOrder(false);
        orderRequest.setCreateVipRequest(createVipRequest);

        return orderRequest;
    }

    private OrderRequest.CustomerInfo convertCustomerToCustomerInfo(Customer customer) {
        OrderRequest.CustomerInfo customerInfo = new OrderRequest.CustomerInfo();
        customerInfo.setId(customer.getId());
        customerInfo.setName(customer.getName());
        customerInfo.setPhone(customer.getPhone());
        customerInfo.setAddress(customer.getAddress());
        customerInfo.setVip(customer.isVip());
        customerInfo.setPendingVip(customer.isPendingVip());
        return customerInfo;
    }

    private List<OrderRequest.OrderItemInfo> convertCartItemsToOrderItems(List<CartItem> cartItems) {
        List<OrderRequest.OrderItemInfo> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderRequest.OrderItemInfo itemInfo = new OrderRequest.OrderItemInfo();
            itemInfo.setId(cartItem.getProductId());
            itemInfo.setName(cartItem.getProductName());
            itemInfo.setPrice(cartItem.getPrice());
            itemInfo.setQuantity(cartItem.getQuantity());
            itemInfo.setTotal(cartItem.getPrice() * cartItem.getQuantity());
            orderItems.add(itemInfo);
        }
        return orderItems;
    }

    // ==================== INNER CLASSES ====================
    
    public static class CartItem {
        private Long productId;
        private String productName;
        private Double price;
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class CartResponse {
        private boolean success;
        private String message;
        private int cartSize;
        private double cartTotal;

        public CartResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public CartResponse(boolean success, String message, int cartSize, double cartTotal) {
            this.success = success;
            this.message = message;
            this.cartSize = cartSize;
            this.cartTotal = cartTotal;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public int getCartSize() { return cartSize; }
        public void setCartSize(int cartSize) { this.cartSize = cartSize; }

        public double getCartTotal() { return cartTotal; }
        public void setCartTotal(double cartTotal) { this.cartTotal = cartTotal; }
    }

    public static class CustomerResponse {
        private boolean success;
        private String message;
        private Customer customer;

        public CustomerResponse(boolean success, String message, Customer customer) {
            this.success = success;
            this.message = message;
            this.customer = customer;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Customer getCustomer() { return customer; }
        public void setCustomer(Customer customer) { this.customer = customer; }
    }
}