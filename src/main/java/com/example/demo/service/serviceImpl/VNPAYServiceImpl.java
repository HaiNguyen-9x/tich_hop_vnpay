package com.example.demo.service.serviceImpl;

import com.example.demo.dto.*;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Refund;
import com.example.demo.exception.DoesNotMatchValue;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.repository.RefundRepository;
import com.example.demo.service.VNPAYService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Log4j2
@Service
public class VNPAYServiceImpl implements VNPAYService {

    public static final String vnp_Version = "2.1.0";
    public static final String vnp_CommandPay = "pay";
    public static final String vnp_CommandQuery = "querydr";
    public static final String vnp_CommandRefund = "refund";
    public static final String vnp_Currency_VND = "VND";
    public static final String vnp_LocaleVN = "vn";

    @Value("${vnp.TmnCode}")
    private String vnpTmnCode;

    @Value("${vnp.HashSecret}")
    private String vnpHashSecret;

    @Value("${vnp.ReturnUrl}")
    private String vnpReturnUrl;

    @Value("${vnp.Url}")
    private String vnpUrl;

    @Value("${vnp.QueryRefundUrl}")
    private String vnpQueryRefundUrl;

    private final PaymentRepository paymentRepository;
    private RefundRepository refundRepository;

    public VNPAYServiceImpl(PaymentRepository paymentRepository, RefundRepository refundRepository) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    public String createOrder(HttpServletRequest request, RequestDto requestDto) {
        Map<String, String> order = generateOrderInput(request, requestDto);

        String orderUrl = generateOrderUrl(order);
        String vnp_SecureHash = generateSecureHash(order);
        orderUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        log.info("Create an order");

        Payment payment = new Payment();
        payment.setTxnRef(order.get("vnp_TxnRef"));
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
                if(paymentRepository.existsByTxnRef(txnRef))
                {
                    Payment payment = paymentRepository.findByTxnRef(txnRef);
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

    public ResponseDto queryPayment(HttpServletRequest request, HttpServletResponse resp, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Can not find payment with id: ", paymentId));

        Map<String, String> input = generateQueryInput(request, payment);

        JsonObject  vnp_Params = new JsonObject();
        input.forEach(vnp_Params::addProperty);

        try {
            URL url = new URL(vnpQueryRefundUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(vnp_Params.toString());
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();
            log.info("Sending query request.");
            log.info("Query Data: {}", vnp_Params);
            

            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 200 && responseCode < 300 ? con.getInputStream() : con.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }
            log.info(response);

            JsonObject responseJson = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (!responseJson.get("vnp_ResponseCode").getAsString().equals("00")) {
                log.info("Response code is not equal with 00.");
                throw new RuntimeException("ResponseCode: " + responseJson.get("vnp_ResponseCode").getAsString());
            }

            Map<String, String> dataResponse = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : responseJson.entrySet()) {
                if (entry.getValue().getAsString() != null)
                    dataResponse.put(entry.getKey(), entry.getValue().getAsString());
                if (!dataResponse.containsKey("vnp_PromotionCode")) dataResponse.put("vnp_PromotionCode", "");
                if (!dataResponse.containsKey("vnp_PromotionAmount")) dataResponse.put("vnp_PromotionAmount", "");
            }
            String secureHash =  generateQuerySecureHash(dataResponse);

            if (secureHash.equals(responseJson.get("vnp_SecureHash").getAsString())) {
                updatePayment(dataResponse, paymentId, input.get("vnp_IpAddr"));
                return generateResponseDto(dataResponse);
            } else {
                throw new DoesNotMatchValue("Chu ki khong hop le.");
            }


        } catch (IOException exception) {
            log.warn("IOException");
        }
        return null;
    }

    @Override
    public RefundResponseDto refund(HttpServletRequest request, HttpServletResponse resp, RefundDto refundDto) {
        Payment payment = paymentRepository.findById(refundDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Can not find payment with id: ", refundDto.getId()));

        Map<String, String> input = generateRefundInput(request, refundDto, payment);

        Refund refund = new Refund();
        refund.setAmount(Long.parseLong(input.get("vnp_Amount").substring(0, input.get("vnp_Amount").length() - 2)));
        refund.setTransactionInfo(input.get("vnp_OrderInfo"));
        refund.setTimestamp(Mapper.transDate(input.get("vnp_CreateDate")));
        refund.setStatus(0);
        refund.setPayment(payment);

        JsonObject  vnp_Params = new JsonObject();
        input.forEach(vnp_Params::addProperty);
        
        try {
            URL url = new URL(vnpQueryRefundUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(vnp_Params.toString());
            wr.flush();
            wr.close();
            int responseCode = con.getResponseCode();
            log.info("Sending refund request.");
            log.info("Refund Data : {}", vnp_Params);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String output;
            StringBuilder response = new StringBuilder();
            while ((output = in.readLine()) != null) {
                response.append(output);
            }
            in.close();
            log.info(response.toString());

            JsonObject responseJson = JsonParser.parseString(response.toString()).getAsJsonObject();
            if (!responseJson.get("vnp_ResponseCode").getAsString().equals("00")) {
                refund.setStatus(2);
                refund.setResponseCode(responseJson.get("vnp_ResponseCode").getAsString());
                refundRepository.save(refund);
                log.info("Response code is not equal with 00.");
                throw new RuntimeException("ResponseCode: " + responseJson.get("vnp_ResponseCode").getAsString());
            }

            Map<String, String> dataResponse = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : responseJson.entrySet()) {
                if (entry.getValue().getAsString() != null)
                    dataResponse.put(entry.getKey(), entry.getValue().getAsString());
            }
            String secureHash =  generateRefundSecureHash(dataResponse);

            if (secureHash.equals(responseJson.get("vnp_SecureHash").getAsString())) {
                RefundResponseDto refundResponseDto = generateRefundResponseDto(dataResponse, input, payment);

                refund.setStatus(dataResponse.get("vnp_ResponseCode").equals("00") ? 1 : 2);
                refund.setResponseCode(dataResponse.get("vnp_ResponseCode"));
                refundRepository.save(refund);

                return refundResponseDto;
            }
        } catch (IOException e) {
            log.warn(e);
        }
        return null;
    }

    private Map<String, String> generateOrderInput (HttpServletRequest request, RequestDto requestDto) {
        String orderType, orderInfo;
        if (requestDto.getOrderType() != null) orderType = requestDto.getOrderType();
        else orderType = "other";
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Random rdn = new Random();
        String txnRef = String.valueOf(rdn.nextInt(Integer.MAX_VALUE));
        if(requestDto.getOrderInfo() != null) orderInfo = requestDto.getOrderInfo();
        else orderInfo = "Thanh toan" + txnRef;
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }

        Map<String, String> order = new HashMap<>();
        order.put("vnp_Version", vnp_Version);
        order.put("vnp_Command", vnp_CommandPay);
        order.put("vnp_TmnCode", vnpTmnCode);
        order.put("vnp_Amount", String.valueOf(requestDto.getAmount() * 100));
        String bank_code = requestDto.getBankCode();
        if (bank_code != null && !bank_code.isEmpty())
            order.put("vnp_BankCode", bank_code);
        order.put("vnp_CreateDate", formatter.format(cld.getTime()));
        order.put("vnp_CurrCode", vnp_Currency_VND);
        order.put("vnp_IpAddr", ipAdress);
        if (requestDto.getLocale() != null)
            order.put("vnp_Locale", requestDto.getLocale());
        else order.put("vnp_Locale", vnp_LocaleVN);
        order.put("vnp_OrderInfo", orderInfo);
        order.put("vnp_OrderType", orderType);
        order.put("vnp_ReturnUrl", vnpReturnUrl);
        cld.add(Calendar.MINUTE, 15);
        order.put("vnp_ExpireDate", formatter.format((cld.getTime())));
        order.put("vnp_TxnRef", txnRef);
        return order;
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

    private Map<String, String> generateQueryInput(HttpServletRequest request, Payment payment) {
        Map<String, String> input = new HashMap<>();

        input.put("vnp_RequestId", payment.getId().toString());
        input.put("vnp_Version", vnp_Version);
        input.put("vnp_Command", vnp_CommandQuery);
        input.put("vnp_TmnCode", vnpTmnCode);
        input.put("vnp_TxnRef", payment.getTxnRef());
        input.put("vnp_OrderInfo", "Truy cuu giao dich TxnRef: " + payment.getTxnRef());
        if (payment.getTransactionNo() != null) input.put("vnp_TransactionNo", payment.getTransactionNo());
        else input.put("vnp_TransactionNo", "");

        input.put("vnp_TransactionDate", Mapper.transDate(payment.getTimestamp()));
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        input.put("vnp_CreateDate", formatter.format(cld.getTime()));
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        input.put("vnp_IpAddr", ipAdress);

        String hash_Data= String.join("|", input.get("vnp_RequestId"), input.get("vnp_Version"),
                input.get("vnp_Command"), input.get("vnp_TmnCode"), input.get("vnp_TxnRef"),
                input.get("vnp_TransactionDate"), input.get("vnp_CreateDate"), input.get("vnp_IpAddr"),
                input.get("vnp_OrderInfo"));

        input.put("vnp_SecureHash", Hex.encodeHexString(HmacUtils.hmacSha512(vnpHashSecret, hash_Data)));

        return input;
    }

    private void updatePayment(Map<String, String> dataResponse, Long paymentId, String ipAdress) {
        Payment payment1 = new Payment();

        payment1.setId(paymentId);
        payment1.setTxnRef(dataResponse.get("vnp_TxnRef"));
        payment1.setAmount(Long.parseLong(dataResponse.get("vnp_Amount"))/100);
        payment1.setTransactionInfo(dataResponse.get("vnp_OrderInfo"));
        payment1.setTimestamp(Mapper.transDate(dataResponse.get("vnp_PayDate")));
        payment1.setStatus(dataResponse.get("vnp_TransactionStatus").equals("00") ? 1 : 2);
        payment1.setIpAddr(ipAdress);
        payment1.setTransactionNo(dataResponse.get("vnp_TransactionNo"));

        paymentRepository.save(payment1);
    }

    private ResponseDto generateResponseDto (Map<String, String> dataResponse) {
        String amount = dataResponse.get("vnp_Amount");
        ResponseDto responseDto = new ResponseDto();
        
        responseDto.setStatus("Truy vấn thành công");
        responseDto.setMerchantName("Demo");
        responseDto.setTransactionResponseCode(dataResponse.get("vnp_ResponseCode"));
        responseDto.setTransactionNumber(dataResponse.get("vnp_TransactionNo"));
        responseDto.setAmount(amount.substring(0, amount.length() - 2));
        responseDto.setTransactionInfo(dataResponse.get("vnp_OrderInfo"));
        responseDto.setBank(dataResponse.get("vnp_BankCode"));
        responseDto.setCreateDate(Mapper.transDate(dataResponse.get("vnp_PayDate")));

        return responseDto;
    }

    private Map<String, String> generateRefundInput(HttpServletRequest request, RefundDto refundDto, Payment payment) {
        Map<String, String> input = new HashMap<>();

        input.put("vnp_RequestId", refundDto.getId().toString());
        input.put("vnp_Version", vnp_Version);
        input.put("vnp_Command", vnp_CommandRefund);
        input.put("vnp_TmnCode", vnpTmnCode);
        input.put("vnp_TransactionType", refundDto.getTransactionType());
        input.put("vnp_TxnRef", payment.getTxnRef());
        if (refundDto.getAmount() != null) input.put("vnp_Amount", refundDto.getAmount().toString().concat("00"));
            else input.put("vnp_Amount", payment.getAmount().toString().concat("00"));
        input.put("vnp_OrderInfo", "Hoan tra don hang " + payment.getTxnRef());
        if (payment.getTransactionNo() != null) input.put("vnp_TransactionNo", payment.getTransactionNo());
        else input.put("vnp_TransactionNo", "");
        input.put("vnp_CreateBy", refundDto.getCreateBy());
        input.put("vnp_TransactionDate", Mapper.transDate(payment.getTimestamp()));
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        input.put("vnp_CreateDate", formatter.format(cld.getTime()));
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        input.put("vnp_IpAddr", ipAdress);

        String hash_Data= String.join("|", input.get("vnp_RequestId"), input.get("vnp_Version"),
                input.get("vnp_Command"), input.get("vnp_TmnCode"), input.get("vnp_TransactionType"),
                input.get("vnp_TxnRef"), input.get("vnp_Amount"), input.get("vnp_TransactionNo"),
                input.get("vnp_TransactionDate"), input.get("vnp_CreateBy"), input.get("vnp_CreateDate"),
                input.get("vnp_IpAddr"), input.get("vnp_OrderInfo"));

        input.put("vnp_SecureHash", Hex.encodeHexString(HmacUtils.hmacSha512(vnpHashSecret, hash_Data)));

        return input;
    }
    
    private RefundResponseDto generateRefundResponseDto(Map<String, String> dataResponse, Map<String, String> input, Payment payment) {
        RefundResponseDto refundResponseDto = new RefundResponseDto();
        String amount = dataResponse.get("vnp_Amount");

        refundResponseDto.setStatus("Yêu cầu thành công");
        refundResponseDto.setMerchantName("Demo");
        refundResponseDto.setCreateBy(input.get("vnp_CreateBy"));
        refundResponseDto.setAmount(String.valueOf(payment.getAmount()));
        refundResponseDto.setTransactionType(input.get("vnp_TransactionType").equals("02") ? "Hoàn trả toàn phần" : "Hoàn trả một phần");
        refundResponseDto.setRefundAmount(amount.substring(0, amount.length() - 2));
        refundResponseDto.setTransactionNumber(dataResponse.get("vnp_TransactionNo"));
        refundResponseDto.setTransactionResponseCode(dataResponse.get("vnp_ResponseCode"));
        refundResponseDto.setTransactionStatus(dataResponse.get("vnp_TransactionStatus"));
        refundResponseDto.setBank(dataResponse.get("vnp_BankCode"));
        refundResponseDto.setCreateDate(Mapper.transDate(dataResponse.get("vnp_PayDate")));

        return refundResponseDto;
    }
}