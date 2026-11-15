package com.eldercare.backend.support.demo;

import com.eldercare.backend.common.scheduling.ScheduledJob;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Chạy ngay mọi {@link ScheduledJob} theo tên (cả job của M02, M04, M09 không có trong {@code /testing/clock runJobs})
 * cho script nghiệm thu quickstart 004, 005, 008, 015 ({@code backend/tools/quickstart-004} …). Tên lớp bắt đầu bằng
 * {@code QuickstartDemoApplication} để bộ lọc quét của lớp đó nạp; chỉ có ở profile demo, không thuộc hợp đồng.
 */
@RestController
@Profile("demo")
@RequestMapping("/api/v1/testing/jobs")
class QuickstartDemoApplicationJobs {

    private final List<ScheduledJob> jobs;

    QuickstartDemoApplicationJobs(List<ScheduledJob> jobs) {
        this.jobs = jobs;
    }

    @GetMapping
    List<String> names() {
        return jobs.stream().map(ScheduledJob::name).sorted().toList();
    }

    @PostMapping("/{name}")
    Map<String, String> run(@PathVariable String name) {
        ScheduledJob job = jobs.stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có job " + name));
        job.runNow();
        return Map.of("ran", name);
    }
}
