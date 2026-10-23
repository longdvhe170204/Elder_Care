package com.cambae.backend.m03.api.dto;

/** Kiểu trao đổi LocationView, hợp đồng 003 mục 1.2–1.9 (T003). */
public record LocationView(
        Long areaId,
        String areaName,
        Long floorId,
        String floorName,
        Long roomId,
        String roomName,
        long bedId,
        String bedCode) {}
