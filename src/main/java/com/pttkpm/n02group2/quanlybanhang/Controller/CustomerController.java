package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

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
            } else {
                List<Customer> allCustomers = customerService.getAllCustomers();
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

    // ==================== XEM CHI TIẾT KHÁCH HÀNG ====================
    @GetMapping("/{id}/profile")
    public String viewCustomerProfile(@PathVariable Long id, Model model) {
        try {
            Optional<Customer> customerOpt = customerService.getCustomerById(id);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();

                // Lấy tổng tiền đã mua và kiểm tra điều kiện VIP
                long totalSpent = customerService.getTotalSpentByCustomer(id);
                boolean firstOrderOver2M = customerService.isFirstOrderOver2M(id);
                boolean canBeVip = (firstOrderOver2M || totalSpent >= 10_000_000);

                model.addAttribute("customer", customer);
                model.addAttribute("totalSpent", totalSpent);
                model.addAttribute("firstOrderOver2M", firstOrderOver2M);
                model.addAttribute("canBeVip", canBeVip);

                // Thông tin chi tiết điều kiện VIP
                if (!customer.isVip()) {
                    long remainingToVip = Math.max(0, 10_000_000 - totalSpent);
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
                return "admin/customers/index";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "admin/customers/index";
        }
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
        return "redirect:/admin/customers/" + id;
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
                "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin/customers/" + id;
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
}