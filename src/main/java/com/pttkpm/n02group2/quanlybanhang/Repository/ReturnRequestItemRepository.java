// ReturnRequestItemRepository.java
package com.pttkpm.n02group2.quanlybanhang.Repository;

import com.pttkpm.n02group2.quanlybanhang.Model.ReturnRequestItem;
import com.pttkpm.n02group2.quanlybanhang.Model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReturnRequestItemRepository extends JpaRepository<ReturnRequestItem, Long> {
    List<ReturnRequestItem> findByOrder(Order order);
}