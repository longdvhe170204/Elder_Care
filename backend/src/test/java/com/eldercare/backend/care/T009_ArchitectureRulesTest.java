package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kiểm chính luật kiến trúc bằng bytecode vi phạm, không chỉ chạy xanh trên gói chưa có consumer. */
class T009_ArchitectureRulesTest {
    @TempDir
    Path temporary;

    @Test
    void T009_rejectsDirectHigherModuleCallAndJobCreatingTasks() throws Exception {
        String[] names = {"Foreign.java", "BadRead.java", "TaskCommands.java", "BadJob.java"};
        String[] sources = {
            "package com.eldercare.backend.staffing.api; public class Foreign {}",
            "package com.eldercare.backend.care.internal.task.service; public class BadRead {com.eldercare.backend.staffing.api.Foreign field;}",
            "package com.eldercare.backend.care.internal.task.service; public class TaskCommands {public void createTasks(){}}",
            "package com.eldercare.backend.care.scheduling; public class BadJob {public void run(){new com.eldercare.backend.care.internal.task.service.TaskCommands().createTasks();}}"
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
        var classes = new ClassFileImporter().importPath(temporary);
        assertThat(M04ArchitectureRules.NO_DIRECT_HIGHER_MODULES
                        .evaluate(classes)
                        .hasViolation())
                .isTrue();
        assertThat(M04ArchitectureRules.NO_JOB_CREATES_RECORDS.evaluate(classes).hasViolation())
                .isTrue();
    }

    @Test
    void T009_rejectsTaskCreationFromRegisteredWorkOutsideSchedulingPackage() throws Exception {
        var commands = temporary.resolve("TaskCommands.java");
        var step = temporary.resolve("BadStep.java");
        Files.writeString(
                commands,
                "package com.eldercare.backend.care.internal.task.service; public class TaskCommands {public void createTasks(){}}");
        Files.writeString(
                step,
                "package com.eldercare.backend.care.internal.task.service; public class BadStep implements com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork {public int run(java.time.Instant now){new TaskCommands().createTasks();return 1;}}");
        assertThat(ToolProvider.getSystemJavaCompiler()
                        .run(
                                null,
                                null,
                                null,
                                "-classpath",
                                System.getProperty("java.class.path"),
                                "-d",
                                temporary.toString(),
                                commands.toString(),
                                step.toString()))
                .isZero();
        var classes = new ClassFileImporter().importPath(temporary);
        assertThat(M04ArchitectureRules.NO_JOB_CREATES_RECORDS.evaluate(classes).hasViolation())
                .isTrue();
    }
}
