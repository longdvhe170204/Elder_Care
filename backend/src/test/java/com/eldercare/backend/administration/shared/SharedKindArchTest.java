package com.eldercare.backend.administration.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Quyền sở hữu loại dòng của bảng dùng chung (research R-15 mục 2, module-interfaces 000 mục 1.8): hằng
 * {@link SharedKind}, {@link HistoryTrack} chỉ khai trong module có mã trùng tiền tố; mã loại không trùng nhau; loại
 * không cho module khác đọc thì không đặt ở gói {@code api} (Spring Modulith chặn module khác dùng gói nội bộ).
 */
class SharedKindArchTest {

    private static final Map<String, String> MODULE_CODES = Map.ofEntries(
            Map.entry("resident", "M01"),
            Map.entry("admission", "M02"),
            Map.entry("roombedallocation", "M03"),
            Map.entry("care", "M04"),
            Map.entry("incident", "M05"),
            Map.entry("health", "M06"),
            Map.entry("medication", "M07"),
            Map.entry("nutrition", "M08"),
            Map.entry("staffing", "M09"),
            Map.entry("family", "M10"),
            Map.entry("billing", "M11"),
            Map.entry("belongings", "M12"),
            Map.entry("notification", "M13"),
            Map.entry("reporting", "M14"),
            Map.entry("administration", "M15"));

    @Test
    void sharedKindConstants_belongToTheirModule_andAreUnique() throws IllegalAccessException {
        List<String> problems = new ArrayList<>();
        Map<String, String> declaredAt = new HashMap<>();
        for (JavaField field : constants()) {
            Field f = field.reflect();
            f.setAccessible(true);
            Object value = f.get(null);
            String where = field.getFullName();
            String module = MODULE_CODES.get(field.getOwner().getPackageName().split("\\.")[3]);
            String code;
            String owner;
            boolean sharedRead;
            if (value instanceof SharedKind<?> kind) {
                code = kind.code();
                owner = kind.module();
                sharedRead = kind.sharedRead();
            } else if (value instanceof HistoryTrack<?> track) {
                code = track.entityType() + "#" + track.attribute();
                owner = track.module();
                sharedRead = false;
            } else {
                continue;
            }
            if (!owner.equals(module)) {
                problems.add(where + ": mã " + code + " không thuộc module " + module);
            }
            String previous = declaredAt.put(code, where);
            if (previous != null) {
                problems.add(where + ": trùng mã " + code + " với " + previous);
            }
            boolean inApi = field.getOwner().getPackageName().matches(".*\\.api(\\..*)?");
            if (inApi && !sharedRead) {
                problems.add(where + ": loại đặt ở api nhưng không readableByOtherModules()");
            }
        }
        assertThat(problems).isEmpty();
    }

    private static List<JavaField> constants() {
        return new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages("com.eldercare.backend")
                        .stream()
                        .flatMap(c -> c.getFields().stream())
                        .filter(f -> f.getModifiers().contains(JavaModifier.STATIC))
                        .filter(f -> f.getRawType().isEquivalentTo(SharedKind.class)
                                || f.getRawType().isEquivalentTo(HistoryTrack.class))
                        .toList();
    }
}
