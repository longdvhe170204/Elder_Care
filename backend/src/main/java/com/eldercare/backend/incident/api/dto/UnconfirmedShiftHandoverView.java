package com.eldercare.backend.incident.api.dto;
/** Ca sau đã bắt đầu mà ca trước chưa được xác nhận bàn giao, 007 mục 2.1. */
public record UnconfirmedShiftHandoverView(long previousShiftId, long nextShiftId, long floorAreaId) {}
