package com.cambae.backend.m02.internal.change.service;

import com.cambae.backend.m02.internal.change.domain.ChangeType;
import com.cambae.backend.m02.internal.change.domain.ResidencyChangeContent;
import com.cambae.backend.m02.internal.change.domain.ResidencyChangeContent.ServiceChange;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * "Thay đổi nhỏ" (FR-043, Q-338; research T-06), hàm thuần: yêu cầu chỉ thuộc loại đổi dịch vụ, mọi dịch vụ được thêm hoặc
 * bớt đều thuộc gói của hợp đồng (dòng thêm mang dấu thuộc gói; dòng bớt có trong hợp đồng hiện hành và thuộc gói) và ảnh
 * hưởng chi phí bằng 0 ở mọi khoản. Tính lại ở mỗi lần đọc và lúc duyệt trên hợp đồng hiện hành, không lưu.
 */
public final class IsMinorChange {

    private IsMinorChange() {}

    /**
     * @param costImpact mảng {@code {item, previousUnitPrice, newUnitPrice}} do hệ thống điền lúc lập yêu cầu
     * @param currentContent nội dung phiên bản mới nhất của hợp đồng hiện hành (có {@code services})
     */
    public static boolean test(ResidencyChangeContent change, JsonNode costImpact, JsonNode currentContent) {
        if (!change.changeTypes().equals(Set.of(ChangeType.SERVICE)) || change.changesDeposit()) {
            return false;
        }
        if (change.services().isEmpty()) {
            return false;
        }
        for (ServiceChange line : change.services()) {
            boolean inPackage = switch (line.action()) {
                case ADD -> line.inPackage();
                case REMOVE ->
                    currentLine(currentContent, line.serviceId())
                            .map(current -> current.path("inPackage").asBoolean(false))
                            .orElse(false);
            };
            if (!inPackage) {
                return false;
            }
        }
        return noCostImpact(costImpact);
    }

    private static java.util.Optional<JsonNode> currentLine(JsonNode currentContent, long serviceId) {
        for (JsonNode line : currentContent.path("services")) {
            if (line.path("serviceId").asLong() == serviceId) {
                return java.util.Optional.of(line);
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean noCostImpact(JsonNode costImpact) {
        if (costImpact == null || !costImpact.isArray()) {
            return true;
        }
        for (JsonNode item : costImpact) {
            if (item.path("previousUnitPrice")
                            .decimalValue()
                            .compareTo(item.path("newUnitPrice").decimalValue())
                    != 0) {
                return false;
            }
        }
        return true;
    }
}
