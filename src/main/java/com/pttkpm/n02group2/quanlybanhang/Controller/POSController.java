package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Repository.OrderRepository;
import com.pttkpm.n02group2.quanlybanhang.Service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
private OrderRepository orderRepository;
    @Autowired
    private ProductService productService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private POSService posService;
    
    @Autowired
    private PromotionService promotionService;

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

    // ==================== PRODUCT SEARCH ====================

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

    @GetMapping("/customer/all")
    @ResponseBody
    public List<Customer> getAllCustomers() {
        try {
            return customerService.getAllCustomers();
        } catch (Exception e) {
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
            return new ArrayList<>();
        }
    }

    @PostMapping("/customer/create")
@ResponseBody
public CustomerResponse createCustomer(
    @RequestParam String fullName,
    @RequestParam String phoneNumber,
    @RequestParam(required = false) String email,
    @RequestParam(required = false) String address,
    @RequestParam(required = false) String ward,
    @RequestParam(required = false) String district,
    @RequestParam(required = false) String province,
    @RequestParam(required = false, name = "dob") String dateOfBirth,  // ⬅️ THÊM name = "dob"
    HttpServletRequest request
) {
    try {
        System.out.println("=== CREATE CUSTOMER DEBUG ===");
        System.out.println("Full Name: " + fullName);
        System.out.println("Phone Number: " + phoneNumber);
        System.out.println("Email: " + email);
        System.out.println("Address: " + address);
        System.out.println("Ward: " + ward);
        System.out.println("District: " + district);
        System.out.println("Province: " + province);
        System.out.println("Date of Birth (RAW): '" + dateOfBirth + "'");
        System.out.println("Date of Birth is null? " + (dateOfBirth == null));
        System.out.println("Date of Birth is empty? " + (dateOfBirth != null && dateOfBirth.isEmpty()));
        
        // DEBUG: IN TẤT CẢ PARAMETERS
        System.out.println("=== ALL REQUEST PARAMETERS ===");
        request.getParameterMap().forEach((key, values) -> {
            System.out.println(key + " = " + java.util.Arrays.toString(values));
        });
        System.out.println("=== END PARAMETERS ===");
        
        // Validate required fields
        if (fullName == null || fullName.trim().isEmpty()) {
            return new CustomerResponse(false, "Tên khách hàng không được để trống", null);
        }
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return new CustomerResponse(false, "Số điện thoại không được để trống", null);
        }
        
        if (customerService.existsByPhone(phoneNumber)) {
            return new CustomerResponse(false, "Số điện thoại đã tồn tại", null);
        }
        
        // Tạo customer object
        Customer customer = new Customer();
        customer.setName(fullName.trim());
        customer.setPhone(phoneNumber.trim());
        customer.setEmail(email != null && !email.trim().isEmpty() ? email.trim() : null);
        customer.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
        customer.setWard(ward != null && !ward.trim().isEmpty() ? ward.trim() : null);
        customer.setDistrict(district != null && !district.trim().isEmpty() ? district.trim() : null);
        customer.setProvince(province != null && !province.trim().isEmpty() ? province.trim() : null);
        
        // XỬ LÝ NGÀY SINH CHI TIẾT
        System.out.println("=== PROCESSING DATE OF BIRTH ===");
        System.out.println("dateOfBirth parameter value: '" + dateOfBirth + "'");
        
        if (dateOfBirth != null && !dateOfBirth.trim().isEmpty()) {
            String trimmedDob = dateOfBirth.trim();
            System.out.println("✅ Found valid dateOfBirth: '" + trimmedDob + "'");
            System.out.println("Length: " + trimmedDob.length());
            
            try {
                LocalDate dobParsed = parseDateOfBirth(trimmedDob);
                if (dobParsed != null) {
                    customer.setDateOfBirth(dobParsed);
                    System.out.println("✅ Successfully set dateOfBirth: " + dobParsed);
                } else {
                    System.out.println("⚠️ Parse returned null, setting customer DOB to null");
                    customer.setDateOfBirth(null);
                }
            } catch (Exception e) {
                System.err.println("❌ Exception parsing dateOfBirth: " + e.getMessage());
                e.printStackTrace();
                customer.setDateOfBirth(null);
            }
        } else {
            System.out.println("⚠️ dateOfBirth is null or empty, setting to null");
            customer.setDateOfBirth(null);
        }
        
        System.out.println("=== CUSTOMER OBJECT BEFORE SAVE ===");
        System.out.println("Name: " + customer.getName());
        System.out.println("Phone: " + customer.getPhone());
        System.out.println("Email: " + customer.getEmail());
        System.out.println("Address: " + customer.getAddress());
        System.out.println("Ward: " + customer.getWard());
        System.out.println("District: " + customer.getDistrict());
        System.out.println("Province: " + customer.getProvince());
        System.out.println("Date of Birth: " + customer.getDateOfBirth());
        System.out.println("=== END CUSTOMER OBJECT ===");

        Customer savedCustomer = customerService.createCustomer(customer);
        
        System.out.println("=== FINAL SAVED CUSTOMER ===");
        System.out.println("ID: " + savedCustomer.getId());
        System.out.println("Name: " + savedCustomer.getName());
        System.out.println("Phone: " + savedCustomer.getPhone());
        System.out.println("Date of Birth: " + savedCustomer.getDateOfBirth());
        System.out.println("=== END CREATE CUSTOMER DEBUG ===");
        
        return new CustomerResponse(true, "Đã tạo khách hàng thành công", savedCustomer);

    } catch (Exception e) {
        System.err.println("❌ Lỗi tạo khách hàng: " + e.getMessage());
        e.printStackTrace();
        return new CustomerResponse(false, "Có lỗi xảy ra: " + e.getMessage(), null);
    }
}

