package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.PromotionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionCategoryRepository extends JpaRepository<PromotionCategory, Long> {
    
    List<PromotionCategory> findByPromotionId(Long promotionId);
    
    void deleteByPromotionId(Long promotionId);
}