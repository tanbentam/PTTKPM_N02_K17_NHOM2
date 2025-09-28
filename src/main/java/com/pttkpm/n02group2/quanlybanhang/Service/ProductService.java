package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.Product;
import com.pttkpm.n02group2.quanlybanhang.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    // ==================== MAIN SEARCH METHOD ====================
    
    public Page<Product> searchProducts(String search, String category, String stockStatus, 
                                       Double minPrice, Double maxPrice, int page, int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        
        System.out.println("=== ProductService.searchProducts ===");
        System.out.println("Search: '" + search + "'");
        System.out.println("Category: '" + category + "'");
        System.out.println("Stock Status: '" + stockStatus + "'");
        System.out.println("Price range: " + minPrice + " - " + maxPrice);
        
        // Nếu không có điều kiện gì, trả về tất cả
        if (search == null && category == null && stockStatus == null && minPrice == null && maxPrice == null) {
            System.out.println("No filters, returning all products");
            return productRepository.findAll(pageable);
        }
        
        // Sử dụng method tìm kiếm có điều kiện
        Page<Product> result = productRepository.findProductsWithFilters(
            search, category, stockStatus, minPrice, maxPrice, pageable);
        
        System.out.println("Search result: " + result.getTotalElements() + " products found");
        return result;
    }
    
    // ==================== UTILITY METHODS ====================
    
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
    
    public List<String> getAllCategories() {
        return productRepository.findDistinctCategories();
    }
    
    // ==================== COUNT METHODS FOR STATISTICS ====================
    
    public long countAllProducts() {
        return productRepository.count();
    }
    
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
    
    // ==================== CRUD OPERATIONS ====================
    public void updateProduct(Product product) {
        productRepository.save(product);  // Giả sử có ProductRepository
    }
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }
    
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }
    
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
            return productRepository.save(product);
        }
        throw new RuntimeException("Không tìm thấy sản phẩm với ID: " + id);
    }
    
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
    
    public Product updateInventory(Long id, Integer quantity) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            if (quantity < 0) {
                throw new IllegalArgumentException("Số lượng không thể âm");
            }
            product.setQuantity(quantity);
            return productRepository.save(product);
        }
        throw new RuntimeException("Không tìm thấy sản phẩm với ID: " + id);
    }
    
    // ==================== BUSINESS LOGIC METHODS ====================
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findByQuantityLessThan(threshold);
    }
    
    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }
    
    public List<Product> searchProductsByKeyword(String keyword) {
        return productRepository.searchProducts(keyword);
    }
    
    public List<Product> getAvailableProducts() {
        return productRepository.findAvailableProducts();
    }
    
    // ==================== METHODS FOR USER INTERFACE ====================
    
    public Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(name, pageable);
    }
    
    public Page<Product> findByQuantityGreaterThan(Integer quantity, Pageable pageable) {
        return productRepository.findByQuantityGreaterThan(quantity, pageable);
    }
    
    public List<Product> findByQuantityGreaterThan(Integer quantity) {
        return productRepository.findByQuantityGreaterThan(quantity);
    }
    
    public Page<Product> findByNameContainingIgnoreCaseAndQuantityGreaterThan(String name, Integer quantity, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCaseAndQuantityGreaterThan(name, quantity, pageable);
    }
    
    public List<Product> findByNameContainingIgnoreCaseAndQuantityGreaterThan(String name, Integer quantity) {
        return productRepository.findByNameContainingIgnoreCaseAndQuantityGreaterThan(name, quantity);
    }
    
    public List<Product> findByCategoryAndIdNotAndQuantityGreaterThan(String category, Long id, Integer quantity) {
        return productRepository.findByCategoryAndIdNotAndQuantityGreaterThan(category, id, quantity);
    }
    
    public List<Product> findByQuantityGreaterThanOrderByIdDesc(Integer quantity) {
        return productRepository.findByQuantityGreaterThanOrderByIdDesc(quantity);
    }
    
    // ==================== OPTIMIZED STATISTICS METHODS ====================
    
    public long getTotalProducts() {
        return countAllProducts();
    }
    
    public long getInStockCount() {
        return countProductsByStockStatus("in-stock");
    }
    
    public long getLowStockCount() {
        return countProductsByStockStatus("low-stock");
    }
    
    public long getOutOfStockCount() {
        return countProductsByStockStatus("out-of-stock");
    }
    
    // ==================== BULK OPERATIONS ====================
    
    public void bulkUpdateStock(List<Long> productIds, Integer newQuantity) {
        for (Long id : productIds) {
            updateInventory(id, newQuantity);
        }
    }
    
    public void bulkDelete(List<Long> productIds) {
        for (Long id : productIds) {
            deleteProduct(id);
        }
    }
}