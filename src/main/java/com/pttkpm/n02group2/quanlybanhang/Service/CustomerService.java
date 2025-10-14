package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Repository.CustomerRepository;
import com.pttkpm.n02group2.quanlybanhang.Repository.OrderRepository;
import com.pttkpm.n02group2.quanlybanhang.Model.Order; // Thêm import này
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator; // Thêm import này
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository; // Di chuyển lên đầu

    // ==================== BASIC CRUD OPERATIONS ====================
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }
    
    @Transactional
    public Customer saveCustomer(Customer customer) {
        try {
            System.out.println("=== CustomerService.saveCustomer() DEBUG ===");
            System.out.println("Input customer:");
            System.out.println("- Name: " + customer.getName());
            System.out.println("- Phone: " + customer.getPhone());
            System.out.println("- Date of Birth: " + customer.getDateOfBirth());
            
            // Validate dữ liệu
            if (customer.getName() == null || customer.getName().trim().isEmpty()) {
                throw new RuntimeException("Tên khách hàng không được để trống");
            }

            // Set default values nếu null (NHƯNG KHÔNG ĐỘNG VÀO dateOfBirth)
            if (customer.getPhone() == null) customer.setPhone("");
            if (customer.getProvince() == null) customer.setProvince("");
            if (customer.getDistrict() == null) customer.setDistrict("");
            if (customer.getWard() == null) customer.setWard("");
            if (customer.getAddress() == null) customer.setAddress("");
            if (customer.getVipDiscountPercent() == null) customer.setVipDiscountPercent(0.0);
            if (customer.getTotalSpent() == null) customer.setTotalSpent(0.0);
            if (customer.getOrderCount() == null) customer.setOrderCount(0);
            if (customer.getIsVip() == null) customer.setIsVip(false);

            // QUAN TRỌNG: KHÔNG SET DEFAULT CHO dateOfBirth - GIỮ NGUYÊN GIÁ TRỊ HIỆN TẠI
            System.out.println("dateOfBirth before save: " + customer.getDateOfBirth());

            // Set timestamps
            if (customer.getId() == null) {
                // New customer
                customer.setCreatedAt(LocalDateTime.now());
            }
            customer.setUpdatedAt(LocalDateTime.now());

            Customer savedCustomer = customerRepository.save(customer);
            
            System.out.println("✅ Đã lưu khách hàng: " + savedCustomer.getId() + " - " + savedCustomer.getName());
            System.out.println("dateOfBirth after save: " + savedCustomer.getDateOfBirth());
            System.out.println("=== END CustomerService.saveCustomer() ===");
            
            return savedCustomer;
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi lưu khách hàng: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể lưu khách hàng: " + e.getMessage());
        }
    }
        
    public Customer createCustomer(Customer customer) {
        System.out.println("=== CustomerService.createCustomer() DEBUG ===");
        System.out.println("Input customer dateOfBirth: " + customer.getDateOfBirth());
        
        Customer result = saveCustomer(customer);
        
        System.out.println("Result customer dateOfBirth: " + result.getDateOfBirth());
        System.out.println("=== END createCustomer() ===");
        
        return result;
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customer) {
        Optional<Customer> existingCustomer = customerRepository.findById(id);
        if (existingCustomer.isPresent()) {
            Customer customerToUpdate = existingCustomer.get();
            
            System.out.println("=== CustomerService.updateCustomer() DEBUG ===");
            System.out.println("Existing customer DOB: " + customerToUpdate.getDateOfBirth());
            System.out.println("New customer DOB: " + customer.getDateOfBirth());
            
            // Cập nhật thông tin
            customerToUpdate.setName(customer.getName());
            customerToUpdate.setPhone(customer.getPhone());
            customerToUpdate.setProvince(customer.getProvince());
            customerToUpdate.setDistrict(customer.getDistrict());
            customerToUpdate.setWard(customer.getWard());
            customerToUpdate.setAddress(customer.getAddress());
            
            // XỬ LÝ dateOfBirth CẨN THẬN
            if (customer.getDateOfBirth() != null) {
                customerToUpdate.setDateOfBirth(customer.getDateOfBirth());
                System.out.println("✅ Updated dateOfBirth to: " + customer.getDateOfBirth());
            } else {
                System.out.println("⚠️ New dateOfBirth is null, keeping existing: " + customerToUpdate.getDateOfBirth());
            }
            
            // Giữ nguyên thông tin VIP và order stats
            if (customer.getTotalSpent() != null) {
                customerToUpdate.setTotalSpent(customer.getTotalSpent());
            }
            if (customer.getOrderCount() != null) {
                customerToUpdate.setOrderCount(customer.getOrderCount());
            }
            if (customer.getIsVip() != null) {
                customerToUpdate.setIsVip(customer.getIsVip());
            }
            if (customer.getVipDiscountPercent() != null) {
                customerToUpdate.setVipDiscountPercent(customer.getVipDiscountPercent());
            }
            
            Customer result = saveCustomer(customerToUpdate);
            System.out.println("Final result DOB: " + result.getDateOfBirth());
            System.out.println("=== END updateCustomer() ===");
            
            return result;
        }
        throw new RuntimeException("Customer not found with id: " + id);
    }
    
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> getAllCustomers() {
        try {
            List<Customer> customers = customerRepository.findAll();
            System.out.println("CustomerService.getAllCustomers(): Found " + customers.size() + " customers");
            return customers;
        } catch (Exception e) {
            System.err.println("Error in getAllCustomers(): " + e.getMessage());
            e.printStackTrace();
            return List.of(); // Trả về danh sách trống nếu lỗi
        }
    }
