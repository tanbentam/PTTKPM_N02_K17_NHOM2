package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

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
    
    // Lấy tất cả chương trình khuyến mại
    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }
    
    // Lấy chương trình khuyến mại theo ID
    public Optional<Promotion> getPromotionById(Long id) {
        return promotionRepository.findById(id);
    }
    
    // Lấy các chương trình khuyến mại đang hoạt động
    public List<Promotion> getActivePromotions() {
        return promotionRepository.findCurrentActivePromotions();
    }
    
    // Tạo chương trình khuyến mại mới
    @Transactional
    public Promotion createPromotion(Promotion promotion, 
                                    Map<String, Double> categoryDiscounts,
                                    Map<Long, Double> productDiscounts) {
        Promotion savedPromotion = promotionRepository.save(promotion);
        
        // Thêm giảm giá theo danh mục
        if (categoryDiscounts != null && !categoryDiscounts.isEmpty()) {
            for (Map.Entry<String, Double> entry : categoryDiscounts.entrySet()) {
                PromotionCategory pc = new PromotionCategory();
                pc.setPromotion(savedPromotion);
                pc.setCategory(entry.getKey());
                pc.setDiscountPercent(entry.getValue());
                promotionCategoryRepository.save(pc);
            }
        }
        
        // Thêm giảm giá theo sản phẩm
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
        
        return savedPromotion;
    }
    
    // Cập nhật chương trình khuyến mại
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
        
        // Xóa các giảm giá cũ
        promotionCategoryRepository.deleteByPromotionId(id);
        promotionProductRepository.deleteByPromotionId(id);
        
        // Thêm giảm giá mới theo danh mục
        if (categoryDiscounts != null && !categoryDiscounts.isEmpty()) {
            for (Map.Entry<String, Double> entry : categoryDiscounts.entrySet()) {
                PromotionCategory pc = new PromotionCategory();
                pc.setPromotion(promotion);
                pc.setCategory(entry.getKey());
                pc.setDiscountPercent(entry.getValue());
                promotionCategoryRepository.save(pc);
            }
        }
        
        // Thêm giảm giá mới theo sản phẩm
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
        
        return promotionRepository.save(promotion);
    }
    
    // Xóa chương trình khuyến mại
    @Transactional
    public boolean deletePromotion(Long id) {
        if (promotionRepository.existsById(id)) {
            promotionRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    // Tính giá sau khuyến mại cho sản phẩm
    public double calculateDiscountedPrice(Product product) {
        double originalPrice = product.getPrice();
        double maxDiscount = 0.0;
        
        List<Promotion> activePromotions = getActivePromotions();
        
        for (Promotion promotion : activePromotions) {
            // Kiểm tra giảm giá riêng cho sản phẩm
            Optional<PromotionProduct> ppOpt = promotionProductRepository
                .findByPromotionIdAndProductId(promotion.getId(), product.getId());
            
            if (ppOpt.isPresent()) {
                maxDiscount = Math.max(maxDiscount, ppOpt.get().getDiscountPercent());
            } else {
                // Kiểm tra giảm giá theo danh mục
                List<PromotionCategory> categories = promotionCategoryRepository
                    .findByPromotionId(promotion.getId());
                
                for (PromotionCategory pc : categories) {
                    if (pc.getCategory().equals(product.getCategory())) {
                        maxDiscount = Math.max(maxDiscount, pc.getDiscountPercent());
                    }
                }
            }
        }
        
        return originalPrice * (1 - maxDiscount / 100);
    }
    
    // Lấy thông tin giảm giá cho sản phẩm
    public Map<String, Object> getDiscountInfo(Product product) {
        Map<String, Object> info = new HashMap<>();
        double originalPrice = product.getPrice();
        double maxDiscount = 0.0;
        String discountSource = "Không có khuyến mại";
        
        List<Promotion> activePromotions = getActivePromotions();
        
        for (Promotion promotion : activePromotions) {
            // Kiểm tra giảm giá riêng cho sản phẩm
            Optional<PromotionProduct> ppOpt = promotionProductRepository
                .findByPromotionIdAndProductId(promotion.getId(), product.getId());
            
            if (ppOpt.isPresent()) {
                if (ppOpt.get().getDiscountPercent() > maxDiscount) {
                    maxDiscount = ppOpt.get().getDiscountPercent();
                    discountSource = "Giảm giá riêng: " + promotion.getName();
                }
            } else {
                // Kiểm tra giảm giá theo danh mục
                List<PromotionCategory> categories = promotionCategoryRepository
                    .findByPromotionId(promotion.getId());
                
                for (PromotionCategory pc : categories) {
                    if (pc.getCategory().equals(product.getCategory())) {
                        if (pc.getDiscountPercent() > maxDiscount) {
                            maxDiscount = pc.getDiscountPercent();
                            discountSource = "Giảm giá danh mục " + pc.getCategory() + ": " + promotion.getName();
                        }
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
    
    // Lấy tất cả danh mục sản phẩm
    public List<String> getAllCategories() {
        return productRepository.findAll().stream()
            .map(Product::getCategory)
            .distinct()
            .sorted()
            .toList();
    }

    // Helper method để lấy giá trị discount cho category
    public Double getCategoryDiscount(Promotion promotion, String category) {
        return promotion.getCategoryDiscounts().stream()
            .filter(cd -> cd.getCategory().equals(category))
            .map(PromotionCategory::getDiscountPercent)
            .findFirst()
            .orElse(null);
    }
    
    // Helper method để lấy giá trị discount cho product
    public Double getProductDiscount(Promotion promotion, Long productId) {
        return promotion.getProductDiscounts().stream()
            .filter(pd -> pd.getProduct().getId().equals(productId))
            .map(PromotionProduct::getDiscountPercent)
            .findFirst()
            .orElse(null);
    }
}