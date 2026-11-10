package com.eldercare.backend.belongings.web.request;

import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommand;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryCheck;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryCompletion;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Thân yêu cầu của phiếu kiểm kê (api-013 {@code InventoryCheckResult}). */
public final class InventoryCheckRequests {

    private InventoryCheckRequests() {}

    public record Create(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long storageLocationId) {}

    @Schema(
            name = "InventoryItemCondition",
            requiredProperties = {"levelId"})
    public record ItemCondition(Long levelId, String description) {}

    @Schema(
            name = "InventoryCheckResult",
            requiredProperties = {"belongingId", "result"})
    public record Line(
            Long belongingId,
            String result,
            ItemCondition newCondition,
            Boolean recordDamage,
            List<Long> photoFileIds,
            HandoverRequest.IncidentOptionRequest incident) {}

    public record UnknownOwnerItem(String description, List<Long> photoFileIds) {}

    /** Mục {@code found}: đồ Thất lạc tìm thấy tại vị trí và ảnh (013-2 c). */
    public record Found(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Long belongingId,

            List<Long> photoFileIds) {}

    public record Result(List<Line> lines, List<Found> found, List<UnknownOwnerItem> unknownOwnerItems, String reason) {

        public InventoryCompletion toCompletion() {
            List<InventoryCompletion.LineResult> mapped = new java.util.ArrayList<>();
            for (Line l : lines == null ? List.<Line>of() : lines) {
                if (l == null || l.belongingId() == null) {
                    throw new IllegalArgumentException("Thiếu mã đồ");
                }
                HandoverRequest.IncidentOptionRequest i = l.incident();
                mapped.add(new InventoryCompletion.LineResult(
                        l.belongingId(),
                        l.result(),
                        l.newCondition() == null ? null : l.newCondition().levelId(),
                        l.newCondition() == null ? null : l.newCondition().description(),
                        Boolean.TRUE.equals(l.recordDamage()),
                        l.photoFileIds(),
                        i == null
                                ? null
                                : new HandoverCommand.IncidentChoice(
                                        i.method(), i.incidentId(), i.level(), i.description(), i.place())));
            }
            List<InventoryCheck.UnknownOwnerItem> unknown = new java.util.ArrayList<>();
            for (UnknownOwnerItem u : unknownOwnerItems == null ? List.<UnknownOwnerItem>of() : unknownOwnerItems) {
                unknown.add(new InventoryCheck.UnknownOwnerItem(u.description(), u.photoFileIds()));
            }
            List<InventoryCompletion.FoundItem> foundItems = new java.util.ArrayList<>();
            for (Found f : found == null ? List.<Found>of() : found) {
                if (f == null || f.belongingId() == null) {
                    throw new IllegalArgumentException("Thiếu mã đồ Tìm thấy");
                }
                foundItems.add(new InventoryCompletion.FoundItem(f.belongingId(), f.photoFileIds()));
            }
            return new InventoryCompletion(mapped, foundItems, unknown);
        }
    }
}
