package com.pttkpm.n02group2.quanlybanhang.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pttkpm.n02group2.quanlybanhang.Model.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Additional query methods can be defined here
}