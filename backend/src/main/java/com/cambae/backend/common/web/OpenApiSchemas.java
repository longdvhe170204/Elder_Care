package com.cambae.backend.common.web;

import io.swagger.v3.oas.models.media.ObjectSchema;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;

/**
 * Cách springdoc tả kiểu đặc biệt trong OpenAPI sinh từ mã (R-14). {@link JsonNode} của Jackson 3 (nội dung lệnh do
 * module sở hữu kiểm, ví dụ {@code details} của lệnh chuyển trạng thái 001) là object tự do như hợp đồng ghi, không
 * phải POJO.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiSchemas {

    static {
        SpringDocUtils.getConfig().replaceWithSchema(JsonNode.class, new ObjectSchema());
    }
}
