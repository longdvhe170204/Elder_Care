package com.cambae.backend.m07.web.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Thân yêu cầu chứa lý do (FR-002, api-006.yaml).
 */
public record ReasonBody(
        @JsonProperty("lyDo")
        @JsonAlias({"reason", "lyDo"})
        String reason) {}
