package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.PromotionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PromotionCategoryRepository extends JpaRepository<PromotionCategory, Long> {
    
    List<PromotionCategory> findByPromotionId(Long promotionId);
    
    void deleteByPromotionId(Long promotionId);
    
    // Batch query: Lấy tất cả promotion-category cho nhiều danh mục
    @Query("SELECT pc FROM PromotionCategory pc WHERE pc.category IN :categories")
    List<PromotionCategory> findByCategoryIn(@Param("categories") Set<String> categories);
}