package com.cambae.backend.m15.approval.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApproveBody(@Schema(maxLength = 2000) String comment) {}
