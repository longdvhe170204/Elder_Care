package com.eldercare.backend.resident.internal.health.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

/**
 * Nội dung JSON của mục danh mục M01 ở {@code catalog_item}: {@code legacy} ghi bảng, id cũ của dòng chép từ bảng riêng
 * trước khi gộp (001 T084, {@code V001_95}); mục tạo mới sau đó để {@code null}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogOrigin(Map<String, Object> legacy) {}
