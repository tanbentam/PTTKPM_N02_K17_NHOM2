package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // ==================== PHÂN TRANG METHODS ====================
    // Method phân trang cho khoảng thời gian (dùng LocalDateTime)
    Page<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
Page<Order> findByOrderDateBetweenOrderByOrderDateDesc(LocalDateTime from, LocalDateTime to, Pageable pageable);

    // Method phân trang cho tất cả orders
    Page<Order> findAll(Pageable pageable);
 Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    // Method phân trang sắp xếp theo createdAt desc (dùng trong POSController)
    Page<Order> findAllByOrderByOrderDateDesc(Pageable pageable);

    // ==================== CUSTOM FINDERS ====================
    List<Order> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    List<Order> findByCreatedBy(String username);

    // Tìm đơn hàng theo khoảng thời gian và trạng thái (cho POSService)
    List<Order> findByCreatedAtBetweenAndStatus(LocalDateTime startDate, LocalDateTime endDate, Order.OrderStatus status);

    // ==================== BASIC FINDERS ====================
    @Transactional
    void deleteByCustomerId(Long customerId);

    // Tìm đơn hàng theo khách hàng
    List<Order> findByCustomer(Customer customer);

    // Tìm đơn hàng theo trạng thái
    List<Order> findByStatus(Order.OrderStatus status);

    @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.status = 'COMPLETED'")
    Double sumTotalRevenue();

    // Tìm đơn hàng theo ID khách hàng, sắp xếp theo ngày tạo giảm dần (cho POSService)
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // ==================== COUNT METHODS ====================
    // Thêm method này để lấy đơn hàng theo customerId và sắp xếp theo ngày tạo
    List<Order> findByCustomerIdOrderByOrderDateAsc(Long customerId);

    // Đếm đơn hàng theo trạng thái
    long countByStatus(Order.OrderStatus status);

    // Đếm tổng số đơn hàng
    @Query("SELECT COUNT(o) FROM Order o")
    long countAllOrders();

    // ==================== REVENUE STATISTICS ====================

    // Tổng doanh thu tất cả đơn hàng
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0.0) FROM Order o WHERE o.status != 'CANCELLED'")
    Double getTotalRevenue();

    // Tổng doanh thu theo trạng thái
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0.0) FROM Order o WHERE o.status = :status")
    Double getTotalRevenueByStatus(@Param("status") Order.OrderStatus status);

    // Tổng doanh thu theo khách hàng
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0.0) FROM Order o WHERE o.customer.id = :customerId AND o.status != 'CANCELLED'")
    Double getTotalRevenueByCustomerId(@Param("customerId") Long customerId);

    // ==================== VIP DISCOUNT STATISTICS ====================

    // Tổng số tiền giảm giá VIP
    @Query("SELECT COALESCE(SUM(o.vipDiscountAmount), 0.0) FROM Order o WHERE o.status != 'CANCELLED'")
    Double getTotalVipDiscountAmount();

    // Tổng số tiền giảm giá VIP theo khách hàng
    @Query("SELECT COALESCE(SUM(o.vipDiscountAmount), 0.0) FROM Order o WHERE o.customer.id = :customerId AND o.status != 'CANCELLED'")
    Double getTotalVipDiscountByCustomerId(@Param("customerId") Long customerId);

    // ==================== ORDER SEARCH ====================

    // Tìm đơn hàng theo mã đơn hàng
    Optional<Order> findByOrderNumber(String orderNumber);

    // Tìm đơn hàng theo khoảng thời gian
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);

    // Tìm đơn hàng có giảm giá VIP
    @Query("SELECT o FROM Order o WHERE o.vipDiscountAmount > 0 ORDER BY o.orderDate DESC")
    List<Order> findOrdersWithVipDiscount();

    // Tìm đơn hàng theo từ khóa (order number hoặc customer name)
    @Query("SELECT o FROM Order o WHERE o.orderNumber LIKE %:keyword% OR o.customer.name LIKE %:keyword% ORDER BY o.orderDate DESC")
    List<Order> searchOrders(@Param("keyword") String keyword);

    // ==================== CUSTOMER ORDER HISTORY ====================

    // Lịch sử đơn hàng của khách hàng (mới nhất trước)
    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.orderDate DESC")
    List<Order> findCustomerOrderHistory(@Param("customerId") Long customerId);

    // Method với phân trang
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    // Method không phân trang
    List<Order> findByCustomerId(Long customerId);

    // ==================== RECENT ORDERS ====================

    // Đơn hàng gần đây nhất (cho dashboard)
    @Query("SELECT o FROM Order o ORDER BY o.orderDate DESC")
    List<Order> findRecentOrders(Pageable pageable);

    // ==================== PENDING ORDERS ====================

    // Đơn hàng đang chờ xử lý
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' ORDER BY o.orderDate ASC")
    List<Order> findPendingOrders();

    // Đơn hàng hoàn thành
    @Query("SELECT o FROM Order o WHERE o.status = 'DELIVERED' ORDER BY o.orderDate DESC")
    List<Order> findCompletedOrders();

    // Đơn hàng đã hủy
    @Query("SELECT o FROM Order o WHERE o.status = 'CANCELLED' ORDER BY o.orderDate DESC")
    List<Order> findCancelledOrders();

    // ==================== DAILY STATISTICS ====================

    // Doanh thu hôm nay
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0.0) FROM Order o WHERE DATE(o.orderDate) = CURRENT_DATE AND o.status != 'CANCELLED'")
    Double getTodayRevenue();

    // Số đơn hàng hôm nay
    @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.orderDate) = CURRENT_DATE")
    Long getTodayOrderCount();

    // ==================== MONTHLY STATISTICS ====================

    // Doanh thu theo tháng
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0.0) FROM Order o WHERE YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month AND o.status != 'CANCELLED'")
    Double getMonthlyRevenue(@Param("year") int year, @Param("month") int month);

    // Số đơn hàng theo tháng
    @Query("SELECT COUNT(o) FROM Order o WHERE YEAR(o.orderDate) = :year AND MONTH(o.orderDate) = :month")
    Long getMonthlyOrderCount(@Param("year") int year, @Param("month") int month);

    // Doanh thu tháng hiện tại
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0.0) FROM Order o WHERE YEAR(o.orderDate) = YEAR(CURRENT_DATE) AND MONTH(o.orderDate) = MONTH(CURRENT_DATE) AND o.status != 'CANCELLED'")
    Double getCurrentMonthRevenue();

    // ==================== YEARLY STATISTICS ====================

    // Doanh thu theo năm
    @Query("SELECT COALESCE(SUM(o.finalAmount), 0.0) FROM Order o WHERE YEAR(o.orderDate) = :year AND o.status != 'CANCELLED'")
    Double getYearlyRevenue(@Param("year") int year);

    // Số đơn hàng theo năm
    @Query("SELECT COUNT(o) FROM Order o WHERE YEAR(o.orderDate) = :year")
    Long getYearlyOrderCount(@Param("year") int year);

    // ==================== TOP CUSTOMERS ====================

    // Top khách hàng theo doanh thu
    @Query("SELECT o.customer, SUM(o.finalAmount) as total FROM Order o WHERE o.status != 'CANCELLED' GROUP BY o.customer ORDER BY total DESC")
    List<Object[]> findTopCustomersByRevenue(Pageable pageable);

    // Top khách hàng theo số lượng đơn hàng
    @Query("SELECT o.customer, COUNT(o) as orderCount FROM Order o WHERE o.status != 'CANCELLED' GROUP BY o.customer ORDER BY orderCount DESC")
    List<Object[]> findTopCustomersByOrderCount(Pageable pageable);

    // ==================== ORDER VALIDATION ====================

    // Kiểm tra mã đơn hàng đã tồn tại chưa
    boolean existsByOrderNumber(String orderNumber);

    // Kiểm tra khách hàng có đơn hàng nào chưa
    boolean existsByCustomer(Customer customer);

    // ==================== ADVANCED QUERIES ====================

    // Đơn hàng có giá trị cao nhất
    @Query("SELECT o FROM Order o WHERE o.finalAmount = (SELECT MAX(o2.finalAmount) FROM Order o2)")
    List<Order> findHighestValueOrders();

    // Đơn hàng có nhiều item nhất
    @Query("SELECT o FROM Order o WHERE SIZE(o.items) = (SELECT MAX(SIZE(o2.items)) FROM Order o2)")
    List<Order> findOrdersWithMostItems();

    // Tỷ lệ đơn hàng hoàn thành
    @Query("SELECT (COUNT(o) * 100.0 / (SELECT COUNT(o2) FROM Order o2)) FROM Order o WHERE o.status = 'DELIVERED'")
    Double getCompletionRate();
}