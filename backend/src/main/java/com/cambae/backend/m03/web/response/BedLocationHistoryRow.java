package com.cambae.backend.m03.web.response;

import java.util.List;

/** Có cả giường không có người trong thời điểm hoặc ngày tra cứu. */
public record BedLocationHistoryRow(LocationResponse location, List<BedResidentHistoryRow> residents) {}
