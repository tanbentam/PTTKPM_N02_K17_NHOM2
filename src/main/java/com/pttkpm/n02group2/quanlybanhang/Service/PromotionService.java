package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PromotionService {
    
    @Autowired
    private PromotionRepository promotionRepository;
    
    @Autowired
    private PromotionCategoryRepository promotionCategoryRepository;
    
    @Autowired
    private PromotionProductRepository promotionProductRepository;
    
    @Autowired
    private ProductRepository productRepository;

    // Cache khuyến mại đang hoạt động để tránh truy vấn lặp lại
    private List<Promotion> cachedActivePromotions;
    private LocalDate lastCacheDate;
    
    // Cache promotion mappings
    private Map<Long, List<PromotionProduct>> cachedProductPromotions;
    private Map<String, List<PromotionCategory>> cachedCategoryPromotions;
    
    // Lấy các chương trình khuyến mại đang hoạt động (có cache)
    public List<Promotion> getActivePromotions() {
        LocalDate today = LocalDate.now();
        
        // Kiểm tra cache
        if (cachedActivePromotions != null && 
            lastCacheDate != null && 
            lastCacheDate.equals(today)) {
            return cachedActivePromotions;
        }
        
        // Làm mới cache
        cachedActivePromotions = promotionRepository.findCurrentActivePromotions();
        lastCacheDate = today;
        
        return cachedActivePromotions;
    }
    
    // Tối ưu: Lấy thông tin khuyến mại cho nhiều sản phẩm cùng lúc
    public Map<Long, Map<String, Object>> getBulkDiscountInfo(List<Product> products) {
        Map<Long, Map<String, Object>> result = new HashMap<>();
        
        if (products.isEmpty()) {
            return result;
        }
        
        // Lấy khuyến mại đang hoạt động
        List<Promotion> activePromotions = getActivePromotions();
        
        if (activePromotions.isEmpty()) {
            // Không có khuyến mại nào, trả về giá gốc
            for (Product product : products) {
                Map<String, Object> info = createNoDiscountInfo(product);
                result.put(product.getId(), info);
            }
            return result;
        }
        
        // Lấy tất cả product IDs và categories
        Set<Long> productIds = products.stream()
            .map(Product::getId)
            .collect(Collectors.toSet());
            
        Set<String> categories = products.stream()
            .map(Product::getCategory)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        // Batch query: Lấy tất cả promotion-product mappings cần thiết
        Map<Long, List<PromotionProduct>> productPromotionMap = getProductPromotionMappings(productIds);
        
        // Batch query: Lấy tất cả promotion-category mappings cần thiết  
        Map<String, List<PromotionCategory>> categoryPromotionMap = getCategoryPromotionMappings(categories);
        
        // Xử lý từng sản phẩm
        for (Product product : products) {
            Map<String, Object> discountInfo = calculateDiscountForProduct(
                product, 
                activePromotions,
                productPromotionMap.getOrDefault(product.getId(), Collections.emptyList()),
                categoryPromotionMap.getOrDefault(product.getCategory(), Collections.emptyList())
            );
            result.put(product.getId(), discountInfo);
        }
        
        return result;
    }
    
    // Batch query cho product promotions
    private Map<Long, List<PromotionProduct>> getProductPromotionMappings(Set<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Query một lần tất cả promotion-product cho các sản phẩm cần thiết
        List<PromotionProduct> allProductPromotions = promotionProductRepository.findByProductIdIn(productIds);
        
        // Group theo productId
        return allProductPromotions.stream()
            .collect(Collectors.groupingBy(pp -> pp.getProduct().getId()));
    }
    
    // Batch query cho category promotions
    private Map<String, List<PromotionCategory>> getCategoryPromotionMappings(Set<String> categories) {
        if (categories.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Query một lần tất cả promotion-category cho các danh mục cần thiết
        List<PromotionCategory> allCategoryPromotions = promotionCategoryRepository.findByCategoryIn(categories);
        
        // Group theo category
        return allCategoryPromotions.stream()
            .collect(Collectors.groupingBy(PromotionCategory::getCategory));
    }
    
    // Tính toán khuyến mại cho một sản phẩm với dữ liệu đã load sẵn
    private Map<String, Object> calculateDiscountForProduct(
            Product product,
            List<Promotion> activePromotions,
            List<PromotionProduct> productPromotions,
            List<PromotionCategory> categoryPromotions) {
        
        Map<String, Object> info = new HashMap<>();
        double originalPrice = product.getPrice();
        double maxDiscount = 0.0;
        String discountSource = "Không có khuyến mại";
        
        // Kiểm tra khuyến mại riêng cho sản phẩm
        for (PromotionProduct pp : productPromotions) {
            if (isPromotionActive(pp.getPromotion(), activePromotions)) {
                if (pp.getDiscountPercent() > maxDiscount) {
                    maxDiscount = pp.getDiscountPercent();
                    discountSource = "Giảm giá riêng: " + pp.getPromotion().getName();
                }
            }
        }
        
        // Nếu chưa có khuyến mại riêng, kiểm tra khuyến mại theo danh mục
        if (maxDiscount == 0.0) {
            for (PromotionCategory pc : categoryPromotions) {
                if (isPromotionActive(pc.getPromotion(), activePromotions)) {
                    if (pc.getDiscountPercent() > maxDiscount) {
                        maxDiscount = pc.getDiscountPercent();
                        discountSource = "Giảm giá danh mục " + pc.getCategory() + ": " + pc.getPromotion().getName();
                    }
                }
            }
        }
        
        info.put("originalPrice", originalPrice);
        info.put("discountPercent", maxDiscount);
        info.put("discountedPrice", originalPrice * (1 - maxDiscount / 100));
        info.put("discountSource", discountSource);
        info.put("hasDiscount", maxDiscount > 0);
        
        return info;
    }
    
    // Helper method để kiểm tra promotion có trong danh sách active không
    private boolean isPromotionActive(Promotion promotion, List<Promotion> activePromotions) {
        return activePromotions.stream()
            .anyMatch(ap -> ap.getId().equals(promotion.getId()));
    }
    
    // Tạo thông tin không có khuyến mại
    private Map<String, Object> createNoDiscountInfo(Product product) {
        Map<String, Object> info = new HashMap<>();
        double originalPrice = product.getPrice();
        
        info.put("originalPrice", originalPrice);
        info.put("discountPercent", 0.0);
        info.put("discountedPrice", originalPrice);
        info.put("discountSource", "Không có khuyến mại");
        info.put("hasDiscount", false);
        
        return info;
    }
    
    // Giữ nguyên method cũ để backward compatibility
    public Map<String, Object> getDiscountInfo(Product product) {
        List<Product> products = Arrays.asList(product);
        Map<Long, Map<String, Object>> bulkResult = getBulkDiscountInfo(products);
        return bulkResult.get(product.getId());
    }
    
    // Method để clear cache khi có thay đổi promotion
    public void clearPromotionCache() {
        cachedActivePromotions = null;
        lastCacheDate = null;
        cachedProductPromotions = null;
        cachedCategoryPromotions = null;
    }
    
    // ...existing code... (giữ nguyên các method khác)
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }
    
    public Optional<Promotion> getPromotionById(Long id) {
        return promotionRepository.findById(id);
    }
    
    @Transactional
    public Promotion createPromotion(Promotion promotion, 
                                    Map<String, Double> categoryDiscounts,
                                    Map<Long, Double> productDiscounts) {
        Promotion savedPromotion = promotionRepository.save(promotion);
        
        if (categoryDiscounts != null && !categoryDiscounts.isEmpty()) {
            for (Map.Entry<String, Double> entry : categoryDiscounts.entrySet()) {
                PromotionCategory pc = new PromotionCategory();
                pc.setPromotion(savedPromotion);
                pc.setCategory(entry.getKey());
                pc.setDiscountPercent(entry.getValue());
                promotionCategoryRepository.save(pc);
            }
        }
        
        if (productDiscounts != null && !productDiscounts.isEmpty()) {
            for (Map.Entry<Long, Double> entry : productDiscounts.entrySet()) {
                Optional<Product> productOpt = productRepository.findById(entry.getKey());
                if (productOpt.isPresent()) {
                    PromotionProduct pp = new PromotionProduct();
                    pp.setPromotion(savedPromotion);
                    pp.setProduct(productOpt.get());
                    pp.setDiscountPercent(entry.getValue());
                    promotionProductRepository.save(pp);
                }
            }
        }
        
        // Clear cache sau khi tạo promotion mới
        clearPromotionCache();
        
        return savedPromotion;
    }
    
    @Transactional
    public Promotion updatePromotion(Long id, Promotion promotionDetails,
                                    Map<String, Double> categoryDiscounts,
                                    Map<Long, Double> productDiscounts) {
        Promotion promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy chương trình khuyến mại"));
        
        promotion.setName(promotionDetails.getName());
        promotion.setDescription(promotionDetails.getDescription());
        promotion.setStartDate(promotionDetails.getStartDate());
        promotion.setEndDate(promotionDetails.getEndDate());
        promotion.setActive(promotionDetails.isActive());
        
        promotionCategoryRepository.deleteByPromotionId(id);
        promotionProductRepository.deleteByPromotionId(id);
        
        if (categoryDiscounts != null && !categoryDiscounts.isEmpty()) {
            for (Map.Entry<String, Double> entry : categoryDiscounts.entrySet()) {
                PromotionCategory pc = new PromotionCategory();
                pc.setPromotion(promotion);
                pc.setCategory(entry.getKey());
                pc.setDiscountPercent(entry.getValue());
                promotionCategoryRepository.save(pc);
            }
        }
        
        if (productDiscounts != null && !productDiscounts.isEmpty()) {
            for (Map.Entry<Long, Double> entry : productDiscounts.entrySet()) {
                Optional<Product> productOpt = productRepository.findById(entry.getKey());
                if (productOpt.isPresent()) {
                    PromotionProduct pp = new PromotionProduct();
                    pp.setPromotion(promotion);
                    pp.setProduct(productOpt.get());
                    pp.setDiscountPercent(entry.getValue());
                    promotionProductRepository.save(pp);
                }
            }
        }
        
        // Clear cache sau khi update
        clearPromotionCache();
        
        return promotionRepository.save(promotion);
    }
    
    @Transactional
    public boolean deletePromotion(Long id) {
        if (promotionRepository.existsById(id)) {
            promotionRepository.deleteById(id);
            clearPromotionCache(); // Clear cache sau khi xóa
            return true;
        }
        return false;
    }
    
    public double calculateDiscountedPrice(Product product) {
        Map<String, Object> discountInfo = getDiscountInfo(product);
        return (Double) discountInfo.get("discountedPrice");
    }
    
    public List<String> getAllCategories() {
        return productRepository.findAll().stream()
            .map(Product::getCategory)
            .distinct()
            .sorted()
            .toList();
    }

    public Double getCategoryDiscount(Promotion promotion, String category) {
        return promotion.getCategoryDiscounts().stream()
            .filter(cd -> cd.getCategory().equals(category))
            .map(PromotionCategory::getDiscountPercent)
            .findFirst()
            .orElse(null);
    }
    
    public Double getProductDiscount(Promotion promotion, Long productId) {
        return promotion.getProductDiscounts().stream()
            .filter(pd -> pd.getProduct().getId().equals(productId))
            .map(PromotionProduct::getDiscountPercent)
            .findFirst()
            .orElse(null);
    }
}