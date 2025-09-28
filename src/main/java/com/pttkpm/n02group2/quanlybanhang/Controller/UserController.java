package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.User;
import com.pttkpm.n02group2.quanlybanhang.Model.Product;
import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Service.ProductService;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import com.pttkpm.n02group2.quanlybanhang.Service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private CustomerService customerService;

    // Kiểm tra đăng nhập
    private boolean checkUserLogin(HttpSession session) {
        String username = (String) session.getAttribute("username");
        String userRole = (String) session.getAttribute("userRole");
        return username != null && "USER".equals(userRole);
    }

    // ==================== ROUTING - REDIRECT TO POS ====================
    
    // Redirect từ root user URL về POSController
    @GetMapping({"", "/"})
    public String userHome(HttpSession session) {
        if (!checkUserLogin(session)) {
            return "redirect:/login";
        }
        // Redirect về POSController
        return "redirect:/user/pos";
    }

    // Redirect dashboard về POSController
    @GetMapping("/dashboard")
    public String userDashboard(HttpSession session) {
        if (!checkUserLogin(session)) {
            return "redirect:/login";
        }
        // Redirect về POSController
        return "redirect:/user/pos";
    }

    // ==================== API FOR POS CONTROLLER ====================
    
    // API: Tìm khách hàng cho POS
    @GetMapping("/api/customers/search")
    @ResponseBody
    public ResponseEntity<List<Customer>> searchCustomers(@RequestParam String query) {
        try {
            List<Customer> customers = customerService.searchCustomersForPOS(query);
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // API: Tìm khách hàng theo số điện thoại
   @GetMapping("/api/customers/phone/{phone}")
    @ResponseBody
    public ResponseEntity<Customer> findCustomerByPhone(@PathVariable String phone) {
        try {
            Optional<Customer> customer = Optional.ofNullable(customerService.findByPhone(phone));
            if (customer.isPresent()) {
                return ResponseEntity.ok(customer.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }


    // ...existing code...

    // API: Lưu khách hàng mới từ POS
    @PostMapping("/api/customers")
    @ResponseBody 
    public ResponseEntity<Map<String, Object>> createCustomer(@RequestBody Customer customer) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate required fields - chỉ yêu cầu tên
            if (customer.getName() == null || customer.getName().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Tên khách hàng không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Phone không bắt buộc nữa, nhưng nếu có thì check unique
            if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
                if (customerService.existsByPhone(customer.getPhone())) {
                    response.put("success", false);
                    response.put("message", "Số điện thoại đã tồn tại");
                    return ResponseEntity.badRequest().body(response);
                }
            }
            
            Customer savedCustomer = customerService.createCustomer(customer);
            response.put("success", true);
            response.put("message", "Tạo khách hàng thành công");
            response.put("customer", savedCustomer);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

// ...existing code...
    // API: Cập nhật khách hàng
    @PutMapping("/api/customers/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateCustomer(@PathVariable Long id, @RequestBody Customer customer) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Customer updatedCustomer = customerService.updateCustomer(id, customer);
            response.put("success", true);
            response.put("message", "Cập nhật khách hàng thành công");
            response.put("customer", updatedCustomer);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== TRANSACTION HISTORY ====================
    
    @GetMapping("/view-transactions")
public String viewTransactions(Model model, HttpSession session) {
    try {
        if (!checkUserLogin(session)) {
            return "redirect:/login";
        }

            // Load transaction history
            List<Order> transactions = orderService.getAllOrders(); // Lấy tất cả orders
            model.addAttribute("transactions", transactions);
            return "user/transactions";
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "user/transactions";
        }
    }

    // Xem chi tiết giao dịch
    @GetMapping("/transactions/{id}")
    public String viewTransactionDetail(@PathVariable Long id, Model model, HttpSession session) {
        if (!checkUserLogin(session)) {
            return "redirect:/login";
        }

        try {
            Optional<Order> orderOpt = orderService.getOrderById(id);
            if (orderOpt.isPresent()) {
                model.addAttribute("transaction", orderOpt.get());
            } else {
                model.addAttribute("error", "Không tìm thấy giao dịch!");
                return "redirect:/user/transactions";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi khi tải thông tin giao dịch: " + e.getMessage());
        }

        model.addAttribute("username", session.getAttribute("username"));
        return "user/transaction-detail";
    }

    // ==================== PRODUCT API FOR POS ====================
    
    // API: Tìm kiếm sản phẩm cho POS
    @GetMapping("/api/products/search")
    @ResponseBody
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String query) {
        try {
            List<Product> products = productService.searchProductsByKeyword(query);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // API: Lấy thông tin sản phẩm theo ID
    @GetMapping("/api/products/{id}")
    @ResponseBody
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        try {
            Optional<Product> product = productService.getProductById(id);
            if (product.isPresent()) {
                return ResponseEntity.ok(product.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== ORDER PROCESSING ====================
    
    // API: Tạo đơn hàng từ POS
    @PostMapping("/api/orders")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> orderData, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate session
            if (!checkUserLogin(session)) {
                response.put("success", false);
                response.put("message", "Phiên đăng nhập hết hạn");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // Process order creation
            // TODO: Implement order creation logic
            response.put("success", true);
            response.put("message", "Tạo đơn hàng thành công");
            response.put("orderId", System.currentTimeMillis()); // Temporary order ID
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== UTILITIES ====================
    
    // Trang About/Giới thiệu
    @GetMapping("/about")
    public String about(Model model, HttpSession session) {
        if (!checkUserLogin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("username", session.getAttribute("username"));
        return "user/about";
    }

    // Trang Contact/Liên hệ
    @GetMapping("/contact")
    public String contact(Model model, HttpSession session) {
        if (!checkUserLogin(session)) {
            return "redirect:/login";
        }
        model.addAttribute("username", session.getAttribute("username"));
        return "user/contact";
    }

    // API: Health check
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "User Service");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}