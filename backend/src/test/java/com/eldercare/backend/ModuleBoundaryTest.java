package com.eldercare.backend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Constitution X (000 T037): module chỉ dùng phần công khai của nhau, không có vòng phụ thuộc. */
class ModuleBoundaryTest {

    @Test
    void X_modules_respectBoundaries() {
        ApplicationModules.of(BackendApplication.class).verify();
    }
}
