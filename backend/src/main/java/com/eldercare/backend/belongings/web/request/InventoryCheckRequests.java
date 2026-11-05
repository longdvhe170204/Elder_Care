package com.eldercare.backend.belongings.web.request;

import com.eldercare.backend.belongings.internal.belonging.domain.HandoverCommand;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryCheck;
import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryCompletion;
import java.util.List;

/** Thân yêu cầu của phiếu kiểm kê (api-013 {@code InventoryCheckResult}). */
public final class InventoryCheckRequests {

    private InventoryCheckRequests() {}

    public record Create(Long storageLocationId) {}

    public record ItemCondition(Long levelId, String description) {}

    public record Line(
            Long belongingId,
            String result,
            ItemCondition newCondition,
            Boolean recordDamage,
            List<Long> photoFileIds,
            HandoverRequest.IncidentOptionRequest incident) {}

    public record UnknownOwnerItem(String description, List<Long> photoFileIds) {}

    public record Result(List<Line> lines, List<Long> found, List<UnknownOwnerItem> unknownOwnerItems, String reason) {

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
            return new InventoryCompletion(mapped, found, unknown);
        }
    }
}
