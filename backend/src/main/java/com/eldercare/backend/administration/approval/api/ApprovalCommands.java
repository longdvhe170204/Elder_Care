package com.eldercare.backend.administration.approval.api;

/** Lệnh của hợp đồng {@code allowedCommands} (glossary 6.2: tên lệnh trong đường dẫn). */
public final class ApprovalCommands {

    public static final String EDIT_CONTENT = "edit-content";
    public static final String SUBMIT = "submit";
    public static final String APPROVE = "approve";
    public static final String REJECT = "reject";
    public static final String CANCEL = "cancel";

    private ApprovalCommands() {}
}
