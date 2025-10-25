package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // ===== EXISTING METHODS (giữ nguyên) =====
    List<Product> findByCategory(String category);
    List<Product> findByQuantityLessThan(Integer quantity);
    
    @Query("SELECT p FROM Product p WHERE p.quantity > 0 ORDER BY p.updatedDate DESC")
    List<Product> findAvailableProducts();
    
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> searchProducts(@Param("keyword") String keyword);
    
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
    
    // ===== NEW METHODS FOR ADVANCED SEARCH =====
    
    // Tìm kiếm sản phẩm với nhiều điều kiện và phân trang
    @Query("SELECT p FROM Product p WHERE " +
           "(:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:stockStatus IS NULL OR :stockStatus = '' OR " +
           "  (:stockStatus = 'in-stock' AND p.quantity > 10) OR " +
           "  (:stockStatus = 'low-stock' AND p.quantity BETWEEN 1 AND 10) OR " +
           "  (:stockStatus = 'out-of-stock' AND p.quantity = 0)" +
           ") ORDER BY p.id DESC")
    Page<Product> findProductsWithFilters(
        @Param("search") String search,
        @Param("category") String category,
        @Param("stockStatus") String stockStatus,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        Pageable pageable
    );
    
    // Lấy tất cả categories có trong database
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findDistinctCategories();
    
    // ===== COUNT METHODS FOR STATISTICS =====
    
    // Đếm sản phẩm theo số lượng
    long countByQuantityGreaterThan(int quantity);
    long countByQuantityBetween(int min, int max);
    long countByQuantity(int quantity);
    
    // Đếm sản phẩm theo danh mục
    long countByCategory(String category);
    

    // Trong ProductRepository.java (nếu dùng Spring Data JPA)
Optional<Product> findById(Long id);
    // ===== ADDITIONAL USEFUL METHODS =====
    
    // Tìm sản phẩm sắp hết hàng (quantity <= 10)
    @Query("SELECT p FROM Product p WHERE p.quantity > 0 AND p.quantity <= 10 ORDER BY p.quantity ASC")
    List<Product> findLowStockProducts();
    
    // Tìm sản phẩm hết hàng
    @Query("SELECT p FROM Product p WHERE p.quantity = 0 ORDER BY p.updatedDate DESC")
    List<Product> findOutOfStockProducts();
    
    // Tìm sản phẩm theo khoảng giá với phân trang
    Page<Product> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);
    
    // Tìm sản phẩm theo danh mục với phân trang
    Page<Product> findByCategory(String category, Pageable pageable);
    Page<Product> findByQuantityGreaterThan(Integer quantity, Pageable pageable);
    
    // Tìm sản phẩm có số lượng > quantity
    List<Product> findByQuantityGreaterThan(Integer quantity);
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.quantity > :quantity")
    Page<Product> findByNameContainingIgnoreCaseAndQuantityGreaterThan(@Param("name") String name, @Param("quantity") Integer quantity, Pageable pageable);
    // Tìm sản phẩm có số lượng > quantity, sắp xếp theo ID giảm dần
    @Query("SELECT p FROM Product p WHERE p.quantity > :quantity ORDER BY p.id DESC")
    List<Product> findByQuantityGreaterThanOrderByIdDesc(@Param("quantity") Integer quantity);
    
    // Tìm sản phẩm theo tên và số lượng > quantity
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.quantity > :quantity")
    List<Product> findByNameContainingIgnoreCaseAndQuantityGreaterThan(@Param("name") String name, @Param("quantity") Integer quantity);
    @Query("SELECT p FROM Product p WHERE p.category = :category AND p.id != :id AND p.quantity > :quantity")
    List<Product> findByCategoryAndIdNotAndQuantityGreaterThan(@Param("category") String category, @Param("id") Long id, @Param("quantity") Integer quantity);
    
    // Tìm kiếm theo tên với phân trang
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY p.name")
    Page<Product> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);
}