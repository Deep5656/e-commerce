package com.deepweb.Order_Service.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.deepweb.Order_Service.dto.InventoryResponse;
import com.deepweb.Order_Service.exception.InventoryNotFoundException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final WebClient webClient;

    @Retry(name = "inventory", fallbackMethod = "fallbackInventoryResponse")
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackInventoryResponse")
    public InventoryResponse[] getInventoryStatus(List<String> skuCodes) {
        return webClient.get()
                .uri("http://Inventory-Service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();
    }

    public InventoryResponse[] fallbackInventoryResponse(List<String> skuCodes, Exception ex) {
        log.error("Inventory service is down or failed! Fallback executed. Error: {}", ex.getMessage());
        throw new InventoryNotFoundException(
                "Inventory service is currently unavailable. Please try again later.");
    }
}
