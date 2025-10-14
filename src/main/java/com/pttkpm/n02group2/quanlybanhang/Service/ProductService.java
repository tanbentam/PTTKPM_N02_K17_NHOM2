package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.Product;
import com.pttkpm.n02group2.quanlybanhang.Repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository productRepository;

    // ==================== SEARCH AND FILTER METHODS ====================

    /**
     * Tìm kiếm sản phẩm với các bộ lọc.
     */
    public Page<Product> searchProducts(String search, String category, String stockStatus,
                                       Double minPrice, Double maxPrice, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        logger.info("Searching products with filters - search: {}, category: {}, stockStatus: {}, price: {}-{}, page: {}, size: {}",
                    search, category, stockStatus, minPrice, maxPrice, page, size);

        if (search == null && category == null && stockStatus == null && minPrice == null && maxPrice == null) {
            return productRepository.findAll(pageable);
        }

        Page<Product> result = productRepository.findProductsWithFilters(search, category, stockStatus, minPrice, maxPrice, pageable);
        logger.info("Found {} products", result.getTotalElements());
        return result;
    }

    /**
     * Lấy tất cả danh mục.
     */
    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }

    // ==================== CRUD OPERATIONS ====================

    /**
     * Lưu sản phẩm (tạo mới hoặc cập nhật).
     */
    public Product saveProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        logger.info("Saving product: {}", product.getName());
        return productRepository.save(product);
    }

    /**
     * Tạo sản phẩm mới.
     */
    public Product createProduct(Product product) {
        if (product.getId() != null) {
            throw new IllegalArgumentException("New product should not have ID");
        }
        return saveProduct(product);
    }

    /**
     * Cập nhật sản phẩm theo ID.
     */
    public Product updateProduct(Long id, Product productDetails) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setName(productDetails.getName());
            product.setDescription(productDetails.getDescription());
            product.setPrice(productDetails.getPrice());
            product.setQuantity(productDetails.getQuantity());
            product.setCategory(productDetails.getCategory());
            product.setImagePath(productDetails.getImagePath());
            logger.info("Updating product ID: {}", id);
            return productRepository.save(product);
        }
        throw new RuntimeException("Product not found with ID: " + id);
    }

    /**
     * Xóa sản phẩm theo ID.
     */
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with ID: " + id);
        }
        logger.info("Deleting product ID: {}", id);
        productRepository.deleteById(id);
    }


     public long countAll() {
        return productRepository.count();
    }
    
    /**
     * Lấy sản phẩm theo ID.
     */
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // ==================== INVENTORY MANAGEMENT ====================

    /**
     * Cập nhật tồn kho cho sản phẩm.
     */
    public Product updateInventory(Long id, Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity must be non-negative");
        }
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            product.setQuantity(quantity);
            logger.info("Updating inventory for product ID: {} to quantity: {}", id, quantity);
            return productRepository.save(product);
        }
        throw new RuntimeException("Product not found with ID: " + id);
    }

    // ==================== BUSINESS LOGIC METHODS ====================

    /**
     * Lấy tất cả sản phẩm.
     */
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    /**
     * Lấy sản phẩm theo danh mục.
     */
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    /**
     * Lấy sản phẩm có tồn kho thấp.
     */
    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findByQuantityLessThan(threshold);
    }

    /**
     * Lấy sản phẩm theo khoảng giá.
     */
    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa.
     */
    public List<Product> searchProductsByKeyword(String keyword) {
        return productRepository.searchProducts(keyword);
    }

    /**
     * Lấy sản phẩm có sẵn (quantity > 0).
     */
    public List<Product> getAvailableProducts() {
        return productRepository.findByQuantityGreaterThan(0);
    }

    // ==================== PAGINATED METHODS ====================

    /**
     * Lấy tất cả sản phẩm có phân trang.
     */
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    /**
     * Tìm sản phẩm theo tên (không phân biệt hoa thường) có phân trang.
     */
    public Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    /**
     * Lấy sản phẩm có quantity > threshold có phân trang.
     */
    public Page<Product> findByQuantityGreaterThan(Integer quantity, Pageable pageable) {
        return productRepository.findByQuantityGreaterThan(quantity, pageable);
    }

    /**
     * Tìm sản phẩm theo tên và quantity > threshold có phân trang.
     */
    public Page<Product> findByNameContainingIgnoreCaseAndQuantityGreaterThan(String name, Integer quantity, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCaseAndQuantityGreaterThan(name, quantity, pageable);
    }

    /**
     * Lấy sản phẩm theo danh mục, không phải ID, và quantity > threshold.
     */
    public List<Product> findByCategoryAndIdNotAndQuantityGreaterThan(String category, Long id, Integer quantity) {
        return productRepository.findByCategoryAndIdNotAndQuantityGreaterThan(category, id, quantity);
    }

    /**
     * Lấy sản phẩm có quantity > threshold, sắp xếp theo ID giảm dần.
     */
    public List<Product> findByQuantityGreaterThanOrderByIdDesc(Integer quantity) {
        return productRepository.findByQuantityGreaterThanOrderByIdDesc(quantity);
    }

    // ==================== STATISTICS METHODS ====================

    /**
     * Đếm tổng số sản phẩm.
     */
    public long getTotalProducts() {
        return productRepository.count();
    }

    /**
     * Đếm sản phẩm theo trạng thái tồn kho.
     */
    public long countProductsByStockStatus(String status) {
        switch (status) {
            case "in-stock":
                return productRepository.countByQuantityGreaterThan(10);
            case "low-stock":
                return productRepository.countByQuantityBetween(1, 10);
            case "out-of-stock":
                return productRepository.countByQuantity(0);
            default:
                return 0;
        }
    }

    /**
     * Lấy số lượng sản phẩm có sẵn.
     */
    public long getInStockCount() {
        return countProductsByStockStatus("in-stock");
    }

    /**
     * Lấy số lượng sản phẩm tồn kho thấp.
     */
    public long getLowStockCount() {
        return countProductsByStockStatus("low-stock");
    }

    /**
     * Lấy số lượng sản phẩm hết hàng.
     */
    public long getOutOfStockCount() {
        return countProductsByStockStatus("out-of-stock");
    }

    // ==================== BULK OPERATIONS ====================

    /**
     * Cập nhật tồn kho hàng loạt.
     */
    @Transactional
    public void bulkUpdateStock(List<Long> productIds, Integer newQuantity) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Product IDs cannot be null or empty");
        }
        if (newQuantity == null || newQuantity < 0) {
            throw new IllegalArgumentException("New quantity must be non-negative");
        }
        logger.info("Bulk updating stock for {} products to quantity: {}", productIds.size(), newQuantity);
        for (Long id : productIds) {
            updateInventory(id, newQuantity);
        }
    }

    /**
     * Xóa sản phẩm hàng loạt.
     */
    @Transactional
    public void bulkDelete(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            throw new IllegalArgumentException("Product IDs cannot be null or empty");
        }
        logger.info("Bulk deleting {} products", productIds.size());
        for (Long id : productIds) {
            deleteProduct(id);
        }
    }
}