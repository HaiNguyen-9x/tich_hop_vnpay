package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RefundResponseDto {
    private String status;
    private String merchantName;
    private String createBy;
    private String amount;
    private String transactionType;
    private String refundAmount;
    private String transactionNumber;
    private String transactionResponseCode;
    private String transactionStatus;
    private String bank;
    private String createDate;
}