// THÊM METHOD PARSE AN TOÀN HỚN
private LocalDate parseDateOfBirthSafely(String dobStr) {
    System.out.println("=== PARSE DATE OF BIRTH SAFELY ===");
    System.out.println("Input: '" + dobStr + "'");
    
    if (dobStr == null || dobStr.trim().isEmpty()) {
        System.out.println("❌ Input is null or empty");
        return null;
    }
    
    dobStr = dobStr.trim();
    System.out.println("Trimmed input: '" + dobStr + "'");
    
    // DANH SÁCH CÁC FORMAT ĐƯỢC HỖ TRỢ
    String[][] patterns = {
        {"dd/MM/yyyy", "\\d{2}/\\d{2}/\\d{4}"},    // 25/12/1990
        {"dd-MM-yyyy", "\\d{2}-\\d{2}-\\d{4}"},    // 25-12-1990
        {"yyyy-MM-dd", "\\d{4}-\\d{2}-\\d{2}"},    // 1990-12-25
        {"MM/dd/yyyy", "\\d{2}/\\d{2}/\\d{4}"},    // 12/25/1990 (US format)
        {"dd/MM/yy", "\\d{2}/\\d{2}/\\d{2}"},      // 25/12/90
        {"dd-MM-yy", "\\d{2}-\\d{2}-\\d{2}"}       // 25-12-90
    };
    
    for (String[] patternPair : patterns) {
        String pattern = patternPair[0];
        String regex = patternPair[1];
        
        try {
            if (dobStr.matches(regex)) {
                System.out.println("✅ Matches pattern: " + pattern);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDate parsed = LocalDate.parse(dobStr, formatter);
                
                // KIỂM TRA NĂM HỢP LÝ (1900-2030)
                int year = parsed.getYear();
                if (year < 1900 || year > 2030) {
                    System.out.println("⚠️ Invalid year: " + year + " for pattern " + pattern);
                    continue;
                }
                
                System.out.println("✅ Successfully parsed: " + dobStr + " -> " + parsed);
                return parsed;
            }
        } catch (DateTimeParseException e) {
            System.out.println("❌ Failed with pattern " + pattern + ": " + e.getMessage());
        }
    }
    
    // NẾU TẤT CẢ PATTERN ĐỀU THẤT BẠI
    System.err.println("❌ Could not parse date: '" + dobStr + "'");
    System.err.println("❌ Supported formats: dd/MM/yyyy, dd-MM-yyyy, yyyy-MM-dd, MM/dd/yyyy, dd/MM/yy, dd-MM-yy");
    return null;
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
                Long orderId = (Long) session.getAttribute("orderId");
                if (orderId != null) {
                    Map<String, Object> orderResult = posService.getOrderById(orderId);
                    if ((Boolean) orderResult.get("success")) {
                        Order order = (Order) orderResult.get("order");
                        model.addAttribute("order", order);
                        model.addAttribute("showReceipt", true);
                        model.addAttribute("cartItems", new ArrayList<>());
                    }
                } else {
                    model.addAttribute("error", "Giỏ hàng trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
                    return "redirect:/user/pos";
                }
            } else {
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
            }

            model.addAttribute("staffName", username);
            return "user/pos/payment";

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải trang thanh toán: " + e.getMessage());
            return "redirect:/user/pos";
        }
    }

    @PostMapping("/process-payment")
    public String processPayment(@RequestParam String paymentMethod,
                                 @RequestParam(required = false) String notes,
                                 @RequestParam(defaultValue = "false") boolean createVipRequest,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
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
        if (selectedCustomer != null && selectedCustomer.isVip()) {
            if (selectedCustomer.getAddress() == null || selectedCustomer.getAddress().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Khách VIP phải nhập đầy đủ địa chỉ");
                return "redirect:/user/pos/payment";
            }
        }

        try {
            OrderRequest orderRequest = new OrderRequest();
            orderRequest.setCustomer(selectedCustomer != null ? convertCustomerToCustomerInfo(selectedCustomer) : null);
            orderRequest.setItems(convertCartItemsToOrderItems(cartItems));
            orderRequest.setCreateVipRequest(createVipRequest);

            double subtotal = cartItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();
            double vipDiscountAmount = (selectedCustomer != null && selectedCustomer.isVip())
                ? subtotal * (selectedCustomer.getVipDiscountPercent() / 100.0) : 0.0;
            double finalAmount = subtotal - vipDiscountAmount;

            orderRequest.setTotalAmount(subtotal);
            orderRequest.setVipDiscountAmount(vipDiscountAmount);
            orderRequest.setFinalAmount(finalAmount);
            orderRequest.setVipOrder(selectedCustomer != null && selectedCustomer.isVip());
            orderRequest.setPaymentMethod(paymentMethod);

            Map<String, Object> result = posService.processOrderAndUpdateCustomerAndInventory(orderRequest, cartItems, username);

            if (Boolean.TRUE.equals(result.get("success"))) {
                Long orderId = (Long) result.get("orderId");
                String orderNumber = (String) result.get("orderNumber");
                session.setAttribute("orderNumber", orderNumber);
                session.setAttribute("orderId", orderId);

                session.removeAttribute("cartItems");

                Boolean vipRequestCreated = (Boolean) result.get("vipRequestCreated");
                StringBuilder successMsg = new StringBuilder();
                successMsg.append("<b>Đơn hàng ").append(orderNumber)
                    .append(" (ID: ").append(orderId).append(")</b> đã được tạo thành công!<br>");
                successMsg.append("Tổng tiền: <b>").append(finalAmount).append(" VND</b><br>");
                successMsg.append("Thời gian: ").append(java.time.LocalDateTime.now()).append("<br>");
                if (vipRequestCreated != null && vipRequestCreated) {
                    successMsg.append("<span style='color:green'>Yêu cầu VIP đã được tạo và đang chờ admin xác nhận!</span><br>");
                }
                successMsg.append("Bạn có thể <b>in hóa đơn</b> hoặc bắt đầu giao dịch mới.");

                redirectAttributes.addFlashAttribute("success", successMsg.toString());
                redirectAttributes.addFlashAttribute("orderId", orderId);
                redirectAttributes.addFlashAttribute("orderNumber", orderNumber);
                redirectAttributes.addFlashAttribute("finalAmount", finalAmount);

                return "redirect:/user/pos/payment";
            } else {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi tạo đơn hàng: " + result.get("message"));
                return "redirect:/user/pos/payment";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra khi xử lý thanh toán: " + e.getMessage());
            return "redirect:/user/pos/payment";
        }
    }

    @PostMapping("/payment")
    public String showPaymentPagePost(
        @RequestParam("customerId") String customerId,
        @RequestParam("customerName") String customerName,
        @RequestParam("customerPhone") String customerPhone,
        @RequestParam("cartData") String cartDataJson,
        @RequestParam("customerData") String customerDataJson,
        Model model, HttpSession session) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            List<Map<String, Object>> cart = objectMapper.readValue(cartDataJson, List.class);
            Map<String, Object> customer = objectMapper.readValue(customerDataJson, Map.class);

            // Xử lý cart items
            List<CartItem> cartItems = new ArrayList<>();
            for (Map<String, Object> item : cart) {
                CartItem ci = new CartItem();
                ci.setProductId(Long.valueOf(item.get("id").toString()));
                ci.setProductName((String) item.get("name"));
                ci.setPrice(Double.valueOf(item.get("price").toString()));
                ci.setQuantity(Integer.valueOf(item.get("quantity").toString()));
                cartItems.add(ci);
            }

            // XỬ LÝ THÔNG TIN KHÁCH HÀNG
            Customer selectedCustomer = new Customer();
            
            String customerIdStr = customer.get("id") != null ? customer.get("id").toString() : null;
            boolean isExistingCustomer = customerIdStr != null && 
                                       !customerIdStr.isEmpty() && 
                                       !"null".equals(customerIdStr) &&
                                       !customerIdStr.equals("0");
            
            if (isExistingCustomer) {
                // KHÁCH HÀNG CŨ - LOAD TỪ DATABASE
                try {
                    Long customerIdLong = Long.valueOf(customerIdStr);
                    Optional<Customer> existingCustomerOpt = customerService.getCustomerById(customerIdLong);
                    
                    if (existingCustomerOpt.isPresent()) {
                        selectedCustomer = existingCustomerOpt.get();
                        
                        System.out.println("=== LOADED EXISTING CUSTOMER ===");
                        System.out.println("ID: " + selectedCustomer.getId());
                        System.out.println("Name: " + selectedCustomer.getName());
                        System.out.println("Date of Birth: " + selectedCustomer.getDateOfBirth());
                        System.out.println("=================================");
                        
                    } else {
                        System.err.println("Không tìm thấy khách hàng với ID: " + customerIdLong);
                        selectedCustomer = createCustomerFromFormData(customer);
                    }
                    
                } catch (NumberFormatException e) {
                    System.err.println("Lỗi parse customer ID: " + customerIdStr);
                    selectedCustomer = createCustomerFromFormData(customer);
                } catch (Exception e) {
                    System.err.println("Lỗi load khách hàng từ DB: " + e.getMessage());
                    e.printStackTrace();
                    selectedCustomer = createCustomerFromFormData(customer);
                }
                
            } else {
                // KHÁCH HÀNG MỚI - TỪ FORM
                selectedCustomer = createCustomerFromFormData(customer);
                
                System.out.println("=== NEW CUSTOMER FROM FORM ===");
                System.out.println("Name: " + selectedCustomer.getName());
                System.out.println("Date of Birth: " + selectedCustomer.getDateOfBirth());
                System.out.println("===============================");
            }

            // Tính toán giá
            double subtotal = cartItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();
                
            double vipDiscountAmount = 0.0;
            if (selectedCustomer.isVip() && selectedCustomer.getVipDiscountPercent() != null) {
                vipDiscountAmount = subtotal * (selectedCustomer.getVipDiscountPercent() / 100.0);
            }
            
            double finalAmount = subtotal - vipDiscountAmount;

            // Lưu vào session và model
            session.setAttribute("cartItems", cartItems);
            session.setAttribute("selectedCustomer", selectedCustomer);
            session.setAttribute("subtotal", subtotal);
            session.setAttribute("vipDiscountAmount", vipDiscountAmount);
            session.setAttribute("finalAmount", finalAmount);

            model.addAttribute("cartItems", cartItems);
            model.addAttribute("selectedCustomer", selectedCustomer);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("vipDiscountAmount", vipDiscountAmount);
            model.addAttribute("finalAmount", finalAmount);

            return "user/pos/payment";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi xử lý dữ liệu thanh toán: " + e.getMessage());
            return "user/pos/interface";
        }
    }

    // ==================== HELPER METHODS FOR DATE PARSING ====================

    /**
     * Parse date of birth from various formats
     */
    private LocalDate parseDateOfBirth(String dobStr) {
        if (dobStr == null || dobStr.trim().isEmpty()) {
            return null;
        }
        
        dobStr = dobStr.trim();
        
        try {
            // Format dd/MM/yyyy (from frontend)
            if (dobStr.matches("\\d{2}/\\d{2}/\\d{4}")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate parsed = LocalDate.parse(dobStr, formatter);
                System.out.println("✅ Parsed dd/MM/yyyy: " + dobStr + " -> " + parsed);
                return parsed;
            }
            
            // Format yyyy-MM-dd (ISO format)
            if (dobStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate parsed = LocalDate.parse(dobStr);
                System.out.println("✅ Parsed yyyy-MM-dd: " + dobStr + " -> " + parsed);
                return parsed;
            }
            
            // Format dd-MM-yyyy
            if (dobStr.matches("\\d{2}-\\d{2}-\\d{4}")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                LocalDate parsed = LocalDate.parse(dobStr, formatter);
                System.out.println("✅ Parsed dd-MM-yyyy: " + dobStr + " -> " + parsed);
                return parsed;
            }
            
            // Fallback: try ISO format
            LocalDate parsed = LocalDate.parse(dobStr);
            System.out.println("✅ Parsed ISO fallback: " + dobStr + " -> " + parsed);
            return parsed;
            
        } catch (DateTimeParseException e) {
            System.err.println("❌ Cannot parse date: " + dobStr + " - " + e.getMessage());
            System.err.println("❌ Supported formats: dd/MM/yyyy, yyyy-MM-dd, dd-MM-yyyy");
            throw new RuntimeException("Invalid date format: " + dobStr + ". Expected formats: dd/MM/yyyy, yyyy-MM-dd, dd-MM-yyyy", e);
        }
    }

    private Customer createCustomerFromFormData(Map<String, Object> customerData) {
        Customer customer = new Customer();
        
        // Xử lý ID
        if (customerData.get("id") != null && !customerData.get("id").toString().isEmpty() && !"null".equals(customerData.get("id").toString())) {
            try {
                customer.setId(Long.valueOf(customerData.get("id").toString()));
            } catch (NumberFormatException e) {
                customer.setId(null);
            }
        }
        
        // Thông tin cơ bản
        customer.setName(getStringValue(customerData, "name"));
        customer.setPhone(getStringValue(customerData, "phone"));
        customer.setEmail(getStringValue(customerData, "email"));
        
        // Địa chỉ
        customer.setAddress(getStringValue(customerData, "address"));
        customer.setWard(getStringValue(customerData, "ward"));
        customer.setDistrict(getStringValue(customerData, "district"));
        customer.setProvince(getStringValue(customerData, "province"));
        
        // XỬ LÝ NGÀY SINH VỚI PARSE AN TOÀN
        String dobStr = getStringValue(customerData, "dateOfBirth");
        if (dobStr != null && !dobStr.trim().isEmpty()) {
            try {
                LocalDate dateOfBirth = parseDateOfBirth(dobStr);
                customer.setDateOfBirth(dateOfBirth);
                System.out.println("✅ Set date of birth for customer: " + dobStr + " -> " + dateOfBirth);
            } catch (Exception e) {
                System.err.println("❌ Error parsing date of birth in createCustomerFromFormData: " + dobStr + " - " + e.getMessage());
                customer.setDateOfBirth(null);
            }
        } else {
            System.out.println("⚠️ Date of birth is empty or null");
            customer.setDateOfBirth(null);
        }
        
        // Thông tin VIP
        customer.setVip(getBooleanValue(customerData, "vip", false));
        customer.setPendingVip(getBooleanValue(customerData, "pendingVip", false));
        
        // VIP discount percent
        try {
            double vipDiscount = getDoubleValue(customerData, "vipDiscountPercent", 0.0);
            customer.setVipDiscountPercent(vipDiscount);
        } catch (Exception e) {
            customer.setVipDiscountPercent(0.0);
        }
        
        return customer;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null || "null".equals(value.toString()) || value.toString().trim().isEmpty()) {
            return null;
        }
        return value.toString().trim();
    }

    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        try {
            Object value = map.get(key);
            if (value == null) return defaultValue;
            return Boolean.parseBoolean(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        try {
            Object value = map.get(key);
            if (value == null) return defaultValue;
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @GetMapping("/customer/{id}")
    @ResponseBody
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        try {
            Optional<Customer> customer = customerService.getCustomerById(id);
            if (customer.isPresent()) {
                Customer c = customer.get();
                
                System.out.println("=== API GET CUSTOMER BY ID ===");
                System.out.println("ID: " + c.getId());
                System.out.println("Name: " + c.getName());
                System.out.println("Phone: " + c.getPhone());
                System.out.println("Email: " + c.getEmail());
                System.out.println("Date of Birth: " + c.getDateOfBirth());
                System.out.println("VIP: " + c.isVip());
                System.out.println("===============================");
                
                return ResponseEntity.ok(c);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

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
            
            // THÊM THÔNG TIN THU NGÂN
            data.put("cashierName", order.getCreatedBy()); // Trả về username
            data.put("createdBy", order.getCreatedBy()); // Backup field
            
            if (order.getCustomer() != null) {
                Map<String, Object> customerMap = new HashMap<>();
                customerMap.put("name", order.getCustomer().getName());
                customerMap.put("phone", order.getCustomer().getPhone());
                customerMap.put("email", order.getCustomer().getEmail());
                customerMap.put("address", order.getCustomer().getAddress());
                customerMap.put("ward", order.getCustomer().getWard());
                customerMap.put("district", order.getCustomer().getDistrict());
                customerMap.put("province", order.getCustomer().getProvince());
                customerMap.put("dateOfBirth", order.getCustomer().getDateOfBirth());
                customerMap.put("vip", order.getCustomer().isVip());
                customerMap.put("pendingVip", order.getCustomer().getPendingVip()); // Thêm pendingVip
                data.put("customer", customerMap);
            } else {
                Map<String, Object> customerMap = new HashMap<>();
                customerMap.put("name", "Khách lẻ");
                customerMap.put("phone", null);
                customerMap.put("address", null);
                customerMap.put("ward", null);
                customerMap.put("district", null);
                customerMap.put("province", null);
                customerMap.put("dateOfBirth", null);
                customerMap.put("vip", false);
                customerMap.put("pendingVip", false); // Thêm pendingVip
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
            data.put("paymentMethod", order.getPaymentMethod());
            
            return Map.of("success", true, "data", data);
        } else {
            return Map.of("success", false, "message", orderResult.get("message"));
        }
    } catch (Exception e) {
        return Map.of("success", false, "message", e.getMessage());
    }
}
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

    // ==================== ORDER HISTORY ====================

@GetMapping("/history")
public String userOrderHistory(
        @RequestParam(required = false) String date,
        @RequestParam(required = false) String customerName,
        @RequestParam(required = false) String cashier,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        Model model,
        HttpSession session) {
    String username = (String) session.getAttribute("username");
    if (username == null) {
        return "redirect:/login";
    }

    try {
        boolean hasFilter = (date != null && !date.isEmpty())
                || (customerName != null && !customerName.trim().isEmpty())
                || (cashier != null && !cashier.trim().isEmpty());

        List<Order> filteredOrders;
        int totalElements;
        int totalPages;
        List<Order> pageOrders;

        if (!hasFilter) {
            // Không filter: phân trang trực tiếp từ DB
            Page<Order> ordersPage = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
            filteredOrders = ordersPage.getContent();
            totalElements = (int) ordersPage.getTotalElements();
            totalPages = ordersPage.getTotalPages();
            pageOrders = filteredOrders;
        } else {
            // Có filter: lấy toàn bộ, lọc rồi phân trang thủ công
            List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            filteredOrders = allOrders;
            if (date != null && !date.isEmpty()) {
                filteredOrders = filteredOrders.stream()
                        .filter(order -> order.getCreatedAt() != null &&
                                order.getCreatedAt().toLocalDate().toString().equals(date))
                        .toList();
            }
            if (cashier != null && !cashier.trim().isEmpty()) {
                String searchCashier = cashier.trim().toLowerCase();
                filteredOrders = filteredOrders.stream()
                        .filter(order -> order.getCreatedBy() != null &&
                                order.getCreatedBy().toLowerCase().contains(searchCashier))
                        .toList();
            }
            if (customerName != null && !customerName.trim().isEmpty()) {
                String searchName = customerName.trim().toLowerCase();
                filteredOrders = filteredOrders.stream()
                        .filter(order -> order.getCustomer() != null &&
                                order.getCustomer().getName() != null &&
                                order.getCustomer().getName().trim().toLowerCase().contains(searchName))
                        .toList();
            }
            totalElements = filteredOrders.size();
            totalPages = (int) Math.ceil((double) totalElements / size);
            if (page >= totalPages && totalPages > 0) page = totalPages - 1;
            if (page < 0) page = 0;
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, totalElements);
            pageOrders = (startIndex < totalElements) ? filteredOrders.subList(startIndex, endIndex) : new ArrayList<>();
        }

        model.addAttribute("orders", pageOrders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        model.addAttribute("pageSize", size);
        model.addAttribute("staffName", username);

        // Giữ lại parameters
        model.addAttribute("date", date);
        model.addAttribute("customerName", customerName);
        model.addAttribute("cashier", cashier);

        return "user/pos/history";

    } catch (Exception e) {
        model.addAttribute("error", "Có lỗi xảy ra khi tải lịch sử đơn hàng: " + e.getMessage());
        return "user/pos/history";
    }
}

@GetMapping("/history/{id}")
public String viewOrderDetail(@PathVariable Long id, Model model, HttpSession session) {
    String username = (String) session.getAttribute("username");
    if (username == null) {
        return "redirect:/login";
    }
    
    try {
        Map<String, Object> orderResult = posService.getOrderById(id);
        
        if (orderResult.get("success") != null && (Boolean) orderResult.get("success")) {
            Order order = (Order) orderResult.get("order");
            
            // Thêm thông tin thu ngân (cashier name)
            model.addAttribute("order", order);
            model.addAttribute("cashierName", username);
            
            return "user/pos/historyoder";
        } else {
            model.addAttribute("error", "Không tìm thấy hóa đơn với ID: " + id);
            return "redirect:/user/pos/history";
        }
        
    } catch (Exception e) {
        System.err.println("❌ Lỗi khi tải chi tiết đơn hàng ID " + id + ": " + e.getMessage());
        e.printStackTrace();
        model.addAttribute("error", "Có lỗi xảy ra khi tải chi tiết hóa đơn: " + e.getMessage());
        return "redirect:/user/pos/history";
    }
}

// THÊM METHOD ĐỂ HỖ TRỢ BACKWARD COMPATIBILITY (nếu cần)
@GetMapping("/historyoder")
public String viewOrderDetailLegacy(@RequestParam("id") Long id, Model model, HttpSession session) {
    // Redirect to new endpoint
    return "redirect:/user/pos/history/" + id;
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
        customerInfo.setEmail(customer.getEmail());
        
        customerInfo.setAddress(customer.getAddress());
        customerInfo.setWard(customer.getWard());
        customerInfo.setDistrict(customer.getDistrict());
        customerInfo.setProvince(customer.getProvince());
        customerInfo.setDateOfBirth(customer.getDateOfBirth()); // ĐẢM BẢO NGÀY SINH ĐƯỢC TRUYỀN
        customerInfo.setVip(customer.isVip());
        customerInfo.setPendingVip(customer.getPendingVip() != null ? customer.getPendingVip() : false);
        customerInfo.setVipDiscountPercent(customer.getVipDiscountPercent() != null ? customer.getVipDiscountPercent().intValue() : 0);
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
    
    // ==================== PROMOTION APIs ====================
    
    /**
     * API: Lấy thông tin sản phẩm kèm khuyến mại
     */
    @GetMapping("/api/products/{id}/with-promotion")
    @ResponseBody
    public ResponseEntity<?> getProductWithPromotion(@PathVariable Long id) {
        try {
            Optional<Product> productOpt = productService.getProductById(id);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                Map<String, Object> discountInfo = promotionService.getDiscountInfo(product);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("product", product);
                response.put("discountInfo", discountInfo);
                
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * API: Lấy danh sách khuyến mại đang hoạt động
     */
    @GetMapping("/api/active-promotions")
    @ResponseBody
    public ResponseEntity<?> getActivePromotions() {
        try {
            List<Promotion> promotions = promotionService.getActivePromotions();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("promotions", promotions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * API: Lấy tất cả sản phẩm với thông tin khuyến mại
     */
    @GetMapping("/api/products-with-promotions")
    @ResponseBody
    public ResponseEntity<?> getAllProductsWithPromotions() {
        try {
            List<Product> products = productService.getAllProducts();
            List<Map<String, Object>> productsWithPromotions = new ArrayList<>();
            
            for (Product product : products) {
                Map<String, Object> productData = new HashMap<>();
                productData.put("id", product.getId());
                productData.put("name", product.getName());
                productData.put("category", product.getCategory());
                productData.put("quantity", product.getQuantity());
                
                // Lấy thông tin khuyến mại
                Map<String, Object> discountInfo = promotionService.getDiscountInfo(product);
                productData.put("originalPrice", discountInfo.get("originalPrice"));
                productData.put("discountPercent", discountInfo.get("discountPercent"));
                productData.put("discountedPrice", discountInfo.get("discountedPrice"));
                productData.put("hasDiscount", discountInfo.get("hasDiscount"));
                productData.put("discountSource", discountInfo.get("discountSource"));
                
                productsWithPromotions.add(productData);
            }
            
            return ResponseEntity.ok(productsWithPromotions);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}