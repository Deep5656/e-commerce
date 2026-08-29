package com.deepweb.ProductService.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.deepweb.ProductService.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product,String> {
    
}
