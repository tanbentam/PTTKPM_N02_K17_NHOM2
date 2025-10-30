package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.OrderItem;
import com.pttkpm.n02group2.quanlybanhang.Model.ReturnRequestItem;
import com.pttkpm.n02group2.quanlybanhang.Service.CustomerService;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import com.pttkpm.n02group2.quanlybanhang.Repository.ReturnRequestItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReturnRequestItemRepository returnRequestItemRepository;

    // ==================== LIST CUSTOMERS ====================
    @GetMapping
    public String listCustomers(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String search,
                               @RequestParam(defaultValue = "id") String sortBy,
                               @RequestParam(defaultValue = "desc") String sortDir,
                               Model model) {
        try {
            Page<Customer> customerPage;

            if (search != null && !search.trim().isEmpty()) {
                customerPage = customerService.quickSearch(search.trim(), PageRequest.of(page, size));
                for (Customer customer : customerPage.getContent()) {
                    List<Order> orders = orderService.findByCustomer(customer);
                    double totalSpent = 0.0;
                    for (Order order : orders) {
                        if (order.getStatus() != null && order.getStatus().name().equals("RETURNED")) {
                            // Tính lại số tiền sau đổi trả
                            List<ReturnRequestItem> returnItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RETURN");
                            List<ReturnRequestItem> receiveItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RECEIVE");
                            List<OrderItem> orderItems = order.getItems();
                            double baseTotal = (orderItems != null && !orderItems.isEmpty())
                                    ? orderItems.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum()
                                    : 0.0;
                            double vipDiscount = order.getVipDiscountAmount() != null ? order.getVipDiscountAmount() : 0.0;
                            double originalTotal = baseTotal - vipDiscount;
                            double totalReturnAmount = returnItems != null
                                    ? returnItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                                    : 0.0;
                            double totalReceiveAmount = receiveItems != null
                                    ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                                    : 0.0;
                            double receiveVipDiscount = (receiveItems != null && baseTotal > 0)
                                    ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice() * (vipDiscount / baseTotal)).sum()
                                    : 0.0;
                            double actualPaidAmount = originalTotal - totalReturnAmount + totalReceiveAmount - receiveVipDiscount;
                            totalSpent += actualPaidAmount;
                        } else {
                            if (order.getFinalAmount() != null) {
                                totalSpent += order.getFinalAmount();
                            } else if (order.getTotalAmount() != null) {
                                totalSpent += order.getTotalAmount();
                            }
                        }
                    }
                    customer.setTotalSpent(totalSpent);
                }
            } else {
                List<Customer> allCustomers = customerService.getAllCustomers();
                for (Customer customer : allCustomers) {
                    List<Order> orders = orderService.findByCustomer(customer);
                    double totalSpent = 0.0;
                    for (Order order : orders) {
                        if (order.getStatus() != null && order.getStatus().name().equals("RETURNED")) {
                            List<ReturnRequestItem> returnItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RETURN");
                            List<ReturnRequestItem> receiveItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RECEIVE");
                            List<OrderItem> orderItems = order.getItems();
                            double baseTotal = (orderItems != null && !orderItems.isEmpty())
                                    ? orderItems.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum()
                                    : 0.0;
                            double vipDiscount = order.getVipDiscountAmount() != null ? order.getVipDiscountAmount() : 0.0;
                            double originalTotal = baseTotal - vipDiscount;
                            double totalReturnAmount = returnItems != null
                                    ? returnItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                                    : 0.0;
                            double totalReceiveAmount = receiveItems != null
                                    ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                                    : 0.0;
                            double receiveVipDiscount = (receiveItems != null && baseTotal > 0)
                                    ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice() * (vipDiscount / baseTotal)).sum()
                                    : 0.0;
                            double actualPaidAmount = originalTotal - totalReturnAmount + totalReceiveAmount - receiveVipDiscount;
                            totalSpent += actualPaidAmount;
                        } else {
                            if (order.getFinalAmount() != null) {
                                totalSpent += order.getFinalAmount();
                            } else if (order.getTotalAmount() != null) {
                                totalSpent += order.getTotalAmount();
                            }
                        }
                    }
                    customer.setTotalSpent(totalSpent);
                }
                int total = allCustomers.size();
                int start = Math.min(page * size, total);
                int end = Math.min(start + size, total);
                List<Customer> pageContent = allCustomers.subList(start, end);
                customerPage = new PageImpl<>(pageContent, PageRequest.of(page, size), total);
            }

            long totalCustomers = customerService.getTotalCustomers();
            long vipCustomers = customerService.getVipCustomersCount();
            long confirmedVip = customerService.getConfirmedVipCustomersCount();
            long pendingVip = customerService.getPendingVipCustomersCount();

            model.addAttribute("customers", customerPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", customerPage.getTotalPages());
            model.addAttribute("totalElements", customerPage.getTotalElements());
            model.addAttribute("search", search);
            model.addAttribute("size", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);

            model.addAttribute("totalCustomers", totalCustomers);
            model.addAttribute("vipCustomers", vipCustomers);
            model.addAttribute("confirmedVip", confirmedVip);
            model.addAttribute("pendingVip", pendingVip);

            model.addAttribute("newCustomer", new Customer());

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
            model.addAttribute("customers", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 1);
            model.addAttribute("totalElements", 0);
            model.addAttribute("size", size);
            model.addAttribute("search", search);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
            model.addAttribute("totalCustomers", 0L);
            model.addAttribute("vipCustomers", 0L);
            model.addAttribute("confirmedVip", 0L);
            model.addAttribute("pendingVip", 0L);
        }

        return "admin/customers/index";
    }

    // ==================== REDIRECT /{id} về /{id}/profile ====================
    

    // ==================== XEM CHI TIẾT KHÁCH HÀNG ====================
    // ...existing code...
