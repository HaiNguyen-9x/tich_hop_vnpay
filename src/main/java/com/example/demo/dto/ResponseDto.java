package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

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

    public ResponseDto(Map<String, String> dataResponse) {
        String amount = dataResponse.get("vnp_Amount");

        this.status = "Truy vấn thành công";
        this.merchantName = "Demo";
        this.transactionResponseCode = dataResponse.get("vnp_TransactionStatus");
        this.transactionNumber = dataResponse.get("vnp_TransactionNo");
        this.amount = amount.substring(0, amount.length() - 2);
        this.transactionInfo = dataResponse.get("vnp_OrderInfo");
        this.bank = dataResponse.get("vnp_BankCode");
        this.createDate = Mapper.transDate(dataResponse.get("vnp_PayDate"));
    }
}
