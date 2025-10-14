package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.VipRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VipRequestRepository extends JpaRepository<VipRequest, Long> {
    
    // Kiểm tra VIP request đã tồn tại chưa
    boolean existsByCustomerIdAndStatus(Long customerId, VipRequest.RequestStatus status);
    
    // Tìm VIP request theo customer và status
    Optional<VipRequest> findByCustomerIdAndStatus(Long customerId, VipRequest.RequestStatus status);
    
    // Tìm tất cả VIP request theo status
    java.util.List<VipRequest> findByStatus(VipRequest.RequestStatus status);
    
    // Tìm VIP request theo customer
    java.util.List<VipRequest> findByCustomerId(Long customerId);
    
    // Đếm VIP request theo status
    long countByStatus(VipRequest.RequestStatus status);
}