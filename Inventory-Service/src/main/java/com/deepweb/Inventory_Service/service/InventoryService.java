package com.deepweb.Inventory_Service.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deepweb.Inventory_Service.dto.InventoryResponse;
import com.deepweb.Inventory_Service.repository.InventoryRepositoy;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepositoy inventoryRepositoy;

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCode) {
        return inventoryRepositoy.findBySkuCodeIn(skuCode).stream()
                .map(inventory -> InventoryResponse.builder()
                        .skuCode(inventory.getSkuCode())
                        .isInStock(Integer.parseInt(inventory.getQuantity()) > 0)
                        .build())
                .toList();
    }
}
