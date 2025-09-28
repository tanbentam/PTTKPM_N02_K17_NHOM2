package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import com.pttkpm.n02group2.quanlybanhang.Repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    // ==================== BASIC CRUD OPERATIONS ====================
    
    @Transactional
    public Customer saveCustomer(Customer customer) {
        try {
            // Validate dữ liệu
            if (customer.getName() == null || customer.getName().trim().isEmpty()) {
                throw new RuntimeException("Tên khách hàng không được để trống");
            }

            // Set default values nếu null
            if (customer.getPhone() == null) customer.setPhone("");
            if (customer.getProvince() == null) customer.setProvince("");
            if (customer.getDistrict() == null) customer.setDistrict("");
            if (customer.getWard() == null) customer.setWard("");
            if (customer.getAddress() == null) customer.setAddress("");
            if (customer.getVipDiscountPercent() == null) customer.setVipDiscountPercent(0.0);
            if (customer.getTotalSpent() == null) customer.setTotalSpent(0.0);
            if (customer.getOrderCount() == null) customer.setOrderCount(0);
            if (customer.getIsVip() == null) customer.setIsVip(false);

            // Set timestamps
            if (customer.getId() == null) {
                // New customer
                customer.setCreatedAt(LocalDateTime.now());
            }
            customer.setUpdatedAt(LocalDateTime.now());

            Customer savedCustomer = customerRepository.save(customer);
            System.out.println("✅ Đã lưu khách hàng: " + savedCustomer.getId() + " - " + savedCustomer.getName());
            return savedCustomer;
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi lưu khách hàng: " + e.getMessage());
            throw new RuntimeException("Không thể lưu khách hàng: " + e.getMessage());
        }
    }
    
    public Customer createCustomer(Customer customer) {
        // Sử dụng saveCustomer để có validation
        return saveCustomer(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customer) {
        Optional<Customer> existingCustomer = customerRepository.findById(id);
        if (existingCustomer.isPresent()) {
            Customer customerToUpdate = existingCustomer.get();
            
            // Cập nhật thông tin
            customerToUpdate.setName(customer.getName());
            customerToUpdate.setPhone(customer.getPhone());
            customerToUpdate.setProvince(customer.getProvince());
            customerToUpdate.setDistrict(customer.getDistrict());
            customerToUpdate.setWard(customer.getWard());
            customerToUpdate.setAddress(customer.getAddress());
            customerToUpdate.setDateOfBirth(customer.getDateOfBirth());
            
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
            
            return saveCustomer(customerToUpdate);
        }
        throw new RuntimeException("Customer not found with id: " + id);
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
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
        Optional<Customer> customerOpt = getCustomerById(id);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            if (customer.getTotalSpent() != null && customer.getTotalSpent() >= 2000000) {
                customer.setIsVip(true);
                customer.setVipDiscountPercent(7.0);
                updateCustomer(id, customer);
                return true;
            }
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