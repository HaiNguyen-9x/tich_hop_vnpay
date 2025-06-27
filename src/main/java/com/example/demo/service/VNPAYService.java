package com.example.demo.service;

import com.example.demo.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public interface VNPAYService {
    String createOrder(HttpServletRequest request, RequestDto requestDto);

    ResponseDto returnUrl(HttpServletRequest request);

    String returnIPN(HttpServletRequest request);

    PagiPaymentDto getPayments(int pageNo, String sortBy, String sortDir, String pageSearch);

    ResponseDto queryPayment(HttpServletRequest request, HttpServletResponse response, Long paymentId);

    RefundResponseDto refund(HttpServletRequest request, HttpServletResponse response, RefundDto refundDto);
}