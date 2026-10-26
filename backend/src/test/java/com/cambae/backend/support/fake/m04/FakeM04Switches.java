package com.cambae.backend.support.fake.m04;

import java.util.*;

/** Công tắc theo module; tắt nguồn gây lỗi rõ ràng, không giả vờ trả dữ liệu rỗng. */
public class FakeM04Switches {
    private static final Set<String> MODULES = Set.of("006", "007", "008", "011", "012", "014");
    private final Set<String> disabled = new HashSet<>();

    public synchronized void enabled(String module, boolean enabled) {
        validateModule(module);
        if (enabled) disabled.remove(module);
        else disabled.add(module);
    }

    public synchronized void requireEnabled(String module) {
        validateModule(module);
        if (disabled.contains(module)) throw new IllegalStateException("Nguồn giả " + module + " đang tắt");
    }

    private void validateModule(String module) {
        if (!MODULES.contains(module)) throw new IllegalArgumentException("Module giả không hỗ trợ: " + module);
    }
}
