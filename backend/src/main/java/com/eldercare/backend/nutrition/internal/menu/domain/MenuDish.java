package com.eldercare.backend.nutrition.internal.menu.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

/**
 * Một món của thực đơn chung trong cột JSON của {@code menu} (FR-024, R-15; Q-445): ngày, bữa, món; thứ tự trong danh sách
 * là thứ tự món trong bữa. Bỏ qua {@code dietId}, {@code substituteDishIds} của nội dung có trước Q-445.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MenuDish(LocalDate date, long mealId, long dishId) {}
