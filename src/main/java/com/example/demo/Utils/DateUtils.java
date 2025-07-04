package com.example.demo.Utils;


import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;

public class DateUtils {
    private DateUtils() {}

    public static String generateDate() {
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format((cld.getTime()));
    }

    public static String add15Minutes(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        // Chuyển chuỗi thành LocalDateTime
        LocalDateTime dateTime = LocalDateTime.parse(date, formatter);

        // Cộng thêm 15 phút
        LocalDateTime newDateTime = dateTime.plusMinutes(15);

        // Chuyển lại thành chuỗi theo định dạng ban đầu
        return newDateTime.format(formatter);
    }
}
