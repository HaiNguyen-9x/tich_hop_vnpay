package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.VNPAYService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Log4j2
@Controller
@RequestMapping("/api/vnp")
public class VNPAYController {
    private VNPAYService vnpayService;

    public VNPAYController(VNPAYService vnpayService) {
        this.vnpayService = vnpayService;
    }

    //http://localhost:8080/api/vnp/order
    @GetMapping("/createOrder")
    public String order(Model model) {
        RequestDto requestDto = new RequestDto();
        requestDto.setOrderInfo("Thanh toan hoa don.");
        model.addAttribute("requestDto", requestDto);

        List<String> listOrderType = Arrays.asList("Nạp tiền điện thoại", "Thanh toán hóa đơn", "Thời trang", "Merchant bán vé");
        model.addAttribute("listOrderType", listOrderType);
        return "order";
    }

    @PostMapping("/order")
    public void createOrder(HttpServletRequest request, HttpServletResponse response,
                            @Valid @ModelAttribute("requestDto") RequestDto requestDto) throws IOException {
        String urlDirect = vnpayService.createOrder(request, requestDto);
        response.sendRedirect(urlDirect);
    }

    @GetMapping("/ipn")
    public ResponseEntity<String> returnIpn(HttpServletRequest request) {
        return ResponseEntity.ok(vnpayService.returnIPN(request));
    }

    @GetMapping("/return")
    public String getReturn(HttpServletRequest request, Model model) {
        ResponseDto responseDto = vnpayService.returnUrl(request);
        if (responseDto != null) model.addAttribute("responseDto", responseDto);
        else model.addAttribute("responseDto", new ResponseDto());
        return "return";
    }

    //http://localhost:8080/api/vnp/payments
    @GetMapping("/payments")
    public String getPayments(@RequestParam(name = "pageNo", defaultValue = "1", required = false) int pageNo,
                              @RequestParam(name = "pageBy", defaultValue = "id", required = false) String pageBy,
                              @RequestParam(name = "pageDir", defaultValue = "desc", required = false) String pageDir,
                              @RequestParam(name = "pageSearch", defaultValue = "", required = false) String pageSearch,
                              Model model) {
        PagiPaymentDto payments = vnpayService.getPayments(pageNo, pageBy, pageDir, pageSearch);
        model.addAttribute("payments", payments.getPayments());
        model.addAttribute("sort", payments.getSort());
        return "payments";
    }

    @GetMapping("/createQuery")
    public String createQuery(Model model) {
        QueryDto queryDto = new QueryDto();
        model.addAttribute("queryDto", queryDto);
        return "queryId";
    }

    //http://localhost:8080/api/vnp/query?paymentId=
    @GetMapping("/query")
    public String queryDr(HttpServletRequest request, HttpServletResponse response,
                          @ModelAttribute("queryDto") QueryDto queryDto, Model model) {
        ResponseDto responseDto = vnpayService.queryPayment(request, response, queryDto.getPaymentId());
        model.addAttribute("responseDto", responseDto);
        return "return";
    }

    //http://localhost:8080/api/vnp/createRefund
    @GetMapping("/createRefund")
    public String refund(Model model) {
        RefundDto refundDto = new RefundDto();
        model.addAttribute("refundDto", refundDto);
        return "refund";
    }

    @GetMapping("/refund")
    public String refund(HttpServletRequest request, HttpServletResponse response,
                       @ModelAttribute("refundDto") RefundDto refundDto, Model model) {
        RefundResponseDto refundResponseDto = vnpayService.refund(request, response, refundDto);
        model.addAttribute("refundResponseDto", refundResponseDto);
        return "refundResponse";
    }
}