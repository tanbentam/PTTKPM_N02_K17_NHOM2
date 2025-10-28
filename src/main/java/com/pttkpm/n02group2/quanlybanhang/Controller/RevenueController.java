package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.User;
import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Model.OrderItem;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        // Tạo danh sách năm từ 2000 đến năm hiện tại (hoặc lùi về 20 năm)
        int currentYear = now.getYear();
        List<Integer> availableYears = IntStream.rangeClosed(currentYear - 20, currentYear)
            .boxed().sorted((a, b) -> b.compareTo(a)).collect(Collectors.toList());
        model.addAttribute("availableYears", availableYears);

        // Tạo danh sách tháng (1-12)
        List<Integer> availableMonths = IntStream.rangeClosed(1, 12)
            .boxed().collect(Collectors.toList());
        model.addAttribute("availableMonths", availableMonths);

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

        // Tổng doanh thu của tất cả hóa đơn (không chỉ trang hiện tại) - ƯU TIÊN totalAmount (đã cập nhật sau đổi trả)
          double totalRevenue;
        if (from != null && to != null) {
            totalRevenue = orderService.findOrdersByDateRange(from, to)
                    .stream()
                    .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0)
                    .sum();
        } else {
            totalRevenue = orderService.findAllOrders()
                    .stream()
                    .mapToDouble(order -> order.getTotalAmount() != null ? order.getTotalAmount() : 0)
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

    // Trang xem chi tiết hóa đơn doanh thu
    // ...existing code...
    // Trang xem chi tiết hóa đơn doanh thu
    @GetMapping("/view/{id}")
    public String viewOrder(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }

        Order order = orderService.findById(id);
        if (order == null) {
            return "redirect:/admin/revenue";
        }
        Customer customer = order.getCustomer();
        List<OrderItem> orderItems = order.getItems();

        // Lấy danh sách sản phẩm đổi/trả và sản phẩm nhận sau đổi (nếu có)
        List<OrderItem> returnItems = order.getReturnItems() != null ? order.getReturnItems() : List.of();
        List<OrderItem> receiveItems = order.getReceiveItems() != null ? order.getReceiveItems() : List.of();

        double totalReturnAmount = (returnItems != null && !returnItems.isEmpty())
            ? returnItems.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum()
            : 0.0;
        double totalReceiveAmount = (receiveItems != null && !receiveItems.isEmpty())
            ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum()
            : 0.0;

        double originalTotal = order.getTotalAmount() != null ? order.getTotalAmount() : 0;
        double actualDiscount = order.getVipDiscountAmount() != null ? order.getVipDiscountAmount() : 0;
        double actualPaidAmount = order.getFinalAmount() != null ? order.getFinalAmount() : originalTotal - actualDiscount;
        boolean hasDiscount = actualDiscount > 0;

        // Chuyển đổi phương thức thanh toán sang tiếng Việt
        String paymentMethodVN;
        String method = order.getPaymentMethod();
        if ("CASH".equalsIgnoreCase(method)) {
            paymentMethodVN = "Tiền mặt";
        } else if ("TRANSFER".equalsIgnoreCase(method)) {
            paymentMethodVN = "Chuyển khoản";
        } else if ("CARD".equalsIgnoreCase(method)) {
            paymentMethodVN = "Thẻ";
        } else {
            paymentMethodVN = "Chưa xác định";
        }

        model.addAttribute("order", order);
        model.addAttribute("customer", customer);
        model.addAttribute("orderItems", orderItems);
        model.addAttribute("returnItems", returnItems);
        model.addAttribute("receiveItems", receiveItems);
        model.addAttribute("totalReturnAmount", totalReturnAmount);
        model.addAttribute("totalReceiveAmount", totalReceiveAmount);
        model.addAttribute("originalTotal", originalTotal);
        model.addAttribute("actualDiscount", actualDiscount);
        model.addAttribute("actualPaidAmount", actualPaidAmount);
        model.addAttribute("hasDiscount", hasDiscount);
        model.addAttribute("paymentMethodVN", paymentMethodVN);

        return "admin/revenue/view";
    }
// ...existing code...
}