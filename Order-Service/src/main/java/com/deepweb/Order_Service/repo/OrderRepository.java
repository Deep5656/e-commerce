package com.deepweb.Order_Service.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.deepweb.Order_Service.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order,String>{
    
}
