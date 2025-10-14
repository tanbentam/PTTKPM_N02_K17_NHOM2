package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Repository.ReturnExchangeRepository;
import com.pttkpm.n02group2.quanlybanhang.Repository.OrderRepository;
import com.pttkpm.n02group2.quanlybanhang.Service.InventoryService;
import com.pttkpm.n02group2.quanlybanhang.Service.ReturnExchangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;

@Controller
public class ReturnExchangeController {

    @Autowired
    private ReturnExchangeRepository returnExchangeRepository;

    @Autowired
    private OrderRepository orderRepository;

    // User (Staff) endpoints
    @GetMapping("/user/returns")
    public String showUserReturnExchangePage(Model model) {
        return "user/returns/index";
    }

    @GetMapping("/user/returns/search")
    @ResponseBody
    public ResponseEntity<?> searchCompletedOrders(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endDate,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String orderNumber) {
        
        List<Order> completedOrders;
        try {
            if (startDate != null && endDate != null) {
                if (customerName != null && !customerName.trim().isEmpty()) {
                    completedOrders = orderRepository.findByStatusAndCreatedAtBetweenAndCustomerNameContaining(
                        Order.OrderStatus.COMPLETED, startDate, endDate, customerName.trim());
                } else {
                    completedOrders = orderRepository.findByStatusAndCreatedAtBetween(
                        Order.OrderStatus.COMPLETED, startDate, endDate);
                }
            } else if (orderNumber != null && !orderNumber.trim().isEmpty()) {
                completedOrders = orderRepository.findByStatusAndOrderNumber(
                    Order.OrderStatus.COMPLETED, orderNumber.trim());
            } else if (customerName != null && !customerName.trim().isEmpty()) {
                completedOrders = orderRepository.findByStatusAndCustomerNameContaining(
                    Order.OrderStatus.COMPLETED, customerName.trim());
            } else {
                // Sử dụng findAllByStatus để lấy danh sách không phân trang
                completedOrders = orderRepository.findAllByStatus(Order.OrderStatus.COMPLETED.toString());
            }
            
            if (completedOrders == null) {
                completedOrders = new ArrayList<>();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Error searching orders: " + e.getMessage()));
        }
        return ResponseEntity.ok(completedOrders);
    }
    
    @GetMapping("/user/returns/status/{orderId}")
    @ResponseBody
    public ResponseEntity<?> getReturnExchangeStatus(@PathVariable Long orderId) {
        ReturnExchangeRequest request = returnExchangeRepository.findByOriginalOrder_Id(orderId);
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(request);
    }

    @Autowired
    private ReturnExchangeService returnExchangeService;

    @PostMapping("/user/returns/create")
    @ResponseBody
    public ResponseEntity<?> createReturnExchange(@RequestBody Map<String, Object> requestData) {
        try {
            ReturnExchangeRequest savedRequest = returnExchangeService.createRequest(requestData);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "request", savedRequest,
                "message", "Yêu cầu đổi/trả hàng đã được tạo thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    // Các endpoint của admin đã được chuyển sang AdminOrderController

    @Autowired
    private InventoryService inventoryService;

    // Endpoint cho admin
    @GetMapping("/admin/returns/list")
    @ResponseBody
    public ResponseEntity<?> getReturnExchangeList(
            @RequestParam(required = false) ReturnExchangeRequest.ReturnExchangeType type,
            @RequestParam(required = false) ReturnExchangeRequest.ReturnExchangeStatus status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endDate) {
        
        List<ReturnExchangeRequest> requests;
        
        if (type != null && status != null) {
            requests = returnExchangeRepository.findByTypeAndStatus(type, status);
        } else if (status != null) {
            requests = returnExchangeRepository.findByStatus(status);
        } else if (startDate != null && endDate != null) {
            requests = returnExchangeRepository.findByCreatedAtBetween(startDate, endDate);
        } else {
            requests = returnExchangeRepository.findAll();
        }
        
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/admin/returns/pending-count")
    @ResponseBody
    public ResponseEntity<Long> getPendingRequestCount() {
        long count = returnExchangeRepository.countByStatus(ReturnExchangeRequest.ReturnExchangeStatus.PENDING);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/admin/products/{productId}/stock")
    @ResponseBody
    public ResponseEntity<?> getProductStock(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }

    @PostMapping("/{id}/approve")
    @ResponseBody
    public ResponseEntity<?> approveRequest(@PathVariable Long id) {
        Optional<ReturnExchangeRequest> requestOpt = returnExchangeRepository.findById(id);
        if (!requestOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Request not found");
        }

        ReturnExchangeRequest request = requestOpt.get();
        request.setStatus(ReturnExchangeRequest.ReturnExchangeStatus.APPROVED);
        
        // Cập nhật số lượng tồn kho
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

        returnExchangeRepository.save(request);

        Order order = request.getOriginalOrder();
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        return ResponseEntity.ok(request);
    }

    @PostMapping("/{id}/reject")
    @ResponseBody
    public ResponseEntity<?> rejectRequest(
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