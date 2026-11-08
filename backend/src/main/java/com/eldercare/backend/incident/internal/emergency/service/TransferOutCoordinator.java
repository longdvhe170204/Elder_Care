package com.eldercare.backend.incident.internal.emergency.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.emergency.domain.TransferOutCommand;
import com.eldercare.backend.incident.internal.emergency.domain.TransferRefusedException;
import org.springframework.stereotype.Service;

/**
 * Điểm vào lệnh Chuyển viện (BR-M05-14): lệnh chạy trong transaction riêng; 001 từ chối thì lệnh rollback toàn bộ (không
 * đổi trạng thái, không bản tóm tắt), sau đó một transaction mới ghi diễn biến {@code TRANSFER_REFUSED} rồi trả lỗi gốc
 * của 001. Ghi sau khi rollback để không chờ khóa dòng sự cố của lệnh đang chạy.
 */
@Service
public class TransferOutCoordinator {
    private final TransferOutService transfers;

    public TransferOutCoordinator(TransferOutService transfers) {
        this.transfers = transfers;
    }

    public long transfer(long incidentId, TransferOutCommand command, CurrentUser user) {
        try {
            return transfers.transfer(incidentId, command, user);
        } catch (TransferRefusedException refused) {
            transfers.recordRefusal(incidentId, refused.refusal(), user);
            throw refused.refusal();
        }
    }
}
