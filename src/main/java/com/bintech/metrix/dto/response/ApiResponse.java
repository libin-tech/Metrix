package com.bintech.metrix.dto.response;

import com.bintech.metrix.constants.ApiConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiConstants.HTTP_STATUS_OK, ApiConstants.DEFAULT_SUCCESS_MSG, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ApiConstants.HTTP_STATUS_OK, message, data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ApiConstants.HTTP_STATUS_INTERNAL_ERROR, message, null, LocalDateTime.now());
    }
}
