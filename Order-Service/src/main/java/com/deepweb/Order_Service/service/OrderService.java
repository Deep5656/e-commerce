package com.deepweb.Order_Service.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.deepweb.Order_Service.dto.InventoryResponse;
import com.deepweb.Order_Service.dto.OrderLineItemsDto;
import com.deepweb.Order_Service.dto.OrderRequest;
import com.deepweb.Order_Service.dto.OrderResponse;
import com.deepweb.Order_Service.event.OrderEventProducer;
import com.deepweb.Order_Service.event.OrderPlacedEvent;
import com.deepweb.Order_Service.exception.InventoryNotFoundException;
import com.deepweb.Order_Service.model.Order;
import com.deepweb.Order_Service.model.OrderLineItems;
import com.deepweb.Order_Service.repo.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderService {

        private final OrderRepository orderRepository;
        private final InventoryServiceClient inventoryServiceClient;
        private final OrderEventProducer orderEventProducer;
        // private final WebClient webClient;

        public void createOrder(OrderRequest orderRequest) {
                Order order = Order.builder()
                                .orderId(UUID.randomUUID().toString())
                                .orderLineItemsList(orderRequest.getOrderLineItemsDtos()
                                                .stream()
                                                .map(this::mapToOrderLineItems)
                                                .collect(Collectors.toList()))
                                .build();

                List<String> skuCodes = order.getOrderLineItemsList().stream()
                                .map(OrderLineItems::getSkuCode)
                                .toList();

                List<String> quantityList = order.getOrderLineItemsList().stream()
                                .map(OrderLineItems::getQuantity).toList();

                boolean hasZeroQuantity = quantityList.stream()
                                .anyMatch(q -> Integer.parseInt(q) <= 0);

                if (hasZeroQuantity) {
                        throw new IllegalArgumentException(
                                        "Please add at least one product (quantity must be greater than 0)");
                }

                // InventoryResponse[] result;
                InventoryResponse[] result = inventoryServiceClient.getInventoryStatus(skuCodes);

                // try {
                // result = webClient.get()
                // .uri("http://Inventory-Service/api/inventory",
                // uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes)
                // .build())
                // .retrieve()
                // .bodyToMono(InventoryResponse[].class)
                // .block();
                // } catch (WebClientResponseException ex) {
                // throw new InventoryNotFoundException(
                // "Inventory service error: " + ex.getResponseBodyAsString());
                // }

                // Inventory only returns rows for SKUs it actually holds, so an unknown
                // SKU comes back as a *missing* entry, not as isInStock=false. Validate
                // against what we asked for rather than only inspecting what came back:
                // allMatch on an empty stream is vacuously true, which is why an order
                // made entirely of unknown SKUs used to be accepted.
                List<InventoryResponse> inventory = result == null ? List.of() : Arrays.asList(result);

                Set<String> inStockSkuCodes = inventory.stream()
                                .filter(InventoryResponse::isInStock)
                                .map(InventoryResponse::getSkuCode)
                                .collect(Collectors.toSet());

                List<String> unavailableSkuCodes = skuCodes.stream()
                                .filter(skuCode -> !inStockSkuCodes.contains(skuCode))
                                .distinct()
                                .toList();

                if (!unavailableSkuCodes.isEmpty()) {
                        throw new InventoryNotFoundException(
                                        "Not in stock: " + String.join(", ", unavailableSkuCodes));
                }

                orderRepository.save(order);
                log.info("Order {} placed successfully", order.getOrderId());

                // send kafka event
                OrderPlacedEvent event = OrderPlacedEvent.builder()
                                .orderId(order.getOrderId())
                                .skuCodes(skuCodes)
                                .message("Order placed successfully for: " + String.join(", ", skuCodes))
                                .build();

                orderEventProducer.sendOrderPlacedEvent(event);

        }

        // @Retry(name = "inventory", fallbackMethod = "fallbackInventoryResponse")
        // @CircuitBreaker(name = "inventory", fallbackMethod =
        // "fallbackInventoryResponse")
        // public InventoryResponse[] getInventoryStatus(List<String> skuCodes) {
        // try {
        // return webClient.get()
        // .uri("http://Inventory-Service/api/inventory",
        // uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes)
        // .build())
        // .retrieve()
        // .bodyToMono(InventoryResponse[].class)
        // .block();
        // } catch (WebClientResponseException ex) {
        // throw new InventoryNotFoundException(
        // "Inventory service error: " + ex.getResponseBodyAsString());
        // }

        // }

        // public InventoryResponse[] fallbackInventoryResponse(List<String> skuCodes,
        // Exception ex) {
        // log.error("Inventory service is down or failed! Fallback executed. Error:
        // {}", ex.getMessage());
        // throw new InventoryNotFoundException(
        // "Inventory service is currently unavailable. Please try again later.");
        // }

        private OrderLineItems mapToOrderLineItems(OrderLineItemsDto dto) {
                return OrderLineItems.builder()
                                .skuCode(dto.getSkuCode())
                                .price(dto.getPrice())
                                .quantity(dto.getQuantity())
                                .build();
        }

        public List<OrderResponse> getAll() {
                List<Order> allOrders = orderRepository.findAll();
                return allOrders.stream()
                                .map(this::mapToDto)
                                .toList();
        }

        private OrderResponse mapToDto(Order order) {
                return OrderResponse.builder()
                                .orderId(order.getOrderId())
                                .id(order.getId())
                                .orderLineItemsDtos(order.getOrderLineItemsList())
                                .build();
        }
}
