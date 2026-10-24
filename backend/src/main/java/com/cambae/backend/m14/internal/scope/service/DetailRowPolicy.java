package com.cambae.backend.m14.internal.scope.service;

import com.cambae.backend.m14.internal.metric.domain.DetailRow;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.dto.Visibility;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giới hạn trường của dòng chi tiết, dùng chung cho màn hình và file xuất (FR-014, FR-054; research O-04):
 *
 * <ul>
 *   <li>người cao tuổi ngoài phạm vi hiện hành: dạng rút gọn, không mở được bản ghi từ danh sách (Q-196);
 *   <li>kết quả thuộc nhóm thông tin người xem không được xem đầy đủ: chỉ còn phần không giới hạn, ví dụ mã vật phẩm
 *       thay tên thuốc (Q-133).
 * </ul>
 *
 * Dòng rút gọn xét nhóm thông tin theo tầng/khu lúc phát sinh của bản ghi, vì người cao tuổi đã ngoài phạm vi hiện hành.
 */
@Service
@Transactional(readOnly = true)
public class DetailRowPolicy {

    private static final String RESIDENT = "M01.RESIDENT";

    private final ScopeApi scopes;
    private final FieldVisibilityApi fields;

    public DetailRowPolicy(ScopeApi scopes, FieldVisibilityApi fields) {
        this.scopes = scopes;
        this.fields = fields;
    }

    public List<DetailRow> apply(CurrentUser user, List<DetailRow> rows) {
        return rows.stream().map(row -> apply(user, row)).toList();
    }

    private DetailRow apply(CurrentUser user, DetailRow row) {
        DetailRow result = row;
        boolean reduced = row.residentId() != null
                && !scopes.inScope(
                        user, new Target(RESIDENT, String.valueOf(row.residentId()), row.residentId(), null));
        if (reduced) {
            result = result.asReduced();
        }
        if (row.resultGroup() != null) {
            Target target = reduced
                    ? new Target(null, null, null, row.floorAreaId())
                    : new Target(null, null, row.residentId(), row.floorAreaId());
            if (fields.canView(user, row.resultGroup(), target) != Visibility.FULL) {
                result = result.withResult(row.restrictedResult());
            }
        }
        return result;
    }
}
