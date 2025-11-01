package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Order;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping("/admin/returns")
public class ReturnAdminController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReturnRequestItemRepository returnRequestItemRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private PromotionService promotionService;

    // Hiển thị danh sách yêu cầu đổi trả chờ duyệt
    @GetMapping
public String listReturnRequests(Model model) {
    try {
        List<Order> returnRequests = orderService.findByStatus(Order.OrderStatus.RETURN_REQUESTED);
        // Các trường returnRequestDate và returnReason đã nằm trong entity Order, không cần xử lý gì thêm
        model.addAttribute("returnRequests", returnRequests);
        return "admin/return/list";
    } catch (Exception e) {
        model.addAttribute("error", "Có lỗi xảy ra khi tải danh sách yêu cầu đổi trả: " + e.getMessage());
        return "admin/return/list";
    }
}

    // Hiển thị chi tiết yêu cầu đổi trả
    @GetMapping("/{orderId}")
    public String viewReturnRequest(@PathVariable Long orderId, Model model) {
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                model.addAttribute("error", "Không tìm thấy đơn hàng!");
                return "redirect:/admin/returns";
            }

            // Lấy tất cả ReturnRequestItem liên quan đến order
            List<ReturnRequestItem> allRequestItems = returnRequestItemRepository.findByOrderFetchProduct(order);
 
            // Debug logging
            System.out.println("=== DEBUG RETURN REQUEST ===");
            System.out.println("Order ID: " + orderId);
            System.out.println("Order Status: " + order.getStatus());
            System.out.println("Total ReturnRequestItems found: " + allRequestItems.size());
            
            for (ReturnRequestItem item : allRequestItems) {
                System.out.println("Item ID: " + item.getId() + 
                    ", Type: '" + item.getType() + "'" +
                    ", Product: " + (item.getProduct() != null ? item.getProduct().getName() : "NULL") +
                    ", Quantity: " + item.getQuantity() +
                    ", UnitPrice: " + item.getUnitPrice());
            }

            // Lọc theo type với null check
            List<ReturnRequestItem> returnItems = allRequestItems.stream()
                    .filter(i -> i.getType() != null && "RETURN".equalsIgnoreCase(i.getType().trim()))
                    .collect(Collectors.toList());

            List<ReturnRequestItem> receiveItems = allRequestItems.stream()
                    .filter(i -> i.getType() != null && "RECEIVE".equalsIgnoreCase(i.getType().trim()))
                    .collect(Collectors.toList());

            System.out.println("Filtered - Return items: " + returnItems.size() + ", Receive items: " + receiveItems.size());

            // Tạo danh sách hiển thị cho sản phẩm trả lại
            List<Map<String, Object>> returnItemsDisplay = createReturnItemsDisplay(returnItems);
            BigDecimal totalReturn = calculateTotalReturn(returnItems);

            // Tạo danh sách hiển thị cho sản phẩm muốn nhận
            List<Map<String, Object>> receiveItemsDisplay = createReceiveItemsDisplay(receiveItems);
            BigDecimal totalReceive = calculateTotalReceive(receiveItems);

            BigDecimal difference = totalReceive.subtract(totalReturn);

            // Add all attributes to model
            model.addAttribute("order", order);
            model.addAttribute("returnItems", returnItems);
            model.addAttribute("receiveItems", receiveItems);
            model.addAttribute("returnItemsDisplay", returnItemsDisplay);
            model.addAttribute("receiveItemsDisplay", receiveItemsDisplay);
            model.addAttribute("totalReturn", totalReturn);
            model.addAttribute("totalReceive", totalReceive);
            model.addAttribute("difference", difference);
            model.addAttribute("returnRequestDate", order.getReturnRequestDate());
            model.addAttribute("returnReason", order.getReturnReason());


            System.out.println("Final totals - Return: " + totalReturn + ", Receive: " + totalReceive + ", Difference: " + difference);
            System.out.println("=== END DEBUG ===");

            return "admin/return/detail";
            
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Có lỗi xảy ra khi xem chi tiết yêu cầu đổi trả: " + e.getMessage());
            return "redirect:/admin/returns";
        }
    }

    // Helper method: Tạo danh sách hiển thị sản phẩm trả lại
    private List<Map<String, Object>> createReturnItemsDisplay(List<ReturnRequestItem> returnItems) {
        List<Map<String, Object>> returnItemsDisplay = new ArrayList<>();
        
        for (ReturnRequestItem item : returnItems) {
            Map<String, Object> displayItem = new HashMap<>();
            Product product = item.getProduct();
            
            if (product == null) {
                System.out.println("WARNING: Product is null for return item " + item.getId());
                continue;
            }
            
            // Tính giá - ưu tiên unitPrice, fallback về product price
            Number unitPrice = item.getUnitPrice();
            double price = (unitPrice != null && unitPrice.doubleValue() > 0) 
                ? unitPrice.doubleValue() 
                : product.getPrice();
            
            BigDecimal itemTotal = BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(0, RoundingMode.HALF_UP);
            
            displayItem.put("productName", product.getName());
            displayItem.put("quantity", item.getQuantity());
            displayItem.put("unitPrice", price);
            displayItem.put("totalPrice", itemTotal.doubleValue());
            displayItem.put("reason", "Đổi trả sản phẩm"); // Default reason
            
            returnItemsDisplay.add(displayItem);
        }
        
        return returnItemsDisplay;
    }

    // Helper method: Tạo danh sách hiển thị sản phẩm muốn nhận
    private List<Map<String, Object>> createReceiveItemsDisplay(List<ReturnRequestItem> receiveItems) {
        List<Map<String, Object>> receiveItemsDisplay = new ArrayList<>();
        
        for (ReturnRequestItem item : receiveItems) {
            Map<String, Object> displayItem = new HashMap<>();
            Product product = item.getProduct();
            
            if (product == null) {
                System.out.println("WARNING: Product is null for receive item " + item.getId());
                continue;
            }
            
            // Áp dụng khuyến mại cho giá sản phẩm muốn nhận
            Double promoPrice = null;
            try {
                promoPrice = promotionService.calculateDiscountedPrice(product);
            } catch (Exception e) {
                System.out.println("Error calculating promotion price for product " + product.getId() + ": " + e.getMessage());
            }
            
            double price = promoPrice != null ? promoPrice : product.getPrice();
            
            BigDecimal itemTotal = BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .setScale(0, RoundingMode.HALF_UP);
            
            displayItem.put("productName", product.getName());
            displayItem.put("productId", product.getId());
            displayItem.put("quantity", item.getQuantity());
            displayItem.put("unitPrice", price);
            displayItem.put("totalPrice", itemTotal.doubleValue());
            displayItem.put("originalPrice", product.getPrice());
            displayItem.put("hasPromotion", promoPrice != null);
            displayItem.put("availableStock", product.getQuantity());
            
            receiveItemsDisplay.add(displayItem);
        }
        
        return receiveItemsDisplay;
    }

    // Helper method: Tính tổng giá trị sản phẩm trả lại
    private BigDecimal calculateTotalReturn(List<ReturnRequestItem> returnItems) {
        BigDecimal total = BigDecimal.ZERO;
        
        for (ReturnRequestItem item : returnItems) {
            if (item.getProduct() == null) continue;
            
            Number unitPrice = item.getUnitPrice();
            double price = (unitPrice != null && unitPrice.doubleValue() > 0) 
                ? unitPrice.doubleValue() 
                : item.getProduct().getPrice();
            
            BigDecimal itemTotal = BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        
        return total.setScale(0, RoundingMode.HALF_UP);
    }

    // Helper method: Tính tổng giá trị sản phẩm muốn nhận
    private BigDecimal calculateTotalReceive(List<ReturnRequestItem> receiveItems) {
        BigDecimal total = BigDecimal.ZERO;
        
        for (ReturnRequestItem item : receiveItems) {
            if (item.getProduct() == null) continue;
            
            Product product = item.getProduct();
            Double promoPrice = null;
            try {
                promoPrice = promotionService.calculateDiscountedPrice(product);
            } catch (Exception e) {
                System.out.println("Error calculating promotion price: " + e.getMessage());
            }
            
            double price = promoPrice != null ? promoPrice : product.getPrice();
            
            BigDecimal itemTotal = BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }
        
        return total.setScale(0, RoundingMode.HALF_UP);
    }

    // Duyệt yêu cầu đổi trả
    @PostMapping("/{orderId}/approve")
    public String approveReturn(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
                return "redirect:/admin/returns";
            }

            // Kiểm tra trạng thái: Chỉ cho phép duyệt nếu đang chờ duyệt, không cho phép nếu đã hủy
            if (order.getStatus() == Order.OrderStatus.CANCELLED) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng đã hủy, không thể đổi trả!");
                return "redirect:/admin/returns/" + orderId;
            }
            if (order.getStatus() != Order.OrderStatus.RETURN_REQUESTED) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng không trong trạng thái chờ duyệt đổi trả!");
                return "redirect:/admin/returns/" + orderId;
            }

            // Lấy các ReturnRequestItem liên quan
            List<ReturnRequestItem> allRequestItems = returnRequestItemRepository.findByOrder(order);
            
            List<ReturnRequestItem> returnItems = allRequestItems.stream()
                    .filter(i -> i.getType() != null && "RETURN".equalsIgnoreCase(i.getType().trim()))
                    .collect(Collectors.toList());
            
            List<ReturnRequestItem> receiveItems = allRequestItems.stream()
                    .filter(i -> i.getType() != null && "RECEIVE".equalsIgnoreCase(i.getType().trim()))
                    .collect(Collectors.toList());

            // Kiểm tra có dữ liệu không
            if (returnItems.isEmpty() && receiveItems.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin sản phẩm đổi trả!");
                return "redirect:/admin/returns/" + orderId;
            }

            // Kiểm tra tồn kho trước khi thực hiện
            for (ReturnRequestItem item : receiveItems) {
                Product product = item.getProduct();
                if (product == null) {
                    redirectAttributes.addFlashAttribute("error", "Có sản phẩm không hợp lệ trong yêu cầu!");
                    return "redirect:/admin/returns/" + orderId;
                }
                
                if (product.getQuantity() < item.getQuantity()) {
                    redirectAttributes.addFlashAttribute("error",
                            "Sản phẩm '" + product.getName() + "' không đủ tồn kho để đổi! (Hiện có: "
                                    + product.getQuantity() + ", cần: " + item.getQuantity() + ")");
                    return "redirect:/admin/returns/" + orderId;
                }
            }

            // 1. TĂNG tồn kho cho sản phẩm trả lại
            BigDecimal totalReturnValue = processReturnItems(returnItems);

            // 2. GIẢM tồn kho cho sản phẩm muốn nhận
            BigDecimal totalReceiveValue = processReceiveItems(receiveItems);

            // 3. Cập nhật tổng tiền đơn hàng: Giá trị mới = Giá trị gốc - tổng giá trị sản phẩm trả lại (giá trên hóa đơn) + tổng giá trị sản phẩm muốn nhận (giá hiện tại)
            double originalTotal = order.getOriginalAmount() != null ? order.getOriginalAmount() : order.getTotalAmount();
            double newTotal = originalTotal - totalReturnValue.doubleValue() + totalReceiveValue.doubleValue();

            // Debug log
            System.out.println("=== APPROVE RETURN DEBUG ===");
            System.out.println("Order ID: " + orderId);
            System.out.println("Original Total: " + originalTotal);
            System.out.println("Total Return Value: " + totalReturnValue);
            System.out.println("Total Receive Value: " + totalReceiveValue);
            System.out.println("New Total: " + newTotal);

            // Cập nhật cả totalAmount và finalAmount để đảm bảo đồng bộ
            order.setTotalAmount(newTotal);
            order.setFinalAmount(newTotal);

            // 4. Cập nhật trạng thái đơn hàng thành RETURNED
            order.setStatus(Order.OrderStatus.RETURNED);
            orderService.save(order);

            System.out.println("Saved order totalAmount: " + order.getTotalAmount() + ", finalAmount: " + order.getFinalAmount());
            System.out.println("=== END APPROVE DEBUG ===");

            // 5. Tính chênh lệch và thông báo
            BigDecimal difference = totalReceiveValue.subtract(totalReturnValue);
            String message = generateSuccessMessage(difference);

            redirectAttributes.addFlashAttribute("message", message);

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Có lỗi xảy ra khi duyệt yêu cầu: " + e.getMessage());
            return "redirect:/admin/returns/" + orderId;
        }

        return "redirect:/admin/returns";
    }

    // Helper method: Xử lý sản phẩm trả lại
    private BigDecimal processReturnItems(List<ReturnRequestItem> returnItems) {
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (ReturnRequestItem item : returnItems) {
            Product product = item.getProduct();
            if (product == null) continue;
            
            // Tăng tồn kho
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productService.save(product);

            // Tính giá trị
            Number unitPrice = item.getUnitPrice();
            double price = (unitPrice != null && unitPrice.doubleValue() > 0) 
                ? unitPrice.doubleValue() 
                : product.getPrice();
            
            BigDecimal itemValue = BigDecimal.valueOf(price)
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalValue = totalValue.add(itemValue);
        }
        
        return totalValue.setScale(0, RoundingMode.HALF_UP);
    }

    // Helper method: Xử lý sản phẩm muốn nhận
    private BigDecimal processReceiveItems(List<ReturnRequestItem> receiveItems) {
        BigDecimal totalValue = BigDecimal.ZERO;
        
        for (ReturnRequestItem item : receiveItems) {
            Product product = item.getProduct();
            if (product == null) continue;
            
            // Giảm tồn kho
            product.setQuantity(product.getQuantity() - item.getQuantity());
            productService.save(product);

            // Tính giá trị (có áp dụng khuyến mại)
            Double currentPrice = null;
            try {
                currentPrice = promotionService.calculateDiscountedPrice(product);
            } catch (Exception e) {
                System.out.println("Error calculating promotion: " + e.getMessage());
            }
            
            if (currentPrice == null) {
                currentPrice = product.getPrice();
            }
            
            BigDecimal itemValue = BigDecimal.valueOf(currentPrice)
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalValue = totalValue.add(itemValue);
        }
        
        return totalValue.setScale(0, RoundingMode.HALF_UP);
    }

    // Helper method: Tạo thông báo thành công
    private String generateSuccessMessage(BigDecimal difference) {
        String message = "Duyệt yêu cầu đổi trả thành công! ";
        
        if (difference.compareTo(BigDecimal.ZERO) > 0) {
            message += "Khách cần bù thêm: " + String.format("%,.0f", difference.doubleValue()) + " VNĐ.";
        } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
            message += "Hoàn lại cho khách: " + String.format("%,.0f", difference.abs().doubleValue()) + " VNĐ.";
        } else {
            message += "Không có chênh lệch giá trị.";
        }
        
        return message;
    }

    // Từ chối yêu cầu đổi trả
    @PostMapping("/{orderId}/reject")
    public String rejectReturn(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
                return "redirect:/admin/returns";
            }

            if (order.getStatus() != Order.OrderStatus.RETURN_REQUESTED) {
                redirectAttributes.addFlashAttribute("error", "Đơn hàng không trong trạng thái chờ duyệt đổi trả!");
                return "redirect:/admin/returns/" + orderId;
            }

            // Cập nhật trạng thái về COMPLETED
            order.setStatus(Order.OrderStatus.COMPLETED);
            orderService.save(order);

            // Xóa các request items liên quan
            List<ReturnRequestItem> allRequestItems = returnRequestItemRepository.findByOrder(order);
            if (!allRequestItems.isEmpty()) {
                returnRequestItemRepository.deleteAll(allRequestItems);
            }

            redirectAttributes.addFlashAttribute("message",
                    "Đã từ chối yêu cầu đổi trả cho đơn hàng " + order.getOrderNumber() + "!");

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Có lỗi xảy ra khi từ chối yêu cầu: " + e.getMessage());
            return "redirect:/admin/returns/" + orderId;
        }

        return "redirect:/admin/returns";
    }

    // Debug endpoint - có thể xóa sau khi test xong
    @GetMapping("/{orderId}/debug")
    @ResponseBody
    public String debugReturnRequest(@PathVariable Long orderId) {
        try {
            Order order = orderService.findById(orderId);
            if (order == null) return "Order not found";

            List<ReturnRequestItem> allItems = returnRequestItemRepository.findByOrder(order);
            
            StringBuilder debug = new StringBuilder();
            debug.append("<h3>Debug Return Request</h3>");
            debug.append("Order ID: ").append(orderId).append("<br>");
            debug.append("Order Status: ").append(order.getStatus()).append("<br>");
            debug.append("Total ReturnRequestItems: ").append(allItems.size()).append("<br><br>");
            
            for (int i = 0; i < allItems.size(); i++) {
                ReturnRequestItem item = allItems.get(i);
                debug.append("<b>Item ").append(i + 1).append(":</b><br>");
                debug.append("- ID: ").append(item.getId()).append("<br>");
                debug.append("- Type: '").append(item.getType()).append("'<br>");
                debug.append("- Product: ").append(item.getProduct() != null ? item.getProduct().getName() : "NULL").append("<br>");
                debug.append("- Quantity: ").append(item.getQuantity()).append("<br>");
                debug.append("- Unit Price: ").append(item.getUnitPrice()).append("<br><br>");
            }
            
            return debug.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}