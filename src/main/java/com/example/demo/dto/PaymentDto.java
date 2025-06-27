package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDto {
    private Long id;
    private String amount;
    private String transactionInfo;
    private String dateCreate;
    private String status;
    private String ipAddr;
}