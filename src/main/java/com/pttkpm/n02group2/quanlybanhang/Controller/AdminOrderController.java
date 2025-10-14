package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Repository.ReturnExchangeRepository;
import com.pttkpm.n02group2.quanlybanhang.Repository.OrderRepository;
import com.pttkpm.n02group2.quanlybanhang.Service.InventoryService;
import com.pttkpm.n02group2.quanlybanhang.Service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminOrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReturnExchangeRepository returnExchangeRepository;

    @Autowired
    private InventoryService inventoryService;

    @GetMapping("/orders/manage")
    public String showOrdersPage(Model model) {
        return "admin/orders/index";
    }

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders/list")
    @ResponseBody
    public ResponseEntity<?> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endDate,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String orderNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            // Tạo Pageable với sắp xếp theo thời gian tạo giảm dần
            PageRequest pageRequest = PageRequest.of(page, size, 
                Sort.by("createdAt").descending());

            // Chuyển đổi status string sang enum nếu có
            Order.OrderStatus orderStatus = null;
            if (status != null && !status.isEmpty()) {
                orderStatus = Order.OrderStatus.valueOf(status);
            }

            // Sử dụng phương thức findOrders từ OrderService
            Page<Order> ordersPage = orderService.findOrders(
                orderStatus,
                startDate,
                endDate,
                customerName,
                orderNumber,
                pageRequest
            );

            return ResponseEntity.ok(ordersPage);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Error searching orders: " + e.getMessage());
        }
    }

    @PostMapping("/returns/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveReturnRequest(@PathVariable Long id) {
        Optional<ReturnExchangeRequest> requestOpt = returnExchangeRepository.findById(id);
        if (!requestOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Request not found");
        }

        ReturnExchangeRequest request = requestOpt.get();
        request.setStatus(ReturnExchangeRequest.ReturnExchangeStatus.APPROVED);
        
        // Cập nhật số lượng tồn kho
        try {
            for (ReturnExchangeItem item : request.getItems()) {
                if (request.getType() == ReturnExchangeRequest.ReturnExchangeType.RETURN) {
                    // Nếu là trả hàng, tăng số lượng tồn
                    inventoryService.updateInventory(item.getProduct().getId(), item.getQuantity());
                } else if (request.getType() == ReturnExchangeRequest.ReturnExchangeType.EXCHANGE) {
                    // Nếu là đổi hàng, tăng số lượng tồn cho sản phẩm trả về
                    inventoryService.updateInventory(item.getProduct().getId(), item.getQuantity());
                    // Và giảm số lượng tồn cho sản phẩm mới (nếu có)
                    if (request.getExchangeOrder() != null) {
                        for (OrderItem exchangeItem : request.getExchangeOrder().getItems()) {
                            inventoryService.updateInventory(
                                exchangeItem.getProduct().getId(), 
                                -exchangeItem.getQuantity()
                            );
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Error updating inventory: " + e.getMessage());
        }

        returnExchangeRepository.save(request);

        Order order = request.getOriginalOrder();
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        return ResponseEntity.ok(request);
    }

    @PostMapping("/returns/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectReturnRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        
        Optional<ReturnExchangeRequest> requestOpt = returnExchangeRepository.findById(id);
        if (!requestOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Request not found");
        }

        String reason = payload.get("reason");
        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Rejection reason is required");
        }

        ReturnExchangeRequest request = requestOpt.get();
        request.setStatus(ReturnExchangeRequest.ReturnExchangeStatus.REJECTED);
        request.setRejectionReason(reason);
        returnExchangeRepository.save(request);

        Order order = request.getOriginalOrder();
        order.setStatus(Order.OrderStatus.COMPLETED);
        orderRepository.save(order);

        return ResponseEntity.ok(request);
    }
}