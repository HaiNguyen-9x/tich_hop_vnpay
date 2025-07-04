package com.example.demo.dto;


import com.example.demo.Utils.DateUtils;
import com.example.demo.Utils.IpUtils;
import com.example.demo.entity.Payment;
import com.example.demo.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InputToVnpay {
    public  final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND_PAY = "pay";
    public  final String VNP_COMMAND_REFUND = "refund";
    public static final String VNP_COMMAND_QUERY = "querydr";
    public static final String VNP_CURRENCY_VND = "VND";
    public static final String VNP_LOCALE_VN = "vn";

    @Value("${vnp.TmnCode}")
    private  String vnpTmnCode;

    @Value("${vnp.HashSecret}")
    private  String vnpHashSecret;

    @Value("${vnp.ReturnUrl}")
    private String vnpReturnUrl;

    public Map<String, String> generateOrderInput (HttpServletRequest request, PaymentRepository paymentRepository,
                                                   RequestDto requestDto) {
        String orderType, orderInfo;
        if (requestDto.getOrderType() != null) orderType = requestDto.getOrderType();
        else orderType = "other";
        String txnRef = String.valueOf(paymentRepository.count() + 1);
        if(requestDto.getOrderInfo() != null) orderInfo = requestDto.getOrderInfo();
        else orderInfo = "Thanh toan" + txnRef;
        String createDate = DateUtils.generateDate();

        Map<String, String> order = new HashMap<>();
        order.put("vnp_Version", VNP_VERSION);
        order.put("vnp_Command", VNP_COMMAND_PAY);
        order.put("vnp_TmnCode", vnpTmnCode);
        order.put("vnp_Amount", String.valueOf(requestDto.getAmount() * 100));
        String bank_code = requestDto.getBankCode();
        if (bank_code != null && !bank_code.isEmpty())
            order.put("vnp_BankCode", bank_code);
        order.put("vnp_CreateDate", createDate);
        order.put("vnp_CurrCode", VNP_CURRENCY_VND);
        order.put("vnp_IpAddr", IpUtils.generateIp(request));
        if (requestDto.getLocale() != null)
            order.put("vnp_Locale", requestDto.getLocale());
        else order.put("vnp_Locale", VNP_LOCALE_VN);
        order.put("vnp_OrderInfo", orderInfo);
        order.put("vnp_OrderType", orderType);
        order.put("vnp_ReturnUrl", vnpReturnUrl);
        order.put("vnp_ExpireDate", DateUtils.add15Minutes(createDate));
        order.put("vnp_TxnRef", txnRef);
        return order;
    }

    public Map<String, String> generateQueryInput(HttpServletRequest request, Payment payment) {
        Map<String, String> input = new HashMap<>();
        Random rd = new Random();

        input.put("vnp_RequestId", DateUtils.generateDate());
        input.put("vnp_Version", VNP_VERSION);
        input.put("vnp_Command", VNP_COMMAND_QUERY);
        input.put("vnp_TmnCode", vnpTmnCode);
        input.put("vnp_TxnRef", payment.getId().toString());
        input.put("vnp_OrderInfo", "Truy cuu giao dich TxnRef: " + payment.getId());
        if (payment.getTransactionNo() != null) input.put("vnp_TransactionNo", payment.getTransactionNo());
        else input.put("vnp_TransactionNo", "");
        input.put("vnp_TransactionDate", Mapper.transDate(payment.getTimestamp()));
        input.put("vnp_CreateDate", DateUtils.generateDate());
        input.put("vnp_IpAddr", IpUtils.generateIp(request));

        String hash_Data= String.join("|", input.get("vnp_RequestId"), input.get("vnp_Version"),
                input.get("vnp_Command"), input.get("vnp_TmnCode"), input.get("vnp_TxnRef"),
                input.get("vnp_TransactionDate"), input.get("vnp_CreateDate"), input.get("vnp_IpAddr"),
                input.get("vnp_OrderInfo"));

        input.put("vnp_SecureHash", Hex.encodeHexString(HmacUtils.hmacSha512(vnpHashSecret, hash_Data)));

        return input;
    }

    public  Map<String, String> generateRefundInput(HttpServletRequest request, RefundDto refundDto, Payment payment) {
        Map<String, String> input = new HashMap<>();

        input.put("vnp_RequestId", refundDto.getId().toString());
        input.put("vnp_Version", VNP_VERSION);
        input.put("vnp_Command", VNP_COMMAND_REFUND);
        input.put("vnp_TmnCode", vnpTmnCode);
        input.put("vnp_TransactionType", refundDto.getTransactionType());
        input.put("vnp_TxnRef", payment.getId().toString());
        if (refundDto.getAmount() != null) input.put("vnp_Amount", refundDto.getAmount().toString().concat("00"));
        else input.put("vnp_Amount", payment.getAmount().toString().concat("00"));
        input.put("vnp_OrderInfo", "Hoan tra don hang " + payment.getId());
        if (payment.getTransactionNo() != null) input.put("vnp_TransactionNo", payment.getTransactionNo());
        else input.put("vnp_TransactionNo", "");
        input.put("vnp_CreateBy", refundDto.getCreateBy());
        input.put("vnp_TransactionDate", Mapper.transDate(payment.getTimestamp()));
        input.put("vnp_CreateDate", DateUtils.generateDate());
        input.put("vnp_IpAddr", IpUtils.generateIp(request));

        String hash_Data= String.join("|", input.get("vnp_RequestId"), input.get("vnp_Version"),
                input.get("vnp_Command"), input.get("vnp_TmnCode"), input.get("vnp_TransactionType"),
                input.get("vnp_TxnRef"), input.get("vnp_Amount"), input.get("vnp_TransactionNo"),
                input.get("vnp_TransactionDate"), input.get("vnp_CreateBy"), input.get("vnp_CreateDate"),
                input.get("vnp_IpAddr"), input.get("vnp_OrderInfo"));

        input.put("vnp_SecureHash", Hex.encodeHexString(HmacUtils.hmacSha512(vnpHashSecret, hash_Data)));

        return input;
    }
}