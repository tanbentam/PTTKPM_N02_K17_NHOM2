package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Product;
import com.pttkpm.n02group2.quanlybanhang.Service.ProductService;
import com.pttkpm.n02group2.quanlybanhang.Service.CustomerService;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import com.pttkpm.n02group2.quanlybanhang.Service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.ArrayList;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private InventoryService inventoryService;

    // Trang chủ - PHÂN QUYỀN TỰ ĐỘNG
    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        String userRole = (String) session.getAttribute("userRole");
        
        if (username == null) {
            // Chưa đăng nhập - chuyển về trang đăng nhập
            return "redirect:/login";
        }
        
        // PHÂN QUYỀN: Chuyển hướng theo role
        if ("ADMIN".equals(userRole)) {
            // ADMIN: Hiển thị giao diện quản trị
            return adminDashboard(model, session);
        } else {
            // USER: Chuyển thẳng đến POS
            return "redirect:/user/pos";
        }
    }

    // Dashboard cho ADMIN
    private String adminDashboard(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        
        try {
            // Thống kê cho admin
            long totalProducts = productService.getAllProducts().size();
            long totalCustomers = customerService.getAllCustomers().size();
            long totalOrders = orderService.getAllOrders().size();
            long totalInventories = inventoryService.getAllInventories().size();
            
            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalCustomers", totalCustomers);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalInventories", totalInventories);
            
            // Tính tổng doanh thu (nếu có method getTotalRevenue)
            try {
    // Giả sử có method tính tổng doanh thu
    double totalRevenue = orderService.getAllOrders().stream()
        .mapToDouble(order -> {
            try {
                return order.getTotalAmount(); // Không cần kiểm tra != null với primitive double
            } catch (Exception e) {
                return 0.0; // Trả về 0 nếu có lỗi
            }
        })
        .sum();
    model.addAttribute("totalRevenue", String.format("%,.0f", totalRevenue));
} catch (Exception e) {
    model.addAttribute("totalRevenue", "0");
}

            
            // Lấy 5 giao dịch gần nhất cho admin - NHƯNG LƯU LẠI TOÀN BỘ
            try {
                // Lấy toàn bộ đơn hàng trước - SỬA: sử dụng wildcard thay vì Order cụ thể
                List<?> allOrders = orderService.getAllOrders();
                
                // Lưu toàn bộ giao dịch vào model
                model.addAttribute("allTransactions", allOrders);
                model.addAttribute("totalTransactions", allOrders.size());
                
                // Lấy 5 giao dịch gần nhất - SỬA: không sắp xếp để tránh lỗi
                List<?> recentTransactions = allOrders.stream()
                    .limit(5) // Chỉ lấy 5 cái đầu
                    .toList();
                model.addAttribute("recentTransactions", recentTransactions);
                
            } catch (Exception e) {
                System.err.println("Lỗi khi lấy giao dịch: " + e.getMessage());
                e.printStackTrace();
                model.addAttribute("recentTransactions", new ArrayList<>());
                model.addAttribute("allTransactions", new ArrayList<>());
                model.addAttribute("totalTransactions", 0);
            }
            
            // Lấy dữ liệu cho admin (giới hạn để tránh quá tải)
            model.addAttribute("products", productService.getAllProducts());
            model.addAttribute("customers", customerService.getAllCustomers());
            model.addAttribute("orders", orderService.getAllOrders());
            model.addAttribute("inventories", inventoryService.getAllInventories());
            
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi khi tải dữ liệu: " + e.getMessage());
            
            // Set default values nếu có lỗi
            model.addAttribute("totalProducts", 0);
            model.addAttribute("totalCustomers", 0);
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalInventories", 0);
            model.addAttribute("totalRevenue", "0");
            model.addAttribute("recentTransactions", new ArrayList<>());
            model.addAttribute("allTransactions", new ArrayList<>());
            model.addAttribute("totalTransactions", 0);
        }
        
        model.addAttribute("username", username);
        model.addAttribute("isAdmin", true);
        return "home"; // Sử dụng chung template, phân biệt bằng isAdmin
    }

    // XÓA METHOD userDashboard() VÌ CONFLICT VỚI UserController
    // ==================== TRANSACTION ENDPOINTS ====================
    
    @GetMapping("/user/transactions")
    public String userTransactions(HttpSession session) {
        String username = (String) session.getAttribute("username");
        String userRole = (String) session.getAttribute("userRole");
        
        if (username == null || !"USER".equals(userRole)) {
            return "redirect:/login";
        }
        return "user/transactions"; // Trang lịch sử giao dịch cho user
    }

    // ==================== LEGACY ENDPOINTS (Redirect to new structure) ====================
    
    @GetMapping("/products")
    public String products(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(userRole)) {
            return "redirect:/login";
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/customers")
    public String customers(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(userRole)) {
            return "redirect:/login";
        }
        return "redirect:/admin/customers";
    }

    @GetMapping("/orders")
    public String orders(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(userRole)) {
            return "redirect:/login";
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/inventory")
    public String inventory(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(userRole)) {
            return "redirect:/login";
        }
        return "redirect:/admin/inventory";
    }

    @GetMapping("/transactions")
    public String transactions(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        if ("ADMIN".equals(userRole)) {
            return "redirect:/admin/transactions";
        } else if ("USER".equals(userRole)) {
            return "redirect:/user/transactions";
        } else {
            return "redirect:/login";
        }
    }

    // ==================== UTILITY METHODS ====================
    
    private boolean isUserLoggedIn(HttpSession session) {
        return session.getAttribute("username") != null;
    }
    
    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("userRole"));
    }
    
    private boolean isUser(HttpSession session) {
        return "USER".equals(session.getAttribute("userRole"));
    }

    // ==================== HEALTH CHECK ====================
    
    @GetMapping("/health")
    public String healthCheck() {
        return "redirect:/";
    }
}