@GetMapping("/{id}/profile")
public String viewCustomerProfile(@PathVariable Long id,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  Model model) {
    try {
        Optional<Customer> customerOpt = customerService.getCustomerById(id);
        Map<Long, Double> finalAmountAfterReturnMap = new HashMap<>();
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();

            // Lấy danh sách đơn hàng và sắp xếp theo ngày tạo mới nhất
            List<Order> allOrders = orderService.findByCustomer(customer);
            allOrders.sort(Comparator.comparing(Order::getCreatedAt).reversed());

            int totalElements = allOrders.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            int start = Math.min(page * size, totalElements);
            int end = Math.min(start + size, totalElements);
            List<Order> orders = allOrders.subList(start, end);

            model.addAttribute("orders", orders);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", totalElements);

            // ...existing code for finalAmountAfterReturnMap and totalSpent...
            for (Order order : orders) {
                if (order.getStatus() != null && order.getStatus().name().equals("RETURNED")) {
                    List<ReturnRequestItem> returnItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RETURN");
                    List<ReturnRequestItem> receiveItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RECEIVE");
                    List<OrderItem> orderItems = order.getItems();

                    double baseTotal = (orderItems != null && !orderItems.isEmpty())
                            ? orderItems.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum()
                            : 0.0;
                    double vipDiscount = order.getVipDiscountAmount() != null ? order.getVipDiscountAmount() : 0.0;
                    double originalTotal = baseTotal - vipDiscount;

                    double totalReturnAmount = returnItems != null
                            ? returnItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                            : 0.0;
                    double totalReceiveAmount = receiveItems != null
                            ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                            : 0.0;
                    double receiveVipDiscount = (receiveItems != null && baseTotal > 0)
                            ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice() * (vipDiscount / baseTotal)).sum()
                            : 0.0;

                    double actualPaidAmount = originalTotal - totalReturnAmount + totalReceiveAmount - receiveVipDiscount;
                    finalAmountAfterReturnMap.put(order.getId(), actualPaidAmount);
                }
            }
            model.addAttribute("finalAmountAfterReturnMap", finalAmountAfterReturnMap);

            // ...existing code for totalSpent, VIP check, etc...
            double totalSpent = 0.0;
            for (Order order : allOrders) {
                if (order.getStatus() != null && order.getStatus().name().equals("RETURNED")) {
                    Double afterReturn = finalAmountAfterReturnMap.get(order.getId());
                    if (afterReturn != null) {
                        totalSpent += afterReturn;
                    } else if (order.getFinalAmount() != null) {
                        totalSpent += order.getFinalAmount();
                    } else if (order.getTotalAmount() != null) {
                        totalSpent += order.getTotalAmount();
                    }
                } else {
                    if (order.getFinalAmount() != null) {
                        totalSpent += order.getFinalAmount();
                    } else if (order.getTotalAmount() != null) {
                        totalSpent += order.getTotalAmount();
                    }
                }
            }
            customer.setTotalSpent(totalSpent);

            boolean firstOrderOver2M = customerService.isFirstOrderOver2M(id);
            boolean canBeVip = (firstOrderOver2M || totalSpent >= 10_000_000);

            model.addAttribute("customer", customer);
            model.addAttribute("totalSpent", totalSpent);
            model.addAttribute("firstOrderOver2M", firstOrderOver2M);
            model.addAttribute("canBeVip", canBeVip);

            if (!customer.isVip()) {
                long remainingToVip = Math.max(0, 10_000_000 - (long) totalSpent);
                model.addAttribute("remainingToVip", remainingToVip);

                String vipConditionMessage;
                if (firstOrderOver2M) {
                    vipConditionMessage = "Đủ điều kiện lên VIP (hóa đơn đầu tiên ≥ 2 triệu)";
                } else if (totalSpent >= 10_000_000) {
                    vipConditionMessage = "Đủ điều kiện lên VIP (tích lũy ≥ 10 triệu)";
                } else {
                    vipConditionMessage = String.format("Còn thiếu %,d VNĐ để lên VIP", remainingToVip);
                }
                model.addAttribute("vipConditionMessage", vipConditionMessage);
            }

            return "admin/customers/view";
        } else {
            model.addAttribute("error", "Không tìm thấy khách hàng với ID: " + id);
            model.addAttribute("finalAmountAfterReturnMap", finalAmountAfterReturnMap);
            return "admin/customers/index";
        }
    } catch (Exception e) {
        model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        model.addAttribute("finalAmountAfterReturnMap", new HashMap<Long, Double>());
        return "admin/customers/index";
    }
}

    // ==================== XEM CHI TIẾT ĐƠN HÀNG (BÌNH THƯỜNG & ĐỔI TRẢ) ====================
    // ...existing code...
