package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
            // Tìm kiếm khách hàng
            Page<Customer> customerPage;
            if (search != null && !search.trim().isEmpty()) {
                // Sử dụng quick search cho tìm kiếm đơn giản
                customerPage = customerService.quickSearch(search.trim(), PageRequest.of(page, size));
            } else {
                // Nếu không có tìm kiếm, lấy tất cả
                List<Customer> allCustomers = customerService.getAllCustomers();
                // Tạo Page giả lập cho tất cả customers
                int start = page * size;
                int end = Math.min(start + size, allCustomers.size());
                List<Customer> pageContent = allCustomers.subList(start, end);
                customerPage = new PageImpl<>(pageContent, PageRequest.of(page, size), allCustomers.size());
            }

            // Thống kê
            long totalCustomers = customerService.getTotalCustomers();
            long vipCustomers = customerService.getVipCustomersCount();
            long confirmedVip = customerService.getConfirmedVipCustomersCount();
            long pendingVip = customerService.getPendingVipCustomersCount();

            model.addAttribute("customers", customerPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", customerPage.getTotalPages());
            model.addAttribute("totalElements", customerPage.getTotalElements());
            model.addAttribute("search", search);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);

            // Thống kê cho sidebar
            model.addAttribute("totalCustomers", totalCustomers);
            model.addAttribute("vipCustomers", vipCustomers);
            model.addAttribute("confirmedVip", confirmedVip);
            model.addAttribute("pendingVip", pendingVip);

            // Form thêm mới
            model.addAttribute("newCustomer", new Customer());

        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi tải dữ liệu: " + e.getMessage());
        }

        return "admin/customers/index";
    }

    // ==================== API FOR POS SYSTEM ====================

    @PostMapping("/api/customers")
    @ResponseBody
    public ResponseEntity<?> createCustomerAPI(@RequestBody Map<String, Object> customerData) {
        try {
            Customer customer = new Customer();
            
            // Handle cả name và fullName
            String name = (String) customerData.get("name");
            String fullName = (String) customerData.get("fullName");
            
            // Set name (required field)
            customer.setName(name != null && !name.trim().isEmpty() ? name.trim() : 
                           fullName != null && !fullName.trim().isEmpty() ? fullName.trim() : "Khách hàng");
            
            // Set phone (optional)
            String phone = (String) customerData.get("phone");
            if (phone != null && !phone.trim().isEmpty()) {
                customer.setPhone(phone.trim());
            }
            
            // Set address (optional)
            String address = (String) customerData.get("address");
            if (address != null && !address.trim().isEmpty()) {
                customer.setAddress(address.trim());
            }
            
            // Set other optional fields
            String province = (String) customerData.get("province");
            if (province != null && !province.trim().isEmpty()) {
                customer.setProvince(province.trim());
            }
            
            String district = (String) customerData.get("district");
            if (district != null && !district.trim().isEmpty()) {
                customer.setDistrict(district.trim());
            }
            
            String ward = (String) customerData.get("ward");
            if (ward != null && !ward.trim().isEmpty()) {
                customer.setWard(ward.trim());
            }
            
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
        // Sử dụng quickSearch thay vì searchCustomers
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
            // Kiểm tra số điện thoại đã tồn tại
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
            boolean success = customerService.confirmVipCustomer(id, adminUsername);
            if (success) {
                redirectAttributes.addFlashAttribute("success", 
                    "Khách hàng đã được xác nhận VIP thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", 
                    "Không thể xác nhận VIP cho khách hàng này. " +
                    "Khách hàng cần có tổng chi tiêu >= 2.000.000 VND");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/admin/customers";
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

        return "redirect:/admin/customers";
    }

    // ==================== VIEW CUSTOMER DETAILS ====================

    @GetMapping("/{id}")
    public String viewCustomer(@PathVariable Long id, Model model) {
        try {
            Optional<Customer> customerOpt = customerService.getCustomerById(id);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                model.addAttribute("customer", customer);
                
                // Thêm thống kê đơn hàng nếu có OrderService
                // model.addAttribute("orderHistory", orderService.getOrdersByCustomer(customer));
                
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

    // ==================== AJAX ENDPOINTS ====================

    @GetMapping("/check-phone")
    @ResponseBody
    public boolean checkPhoneNumber(@RequestParam String phoneNumber, 
                                   @RequestParam(required = false) Long excludeId) {
        if (excludeId != null) {
            // Khi update, exclude customer hiện tại
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
            // Khi update, exclude customer hiện tại
            Optional<Customer> existing = customerService.getCustomerByEmail(email);
            return !existing.isPresent() || existing.get().getId().equals(excludeId);
        }
        return !customerService.isEmailExists(email);
    }
}