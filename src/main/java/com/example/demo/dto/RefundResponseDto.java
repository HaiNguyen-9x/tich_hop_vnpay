package com.example.demo.dto;

import com.example.demo.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

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

    public RefundResponseDto(Map<String, String> dataResponse, Map<String, String> input, Payment payment) {
        String amount = dataResponse.get("vnp_Amount");

        this.status = "Yêu cầu thành công";
        this.merchantName = "Demo";
        this.createBy = input.get("vnp_CreateBy");
        this.amount = String.valueOf(payment.getAmount());
        this.transactionType = input.get("vnp_TransactionType").equals("02") ? "Hoàn trả toàn phần" : "Hoàn trả một phần";
        this.refundAmount = amount.substring(0, amount.length() - 2);
        this.transactionNumber = dataResponse.get("vnp_TransactionNo");
        this.transactionResponseCode = dataResponse.get("vnp_ResponseCode");
        this.transactionStatus = dataResponse.get("vnp_TransactionStatus");
        this.bank = dataResponse.get("vnp_BankCode");
        this.createDate = Mapper.transDate(dataResponse.get("vnp_PayDate"));
    }
}
