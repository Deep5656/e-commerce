package com.deepweb.Inventory_Service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.deepweb.Inventory_Service.model.Inventory;

@Repository
public interface InventoryRepositoy extends JpaRepository<Inventory,String>{

    // An "In" query matches many rows, so it must return a List. Declaring it
    // as Optional<Inventory> made Spring Data cap the result at one row and
    // throw IncorrectResultSizeDataAccessException as soon as two SKUs matched.
    List<Inventory> findBySkuCodeIn(List<String> skuCode);
}
