package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.ReturnExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReturnExchangeRepository extends JpaRepository<ReturnExchangeRequest, Long> {
    List<ReturnExchangeRequest> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<ReturnExchangeRequest> findByOriginalOrder_Customer_Id(Long customerId);
    
    // Tìm yêu cầu theo mã đơn hàng gốc
    ReturnExchangeRequest findByOriginalOrder_Id(Long orderId);
    
    // Tìm yêu cầu theo loại và trạng thái
    List<ReturnExchangeRequest> findByTypeAndStatus(
        ReturnExchangeRequest.ReturnExchangeType type,
        ReturnExchangeRequest.ReturnExchangeStatus status);
    
    // Tìm yêu cầu theo trạng thái
    List<ReturnExchangeRequest> findByStatus(ReturnExchangeRequest.ReturnExchangeStatus status);
    
    // Đếm số yêu cầu đang chờ xử lý
    long countByStatus(ReturnExchangeRequest.ReturnExchangeStatus status);
}