package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Service.PromotionService;
import com.pttkpm.n02group2.quanlybanhang.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/admin/promotions")
public class PromotionController {
    
    @Autowired
    private PromotionService promotionService;
    
    @Autowired
    private ProductService productService;
    
    // Danh sách chương trình khuyến mại
    @GetMapping
    public String listPromotions(Model model) {
        try {
            List<Promotion> promotions = promotionService.getAllPromotions();
            List<String> categories = promotionService.getAllCategories();
            List<Product> products = productService.getAllProducts();
            
            model.addAttribute("promotions", promotions);
            model.addAttribute("categories", categories);
            model.addAttribute("products", products);
            model.addAttribute("newPromotion", new Promotion());
            
            return "admin/promotions/index";
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "admin/promotions/index";
        }
    }
    
    // Xem chi tiết chương trình khuyến mại
    @GetMapping("/{id}")
    public String viewPromotion(@PathVariable Long id, Model model) {
        try {
            Optional<Promotion> promotionOpt = promotionService.getPromotionById(id);
            if (promotionOpt.isPresent()) {
                Promotion promotion = promotionOpt.get();
                List<String> categories = promotionService.getAllCategories();
                List<Product> products = productService.getAllProducts();
                
                model.addAttribute("promotion", promotion);
                model.addAttribute("categories", categories);
                model.addAttribute("products", products);
                
                return "admin/promotions/view";
            } else {
                model.addAttribute("error", "Không tìm thấy chương trình khuyến mại");
                return "redirect:/admin/promotions";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/promotions";
        }
    }
    
    // Tạo chương trình khuyến mại mới
    @PostMapping
    public String createPromotion(@ModelAttribute Promotion promotion,
                                 @RequestParam(required = false) Map<String, String> params,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Parse category discounts
            Map<String, Double> categoryDiscounts = new HashMap<>();
            params.forEach((key, value) -> {
                if (key.startsWith("categoryDiscount_") && value != null && !value.trim().isEmpty()) {
                    String category = key.substring("categoryDiscount_".length());
                    try {
                        double discount = Double.parseDouble(value);
                        if (discount > 0 && discount <= 100) {
                            categoryDiscounts.put(category, discount);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid numbers
                    }
                }
            });
            
            // Parse product discounts
            Map<Long, Double> productDiscounts = new HashMap<>();
            params.forEach((key, value) -> {
                if (key.startsWith("productDiscount_") && value != null && !value.trim().isEmpty()) {
                    String productIdStr = key.substring("productDiscount_".length());
                    try {
                        Long productId = Long.parseLong(productIdStr);
                        double discount = Double.parseDouble(value);
                        if (discount > 0 && discount <= 100) {
                            productDiscounts.put(productId, discount);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid numbers
                    }
                }
            });
            
            promotionService.createPromotion(promotion, categoryDiscounts, productDiscounts);
            redirectAttributes.addFlashAttribute("success", "Tạo chương trình khuyến mại thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/admin/promotions";
    }
    
    // Cập nhật chương trình khuyến mại
    @PostMapping("/{id}")
    public String updatePromotion(@PathVariable Long id,
                                 @ModelAttribute Promotion promotion,
                                 @RequestParam(required = false) Map<String, String> params,
                                 RedirectAttributes redirectAttributes) {
        try {
            // Parse category discounts
            Map<String, Double> categoryDiscounts = new HashMap<>();
            params.forEach((key, value) -> {
                if (key.startsWith("categoryDiscount_") && value != null && !value.trim().isEmpty()) {
                    String category = key.substring("categoryDiscount_".length());
                    try {
                        double discount = Double.parseDouble(value);
                        if (discount > 0 && discount <= 100) {
                            categoryDiscounts.put(category, discount);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            });
            
            // Parse product discounts
            Map<Long, Double> productDiscounts = new HashMap<>();
            params.forEach((key, value) -> {
                if (key.startsWith("productDiscount_") && value != null && !value.trim().isEmpty()) {
                    String productIdStr = key.substring("productDiscount_".length());
                    try {
                        Long productId = Long.parseLong(productIdStr);
                        double discount = Double.parseDouble(value);
                        if (discount > 0 && discount <= 100) {
                            productDiscounts.put(productId, discount);
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            });
            
            promotionService.updatePromotion(id, promotion, categoryDiscounts, productDiscounts);
            redirectAttributes.addFlashAttribute("success", "Cập nhật chương trình khuyến mại thành công!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/admin/promotions";
    }
    
    // Xóa chương trình khuyến mại
    @PostMapping("/{id}/delete")
    public String deletePromotion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            boolean deleted = promotionService.deletePromotion(id);
            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Xóa chương trình khuyến mại thành công!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy chương trình khuyến mại");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/admin/promotions";
    }
    
    // API: Lấy giá sau khuyến mại cho sản phẩm
    @GetMapping("/api/product/{productId}/discount")
    @ResponseBody
    public ResponseEntity<?> getProductDiscount(@PathVariable Long productId) {
        try {
            Optional<Product> productOpt = productService.getProductById(productId);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                Map<String, Object> discountInfo = promotionService.getDiscountInfo(product);
                return ResponseEntity.ok(discountInfo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    
    // API: Lấy các chương trình khuyến mại đang hoạt động
    @GetMapping("/api/active")
    @ResponseBody
    public ResponseEntity<?> getActivePromotions() {
        try {
            List<Promotion> promotions = promotionService.getActivePromotions();
            return ResponseEntity.ok(promotions);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}