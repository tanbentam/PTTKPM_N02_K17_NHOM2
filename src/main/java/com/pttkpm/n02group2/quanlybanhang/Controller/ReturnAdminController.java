package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Model.OrderItem;
import com.pttkpm.n02group2.quanlybanhang.Model.Product;
import com.pttkpm.n02group2.quanlybanhang.Model.ReturnRequestItem;
import com.pttkpm.n02group2.quanlybanhang.Repository.ReturnRequestItemRepository;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import com.pttkpm.n02group2.quanlybanhang.Service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/returns")
public class ReturnAdminController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ReturnRequestItemRepository returnRequestItemRepository;
    
    @Autowired
    private ProductService productService;

    /**
     * Hiển thị danh sách yêu cầu đổi trả chờ duyệt
     */
    @GetMapping
    public String listReturnRequests(Model model) {
        try {
            List<Order> returnRequests = orderService.findByStatus(Order.OrderStatus.RETURN_REQUESTED);
            model.addAttribute("returnRequests", returnRequests);
            return "admin/return/list";
        } catch (Exception e) {
            System.err.println("Error loading return requests: " + e.getMessage());
            model.addAttribute("error", "Có lỗi xảy ra khi tải danh sách yêu cầu đổi trả.");
            return "admin/return/list";
        }
    }

    /**
     * Hiển thị chi tiết yêu cầu đổi trả
     */
    @GetMapping("/{orderId}")
    public String viewReturnRequest(@PathVariable Long orderId, Model model) {
        try {
            Order order = orderService.findById(orderId);
            if (order == null) {
                return "redirect:/admin/returns";
            }
            
            // Lấy danh sách sản phẩm muốn nhận
            List<ReturnRequestItem> newItems = returnRequestItemRepository.findByOrder(order);
            
            model.addAttribute("order", order);
            model.addAttribute("newItems", newItems);
            
            // Log để debug
            System.out.println("=== ADMIN VIEW RETURN REQUEST ===");
            System.out.println("Order ID: " + order.getId());
            System.out.println("Order Number: " + order.getOrderNumber());
            System.out.println("New items count: " + newItems.size());
            for (ReturnRequestItem item : newItems) {
                System.out.println("- " + item.getProduct().getName() + " x " + item.getQuantity());
            }
            System.out.println("================================");
            
            return "admin/return/detail";
            
        } catch (Exception e) {
            System.err.println("Error viewing return request: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/admin/returns";
        }
    }

    /**
     * Duyệt yêu cầu đổi trả
     */
    @PostMapping("/{orderId}/approve")
    public String approveReturn(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
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
            
            // 1. TĂNG tồn kho cho sản phẩm trả lại (sản phẩm đã mua)
            System.out.println("=== APPROVING RETURN REQUEST ===");
            System.out.println("Restoring stock for returned products:");
            
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                int oldStock = product.getQuantity();
                int newStock = oldStock + item.getQuantity();
                
                System.out.println("- " + product.getName() + ": " + oldStock + " + " + item.getQuantity() + " = " + newStock);
                
                product.setQuantity(newStock);
                productService.save(product);
            }
            
            // 2. GIẢM tồn kho cho sản phẩm muốn nhận (sản phẩm đổi cho khách)
            System.out.println("Reducing stock for new products:");
            
            List<ReturnRequestItem> newItems = returnRequestItemRepository.findByOrder(order);
            for (ReturnRequestItem item : newItems) {
                Product product = item.getProduct();
                int oldStock = product.getQuantity();
                int newStock = oldStock - item.getQuantity();
                
                // Kiểm tra tồn kho không bị âm
                if (newStock < 0) {
                    System.err.println("ERROR: Insufficient stock for " + product.getName());
                    redirectAttributes.addFlashAttribute("error", 
                        "Sản phẩm " + product.getName() + " không đủ tồn kho để đổi! " +
                        "(Hiện có: " + oldStock + ", cần: " + item.getQuantity() + ")");
                    return "redirect:/admin/returns/" + orderId;
                }
                
                System.out.println("- " + product.getName() + ": " + oldStock + " - " + item.getQuantity() + " = " + newStock);
                
                product.setQuantity(newStock);
                productService.save(product);
            }
            
            // 3. Cập nhật trạng thái đơn hàng
            order.setStatus(Order.OrderStatus.RETURNED);
            orderService.save(order);
            
            System.out.println("Order status updated to RETURNED");
            System.out.println("===============================");
            
            redirectAttributes.addFlashAttribute("message", 
                "Duyệt yêu cầu đổi trả thành công! Đã cập nhật tồn kho cho " + 
                order.getItems().size() + " sản phẩm trả lại và " + 
                newItems.size() + " sản phẩm muốn nhận.");
                
        } catch (Exception e) {
            System.err.println("ERROR in approveReturn: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "Có lỗi xảy ra khi duyệt yêu cầu: " + e.getMessage());
            return "redirect:/admin/returns/" + orderId;
        }
        
        return "redirect:/admin/returns";
    }

    /**
     * Từ chối yêu cầu đổi trả
     */
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
            
            // Cập nhật trạng thái về COMPLETED (đã hoàn thành, không đổi trả)
            order.setStatus(Order.OrderStatus.COMPLETED);
            orderService.save(order);
            
            // Xóa thông tin sản phẩm muốn nhận (vì đã từ chối)
            List<ReturnRequestItem> newItems = returnRequestItemRepository.findByOrder(order);
            if (!newItems.isEmpty()) {
                returnRequestItemRepository.deleteAll(newItems);
                System.out.println("Deleted " + newItems.size() + " return request items for rejected order");
            }
            
            redirectAttributes.addFlashAttribute("message", 
                "Đã từ chối yêu cầu đổi trả cho đơn hàng " + order.getOrderNumber() + "!");
                
        } catch (Exception e) {
            System.err.println("ERROR in rejectReturn: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "Có lỗi xảy ra khi từ chối yêu cầu: " + e.getMessage());
            return "redirect:/admin/returns/" + orderId;
        }
        
        return "redirect:/admin/returns";
    }
}