public long countAll() {
        return customerRepository.count();
    }
    public void deleteCustomerAndOrders(Long customerId) {
        orderRepository.deleteByCustomerId(customerId);
        customerRepository.deleteById(customerId);
    }

    public long getTotalSpentByCustomer(Long customerId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (!customerOpt.isPresent()) {
            return 0;
        }
        return orderRepository.findByCustomer(customerOpt.get()).stream()
                .mapToLong(order -> order.getTotalAmount().longValue())
                .sum();
    }
    
    public boolean isFirstOrderOver2M(Long customerId) {
        Optional<Customer> customerOpt = customerRepository.findById(customerId);
        if (!customerOpt.isPresent()) {
            return false;
        }
        List<Order> orders = orderRepository.findByCustomer(customerOpt.get());
        if (orders.isEmpty()) return false;
        // Sắp xếp theo ngày tạo để lấy đơn đầu tiên
        orders.sort(Comparator.comparing(Order::getCreatedAt));
        return orders.get(0).getTotalAmount() >= 2_000_000;
    }

    // Xóa khách hàng và tất cả đơn hàng liên quan

    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }

    // ==================== SEARCH METHODS ====================
    
    public List<Customer> findByName(String name) {
        return customerRepository.findByNameContainingIgnoreCase(name);
    }

    public boolean existsByPhone(String phone) {
        return customerRepository.existsByPhone(phone);
    }

    // ==================== POS SPECIFIC METHODS ====================
    
    public List<Customer> getRecentCustomers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return customerRepository.findRecentCustomersForSuggestion(pageable);
    }

    public List<Customer> searchCustomersForPOS(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getRecentCustomers(10);
        }
        Pageable pageable = PageRequest.of(0, 10);
        return customerRepository.findForPOS(query, query);
    }

    public List<Customer> findForAutocomplete(String search) {
        Pageable pageable = PageRequest.of(0, 10);
        return customerRepository.findForAutocomplete(search, pageable);
    }

    // ==================== ADDRESS SEARCH ====================
    
    public List<Customer> findByProvince(String province) {
        return customerRepository.findByProvince(province);
    }

    public List<Customer> findByDistrict(String district) {
        return customerRepository.findByDistrict(district);
    }

    public List<Customer> findByWard(String ward) {
        return customerRepository.findByWard(ward);
    }

    public List<Customer> findByAddressContaining(String address) {
        return customerRepository.findByAddressContaining(address);
    }

    // ==================== DATE OF BIRTH SEARCH ====================
    
    public List<Customer> findByDateOfBirth(LocalDate dateOfBirth) {
        return customerRepository.findByDateOfBirth(dateOfBirth);
    }

    public List<Customer> findCustomersWithBirthdayToday() {
        return customerRepository.findCustomersWithBirthdayToday();
    }

    public List<Customer> findCustomersWithBirthdayInMonth(int month) {
        return customerRepository.findCustomersWithBirthdayInMonth(month);
    }

    public List<Customer> findByAgeBetween(int minAge, int maxAge) {
        return customerRepository.findByAgeBetween(minAge, maxAge);
    }

    // ==================== ADVANCED SEARCH ====================
    
    public Page<Customer> searchCustomers(String name, String phone, String province, String district, Pageable pageable) {
        return customerRepository.searchCustomers(name, phone, province, district, pageable);
    }

    public Page<Customer> quickSearch(String search, Pageable pageable) {
        return customerRepository.quickSearch(search, pageable);
    }

    // ==================== STATISTICS ====================
    
    public List<Object[]> getCustomerStatsByProvince() {
        return customerRepository.countCustomersByProvince();
    }

    public List<Object[]> getCustomerStatsByAgeGroup() {
        return customerRepository.countCustomersByAgeGroup();
    }

    public long getTotalCustomers() {
        return customerRepository.getTotalCustomers();
    }

    public long getNewCustomersThisMonth() {
        return customerRepository.getNewCustomersThisMonth();
    }

    public long getCustomersWithBirthdayThisMonth() {
        return customerRepository.getCustomersWithBirthdayThisMonth();
    }

    // ==================== SPECIAL LISTS ====================
    
    public List<Customer> getLatestCustomers(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return customerRepository.findLatestCustomers(pageable).getContent();
    }

    public List<Customer> getUpcomingBirthdays() {
        return customerRepository.findUpcomingBirthdays();
    }

    public List<Customer> getCustomersWithCompleteAddress() {
        return customerRepository.findCustomersWithCompleteAddress();
    }

    public List<Customer> getCustomersWithIncompleteInfo() {
        return customerRepository.findCustomersWithIncompleteInfo();
    }

    // ==================== VALIDATION ====================
    
    public boolean isPhoneExistsForOtherCustomer(String phone, Long excludeId) {
        return customerRepository.existsByPhoneAndIdNot(phone, excludeId);
    }

    // ==================== VIP STATISTICS ====================
    
    public long getVipCustomersCount() {
        return getVipCustomers().size();
    }
    
    public long getConfirmedVipCustomersCount() {
        return customerRepository.findVipCustomers().stream()
            .filter(c -> c.getIsVip() != null && c.getIsVip())
            .count();
    }
    
    public long getPendingVipCustomersCount() {
        return customerRepository.findVipCustomers().stream()
            .filter(c -> c.getIsVip() == null || !c.getIsVip())
            .count();
    }

    // ==================== VALIDATION METHODS ====================
    
    public boolean isPhoneNumberExists(String phoneNumber) {
        return existsByPhone(phoneNumber);
    }
    
    public boolean isEmailExists(String email) {
        return false; // Luôn trả về false vì không còn email
    }

    // ==================== VIP MANAGEMENT ====================
    
    public boolean confirmVipCustomer(Long id, String adminUsername) {
        try {
            long totalSpent = getTotalSpentByCustomer(id);
            boolean firstOrderOver2M = isFirstOrderOver2M(id);

            // Điều kiện VIP: Hóa đơn đầu tiên trên 2 triệu HOẶC tích lũy đủ 10 triệu
            if (firstOrderOver2M || totalSpent >= 10_000_000) {
                Optional<Customer> customerOpt = getCustomerById(id);
                if (customerOpt.isPresent()) {
                    Customer customer = customerOpt.get();
                    customer.setIsVip(true);
                    customer.setVipDiscountPercent(5.0); // Giảm 5% cho khách VIP
                    updateCustomer(id, customer);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi xác nhận VIP: " + e.getMessage());
        }
        return false;
    }
    
    public boolean revokeVipStatus(Long id) {
        Optional<Customer> customerOpt = getCustomerById(id);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            customer.setIsVip(false);
            customer.setVipDiscountPercent(0.0);
            updateCustomer(id, customer);
            return true;
        }
        return false;
    }

    // ==================== FIND BY CONTACT INFO ====================
    
    public Optional<Customer> getCustomerByPhoneNumber(String phoneNumber) {
        return Optional.ofNullable(findByPhone(phoneNumber));
    }
    
    public Optional<Customer> getCustomerByEmail(String email) {
        return Optional.empty();
    }

    // ==================== DELETE WITH RETURN VALUE ====================
    
    public boolean deleteCustomer(Long id) {
        try {
            customerRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== REPORTS ====================
    
    public List<Customer> getCustomersByDateRange(LocalDate startDate, LocalDate endDate) {
        return customerRepository.findCustomersByDateRange(startDate, endDate);
    }

    public List<Customer> getVipCustomers() {
        return customerRepository.findVipCustomers();
    }

    public List<Object[]> getCustomerStatsByDistrict() {
        return customerRepository.countCustomersByDistrict();
    }

    // ==================== POS HELPER METHODS ====================
    
    public Customer getDefaultCustomer() {
        // Tìm hoặc tạo khách hàng vãng lai
        List<Customer> defaultCustomers = customerRepository.findByNameContainingIgnoreCase("Khách vãng lai");
        if (!defaultCustomers.isEmpty()) {
            return defaultCustomers.get(0);
        }
        
        // Tạo khách hàng vãng lai mới
        Customer defaultCustomer = new Customer();
        defaultCustomer.setName("Khách vãng lai");
        defaultCustomer.setPhone("");
        defaultCustomer.setProvince("");
        defaultCustomer.setDistrict("");
        defaultCustomer.setWard("");
        defaultCustomer.setAddress("");
        defaultCustomer.setIsVip(false);
        defaultCustomer.setVipDiscountPercent(0.0);
        defaultCustomer.setTotalSpent(0.0);
        defaultCustomer.setOrderCount(0);
        
        return saveCustomer(defaultCustomer);
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id).orElse(null);
    }

    public Customer findByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return null;
        Optional<Customer> customerOpt = customerRepository.findByPhone(phone.trim());
        return customerOpt.orElse(null);
    }
}