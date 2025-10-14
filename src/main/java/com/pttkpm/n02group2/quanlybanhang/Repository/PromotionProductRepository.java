package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.PromotionProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PromotionProductRepository extends JpaRepository<PromotionProduct, Long> {
    
    List<PromotionProduct> findByPromotionId(Long promotionId);
    
    Optional<PromotionProduct> findByPromotionIdAndProductId(Long promotionId, Long productId);
    
    void deleteByPromotionId(Long promotionId);
    
    // Batch query: Lấy tất cả promotion-product cho nhiều sản phẩm
    @Query("SELECT pp FROM PromotionProduct pp WHERE pp.product.id IN :productIds")
    List<PromotionProduct> findByProductIdIn(@Param("productIds") Set<Long> productIds);
}