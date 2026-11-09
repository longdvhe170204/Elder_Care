package com.eldercare.backend.billing.internal.export.domain;

public record CostExportFile(byte[] bytes, String contentType, String filename) {}
