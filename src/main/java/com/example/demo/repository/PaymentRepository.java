package com.example.demo.repository;

import com.example.demo.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("SELECT p FROM Payment p WHERE CONCAT('', p.id) LIKE %:pageSearch%")
    Page<Payment> searchByIdLike(@Param("pageSearch") String pageSearch, Pageable pageable);
}
