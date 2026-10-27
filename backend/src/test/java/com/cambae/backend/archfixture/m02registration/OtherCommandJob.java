package com.cambae.backend.archfixture.m02registration;

/** Lớp mẫu: job gọi một lệnh không lập khai báo; ngoài phạm vi luật T001 (chỉ cấm lập khai báo), không bị bắt. */
public class OtherCommandJob {

    private final RegistrationService service;

    public OtherCommandJob(RegistrationService service) {
        this.service = service;
    }

    public void run() {
        service.recordAccepted(1L);
    }
}
