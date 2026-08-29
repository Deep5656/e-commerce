package com.deepweb.Order_Service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.deepweb.Order_Service.dto.OrderRequest;
import com.deepweb.Order_Service.dto.OrderResponse;
import com.deepweb.Order_Service.service.OrderService;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/order")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String createOrder(@RequestBody OrderRequest orderRequest) {
        orderService.createOrder(orderRequest);
        return "Order Placed Succefully";
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        List<OrderResponse> all = orderService.getAll();
        return all;
    }
}
