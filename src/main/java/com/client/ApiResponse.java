package com.client;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 接口返回通用格式
 *
 * @param <T>
 */
@Setter
@Getter
@ToString
public class ApiResponse<T> {

    private T data;
    private String code;
    private String message;

    public ApiResponse() {
    }


    public ApiResponse(T data) {
        this.data = data;
        this.code = "0";
    }

    public ApiResponse(String code, T data) {
        this.code = code;
        this.data = data;
    }

    public ApiResponse(String code, String message, T data) {
        this(code, data);
        this.message = message;
    }

    public String toJson() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(this.toString(), e);
        }
    }
}

