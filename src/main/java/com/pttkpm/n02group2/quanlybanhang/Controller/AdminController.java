package com.pttkpm.n02group2.quanlybanhang.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // ==================== DASHBOARD ====================
    
    // Dashboard Admin  
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        // Kiểm tra quyền admin
        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(role)) {
            return "redirect:/user/dashboard";
        }
        
        // Thống kê cơ bản
        
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
    
    // Quản lý đơn hàng
    @GetMapping("/orders")
    public String adminOrders(Model model, HttpSession session) {
        // Kiểm tra quyền admin
        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(role)) {
            return "redirect:/user/dashboard";
        }
        
        return "admin/orders";
    }

    // ==================== INVENTORY ====================
    
    // Quản lý kho hàng
    @GetMapping("/inventory")
    public String adminInventory(Model model, HttpSession session) {
        // Kiểm tra quyền admin
        String role = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(role)) {
            return "redirect:/user/dashboard";
        }
        
        
        return "admin/inventory";
    }

    // ==================== SHARED ENDPOINTS ====================
    
    // Redirect từ /admin về dashboard
    @GetMapping("")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}