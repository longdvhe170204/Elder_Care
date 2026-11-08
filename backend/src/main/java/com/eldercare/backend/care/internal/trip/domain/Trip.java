package com.eldercare.backend.care.internal.trip.domain;
/** Trạng thái riêng của chuyến, không dùng trạng thái điểm danh buổi trong viện. */
public record Trip(long id, long activityId, Long floorAreaId, TripPlan plan, String status, int version) {}