@GetMapping("/orders/{orderId}")
public String viewOrderDetail(@PathVariable Long orderId, Model model) {
    Order order = orderService.findById(orderId);
    if (order == null) {
        model.addAttribute("error", "Không tìm thấy đơn hàng.");
        return "redirect:/admin/customers";
    }
    List<OrderItem> orderItems = order.getItems();
    double baseTotal = orderItems != null
            ? orderItems.stream().mapToDouble(i -> i.getQuantity() * i.getPrice()).sum()
            : 0.0;
    double actualDiscount = order.getVipDiscountAmount() != null ? order.getVipDiscountAmount() : 0;
    boolean hasDiscount = actualDiscount > 0;
    double originalTotal = baseTotal - actualDiscount;

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
    model.addAttribute("orderItems", orderItems);
    model.addAttribute("baseTotal", baseTotal);
    model.addAttribute("originalTotal", originalTotal);
    model.addAttribute("actualDiscount", actualDiscount);
    model.addAttribute("hasDiscount", hasDiscount);
    model.addAttribute("paymentMethodVN", paymentMethodVN);

    // Nếu là đơn đổi trả thì chuyển sang view đổi trả
    if (order.getStatus() != null && order.getStatus().name().equals("RETURNED")) {
        List<ReturnRequestItem> returnItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RETURN");
        List<ReturnRequestItem> receiveItems = returnRequestItemRepository.findByOrderAndTypeIgnoreCase(order, "RECEIVE");

        double totalReturnAmount = returnItems != null
                ? returnItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                : 0.0;
        double totalReceiveAmount = receiveItems != null
                ? receiveItems.stream().mapToDouble(i -> i.getQuantity() * i.getUnitPrice()).sum()
                : 0.0;

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

        if (order.getCustomer() != null) {
            model.addAttribute("customerId", order.getCustomer().getId());
        }

        return "admin/customers/bill_return";
    } else {
        double actualPaidAmount = order.getFinalAmount() != null ? order.getFinalAmount() : originalTotal;
        model.addAttribute("actualPaidAmount", actualPaidAmount);

        if (order.getCustomer() != null) {
            model.addAttribute("customerId", order.getCustomer().getId());
        }

        // Đổi view trả về bill thông thường nằm trong folder customers
        return "admin/customers/bill";
    }
}
// ...existing code...
    // ==================== VIP MANAGEMENT ====================
    @PostMapping("/{id}/confirm-vip")
    public String confirmVipCustomer(@PathVariable Long id,
                                    @RequestParam String adminUsername,
                                    RedirectAttributes redirectAttributes) {
        try {
            long totalSpent = customerService.getTotalSpentByCustomer(id);
            boolean firstOrderOver2M = customerService.isFirstOrderOver2M(id);

            if (firstOrderOver2M || totalSpent >= 10_000_000) {
                boolean success = customerService.confirmVipCustomer(id, adminUsername);
                if (success) {
                    redirectAttributes.addFlashAttribute("success",
                        "Khách hàng đã được xác nhận VIP thành công!");
                } else {
                    redirectAttributes.addFlashAttribute("error",
                        "Không thể xác nhận VIP cho khách hàng này.");
                }
            } else {
                redirectAttributes.addFlashAttribute("error",
                    "Khách hàng chưa đủ điều kiện lên VIP.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra: " + e.getMessage());
        }
        // Điều hướng về đúng trang profile
        return "redirect:/admin/customers/" + id + "/profile";
    }

    @PostMapping("/{id}/revoke-vip")
    public String revokeVipStatus(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            boolean success = customerService.revokeVipStatus(id);
            if (success) {
                redirectAttributes.addFlashAttribute("success",
                    "Đã hủy trạng thái VIP của khách hàng!");
            } else {
                redirectAttributes.addFlashAttribute("error",
                    "Không tìm thấy khách hàng với ID: " + id);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra khi xóa khách hàng: " + e.getMessage());
        }
        // Điều hướng về đúng trang profile
        return "redirect:/admin/customers/" + id + "/profile";
    }

    // ==================== CREATE CUSTOMER (ADMIN) ====================
    @PostMapping
    public String createCustomer(@ModelAttribute Customer customer,
                                RedirectAttributes redirectAttributes) {
        try {
            if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()
                && customerService.isPhoneNumberExists(customer.getPhone())) {
                redirectAttributes.addFlashAttribute("error",
                    "Số điện thoại đã tồn tại trong hệ thống!");
                return "redirect:/admin/customers";
            }
            customerService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success",
                "Khách hàng đã được tạo thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra khi tạo khách hàng: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // ==================== UPDATE CUSTOMER ====================
    @PostMapping("/{id}")
    public String updateCustomer(@PathVariable Long id,
                                @ModelAttribute Customer customerDetails,
                                RedirectAttributes redirectAttributes) {
        try {
            Customer updatedCustomer = customerService.updateCustomer(id, customerDetails);
            redirectAttributes.addFlashAttribute("success",
                "Thông tin khách hàng đã được cập nhật!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra khi cập nhật khách hàng: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // ==================== DELETE CUSTOMER ====================
    @PostMapping("/{id}/delete")
    public String deleteCustomer(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = customerService.deleteCustomer(id);
            if (deleted) {
                redirectAttributes.addFlashAttribute("success",
                    "Khách hàng đã được xóa thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error",
                    "Không tìm thấy khách hàng với ID: " + id);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                "Có lỗi xảy ra khi xóa khách hàng: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    // ==================== AJAX ENDPOINTS ====================
    @GetMapping("/check-phone")
    @ResponseBody
    public boolean checkPhoneNumber(@RequestParam String phoneNumber,
                                   @RequestParam(required = false) Long excludeId) {
        if (excludeId != null) {
            Optional<Customer> existing = customerService.getCustomerByPhoneNumber(phoneNumber);
            return !existing.isPresent() || existing.get().getId().equals(excludeId);
        }
        return !customerService.isPhoneNumberExists(phoneNumber);
    }

    @GetMapping("/check-email")
    @ResponseBody
    public boolean checkEmail(@RequestParam String email,
                             @RequestParam(required = false) Long excludeId) {
        if (excludeId != null) {
            Optional<Customer> existing = customerService.getCustomerByEmail(email);
            return !existing.isPresent() || existing.get().getId().equals(excludeId);
        }
        return !customerService.isEmailExists(email);
    }

    // ==================== API FOR POS SYSTEM ====================
    @PostMapping("/api/customers")
    @ResponseBody
    public ResponseEntity<?> createCustomerAPI(@RequestBody Map<String, Object> customerData) {
        try {
            Customer customer = new Customer();
            String name = (String) customerData.get("name");
            String fullName = (String) customerData.get("fullName");
            customer.setName(name != null && !name.trim().isEmpty() ? name.trim() :
                           fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : "Khách hàng");
            String phone = (String) customerData.get("phone");
            if (phone != null && !phone.trim().isEmpty()) customer.setPhone(phone.trim());
            String address = (String) customerData.get("address");
            if (address != null && !address.trim().isEmpty()) customer.setAddress(address.trim());
            String province = (String) customerData.get("province");
            if (province != null && !province.trim().isEmpty()) customer.setProvince(province.trim());
            String district = (String) customerData.get("district");
            if (district != null && !district.trim().isEmpty()) customer.setDistrict(district.trim());
            String ward = (String) customerData.get("ward");
            if (ward != null && !ward.trim().isEmpty()) customer.setWard(ward.trim());
            Customer savedCustomer = customerService.createCustomer(customer);
            return ResponseEntity.ok(Map.of("success", true, "customer", savedCustomer));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi tạo khách hàng: " + e.getMessage()));
        }
    }

    @GetMapping("/api/customers/search")
    @ResponseBody
    public ResponseEntity<?> searchCustomersAPI(@RequestParam String q) {
        try {
            Page<Customer> customerPage = customerService.quickSearch(q.trim(), PageRequest.of(0, 50));
            List<Customer> customers = customerPage.getContent();
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi tìm kiếm: " + e.getMessage()));
        }
    }

    @GetMapping("/api/customers/{id}")
    @ResponseBody
    public ResponseEntity<?> getCustomerAPI(@PathVariable Long id) {
        try {
            Optional<Customer> customerOpt = customerService.getCustomerById(id);
            if (customerOpt.isPresent()) {
                return ResponseEntity.ok(customerOpt.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
        }
    }
}