package com.cambae.backend.m02.internal.absence.domain;

import java.time.LocalDate;

/**
 * Ngày vắng đầu tiên mà dòng chính sách áp dụng có giữ giường "Không" hoặc "Quản lý quyết định" (FR-055): ngưỡng giữ giường.
 * {@code policyLine} là dòng đã áp (JSON văn bản) để báo Quản lý viện.
 */
public record ThresholdDay(LocalDate date, int sequenceNo, String policyLine) {}
