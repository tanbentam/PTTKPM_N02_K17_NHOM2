package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.ReturnRequestItem;
import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, Long> {

    // Lấy tất cả sản phẩm đổi trả của một đơn hàng
    List<ReturnRequestItem> findByOrder(Order order);

    // Lấy sản phẩm đổi trả theo đơn hàng và loại (RETURN hoặc RECEIVE)
    List<ReturnRequestItem> findByOrderAndType(Order order, String type);

    // Lấy sản phẩm đổi trả theo id đơn hàng (dùng khi không có object Order)
    @Query("SELECT r FROM ReturnRequestItem r WHERE r.order.id = :orderId")
    List<ReturnRequestItem> findByOrderId(@Param("orderId") Long orderId);
    
@Query("select r from ReturnRequestItem r join fetch r.product where r.order = :order")
    List<ReturnRequestItem> findByOrderFetchProduct(@Param("order") Order order);
    // Kiểm tra đơn hàng đã có sản phẩm đổi trả chưa
    boolean existsByOrder(Order order);

    // Xóa tất cả sản phẩm đổi trả theo đơn hàng
    void deleteByOrder(Order order);

    // Đếm số sản phẩm đổi trả trong một đơn hàng
    @Query("SELECT COUNT(r) FROM ReturnRequestItem r WHERE r.order.id = :orderId")
    long countByOrderId(@Param("orderId") Long orderId);

    // Tìm sản phẩm đổi trả theo đơn hàng và id sản phẩm
    Optional<ReturnRequestItem> findByOrderAndProduct_Id(Order order, Long productId);
}