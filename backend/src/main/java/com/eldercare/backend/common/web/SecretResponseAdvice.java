package com.eldercare.backend.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Báo {@link IdempotencyFilter} rằng thân phản hồi là {@link SecretResponse}; bộ lọc chạy ngoài MVC nên không thấy kiểu. */
@ControllerAdvice
class SecretResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof SecretResponse && request instanceof ServletServerHttpRequest servlet) {
            servlet.getServletRequest().setAttribute(IdempotencyFilter.SECRET_RESULT, Boolean.TRUE);
        }
        return body;
    }
}
