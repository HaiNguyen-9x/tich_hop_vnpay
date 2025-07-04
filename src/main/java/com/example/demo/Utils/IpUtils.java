package com.example.demo.Utils;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtils {
    private IpUtils() {}

    public static String generateIp(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }
}
