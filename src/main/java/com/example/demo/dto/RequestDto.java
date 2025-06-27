package com.example.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RequestDto {
    @Min(value = 5000, message = "So tien can lon hon 5000.")
    @Max(value = 1000000000, message = "So tien can nho hon 1 ty.")
    @NotNull(message = "Khong duoc de tien trong.")
    private Long amount;
    @Pattern(regexp = "[a-zA-Z]{2}", message = "Local chua dung 2 ki tu.")
    private String locale;
    @Size(min = 1, max = 255, message = "Noi dung thanh toan khong thuoc gioi han cho phep.")
    private String orderInfo;
    private String bankCode;
    private String orderType;
}
