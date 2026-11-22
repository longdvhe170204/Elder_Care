package com.eldercare.backend.administration.approval.api.dto;

/** Lệnh của hợp đồng {@code allowedCommands} (glossary 6.2: tên lệnh trong đường dẫn). */
public final class ApprovalCommands {

    public static final String EDIT_CONTENT = "edit-content";
    public static final String SUBMIT = "submit";
    public static final String APPROVE = "approve";
    public static final String REJECT = "reject";
    public static final String CANCEL = "cancel";
    /** Trả về nháp (000 FR-041a (1); Q-419). */
    public static final String RETURN_TO_DRAFT = "return-to-draft";

    private ApprovalCommands() {}
}
