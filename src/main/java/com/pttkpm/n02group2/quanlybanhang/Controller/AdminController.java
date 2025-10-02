package com.pttkpm.n02group2.quanlybanhang.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
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
        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(role)) {
            return "redirect:/user/dashboard";
        }

        long totalProducts = 0;
        long totalCustomers = 0;
        long totalOrders = 0;
        long totalInventories = totalProducts;

        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalInventories", totalInventories);
        model.addAttribute("isAdmin", true);

        return "shared/dashboard";
    }

    
    // ==================== ORDERS ====================
    @GetMapping("/orders")
    public String adminOrders(Model model, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(role)) {
            return "redirect:/user/dashboard";
        }
        return "admin/orders";
    }

    // ==================== INVENTORY ====================
    @GetMapping("/inventory")
    public String adminInventory(Model model, HttpSession session) {
        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(role)) {
            return "redirect:/user/dashboard";
        }
        return "admin/inventory";
    }

    // ==================== SHARED ENDPOINTS ====================
    @GetMapping("")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}