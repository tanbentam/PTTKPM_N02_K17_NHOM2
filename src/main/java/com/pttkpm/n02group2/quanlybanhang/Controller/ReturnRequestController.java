package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.OrderItem;
import com.pttkpm.n02group2.quanlybanhang.Model.Product;
import com.pttkpm.n02group2.quanlybanhang.Model.ReturnRequestItem;
import com.pttkpm.n02group2.quanlybanhang.Repository.ReturnRequestItemRepository;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import com.pttkpm.n02group2.quanlybanhang.Service.ProductService;
import com.pttkpm.n02group2.quanlybanhang.Service.PromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user/pos")
public class ReturnRequestController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private ReturnRequestItemRepository returnRequestItemRepository;

    // Hiển thị form đổi trả hàng
    @GetMapping("/return/{orderId}")
    public String showReturnRequestForm(@PathVariable Long orderId, Model model) {
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                return "redirect:/user/pos/history";
            }

            // Kiểm tra trạng thái đơn hàng có thể đổi trả không
            if (order.getStatus() != Order.OrderStatus.COMPLETED) {
                model.addAttribute("error", "Chỉ có thể đổi trả đơn hàng đã hoàn thành.");
                return "redirect:/user/pos/history";
            }

            // Lấy tất cả sản phẩm có sẵn
            List<Product> products = productService.findAll();

            // Lấy danh sách danh mục sản phẩm duy nhất
            Set<String> categories = products.stream()
                    .map(Product::getCategory)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            // Tính giá khuyến mại cho từng sản phẩm
            List<Map<String, Object>> productViews = new ArrayList<>();
            for (Product p : products) {
                Map<String, Object> pv = new HashMap<>();
                pv.put("id", p.getId());
                pv.put("name", p.getName());
                pv.put("category", p.getCategory());
                pv.put("price", p.getPrice());
                pv.put("quantity", p.getQuantity());
                
                // Tính giá khuyến mại
                Double discountPrice = promotionService.calculateDiscountedPrice(p);
                if (discountPrice != null && !discountPrice.equals(p.getPrice())) {
                    pv.put("discountPrice", discountPrice);
                } else {
                    pv.put("discountPrice", null);
                }
                
                productViews.add(pv);
            }

            model.addAttribute("order", order);
            model.addAttribute("products", productViews);
            model.addAttribute("categories", categories);
            return "user/pos/return";

        } catch (Exception e) {
            System.err.println("Error loading return form: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/user/pos/history";
        }
    }

    // Xử lý gửi yêu cầu đổi trả hàng
    @PostMapping("/return/{orderId}")
    public String submitReturnRequest(
            @PathVariable Long orderId,
            @RequestParam(value = "oldProductIds", required = false) List<Long> oldProductIds,
            @RequestParam(value = "oldQuantities", required = false) List<Integer> oldQuantities,
            @RequestParam(value = "newProductIds", required = false) List<Long> newProductIds,
            @RequestParam(value = "newQuantities", required = false) List<Integer> newQuantities,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {

        try {
            // Kiểm tra đơn hàng tồn tại
            Order oldOrder = orderService.findById(orderId);
            if (oldOrder == null) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng không tồn tại.");
                return "redirect:/user/pos/history";
            }

            // Kiểm tra trạng thái đơn hàng
            if (oldOrder.getStatus() != Order.OrderStatus.COMPLETED) {
                redirectAttributes.addFlashAttribute("error", "Chỉ có thể đổi trả đơn hàng đã hoàn thành.");
                return "redirect:/user/pos/history";
            }

            // Validation dữ liệu đầu vào
            if (oldProductIds == null || oldProductIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một sản phẩm muốn đổi.");
                return "redirect:/user/pos/return/" + orderId;
            }

            if (newProductIds == null || newProductIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một sản phẩm muốn nhận.");
                return "redirect:/user/pos/return/" + orderId;
            }

            if (reason == null || reason.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng nhập lý do đổi trả.");
                return "redirect:/user/pos/return/" + orderId;
            }

            // Validation sản phẩm trả lại
            double totalOld = 0;
            List<OrderItem> orderItems = oldOrder.getItems();
            
            for (int i = 0; i < oldProductIds.size(); i++) {
                Long prodId = oldProductIds.get(i);
                int qty = (i < oldQuantities.size() && oldQuantities.get(i) > 0) ? oldQuantities.get(i) : 1;
                
                // Tìm sản phẩm trong đơn hàng
                OrderItem foundItem = orderItems.stream()
                    .filter(item -> item.getProduct().getId().equals(prodId))
                    .findFirst().orElse(null);
                
                if (foundItem == null) {
                    redirectAttributes.addFlashAttribute("error", "Sản phẩm không tồn tại trong đơn hàng này.");
                    return "redirect:/user/pos/return/" + orderId;
                }
                
                // Kiểm tra số lượng đổi không vượt quá số lượng đã mua
                if (qty > foundItem.getQuantity()) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Số lượng đổi sản phẩm '" + foundItem.getProduct().getName() + 
                        "' không thể vượt quá số lượng đã mua (" + foundItem.getQuantity() + ").");
                    return "redirect:/user/pos/return/" + orderId;
                }
                
                double unitPrice = foundItem.getUnitPrice() != null ? foundItem.getUnitPrice() : 0;
                totalOld += unitPrice * qty;
            }

            // Validation sản phẩm muốn nhận
            double totalNew = 0;
            
            for (int i = 0; i < newProductIds.size(); i++) {
                Long prodId = newProductIds.get(i);
                int qty = (i < newQuantities.size() && newQuantities.get(i) > 0) ? newQuantities.get(i) : 1;
                
                Product p = productService.findById(prodId);
                if (p == null) {
                    redirectAttributes.addFlashAttribute("error", "Sản phẩm muốn nhận không tồn tại.");
                    return "redirect:/user/pos/return/" + orderId;
                }
                
                // Kiểm tra tồn kho
                if (qty > p.getQuantity()) {
                    redirectAttributes.addFlashAttribute("error", 
                        "Số lượng sản phẩm '" + p.getName() + 
                        "' vượt quá tồn kho hiện có (" + p.getQuantity() + ").");
                    return "redirect:/user/pos/return/" + orderId;
                }
                
                // Sử dụng giá có khuyến mại nếu có
                Double discountPrice = promotionService.calculateDiscountedPrice(p);
                double finalPrice = (discountPrice != null) ? discountPrice : p.getPrice();
                totalNew += finalPrice * qty;
            }

            // Kiểm tra giá trị đổi trả
            if (totalNew < totalOld) {
                redirectAttributes.addFlashAttribute("error", 
                    "Tổng giá trị sản phẩm nhận (" + String.format("%.0f", totalNew) + 
                    " VNĐ) phải lớn hơn hoặc bằng tổng giá trị sản phẩm đổi (" + 
                    String.format("%.0f", totalOld) + " VNĐ).");
                return "redirect:/user/pos/return/" + orderId;
            }

            // Cập nhật trạng thái đơn hàng
            oldOrder.setStatus(Order.OrderStatus.RETURN_REQUESTED);
            orderService.save(oldOrder);
 // ...existing code...

 // **QUAN TRỌNG: Lưu sản phẩm muốn nhận vào database**
  for (int i = 0; i < newProductIds.size(); i++) {
    Long productId = newProductIds.get(i);
    Integer quantity = newQuantities.get(i);
    
    Product product = productService.findById(productId);
    if (product != null) {
        // Tính giá cuối cùng (có khuyến mại nếu có)
        Double discountPrice = promotionService.calculateDiscountedPrice(product);
        int finalPrice;
        
        if (discountPrice != null) {
            finalPrice = (int) Math.round(discountPrice);
        } else {
            finalPrice = (int) Math.round(product.getPrice()); // Cast Double sang int
        }
        
        ReturnRequestItem item = new ReturnRequestItem();
        item.setOrder(oldOrder);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPrice(finalPrice);
        
        returnRequestItemRepository.save(item);
    }
}
// ...existing code...


            // Thông báo thành công
            redirectAttributes.addFlashAttribute("message", 
                "Yêu cầu đổi trả đã được gửi thành công! " +
                "Mã đơn hàng: " + oldOrder.getOrderNumber() + ". " +
                "Admin sẽ xem xét và phản hồi trong thời gian sớm nhất.");

            return "redirect:/user/pos/history";

        } catch (Exception e) {
            System.err.println("ERROR in submitReturnRequest: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi xử lý yêu cầu: " + e.getMessage());
            return "redirect:/user/pos/return/" + orderId;
        }
    }
}