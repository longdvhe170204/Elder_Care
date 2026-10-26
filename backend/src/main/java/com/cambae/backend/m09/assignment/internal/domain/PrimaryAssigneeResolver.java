package com.cambae.backend.m09.assignment.internal.domain;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.spi.InvalidPermissionDataException;
import java.time.Instant;
import java.util.*;

/** FR-028: đối tượng hẹp thắng trước, cùng mức thì giới hạn công việc thắng; không phụ thuộc thứ tự đầu vào. */
public final class PrimaryAssigneeResolver {
    private PrimaryAssigneeResolver() {}

    public record TargetRef(String type, long id) {}

    public static Optional<StaffAssignment> resolve(
            Collection<StaffAssignment> rows, Set<TargetRef> position, Role role, Long taskType, Instant at) {
        var candidates = rows.stream()
                .filter(a -> "PRIMARY".equals(a.assignmentKind()) && a.role() == role)
                .filter(a -> a.activeAt(at))
                .filter(a -> position.contains(new TargetRef(a.targetType(), a.targetId())))
                .filter(a -> a.taskTypeId() == null || Objects.equals(taskType, a.taskTypeId()))
                .toList();
        int best = candidates.stream()
                .mapToInt(PrimaryAssigneeResolver::rank)
                .max()
                .orElse(-1);
        var selected = candidates.stream().filter(a -> rank(a) == best).toList();
        if (selected.size() > 1)
            throw new InvalidPermissionDataException(
                    "M09.ASSIGNMENT",
                    "MULTIPLE_PRIMARY_ASSIGNEES",
                    selected.getFirst().id());
        return selected.stream().findFirst();
    }

    private static int rank(StaffAssignment a) {
        return (switch (a.targetType()) {
                            case "RESIDENT" -> 3;
                            case "ROOM" -> 2;
                            case "FLOOR", "AREA" -> 1;
                            default -> 0;
                        })
                        * 2
                + (a.taskTypeId() == null ? 0 : 1);
    }
}
