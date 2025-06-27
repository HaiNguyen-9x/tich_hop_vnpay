package com.example.demo.dto;

import com.example.demo.entity.Payment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Mapper {
    public static PaymentDto getPaymentDto(Payment payment) {
        PaymentDto paymentdto = new PaymentDto();
        paymentdto.setId(payment.getId());
        paymentdto.setAmount(payment.getAmount().toString());
        paymentdto.setTransactionInfo(payment.getTransactionInfo());
        paymentdto.setDateCreate(payment.getTimestamp().toString());
        if (payment.getStatus() == 0) {
            paymentdto.setStatus("Chưa thanh toán");
        } else if (payment.getStatus() == 1) {
            paymentdto.setStatus("Giao dịch thành công");
        } else if (payment.getStatus() == 2) {
            paymentdto.setStatus("Giao dịch thất bại");
        } else if (payment.getStatus() == 3) {
            paymentdto.setStatus("Hoàn trả thành công");
        } else if (payment.getStatus() == 4) {
            paymentdto.setStatus("Hoàn trả thất bại");
        }
        paymentdto.setIpAddr(payment.getIpAddr());
        return paymentdto;
    }

    public static List<PaymentDto> getListofPaymentDto(List<Payment> payments) {
        return payments.stream().map(Mapper::getPaymentDto).toList();
    }

    public static String transDate(String inputDate) {
        if (inputDate.length() > 14) {
            return inputDate.replace(":", "")
                    .replace(" ", "")
                    .replace("-", "");
        } else {
            try {
                // Định dạng đầu vào
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                // Parse chuỗi thành Date
                Date date = inputFormat.parse(inputDate);

                // Định dạng đầu ra
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                // Format Date thành chuỗi mới
                return outputFormat.format(date);
            } catch (Exception e) {
                System.err.println("Lỗi khi chuyển đổi: " + e.getMessage());
            }

        }
        return null;
    }
}
