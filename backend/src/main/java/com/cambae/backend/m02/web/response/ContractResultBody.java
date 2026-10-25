package com.cambae.backend.m02.web.response;

import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.m02.internal.contract.service.ContractService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Hợp đồng kèm cảnh báo FR-025 không chặn (contracts/api-004.yaml, schema {@code ContractResult}). */
@Schema(name = "ContractResult")
public record ContractResultBody(ContractBody contract, List<Warning> warnings) {

    public static ContractResultBody of(ContractService.Result result) {
        return new ContractResultBody(ContractBody.of(result.contract()), result.warnings());
    }
}
