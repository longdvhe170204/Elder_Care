package com.eldercare.backend.belongings.internal.ending.domain;

import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatusView;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import java.util.List;
import java.util.Set;

/**
 * Mục "xử lý đồ gửi" của danh sách việc sau qua đời ({@code MucDoGuiSauQuaDoi}; 013 T024, FR-026, FR-026a, research
 * K-08, Q-152): Không áp dụng khi tại lúc qua đời không có đồ chưa trả; Hoàn thành khi mọi đồ ở Đã trả, Thất lạc hoặc Đã
 * xử lý. Đồ Tìm thấy sau khi mục đã Hoàn thành theo dõi riêng bằng nhắc nên không tính, mục không mở lại.
 */
public final class PostDeathBelongings {

    private static final Set<BelongingStatus> UNRETURNED =
            Set.of(BelongingStatus.HELD, BelongingStatus.IN_USE, BelongingStatus.DAMAGED);
    private static final Set<BelongingStatus> SETTLED =
            Set.of(BelongingStatus.RETURNED, BelongingStatus.LOST, BelongingStatus.DISPOSED);

    private PostDeathBelongings() {}

    /**
     * Một đồ chưa bị loại: trạng thái theo bản ghi hiện hành không muộn hơn lúc qua đời ({@code null} khi chưa có bản
     * ghi nào, ví dụ đồ tách sau đó), trạng thái hiện tại, và có thuộc dạng theo dõi riêng sau Tìm thấy (FR-026a) không.
     */
    public record Entry(BelongingStatus atDeath, BelongingStatus now, boolean trackedSeparately) {}

    public static PostDeathItemStatusView evaluate(List<Entry> entries) {
        long unreturnedAtDeath = entries.stream()
                .filter(e -> e.atDeath() != null && UNRETURNED.contains(e.atDeath()))
                .count();
        if (unreturnedAtDeath == 0) {
            return new PostDeathItemStatusView(
                    PostDeathItemStatus.NOT_APPLICABLE, "Không có đồ gửi chưa trả tại thời điểm qua đời (FR-026)");
        }
        long open = entries.stream()
                .filter(e -> !e.trackedSeparately() && !SETTLED.contains(e.now()))
                .count();
        return open == 0
                ? new PostDeathItemStatusView(
                        PostDeathItemStatus.COMPLETED, "Mọi đồ gửi đã trả, thất lạc hoặc đã xử lý (FR-026)")
                : new PostDeathItemStatusView(
                        PostDeathItemStatus.NOT_COMPLETED, "Còn " + open + " đồ gửi chưa trả hoặc chưa xử lý (FR-026)");
    }
}
