package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    // Tìm inventory theo tên sản phẩm (không phân biệt hoa thường) - PHƯƠNG THỨC BỊ THIẾU
    List<Inventory> findByProductNameContainingIgnoreCase(String productName);
    
    // Tìm inventory theo danh mục
    List<Inventory> findByCategory(String category);
    
    // Tìm inventory có số lượng ít hơn ngưỡng
    List<Inventory> findByQuantityLessThan(Integer quantity);
    
    // Tìm inventory theo khoảng giá
    List<Inventory> findByUnitPriceBetween(Double minPrice, Double maxPrice);
    
    // Tìm inventory theo tên sản phẩm chính xác
    List<Inventory> findByProductName(String productName);
    
    // Tìm inventory có số lượng lớn hơn ngưỡng
    List<Inventory> findByQuantityGreaterThan(Integer quantity);
    
    // Tìm inventory theo tên sản phẩm và danh mục
    List<Inventory> findByProductNameContainingIgnoreCaseAndCategory(String productName, String category);
    
    // Tìm inventory có giá trong khoảng và danh mục
    List<Inventory> findByUnitPriceBetweenAndCategory(Double minPrice, Double maxPrice, String category);

    // Tìm inventory theo ID
    Optional<Inventory> findById(Long id);
    
    // Tìm inventory theo ID sản phẩm
    Optional<Inventory> findByProduct_Id(Long productId);
}