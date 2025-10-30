package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.OrderItem;
import com.pttkpm.n02group2.quanlybanhang.Model.ReturnRequestItem;
import com.pttkpm.n02group2.quanlybanhang.Repository.ReturnRequestItemRepository;
import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/user/pos")
public class UserRevenueController {
     @Autowired
    private ReturnRequestItemRepository returnRequestItemRepository; // <-- THÊM DÒNG NÀY

    @Autowired
    private OrderService orderService;

   // ...existing code...
@GetMapping("/revenue")
public String revenuePage(
        @RequestParam(value = "summaryType", required = false, defaultValue = "month") String summaryType,
        @RequestParam(value = "selectedDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate selectedDate,
        @RequestParam(value = "selectedMonth", required = false) Integer selectedMonth,
        @RequestParam(value = "selectedMonthYear", required = false) Integer selectedMonthYear,
        @RequestParam(value = "selectedYear", required = false) Integer selectedYear,
        @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(value = "page", required = false, defaultValue = "0") int page,
        @RequestParam(value = "size", required = false, defaultValue = "10") int size,
        Model model) {

    LocalDate now = LocalDate.now();

    // Mặc định hiển thị doanh số và đơn hàng của tháng thực tế
    if (selectedMonth == null && "month".equals(summaryType)) {
        selectedMonth = now.getMonthValue();
    }
    if (selectedMonthYear == null && "month".equals(summaryType)) {
        selectedMonthYear = now.getYear();
    }
    if (selectedDate == null && "day".equals(summaryType)) {
        selectedDate = now;
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

    // Tạo Pageable object
    Pageable pageable = PageRequest.of(page, size);
    Page<Order> orderPage;

    // Lấy dữ liệu đơn hàng theo khoảng thời gian
    if (from != null && to != null) {
        orderPage = orderService.findOrdersByDateRange(from, to, pageable);
    } else {
        orderPage = orderService.findAllOrders(pageable);
    }

    // Tính finalAmountAfterReturn cho các đơn RETURNED trong trang hiện tại (chuẩn logic)
    Map<Long, Double> finalAmountAfterReturnMap = new HashMap<>();
    for (Order order : orderPage.getContent()) {
        if (order.getStatus() != null && order.getStatus().name().equals("RETURNED")) {
            // Lấy danh sách sản phẩm đổi và nhận
            List<ReturnRequestItem> returnItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RETURN");
            List<ReturnRequestItem> receiveItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RECEIVE");

            // Tổng tiền sản phẩm đổi
            double totalReturnAmount = returnItems != null
                    ? returnItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                    : 0.0;
            // Tổng tiền sản phẩm nhận
            double totalReceiveAmount = receiveItems != null
                    ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                    : 0.0;

            // Tính tổng tiền hàng hóa gốc và giảm giá VIP
            List<OrderItem> orderItems = order.getItems();
            double baseTotal = orderItems != null
                    ? orderItems.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum()
                    : 0.0;
            double actualDiscount = order.getVipDiscountAmount() != null ? order.getVipDiscountAmount() : 0;
            boolean hasDiscount = actualDiscount > 0;
            double originalTotal = baseTotal - actualDiscount;

            // Tính giảm giá VIP cho sản phẩm nhận (nếu có)
            double receiveVipDiscount = 0.0;
            if (hasDiscount && baseTotal > 0 && receiveItems != null) {
                receiveVipDiscount = receiveItems.stream()
                        .mapToDouble(i -> i.getQuantity() * i.getUnitPrice() * (actualDiscount / baseTotal))
                        .sum();
            }

            // Số tiền thực tế sau đổi trả
            double finalAmountAfterReturn = originalTotal - totalReturnAmount + totalReceiveAmount - receiveVipDiscount;
            finalAmountAfterReturnMap.put(order.getId(), finalAmountAfterReturn);
        }
    }
    model.addAttribute("finalAmountAfterReturnMap", finalAmountAfterReturnMap);

    // Tính tổng doanh thu của tất cả hóa đơn (không chỉ trang hiện tại) - luôn tính đúng cho RETURNED
    double totalRevenue = 0;
    List<Order> allOrders;
    if (from != null && to != null) {
        allOrders = orderService.findOrdersByDateRange(from, to);
    } else {
        allOrders = orderService.findAllOrders();
    }
    for (Order order : allOrders) {
        if (order.getStatus() != null && order.getStatus().name().equals("RETURNED")) {
            List<ReturnRequestItem> returnItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RETURN");
            List<ReturnRequestItem> receiveItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RECEIVE");

            double totalReturnAmount = returnItems != null
                    ? returnItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                    : 0.0;
            double totalReceiveAmount = receiveItems != null
                    ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                    : 0.0;

            List<OrderItem> orderItems = order.getItems();
            double baseTotal = orderItems != null
                    ? orderItems.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum()
                    : 0.0;
            double actualDiscount = order.getVipDiscountAmount() != null ? order.getVipDiscountAmount() : 0;
            boolean hasDiscount = actualDiscount > 0;
            double originalTotal = baseTotal - actualDiscount;

            double receiveVipDiscount = 0.0;
            if (hasDiscount && baseTotal > 0 && receiveItems != null) {
                receiveVipDiscount = receiveItems.stream()
                        .mapToDouble(i -> i.getQuantity() * i.getUnitPrice() * (actualDiscount / baseTotal))
                        .sum();
            }

            double finalAmountAfterReturn = originalTotal - totalReturnAmount + totalReceiveAmount - receiveVipDiscount;
            totalRevenue += finalAmountAfterReturn;
        } else {
            totalRevenue += order.getFinalAmount() != null ? order.getFinalAmount() :
                            (order.getTotalAmount() != null ? order.getTotalAmount() : 0);
        }
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

    return "user/pos/revenue";
}

    // Trang xem chi tiết hóa đơn
@GetMapping("/bill/{id}")
public String viewOrder(@PathVariable Long id, Model model) {
    Order order = orderService.findById(id);
    if (order == null) {
        return "redirect:/user/pos/revenue";
    }

    Customer customer = order.getCustomer();
    List<OrderItem> orderItems = order.getItems();

    // Tính tổng tiền hàng hóa gốc (chưa trừ VIP)
    double baseTotal = orderItems != null
            ? orderItems.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum()
            : 0.0;
    double actualDiscount = order.getVipDiscountAmount() != null ? order.getVipDiscountAmount() : 0;
    boolean hasDiscount = actualDiscount > 0;
    double originalTotal = baseTotal - actualDiscount;

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
    model.addAttribute("baseTotal", baseTotal); // <-- Thêm dòng này
    model.addAttribute("originalTotal", originalTotal);
    model.addAttribute("actualDiscount", actualDiscount);
    model.addAttribute("hasDiscount", hasDiscount);
    model.addAttribute("paymentMethodVN", paymentMethodVN);

    // Nếu là hóa đơn đổi trả thì truyền thêm thông tin đổi trả và chuyển sang view riêng
    if (order.getStatus() != null && order.getStatus().name().equals("RETURNED")) {
        // Lấy thông tin sản phẩm đổi trả và nhận từ DB
        List<ReturnRequestItem> returnItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RETURN");
        List<ReturnRequestItem> receiveItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RECEIVE");

        double totalReturnAmount = returnItems != null
                ? returnItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                : 0.0;
        double totalReceiveAmount = receiveItems != null
                ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                : 0.0;

        // Tính giảm giá VIP cho sản phẩm nhận (nếu có)
        double receiveVipDiscount = 0.0;
        if (hasDiscount && baseTotal > 0 && receiveItems != null) {
            receiveVipDiscount = receiveItems.stream()
                    .mapToDouble(i -> i.getQuantity() * i.getUnitPrice() * (actualDiscount / baseTotal))
                    .sum();
        }

        double actualPaidAmount = originalTotal - totalReturnAmount + totalReceiveAmount - receiveVipDiscount;

        model.addAttribute("returnItems", returnItems);
        model.addAttribute("receiveItems", receiveItems);
        model.addAttribute("totalReturnAmount", totalReturnAmount);
        model.addAttribute("totalReceiveAmount", totalReceiveAmount);
        model.addAttribute("receiveVipDiscount", receiveVipDiscount);
        model.addAttribute("actualPaidAmount", actualPaidAmount);

        return "user/pos/bill_returned";
    } else {
        double actualPaidAmount = order.getFinalAmount() != null ? order.getFinalAmount() : originalTotal;
        model.addAttribute("actualPaidAmount", actualPaidAmount);
        return "user/pos/bill";
    }
}
// ...existing code...
}