package com.deepweb.Inventory_Service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.deepweb.Inventory_Service.model.Inventory;
import com.deepweb.Inventory_Service.repository.InventoryRepositoy;

@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner loadData(InventoryRepositoy inventoryRepositoy){
        return agrs -> {
            Inventory inventory = new Inventory();
            inventory.setSkuCode("iphone_13");
            inventory.setQuantity("1000");

            Inventory inventory2 = new Inventory();
            inventory2.setSkuCode("iphone_13_pro");
            inventory2.setQuantity("11");

            inventoryRepositoy.save(inventory);
            inventoryRepositoy.save(inventory2);
        };
    }
}
