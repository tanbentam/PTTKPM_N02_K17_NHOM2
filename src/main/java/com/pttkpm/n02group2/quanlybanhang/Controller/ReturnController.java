package com.pttkpm.n02group2.quanlybanhang.Controller;

import com.pttkpm.n02group2.quanlybanhang.Service.ReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user/returns")
public class ReturnController {

    @Autowired
    private ReturnService returnService;

    @GetMapping("/statistics")
    @ResponseBody
    public Map<String, Integer> getReturnStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", returnService.countAllRequests());
        stats.put("pending", returnService.countRequestsByStatus("PENDING"));
        stats.put("approved", returnService.countRequestsByStatus("APPROVED"));
        stats.put("rejected", returnService.countRequestsByStatus("REJECTED"));
        return stats;
    }
}