package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.User;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/revenue")
public class RevenueController {
    @Autowired
    private OrderService orderService;

    @GetMapping("")
    public String revenuePage(
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }

        if (from == null || to == null) {
            LocalDate now = LocalDate.now();
            from = now.withDayOfMonth(1);
            to = now;
        }

        List<Order> orders = orderService.findOrdersByDateRange(from, to);
        double totalRevenue = orders.stream()
                .mapToDouble(order -> order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount())
                .sum();

       
        model.addAttribute("orders", orders);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("totalRevenue", String.format("%,.0f", totalRevenue));
        return "admin/revenue/index";

    }
}