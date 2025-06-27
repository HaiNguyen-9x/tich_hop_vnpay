package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDto {
    private String status;
    private String merchantName;
    private String transactionInfo;
    private String amount;
    private String transactionResponseCode;
    private String transactionNumber;
    private String bank;
    private String createDate;
}
