package com.eldercare.backend.roombedallocation.web.response;

import java.util.List;

/** Có cả giường không có người trong thời điểm hoặc ngày tra cứu. */
public record BedLocationHistoryRow(LocationResponse location, List<BedResidentHistoryRow> residents) {}
