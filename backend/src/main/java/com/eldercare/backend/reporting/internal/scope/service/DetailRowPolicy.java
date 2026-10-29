package com.eldercare.backend.reporting.internal.scope.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.reporting.internal.metric.domain.DetailRow;
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
 * Dòng rút gọn xét nhóm thông tin theo vai trò của người xem, không xét phạm vi hiện hành (FR-012: "chỉ khi vai trò của
 * người xem được xem nhóm trường đó"; {@code FieldVisibilityApi.canViewByRole}).
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
            Visibility level = reduced
                    ? fields.canViewByRole(user, row.resultGroup())
                    : fields.canView(
                            user, row.resultGroup(), new Target(null, null, row.residentId(), row.floorAreaId()));
            if (level != Visibility.FULL) {
                result = result.withResult(row.restrictedResult());
            }
        }
        return result;
    }
}
