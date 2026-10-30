package com.eldercare.backend.notification.internal.domain;

/** Người cao tuổi và tầng/khu vực của yêu cầu mà một bản ghi M13 thuộc về (phạm vi, đính chính). */
public record ResidentFloor(Long residentId, Long floorAreaId) {}
