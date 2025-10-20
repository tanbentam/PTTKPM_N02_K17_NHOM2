package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.User;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@Controller
@RequestMapping("/admin/revenue")
public class RevenueController {
    @Autowired
    private OrderService orderService;

    @GetMapping("")
    public String revenuePage(
            @RequestParam(value = "summaryType", required = false, defaultValue = "all") String summaryType,
            @RequestParam(value = "selectedDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDate,
            @RequestParam(value = "selectedMonth", required = false) Integer selectedMonth,
            @RequestParam(value = "selectedMonthYear", required = false) Integer selectedMonthYear,
            @RequestParam(value = "selectedYear", required = false) Integer selectedYear,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }

        LocalDate now = LocalDate.now();

        // Set giá trị mặc định nếu chưa có
        if (selectedDate == null && "day".equals(summaryType)) {
            selectedDate = now;
        }
        if (selectedMonth == null && "month".equals(summaryType)) {
            selectedMonth = now.getMonthValue();
        }
        if (selectedMonthYear == null && "month".equals(summaryType)) {
            selectedMonthYear = now.getYear();
        }
        if (selectedYear == null && "year".equals(summaryType)) {
            selectedYear = now.getYear();
        }
        if (from == null && "range".equals(summaryType)) {
            from = now;
        }
        if (to == null && "range".equals(summaryType)) {
            to = now;
        }

        // Xử lý logic theo summaryType
        switch (summaryType) {
            case "day":
                if (selectedDate != null) {
                    from = selectedDate;
                    to = selectedDate;
                }
                break;
            case "month":
                if (selectedMonth != null && selectedMonthYear != null) {
                    from = LocalDate.of(selectedMonthYear, selectedMonth, 1);
                    to = from.with(TemporalAdjusters.lastDayOfMonth());
                }
                break;
            case "year":
                if (selectedYear != null) {
                    from = LocalDate.of(selectedYear, 1, 1);
                    to = LocalDate.of(selectedYear, 12, 31);
                }
                break;
            case "range":
                // from và to đã được set từ parameter hoặc mặc định
                break;
            default: // "all"
                from = null;
                to = null;
                break;
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage;
        if (from != null && to != null) {
            orderPage = orderService.findOrdersByDateRange(from, to, pageable);
        } else {
            orderPage = orderService.findAllOrders(pageable);
        }

        // Tổng doanh thu của tất cả hóa đơn (không chỉ trang hiện tại)
        double totalRevenue;
        if (from != null && to != null) {
            totalRevenue = orderService.findOrdersByDateRange(from, to)
                    .stream()
                    .mapToDouble(order -> order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount())
                    .sum();
        } else {
            totalRevenue = orderService.findAllOrders()
                    .stream()
                    .mapToDouble(order -> order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount())
                    .sum();
        }

        // Thêm các attribute vào model
        model.addAttribute("orders", orderPage.getContent());
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("summaryType", summaryType);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedMonth", selectedMonth);
        model.addAttribute("selectedMonthYear", selectedMonthYear);
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("totalRevenue", String.format("%,.0f", totalRevenue));
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        return "admin/revenue/index";
    }
}