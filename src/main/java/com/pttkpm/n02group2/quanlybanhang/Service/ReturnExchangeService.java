package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.*;
import com.pttkpm.n02group2.quanlybanhang.Repository.ReturnExchangeRepository;
import com.pttkpm.n02group2.quanlybanhang.Repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ReturnExchangeService {

    @Autowired
    private ReturnExchangeRepository returnExchangeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderService orderService;

    @Transactional
    public ReturnExchangeRequest createRequest(Map<String, Object> requestData) {
        try {
            Long orderId = Long.parseLong(requestData.get("orderId").toString());
            String type = requestData.get("type").toString();
            String reason = requestData.get("reason").toString();
            
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                throw new RuntimeException("Không tìm thấy đơn hàng");
            }

            Order order = orderOpt.get();
            if (order.getStatus() != Order.OrderStatus.COMPLETED) {
                throw new RuntimeException("Chỉ đơn hàng đã hoàn thành mới có thể đổi/trả");
            }

            ReturnExchangeRequest request = new ReturnExchangeRequest();
            request.setOriginalOrder(order);
            request.setType(ReturnExchangeRequest.ReturnExchangeType.valueOf(type));
            request.setReason(reason);
            request.setStatus(ReturnExchangeRequest.ReturnExchangeStatus.PENDING);
            request.setCreatedAt(LocalDateTime.now());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) requestData.get("items");
            List<ReturnExchangeItem> returnItems = new ArrayList<>();

            for (Map<String, Object> item : items) {
                ReturnExchangeItem returnItem = new ReturnExchangeItem();
                returnItem.setReturnExchangeRequest(request);
                returnItem.setProduct(orderService.findProductById(Long.parseLong(item.get("id").toString())));
                returnItem.setQuantity(Integer.parseInt(item.get("quantity").toString()));
                returnItems.add(returnItem);
            }

            request.setItems(returnItems);

            // Nếu là yêu cầu đổi hàng, xử lý thêm sản phẩm đổi
            if (request.getType() == ReturnExchangeRequest.ReturnExchangeType.EXCHANGE && requestData.containsKey("exchangeProducts")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> exchangeProducts = (List<Map<String, Object>>) requestData.get("exchangeProducts");
                
                // Tạo đơn hàng mới cho sản phẩm đổi
                Order exchangeOrder = new Order();
                exchangeOrder.setCustomer(order.getCustomer());
                exchangeOrder.setStatus(Order.OrderStatus.PENDING);
                
                List<OrderItem> exchangeItems = new ArrayList<>();
                for (Map<String, Object> product : exchangeProducts) {
                    OrderItem item = new OrderItem();
                    item.setOrder(exchangeOrder);
                    item.setProduct(orderService.findProductById(Long.parseLong(product.get("id").toString())));
                    item.setQuantity(Integer.parseInt(product.get("quantity").toString()));
                    exchangeItems.add(item);
                }
                
                exchangeOrder.setItems(exchangeItems);
                orderRepository.save(exchangeOrder);
                request.setExchangeOrder(exchangeOrder);
            }

            // Cập nhật trạng thái đơn hàng gốc
            if (type.equals("RETURN")) {
                order.setStatus(Order.OrderStatus.PROCESSING_RETURN);
            } else {
                order.setStatus(Order.OrderStatus.PROCESSING_EXCHANGE);
            }
            orderRepository.save(order);

            return returnExchangeRepository.save(request);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo yêu cầu đổi/trả: " + e.getMessage());
        }
    }

    public Optional<ReturnExchangeRequest> findByOrderId(Long orderId) {
        return Optional.ofNullable(returnExchangeRepository.findByOriginalOrder_Id(orderId));
    }
}
