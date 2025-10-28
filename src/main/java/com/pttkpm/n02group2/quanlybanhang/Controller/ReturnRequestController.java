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
                model.addAttribute("error", "Không tìm thấy đơn hàng!");
                return "redirect:/user/pos/history";
            }

            // Kiểm tra trạng thái đơn hàng có thể đổi trả không
            if (order.getStatus() != Order.OrderStatus.COMPLETED) {
                model.addAttribute("error", "Chỉ có thể đổi trả đơn hàng đã hoàn thành.");
                return "redirect:/user/pos/history";
            }

            // Kiểm tra xem đã có yêu cầu đổi trả chưa
            if (order.getStatus() == Order.OrderStatus.RETURN_REQUESTED) {
                model.addAttribute("error", "Đơn hàng này đã có yêu cầu đổi trả đang chờ xử lý.");
                return "redirect:/user/pos/history";
            }

            // Lấy tất cả sản phẩm có sẵn
            List<Product> products = productService.findAll()
                .stream()
                .filter(p -> p.getQuantity() > 0) // Chỉ lấy sản phẩm còn hàng
                .collect(Collectors.toList());

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
                try {
                    Double discountPrice = promotionService.calculateDiscountedPrice(p);
                    if (discountPrice != null && !discountPrice.equals(p.getPrice())) {
                        pv.put("discountPrice", discountPrice);
                        pv.put("hasPromotion", true);
                    } else {
                        pv.put("discountPrice", null);
                        pv.put("hasPromotion", false);
                    }
                } catch (Exception e) {
                    System.err.println("Error calculating promotion for product " + p.getId() + ": " + e.getMessage());
                    pv.put("discountPrice", null);
                    pv.put("hasPromotion", false);
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
            model.addAttribute("error", "Có lỗi xảy ra khi tải trang đổi trả.");
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
            System.out.println("=== SUBMIT RETURN REQUEST DEBUG ===");
            System.out.println("Order ID: " + orderId);
            System.out.println("Old Product IDs: " + oldProductIds);
            System.out.println("Old Quantities: " + oldQuantities);
            System.out.println("New Product IDs: " + newProductIds);
            System.out.println("New Quantities: " + newQuantities);
            System.out.println("Reason: " + reason);

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

            // Validation và tính toán sản phẩm trả lại
            double totalOld = validateAndCalculateReturnItems(oldOrder, oldProductIds, oldQuantities, redirectAttributes);
            if (totalOld < 0) {
                return "redirect:/user/pos/return/" + orderId; // Error đã được set trong method
            }

            // Validation và tính toán sản phẩm muốn nhận
            double totalNew = validateAndCalculateReceiveItems(newProductIds, newQuantities, redirectAttributes);
            if (totalNew < 0) {
                return "redirect:/user/pos/return/" + orderId; // Error đã được set trong method
            }

            // Kiểm tra giá trị đổi trả
            if (totalNew < totalOld) {
                redirectAttributes.addFlashAttribute("error", 
                    "Tổng giá trị sản phẩm nhận (" + String.format("%,.0f", totalNew) + 
                    " VNĐ) phải lớn hơn hoặc bằng tổng giá trị sản phẩm đổi (" + 
                    String.format("%,.0f", totalOld) + " VNĐ).");
                return "redirect:/user/pos/return/" + orderId;
            }

            // Xóa các return request items cũ nếu có
            List<ReturnRequestItem> existingItems = returnRequestItemRepository.findByOrder(oldOrder);
            if (!existingItems.isEmpty()) {
                returnRequestItemRepository.deleteAll(existingItems);
                System.out.println("Deleted " + existingItems.size() + " existing return request items");
            }

            // Lưu sản phẩm trả lại (RETURN)
            saveReturnItems(oldOrder, oldProductIds, oldQuantities);

            // Lưu sản phẩm muốn nhận (RECEIVE)
            saveReceiveItems(oldOrder, newProductIds, newQuantities);

            // Cập nhật trạng thái đơn hàng
            oldOrder.setStatus(Order.OrderStatus.RETURN_REQUESTED);
            orderService.save(oldOrder);

            System.out.println("=== RETURN REQUEST COMPLETED ===");

            // Thông báo thành công
            redirectAttributes.addFlashAttribute("message", 
                "Yêu cầu đổi trả đã được gửi thành công! " +
                "Mã đơn hàng: " + oldOrder.getOrderNumber() + ". " +
                "Admin sẽ xem xét và phản hồi trong thời gian sớm nhất. " +
                "Chênh lệch cần thanh toán: " + String.format("%,.0f", (totalNew - totalOld)) + " VNĐ.");

            return "redirect:/user/pos/history";

        } catch (Exception e) {
            System.err.println("ERROR in submitReturnRequest: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi xử lý yêu cầu: " + e.getMessage());
            return "redirect:/user/pos/return/" + orderId;
        }
    }

    // Helper method: Validate và tính toán sản phẩm trả lại
    private double validateAndCalculateReturnItems(Order order, List<Long> productIds, List<Integer> quantities, RedirectAttributes redirectAttributes) {
        double total = 0;
        List<OrderItem> orderItems = order.getItems();
        
        for (int i = 0; i < productIds.size(); i++) {
            Long prodId = productIds.get(i);
            int qty = (i < quantities.size() && quantities.get(i) > 0) ? quantities.get(i) : 1;
            
            // Tìm sản phẩm trong đơn hàng
            OrderItem foundItem = orderItems.stream()
                .filter(item -> item.getProduct().getId().equals(prodId))
                .findFirst().orElse(null);
            
            if (foundItem == null) {
                redirectAttributes.addFlashAttribute("error", "Sản phẩm không tồn tại trong đơn hàng này.");
                return -1;
            }
            
            // Kiểm tra số lượng đổi không vượt quá số lượng đã mua
            if (qty > foundItem.getQuantity()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Số lượng đổi sản phẩm '" + foundItem.getProduct().getName() + 
                    "' không thể vượt quá số lượng đã mua (" + foundItem.getQuantity() + ").");
                return -1;
            }
            
            double unitPrice = foundItem.getUnitPrice() != null ? foundItem.getUnitPrice() : 0;
            total += unitPrice * qty;
        }
        
        return total;
    }

    // Helper method: Validate và tính toán sản phẩm muốn nhận
    private double validateAndCalculateReceiveItems(List<Long> productIds, List<Integer> quantities, RedirectAttributes redirectAttributes) {
        double total = 0;
        
        for (int i = 0; i < productIds.size(); i++) {
            Long prodId = productIds.get(i);
            int qty = (i < quantities.size() && quantities.get(i) > 0) ? quantities.get(i) : 1;
            
            Product p = productService.findById(prodId);
            if (p == null) {
                redirectAttributes.addFlashAttribute("error", "Sản phẩm muốn nhận không tồn tại.");
                return -1;
            }
            
            // Kiểm tra tồn kho
            if (qty > p.getQuantity()) {
                redirectAttributes.addFlashAttribute("error", 
                    "Số lượng sản phẩm '" + p.getName() + 
                    "' vượt quá tồn kho hiện có (" + p.getQuantity() + ").");
                return -1;
            }
            
            // Sử dụng giá có khuyến mại nếu có
            try {
                Double discountPrice = promotionService.calculateDiscountedPrice(p);
                double finalPrice = (discountPrice != null) ? discountPrice : p.getPrice();
                total += finalPrice * qty;
            } catch (Exception e) {
                System.err.println("Error calculating promotion price: " + e.getMessage());
                total += p.getPrice() * qty;
            }
        }
        
        return total;
    }

    // Helper method: Lưu sản phẩm trả lại
    private void saveReturnItems(Order order, List<Long> productIds, List<Integer> quantities) {
        List<OrderItem> orderItems = order.getItems();
        
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer quantity = quantities.get(i);
            
            // Tìm OrderItem tương ứng để lấy giá đã mua
            OrderItem foundItem = orderItems.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst().orElse(null);
            
            if (foundItem != null && quantity > 0) {
                ReturnRequestItem item = new ReturnRequestItem();
                item.setOrder(order);
                item.setProduct(foundItem.getProduct());
                item.setQuantity(quantity);
                item.setUnitPrice(foundItem.getUnitPrice() != null ? foundItem.getUnitPrice() : 0.0);
                item.setType("RETURN"); // QUAN TRỌNG: Set type
                
                returnRequestItemRepository.save(item);
                System.out.println("Saved RETURN item: " + foundItem.getProduct().getName() + 
                    ", qty: " + quantity + ", price: " + item.getUnitPrice());
            }
        }
    }

    // Helper method: Lưu sản phẩm muốn nhận
    private void saveReceiveItems(Order order, List<Long> productIds, List<Integer> quantities) {
        for (int i = 0; i < productIds.size(); i++) {
            Long productId = productIds.get(i);
            Integer quantity = quantities.get(i);
            
            Product product = productService.findById(productId);
            if (product != null && quantity > 0) {
                // Tính giá cuối cùng (có khuyến mại nếu có)
                double finalPrice;
                try {
                    Double discountPrice = promotionService.calculateDiscountedPrice(product);
                    finalPrice = (discountPrice != null) ? discountPrice : product.getPrice();
                } catch (Exception e) {
                    System.err.println("Error calculating promotion price: " + e.getMessage());
                    finalPrice = product.getPrice();
                }
                
                ReturnRequestItem item = new ReturnRequestItem();
                item.setOrder(order);
                item.setProduct(product);
                item.setQuantity(quantity);
                item.setUnitPrice(finalPrice);
                item.setType("RECEIVE"); // QUAN TRỌNG: Set type
                
                returnRequestItemRepository.save(item);
                System.out.println("Saved RECEIVE item: " + product.getName() + 
                    ", qty: " + quantity + ", price: " + finalPrice);
            }
        }
    }
}