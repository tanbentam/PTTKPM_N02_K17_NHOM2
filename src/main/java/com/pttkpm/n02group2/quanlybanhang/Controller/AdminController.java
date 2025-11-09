package com.pttkpm.n02group2.quanlybanhang.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.OrderItem;
import com.pttkpm.n02group2.quanlybanhang.Model.User;
import com.pttkpm.n02group2.quanlybanhang.Model.ReturnRequestItem;
import com.pttkpm.n02group2.quanlybanhang.Service.CustomerService;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import com.pttkpm.n02group2.quanlybanhang.Service.ProductService;
import com.pttkpm.n02group2.quanlybanhang.Repository.ReturnRequestItemRepository;

import java.util.List;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private CustomerService customerService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductService productService;
    @Autowired
    private ReturnRequestItemRepository returnRequestItemRepository;

    // ==================== DASHBOARD ====================
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }

        try {
            model.addAttribute("username", user.getUsername());

            long totalProducts = productService.countAll();
            long totalCustomers = customerService.countAll();
            long totalOrders = orderService.countAll();

            // Tính tổng doanh thu đúng logic đổi trả và giảm giá VIP
            double totalRevenue = 0;
            List<Order> allOrders = orderService.findAllOrders();
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

            model.addAttribute("totalProducts", totalProducts);
            model.addAttribute("totalCustomers", totalCustomers);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalRevenue", (long) totalRevenue);

            return "admin/dashboard";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("totalProducts", 0);
            model.addAttribute("totalCustomers", 0);
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalRevenue", 0);
            model.addAttribute("error", "Không thể tải thống kê: " + e.getMessage());
            return "admin/dashboard";
        }
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

    @GetMapping("/orders/{id}")
    public String viewOrder(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }

        try {
            Order order = orderService.findById(id);
            if (order == null) {
                return "redirect:/admin/customers";
            }

            Customer customer = order.getCustomer();

            // Lấy danh sách sản phẩm đã mua
            List<OrderItem> orderItems = order.getItems();

            // Tính tổng tiền gốc từ các sản phẩm
            double originalTotal = orderItems.stream()
                .mapToDouble(item -> item.getQuantity() * item.getPrice())
                .sum();

            // Số tiền thực tế khách đã thanh toán (từ database)
            double actualPaidAmount = order.getFinalAmount() != null ? order.getFinalAmount() : order.getTotalAmount();

            // Tính số tiền giảm giá thực tế (nếu có)
            double actualDiscount = originalTotal - actualPaidAmount;

            // Chỉ hiển thị giảm giá nếu thực sự có giảm giá
            boolean hasDiscount = actualDiscount > 0;

            model.addAttribute("order", order);
            model.addAttribute("customer", customer);
            model.addAttribute("orderItems", orderItems);
            model.addAttribute("originalTotal", originalTotal);  // Tổng tiền gốc
            model.addAttribute("actualDiscount", hasDiscount ? actualDiscount : 0);  // Giảm giá thực tế
            model.addAttribute("actualPaidAmount", actualPaidAmount);  // Số tiền thực tế đã thanh toán
            model.addAttribute("hasDiscount", hasDiscount);  // Có giảm giá hay không

            return "admin/customers/bill";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    // ==================== CUSTOMERS ====================
    @GetMapping("/customers/{id}/delete")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }
        try {
            customerService.deleteCustomerAndOrders(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa khách hàng và toàn bộ đơn hàng liên quan!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa khách hàng: " + e.getMessage());
        }
        return "redirect:/admin/customers";
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

    @GetMapping("/customers/{id}")
    public String viewCustomer(@PathVariable Long id,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/login";
        }

        try {
            Customer customer = customerService.findById(id);
            if (customer == null) {
                model.addAttribute("error", "Không tìm thấy khách hàng với ID: " + id);
                return "admin/customers/index"; // Hoặc redirect về danh sách
            }

            // Lấy danh sách hóa đơn với phân trang
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Order> ordersPage = orderService.findByCustomerId(id, pageable);

            // Tính tổng tiền đã mua và kiểm tra điều kiện VIP
            long totalSpent = customerService.getTotalSpentByCustomer(id);
            boolean firstOrderOver2M = customerService.isFirstOrderOver2M(id);
            boolean canBeVip = (firstOrderOver2M || totalSpent >= 10_000_000);

            // Cập nhật số đơn hàng trong customer (ép kiểu từ long sang int)
            customer.setOrderCount((int) ordersPage.getTotalElements());

            model.addAttribute("customer", customer);
            model.addAttribute("orders", ordersPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", ordersPage.getTotalPages());
            model.addAttribute("totalElements", ordersPage.getTotalElements());

            // Thêm các biến cho VIP và điều kiện
            model.addAttribute("totalSpent", totalSpent);
            model.addAttribute("firstOrderOver2M", firstOrderOver2M);
            model.addAttribute("canBeVip", canBeVip);

            return "admin/customers/view";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
            return "admin/customers/index";
        }
    }

    // ==================== SHARED ENDPOINTS ====================
    @GetMapping("")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }
}