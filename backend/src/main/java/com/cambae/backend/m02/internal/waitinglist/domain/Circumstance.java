package com.cambae.backend.m02.internal.waitinglist.domain;

/** Một tình huống đặc biệt của hồ sơ chờ; {@code code} là mã trong bảng CFG-M02-08. */
public record Circumstance(String code, CircumstanceStatus status) {}
