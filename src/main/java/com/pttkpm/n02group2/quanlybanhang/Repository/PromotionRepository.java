package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    List<Promotion> findByActiveTrue();
    
    @Query("SELECT p FROM Promotion p WHERE p.active = true AND :date BETWEEN p.startDate AND p.endDate")
    List<Promotion> findActivePromotionsOnDate(@Param("date") LocalDate date);
    
    @Query("SELECT p FROM Promotion p WHERE p.active = true AND CURRENT_DATE BETWEEN p.startDate AND p.endDate")
    List<Promotion> findCurrentActivePromotions();
}