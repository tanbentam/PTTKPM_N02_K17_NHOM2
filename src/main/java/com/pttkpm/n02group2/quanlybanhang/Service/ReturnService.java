package com.pttkpm.n02group2.quanlybanhang.Service;

import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import com.pttkpm.n02group2.quanlybanhang.Repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReturnService {

    @Autowired
    private OrderRepository orderRepository;

    public int countAllRequests() {
        // Đếm tất cả các yêu cầu đổi/trả
        return orderRepository.countByHasReturnExchangeRequest(true);  // parameter không còn ý nghĩa nhưng giữ lại để không phải thay đổi nhiều code
    }

    public int countRequestsByStatus(String status) {
        // Đếm số yêu cầu theo trạng thái
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status);
            return orderRepository.countByReturnExchangeRequestStatus(orderStatus);
        } catch (IllegalArgumentException e) {
            // Log lỗi và trả về 0 nếu status không hợp lệ
            System.err.println("Invalid order status: " + status);
            return 0;
        }
    }
}