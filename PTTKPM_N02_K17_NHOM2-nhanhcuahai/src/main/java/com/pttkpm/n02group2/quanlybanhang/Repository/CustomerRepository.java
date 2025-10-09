package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    // ==================== TÌM KIẾM CƠ BẢN ====================
    Optional<Customer> findByName(String name);
    // Tìm khách hàng theo số điện thoại
    Optional<Customer> findByPhone(String phone);
    
    // Tìm khách hàng theo tên
    List<Customer> findByNameContainingIgnoreCase(String name);
    
    // Kiểm tra số điện thoại đã tồn tại
    boolean existsByPhone(String phone);
    
    // ==================== TÌM KIẾM THEO ĐỊA CHỈ ====================
    
    // Tìm khách hàng theo tỉnh
    List<Customer> findByProvince(String province);
    
    // Tìm khách hàng theo quận/huyện
    List<Customer> findByDistrict(String district);
    
    // Tìm khách hàng theo phường/xã
    List<Customer> findByWard(String ward);
    
    // Tìm khách hàng theo địa chỉ đầy đủ
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.address) LIKE LOWER(CONCAT('%', :address, '%')) OR " +
           "LOWER(c.ward) LIKE LOWER(CONCAT('%', :address, '%')) OR " +
           "LOWER(c.district) LIKE LOWER(CONCAT('%', :address, '%')) OR " +
           "LOWER(c.province) LIKE LOWER(CONCAT('%', :address, '%'))")
    List<Customer> findByAddressContaining(@Param("address") String address);
    
    // ==================== TÌM KIẾM THEO NGÀY SINH ====================
    
    // Tìm khách hàng theo ngày sinh
    List<Customer> findByDateOfBirth(LocalDate dateOfBirth);
    
    // Tìm khách hàng sinh nhật hôm nay
    @Query("SELECT c FROM Customer c WHERE MONTH(c.dateOfBirth) = MONTH(CURRENT_DATE) AND DAY(c.dateOfBirth) = DAY(CURRENT_DATE)")
    List<Customer> findCustomersWithBirthdayToday();
    
    // Tìm khách hàng sinh nhật trong tháng
    @Query("SELECT c FROM Customer c WHERE MONTH(c.dateOfBirth) = :month")
    List<Customer> findCustomersWithBirthdayInMonth(@Param("month") int month);
    
    // Tìm khách hàng theo độ tuổi
    @Query("SELECT c FROM Customer c WHERE YEAR(CURRENT_DATE) - YEAR(c.dateOfBirth) BETWEEN :minAge AND :maxAge")
    List<Customer> findByAgeBetween(@Param("minAge") int minAge, @Param("maxAge") int maxAge);
    
    // ==================== TÌM KIẾM TỔNG HỢP ====================
    
    // Tìm kiếm khách hàng đa điều kiện
    @Query("SELECT c FROM Customer c WHERE " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:phone IS NULL OR c.phone LIKE CONCAT('%', :phone, '%')) AND " +
           "(:province IS NULL OR LOWER(c.province) = LOWER(:province)) AND " +
           "(:district IS NULL OR LOWER(c.district) = LOWER(:district))")
    Page<Customer> searchCustomers(
        @Param("name") String name,
        @Param("phone") String phone, 
        @Param("province") String province,
        @Param("district") String district,
        Pageable pageable
    );
    
    // Tìm kiếm nhanh (tên hoặc số điện thoại)
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "c.phone LIKE CONCAT('%', :search, '%')")
    Page<Customer> quickSearch(@Param("search") String search, Pageable pageable);
    
    // ==================== THỐNG KÊ ====================
    
    // Đếm khách hàng theo tỉnh
    @Query("SELECT c.province, COUNT(c) FROM Customer c WHERE c.province IS NOT NULL GROUP BY c.province")
    List<Object[]> countCustomersByProvince();
    
    // Đếm khách hàng theo độ tuổi
    @Query("SELECT " +
           "CASE " +
           "WHEN YEAR(CURRENT_DATE) - YEAR(c.dateOfBirth) < 18 THEN 'Dưới 18' " +
           "WHEN YEAR(CURRENT_DATE) - YEAR(c.dateOfBirth) BETWEEN 18 AND 30 THEN '18-30' " +
           "WHEN YEAR(CURRENT_DATE) - YEAR(c.dateOfBirth) BETWEEN 31 AND 50 THEN '31-50' " +
           "ELSE 'Trên 50' END as ageGroup, " +
           "COUNT(c) " +
           "FROM Customer c " +
           "WHERE c.dateOfBirth IS NOT NULL " +
           "GROUP BY " +
           "CASE " +
           "WHEN YEAR(CURRENT_DATE) - YEAR(c.dateOfBirth) < 18 THEN 'Dưới 18' " +
           "WHEN YEAR(CURRENT_DATE) - YEAR(c.dateOfBirth) BETWEEN 18 AND 30 THEN '18-30' " +
           "WHEN YEAR(CURRENT_DATE) - YEAR(c.dateOfBirth) BETWEEN 31 AND 50 THEN '31-50' " +
           "ELSE 'Trên 50' END")
    List<Object[]> countCustomersByAgeGroup();
    
    // Tổng số khách hàng
    @Query("SELECT COUNT(c) FROM Customer c")
    long getTotalCustomers();
    
    // Khách hàng mới trong tháng
    @Query("SELECT COUNT(c) FROM Customer c WHERE MONTH(c.createdAt) = MONTH(CURRENT_DATE) AND YEAR(c.createdAt) = YEAR(CURRENT_DATE)")
    long getNewCustomersThisMonth();
    
    // Khách hàng có sinh nhật trong tháng
    @Query("SELECT COUNT(c) FROM Customer c WHERE MONTH(c.dateOfBirth) = MONTH(CURRENT_DATE)")
    long getCustomersWithBirthdayThisMonth();
    
    // ==================== DANH SÁCH ĐẶC BIỆT ====================
    
    // Khách hàng mới nhất
    @Query("SELECT c FROM Customer c ORDER BY c.createdAt DESC")
    Page<Customer> findLatestCustomers(Pageable pageable);
    
    // Khách hàng cần chúc mừng sinh nhật
    @Query("SELECT c FROM Customer c WHERE " +
           "MONTH(c.dateOfBirth) = MONTH(CURRENT_DATE) AND " +
           "DAY(c.dateOfBirth) >= DAY(CURRENT_DATE) AND " +
           "DAY(c.dateOfBirth) <= DAY(CURRENT_DATE) + 7")
    List<Customer> findUpcomingBirthdays();
    
    // ==================== VALIDATION ====================
    
    // Kiểm tra trùng lặp
    @Query("SELECT COUNT(c) > 0 FROM Customer c WHERE c.phone = :phone AND c.id != :excludeId")
    boolean existsByPhoneAndIdNot(@Param("phone") String phone, @Param("excludeId") Long excludeId);
    
    // ==================== FOR POS SYSTEM ====================
    
    // Tìm nhanh khách hàng cho POS (theo SĐT hoặc tên)
    @Query("SELECT c FROM Customer c WHERE " +
           "c.phone = :phone OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY " +
           "CASE WHEN c.phone = :phone THEN 1 ELSE 2 END, " +
           "c.name ASC")
    List<Customer> findForPOS(@Param("phone") String phone, @Param("search") String search);
    
    // Top 10 khách hàng thường xuyên (để suggestion trong POS)
    @Query("SELECT c FROM Customer c ORDER BY c.createdAt DESC")
    List<Customer> findTop10RecentCustomers(Pageable pageable);
    
    // Tìm khách hàng cho autocomplete trong POS
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY c.name ASC")
    List<Customer> findForAutocomplete(@Param("search") String search, Pageable pageable);
    
    // ==================== RECENT CUSTOMERS FOR SUGGESTION ====================
    
    // Tìm khách hàng gần đây cho suggestion (sắp xếp theo updatedAt)
    @Query("SELECT c FROM Customer c ORDER BY c.updatedAt DESC")
    List<Customer> findRecentCustomersForSuggestion(Pageable pageable);
    
    // ==================== REPORTS ====================
    
    // Báo cáo khách hàng theo khoảng thời gian
    @Query("SELECT c FROM Customer c WHERE DATE(c.createdAt) BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC")
    List<Customer> findCustomersByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Khách hàng có địa chỉ đầy đủ
    @Query("SELECT c FROM Customer c WHERE c.province IS NOT NULL AND c.district IS NOT NULL AND c.ward IS NOT NULL AND c.address IS NOT NULL")
    List<Customer> findCustomersWithCompleteAddress();
    
    // Khách hàng thiếu thông tin
    @Query("SELECT c FROM Customer c WHERE c.dateOfBirth IS NULL OR c.province IS NULL OR c.district IS NULL")
    List<Customer> findCustomersWithIncompleteInfo();
    
    // ==================== POS SPECIFIC QUERIES ====================
    
    // Khách hàng VIP (có thể mở rộng sau)
    @Query("SELECT c FROM Customer c WHERE " +
           "YEAR(CURRENT_DATE) - YEAR(c.dateOfBirth) > 50 OR " +
           "c.province IN ('Hà Nội', 'TP. Hồ Chí Minh', 'Đà Nẵng')")
    List<Customer> findVipCustomers();
    
    // Count customers by location for analytics
    @Query("SELECT c.district, COUNT(c) FROM Customer c WHERE c.district IS NOT NULL GROUP BY c.district ORDER BY COUNT(c) DESC")
    List<Object[]> countCustomersByDistrict();
}