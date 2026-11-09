package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.nio.file.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 014 T001 (research L-01, Q-306, Q-307): năm gói con của 014 và luật job không tạo bản ghi của 014. */
class M04ActivityArchTest {
    static final String[] PACKAGES = {"activity", "session", "trip", "wellbeing", "qualitycheck"};

    @TempDir
    Path temporary;

    @Test
    void L_01_fiveSubpackagesExist() {
        for (var name : PACKAGES)
            assertThat(Package.getPackage("com.eldercare.backend.care.internal." + name) != null
                            || exists("com.eldercare.backend.care.internal." + name + ".package-info"))
                    .as(name)
                    .isTrue();
    }

    @Test
    void Q_306_applicationJobsDoNotCreateActivityRecords() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.eldercare.backend.care");
        M04ArchitectureRules.NO_JOB_CREATES_ACTIVITY_RECORDS.check(classes);
    }

    @Test
    void Q_306_ruleRejectsJobsCreatingSessionsChecklistsItemsOrRework() throws Exception {
        String[] names = {"SessionCommands.java", "ChecklistCommands.java", "TaskCommands.java", "BadJob.java"};
        String[] sources = {
            "package com.eldercare.backend.care.internal.session.service; public class SessionCommands {public void createSession(){}}",
            "package com.eldercare.backend.care.internal.qualitycheck.service; public class ChecklistCommands {public void createChecklist(){} public void addItem(){}}",
            "package com.eldercare.backend.care.internal.task.service; public class TaskCommands {public void createRework(){}}",
            "package com.eldercare.backend.care.scheduling; public class BadJob {public void run(){"
                    + "new com.eldercare.backend.care.internal.session.service.SessionCommands().createSession();"
                    + "new com.eldercare.backend.care.internal.qualitycheck.service.ChecklistCommands().createChecklist();"
                    + "new com.eldercare.backend.care.internal.qualitycheck.service.ChecklistCommands().addItem();"
                    + "new com.eldercare.backend.care.internal.task.service.TaskCommands().createRework();}}"
        };
        String[] args = new String[names.length + 2];
        args[0] = "-d";
        args[1] = temporary.toString();
        for (int i = 0; i < names.length; i++) {
            var file = temporary.resolve(names[i]);
            Files.writeString(file, sources[i]);
            args[i + 2] = file.toString();
        }
        assertThat(ToolProvider.getSystemJavaCompiler().run(null, null, null, args))
                .isZero();
        var result = M04ArchitectureRules.NO_JOB_CREATES_ACTIVITY_RECORDS.evaluate(
                new ClassFileImporter().importPath(temporary));
        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails()).hasSize(4);
    }

    private static boolean exists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
