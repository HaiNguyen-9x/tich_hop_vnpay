package com.example.demo.service.serviceImpl;

import com.example.demo.dto.*;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Refund;
import com.example.demo.exception.DoesNotMatchValue;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RefundRepository;
import com.example.demo.service.VNPAYService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Log4j2
@Service
public class VNPAYServiceImpl implements VNPAYService {

    @Value("${vnp.HashSecret}")
    private String vnpHashSecret;

    @Value("${vnp.Url}")
    private String vnpUrl;

    @Value("${vnp.QueryRefundUrl}")
    private String vnpQueryRefundUrl;

    private final PaymentRepository paymentRepository;
    private RefundRepository refundRepository;
    private InputToVnpay inputToVnpay;
    private WebClient webClient;

    public VNPAYServiceImpl(PaymentRepository paymentRepository,
                            RefundRepository refundRepository,
                            InputToVnpay inputToVnpay,
                            WebClient webClient) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.inputToVnpay = inputToVnpay;
        this.webClient = webClient;
    }

    public String createOrder(HttpServletRequest request, RequestDto requestDto) {
        Map<String, String> order = inputToVnpay.generateOrderInput(request, paymentRepository, requestDto);

        String orderUrl = generateOrderUrl(order);
        String vnp_SecureHash = generateSecureHash(order);
        orderUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        log.info("Create an order");

        Payment payment = new Payment();
        payment.setAmount(requestDto.getAmount());
        payment.setTransactionInfo(order.get("vnp_OrderInfo"));
        Calendar createDate = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        payment.setTimestamp(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(createDate.getTime()));
        payment.setStatus(0);
        payment.setIpAddr(order.get("vnp_IpAddr"));
        paymentRepository.save(payment);

        return vnpUrl + "?" + orderUrl;
    }

    public ResponseDto returnUrl(HttpServletRequest request) {
        log.info("Return order information");

        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = (String) params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                fields.put(fieldName, fieldValue);
            }
        }

        String secureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String returnHash = generateSecureHash(fields);

        ResponseDto responseDto = new ResponseDto();
        responseDto.setMerchantName("Demo");
        responseDto.setTransactionInfo(request.getParameter("vnp_OrderInfo"));
        String amount = request.getParameter("vnp_Amount");
        responseDto.setAmount(amount.substring(0, amount.length() - 2));
        responseDto.setTransactionResponseCode(request.getParameter("vnp_ResponseCode"));
        responseDto.setTransactionNumber(request.getParameter("vnp_TransactionNo"));
        responseDto.setBank(request.getParameter("vnp_BankCode"));
        responseDto.setCreateDate(Mapper.transDate(request.getParameter("vnp_PayDate")));

        if (!returnHash.equals(secureHash)) {
            log.warn("Secure hash khong trung khop.");
            throw new DoesNotMatchValue("Chu ki khong hop le.");
        } else {
            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                responseDto.setStatus("Giao dịch thành công");
                log.info("Giao dich thanh cong.");
            }
            else {
                responseDto.setStatus("Giao dịch thất bại");
                log.info("Giao dich that bai");
            }
        }
        return responseDto;
    }

    public String returnIPN(HttpServletRequest request) {
        log.info("Update state in database");
        try
        {

        /*  IPN URL: Record payment results from VNPAY
        Implementation steps:
        Check checksum
        Find transactions (vnp_TxnRef) in the database (checkOrderId)
        Check the payment status of transactions before updating (checkOrderStatus)
        Check the amount (vnp_Amount) of transactions before updating (checkAmount)
        Update results to Database
        Return recorded results to VNPAY
        */

            // ex:  	PaymentStatus = 0; pending
            //              PaymentStatus = 1; success
            //              PaymentStatus = 2; Fail

            //Begin process return from VNPAY
            Map<String, String> fields = new HashMap<>();
            for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
                String fieldName = (String) params.nextElement();
                String fieldValue = request.getParameter(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    fields.put(fieldName, fieldValue);
                }
            }

            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");
            String txnRef = request.getParameter("vnp_TxnRef");
            String transactionNo = request.getParameter("vnp_TransactionNo");

            // Check checksum
            String signValue = generateSecureHash(fields);
            if (signValue.equals(vnp_SecureHash))
            {
                if(paymentRepository.existsById(Long.parseLong(txnRef)))
                {
                    Payment payment = paymentRepository.findById(Long.parseLong(txnRef))
                            .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: ", txnRef));
                    paymentRepository.save(payment);

                    if(payment.getAmount().equals(Long.parseLong(request.getParameter("vnp_Amount"))/100))
                    {

                        if (payment.getStatus() == 0)
                        {
                            if ("00".equals(request.getParameter("vnp_ResponseCode")))
                            {
                                payment.setStatus(1);
                                payment.setTransactionNo(transactionNo);
                                paymentRepository.save(payment);
                            }
                            else
                            {
                                payment.setStatus(2);
                                payment.setTransactionNo(transactionNo);
                                paymentRepository.save(payment);
                            }
                            log.info("ResponseCode: 00. Confirm success.");
                            return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
                        }
                        else
                        {
                            log.warn("ResponseCode: 02. TxnRef: {} Order already confirmed.", txnRef);
                            return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
                        }
                    }
                    else
                    {
                        log.warn("ResponseCode: 04. TmnRef: {} Invalid Amount.", txnRef);
                        return "{\"RspCode\":\"04\",\"Message\":\"Invalid Amount\"}";
                    }
                }
                else
                {
                    log.warn("ResponseCode: 01. TmnRef: {} Order not Found.", txnRef);
                    return "{\"RspCode\":\"01\",\"Message\":\"Order not Found\"}";
                }
            }
            else
            {
                log.warn("ResponseCode: 97. Invalid Checksum.");
                return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
            }
        }
        catch(Exception e)
        {
            log.warn("ResponseCode: 99. Unknown error.");
            return  "{\"RspCode\":\"99\",\"Message\":\"Unknown error\"}";
        }
    }

    @Override
    public PagiPaymentDto getPayments(int pageNo, String sortBy, String sortDir, String pageSearch) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo - 1, 10, sort);

        Page<Payment> listOfPayments = paymentRepository.searchByIdLike(pageSearch, pageable);

        PagiPaymentDto pagiPaymentDto = new PagiPaymentDto();
        pagiPaymentDto.setPayments(listOfPayments
                .stream()
                .map(Mapper::getPaymentDto).toList());
        com.example.demo.dto.Sort sortData = new com.example.demo.dto.Sort(
                pageNo, 10, sortBy, sortDir, pageSearch, 0
        );
        sortData.setTotalPages(listOfPayments.getTotalPages());
        pagiPaymentDto.setSort(sortData);

        log.info("Show payments in page: {}, sort by: {}, sort direction: {}, search by: \"{}\"",
                pageNo, sortBy, sortDir, pageSearch);

        return pagiPaymentDto;
    }

    @Override
    public ResponseDto queryPayment(HttpServletRequest request, HttpServletResponse resp, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Can not find payment with id: ", paymentId));

        Map<String, String> input = inputToVnpay.generateQueryInput(request, payment);

        log.info("Sending query request.");
        log.info("Query Data: {}", input);
        try {
            Map<String, String> dataResponse = webClient.post()
                    .uri(vnpQueryRefundUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(input)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            assert dataResponse != null;

            if (!dataResponse.containsKey("vnp_PromotionCode")) dataResponse.put("vnp_PromotionCode", "");
            if (!dataResponse.containsKey("vnp_PromotionAmount")) dataResponse.put("vnp_PromotionAmount", "");

            log.info("Refund response: {}", dataResponse);

            if (!dataResponse.get("vnp_ResponseCode").equals("00")) {
                log.info("Response code is not equal with 00.");
                throw new DoesNotMatchValue("ResponseCode: " + dataResponse.get("vnp_ResponseCode"));
            }

            String secureHash =  generateQuerySecureHash(dataResponse);

            if (secureHash.equals(dataResponse.get("vnp_SecureHash"))) {
                updatePayment(dataResponse, paymentId, input.get("vnp_IpAddr"));
                return new ResponseDto(dataResponse);
            } else {
                throw new DoesNotMatchValue("Chu ki khong hop le.");
            }

        } catch (Exception e) {
            log.warn("Error during refund request: ", e);
        }
        return null;
    }

    @Override
    public RefundResponseDto refund(HttpServletRequest request, HttpServletResponse resp, RefundDto refundDto) {
        Payment payment = paymentRepository.findById(refundDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Can not find payment with id: ", refundDto.getId()));

        Map<String, String> input = inputToVnpay.generateRefundInput(request, refundDto, payment);

        log.info("Sending refund request.");
        log.info("Refund Data : {}", input);

        try {
            Map<String, String> dataResponse = webClient.post()
                    .uri(vnpQueryRefundUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(input)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, String>>() {})
                    .block();

            log.info("Refund response: {}", dataResponse);

            assert dataResponse != null;

            Refund refund = new Refund();
            refund.setRefundType(refundDto.getTransactionType());
            refund.setAmount(refundDto.getAmount());
            refund.setTransactionInfo(input.get("vnp_OrderInfo"));
            refund.setTimestamp(Mapper.transDate(input.get("vnp_CreateDate")));
            refund.setResponseCode(dataResponse.get("vnp_ResponseCode"));
            refund.setPayment(payment);

            // Kiểm tra mã phản hồi
            if (!"00".equals(dataResponse.get("vnp_ResponseCode"))) {
                refund.setStatus(2);
                refundRepository.save(refund);
                log.info("Response code is not equal to 00.");
                throw new DoesNotMatchValue("ResponseCode: " + dataResponse.get("vnp_ResponseCode"));
            }

            // Kiểm tra secure hash
            String secureHash = generateRefundSecureHash(dataResponse);
            if (!secureHash.equals(dataResponse.get("vnp_SecureHash"))) {
                throw new DoesNotMatchValue("Secure hash mismatch.");
            }

            RefundResponseDto refundResponseDto = new RefundResponseDto(dataResponse, input, payment);

            refund.setStatus(1);  // Thành công
            refundRepository.save(refund);

            return refundResponseDto;

        } catch (Exception e) {
            log.warn("Error during refund request: ", e);
        }

        return null;
    }


    private String generateOrderUrl (Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append("=");
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append("&");
                }
            }
        }
        return query.toString();
    }

    private String generateSecureHash(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append("=");
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append("&");
                }
            }
        }
        return Hex.encodeHexString(HmacUtils.hmacSha512(vnpHashSecret, hashData.toString()));
    }

    private String generateQuerySecureHash(Map<String, String> fields) {
        String hashData = fields.get("vnp_ResponseId") + "|" + fields.get("vnp_Command") + "|"
                + fields.get("vnp_ResponseCode") + "|" + fields.get("vnp_Message") + "|" + fields.get("vnp_TmnCode")
                + "|" + fields.get("vnp_TxnRef") + "|" + fields.get("vnp_Amount") + "|" + fields.get("vnp_BankCode")
                + "|" + fields.get("vnp_PayDate") + "|" + fields.get("vnp_TransactionNo") + "|"
                + fields.get("vnp_TransactionType") + "|" + fields.get("vnp_TransactionStatus") + "|"
                + fields.get("vnp_OrderInfo") + "|" + fields.get("vnp_PromotionCode") + "|" + fields.get("vnp_PromotionAmount");
        return Hex.encodeHexString(HmacUtils.hmacSha512(vnpHashSecret, hashData));
    }

    private String generateRefundSecureHash(Map<String, String> fields) {
        String hashData = fields.get("vnp_ResponseId") + "|" + fields.get("vnp_Command") + "|"
                + fields.get("vnp_ResponseCode") + "|" + fields.get("vnp_Message") + "|" + fields.get("vnp_TmnCode")
                + "|" + fields.get("vnp_TxnRef") + "|" + fields.get("vnp_Amount") + "|" + fields.get("vnp_BankCode")
                + "|" + fields.get("vnp_PayDate") + "|" + fields.get("vnp_TransactionNo") + "|"
                + fields.get("vnp_TransactionType") + "|" + fields.get("vnp_TransactionStatus") + "|"
                + fields.get("vnp_OrderInfo");
        return Hex.encodeHexString(HmacUtils.hmacSha512(vnpHashSecret, hashData));
    }

    private void updatePayment(Map<String, String> dataResponse, Long paymentId, String ipAdress) {
        Payment payment1 = new Payment();

        payment1.setId(paymentId);
        payment1.setAmount(Long.parseLong(dataResponse.get("vnp_Amount"))/100);
        payment1.setTransactionInfo(dataResponse.get("vnp_OrderInfo"));
        payment1.setTimestamp(Mapper.transDate(dataResponse.get("vnp_PayDate")));
        payment1.setStatus(dataResponse.get("vnp_TransactionStatus").equals("00") ? 1 : 2);
        payment1.setIpAddr(ipAdress);
        payment1.setTransactionNo(dataResponse.get("vnp_TransactionNo"));

        paymentRepository.save(payment1);
    }
}