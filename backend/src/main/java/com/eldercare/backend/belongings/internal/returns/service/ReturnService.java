package com.eldercare.backend.belongings.internal.returns.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.belongings.internal.belonging.service.BelongingHandoverService;
import com.eldercare.backend.belongings.internal.returns.domain.ReturnCommand;
import org.springframework.stereotype.Service;

/**
 * Lệnh Trả ({@code TraService}; 013 T018; FR-016 → FR-019, BR-M12-04, research K-06): Hành chính trả mọi đồ; Điều dưỡng
 * (trong phạm vi) chỉ đồ không có giá trị cho người có quyền nhận hoặc chính người cao tuổi thuộc ngoại lệ (FR-016a), kiểm
 * ở {@code ReturnRecipient}. Lớp này không có transaction: phần có transaction ({@link ReturnTransaction}) commit cả bản
 * ghi lần chặn rồi mới ném lỗi, nên lần chặn còn lại sau khi lệnh bị từ chối (SC-004).
 */
@Service
public class ReturnService {

    private final ReturnTransaction transaction;

    ReturnService(ReturnTransaction transaction) {
        this.transaction = transaction;
    }

    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.UPDATE)
    public BelongingHandoverService.Result giveBack(Target resident, ReturnCommand command) {
        ReturnTransaction.Outcome outcome = transaction.run(resident, command);
        if (outcome.blocked() != null) {
            throw outcome.blocked();
        }
        return outcome.result();
    }
}
