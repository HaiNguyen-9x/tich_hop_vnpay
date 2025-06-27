package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "refunds")
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long amount;
    private String transactionInfo;
    private String timestamp;
    private int status;
    private String responseCode;

    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;
}
