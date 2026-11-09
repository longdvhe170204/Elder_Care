package com.eldercare.backend.belongings.internal.belonging.domain;

/**
 * Dữ kiện riêng của bản ghi {@code RETURN} hoặc {@code RETURN_BLOCKED} (data-model mục 5): giấy tờ xuất trình, căn cứ,
 * chữ ký, xác nhận người nhận khác dùng; {@code blockReason} chỉ có ở lần chặn. Người nhận ghi ở cột người nhận chung
 * của bản ghi bàn giao.
 */
public record ReturnFacts(
        String documentType,
        String documentNumber,
        ReturnBasis basis,
        VerificationResult verification,
        Long signatureFileId,
        String noSignatureReason,
        String conditionOpinion,
        Long recipientConfirmationId,
        ReturnBlockReason blockReason) {}
