package com.deepweb.Order_Service.dto;

import java.util.List;

import com.deepweb.Order_Service.model.OrderLineItems;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class OrderResponse {
    private String id;
    private String orderId;
    private List<OrderLineItems> orderLineItemsDtos;
}
