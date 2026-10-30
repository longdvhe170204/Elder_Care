package com.eldercare.backend.administration.account.api;

import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import java.util.List;

/** Người có quyền duyệt một loại yêu cầu trên đối tượng (FR-031a, BR-M15-09). 000 khai báo trước (000 T025, contracts/module-interfaces.md mục 2); feature 002 cài bản thật. */
public interface ApproverApi {

    boolean hasAnyApprover(PermissionKey key, Target target);

    /** Id tài khoản đang hoạt động có quyền duyệt, để gửi nhắc. */
    List<Long> approvers(PermissionKey key, Target target);
}
