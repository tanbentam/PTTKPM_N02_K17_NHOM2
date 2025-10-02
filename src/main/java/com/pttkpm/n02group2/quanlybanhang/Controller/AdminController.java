package com.pttkpm.n02group2.quanlybanhang.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Model.User;
import com.pttkpm.n02group2.quanlybanhang.Service.CustomerService;

import java.util.List;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private CustomerService customerService;

    // ==================== DASHBOARD ====================
    @GetMapping("/dashboard")
public String adminDashboard(Model model, HttpSession session) {
    User user = (User) session.getAttribute("user");
    if (user == null || user.getRole() != User.Role.ADMIN) {
        return "redirect:/login";
    }

    // Lấy username để hiển thị
    model.addAttribute("username", user.getUsername());
    
    // Thống kê (tạm thời hard-code cho đến khi có service thực tế)
    long totalProducts = 11;
    long totalCustomers = 52;  // Hard-code thay vì customerService.countAll()
    long totalOrders = 107;
    String totalRevenue = "106,002,000";

    model.addAttribute("totalProducts", totalProducts);
    model.addAttribute("totalCustomers", totalCustomers);
    model.addAttribute("totalOrders", totalOrders);
    model.addAttribute("totalRevenue", totalRevenue);

    return "admin/dashboard";
}

    // ==================== ORDERS ====================
    @GetMapping("/orders")
    public String adminOrders(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }
        return "admin/orders";
    }

    // ==================== INVENTORY ====================
    @GetMapping("/inventory")
    public String adminInventory(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }
        return "admin/inventory";
    }

    // ==================== SHARED ENDPOINTS ====================
    @GetMapping("")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}