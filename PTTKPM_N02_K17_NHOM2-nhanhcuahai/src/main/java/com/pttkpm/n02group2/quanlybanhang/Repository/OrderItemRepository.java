package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.OrderItem;
import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    // ==================== BASIC FINDERS ====================
    
    // Tìm OrderItem theo Product (sử dụng relationship)
    List<OrderItem> findByProduct(Product product);
    
    // Tìm OrderItem theo Product ID (sử dụng @Query)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId")
    List<OrderItem> findByProductId(@Param("productId") Long productId);
    
    // Tìm OrderItem theo Order
    List<OrderItem> findByOrder(Order order);
    
    // Tìm OrderItem theo Order ID (sử dụng @Query)
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findByOrderId(@Param("orderId") Long orderId);
    
    // ==================== QUANTITY & PRICE QUERIES ====================
    
    // Tìm OrderItem theo quantity
    List<OrderItem> findByQuantity(Integer quantity);
    
    // Tìm OrderItem có quantity lớn hơn
    @Query("SELECT oi FROM OrderItem oi WHERE oi.quantity > :minQuantity")
    List<OrderItem> findByQuantityGreaterThan(@Param("minQuantity") Integer minQuantity);
    
    // Tìm OrderItem theo khoảng giá
    @Query("SELECT oi FROM OrderItem oi WHERE oi.unitPrice BETWEEN :minPrice AND :maxPrice")
    List<OrderItem> findByPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);
    
    // ==================== STATISTICS QUERIES ====================
    
    // Tính tổng quantity đã bán của một product
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.product.id = :productId")
    Long getTotalQuantitySoldByProduct(@Param("productId") Long productId);
    
    // Tính tổng revenue của một product
    @Query("SELECT COALESCE(SUM(oi.totalPrice), 0.0) FROM OrderItem oi WHERE oi.product.id = :productId")
    Double getTotalRevenueByProduct(@Param("productId") Long productId);
    
    // Tìm top sản phẩm bán chạy (theo quantity)
    @Query("SELECT oi.product, SUM(oi.quantity) as totalSold FROM OrderItem oi GROUP BY oi.product ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(org.springframework.data.domain.Pageable pageable);
    
    // Tìm top sản phẩm theo revenue
    @Query("SELECT oi.product, SUM(oi.totalPrice) as totalRevenue FROM OrderItem oi GROUP BY oi.product ORDER BY totalRevenue DESC")
    List<Object[]> findTopProductsByRevenue(org.springframework.data.domain.Pageable pageable);
    
    // ==================== ORDER ANALYSIS ====================
    
    // Đếm số items trong một order
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.order.id = :orderId")
    Long countItemsInOrder(@Param("orderId") Long orderId);
    
    // Tính tổng value của một order
    @Query("SELECT COALESCE(SUM(oi.totalPrice), 0.0) FROM OrderItem oi WHERE oi.order.id = :orderId")
    Double getTotalValueByOrder(@Param("orderId") Long orderId);
    
    // ==================== PRODUCT ANALYSIS ====================
    
    // Tìm các order có chứa product cụ thể
    @Query("SELECT DISTINCT oi.order FROM OrderItem oi WHERE oi.product.id = :productId")
    List<Order> findOrdersContainingProduct(@Param("productId") Long productId);
    
    // Đếm số lần product được mua
    @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.product.id = :productId")
    Long countOrdersContainingProduct(@Param("productId") Long productId);
    
    // ==================== VALIDATION QUERIES ====================
    
    // Kiểm tra product có được bán chưa
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi WHERE oi.product.id = :productId")
    boolean existsByProductId(@Param("productId") Long productId);
    
    // Kiểm tra order có items chưa
    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM OrderItem oi WHERE oi.order.id = :orderId")
    boolean existsByOrderId(@Param("orderId") Long orderId);
    
    // ==================== RECENT ITEMS ====================
    
    // Lấy các items gần đây
    @Query("SELECT oi FROM OrderItem oi ORDER BY oi.createdDate DESC")
    List<OrderItem> findRecentItems(org.springframework.data.domain.Pageable pageable);
    
    // Lấy items của product trong khoảng thời gian
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id = :productId AND oi.createdDate BETWEEN :startDate AND :endDate")
    List<OrderItem> findProductItemsBetweenDates(@Param("productId") Long productId, 
                                                @Param("startDate") java.time.LocalDateTime startDate,
                                                @Param("endDate") java.time.LocalDateTime endDate);
}