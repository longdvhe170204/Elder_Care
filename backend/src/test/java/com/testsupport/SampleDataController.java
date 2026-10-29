package com.testsupport;

import com.eldercare.backend.administration.sample.SampleService;
import com.eldercare.backend.common.Reason;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint mẫu cho test US1 của 000 theo quy ước R-03 (nhóm 1 có DELETE; nhóm 2 chỉ POST lệnh). */
@RestController
@RequestMapping("/api/v1/test-support")
public class SampleDataController {

    private final SampleService samples;

    public SampleDataController(SampleService samples) {
        this.samples = samples;
    }

    public record NameBody(String name) {}

    public record CaseBody(long catalogId) {}

    public record ReasonBody(Reason reason) {}

    @PostMapping("/sample-catalog")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createCatalogItem(@RequestBody NameBody body) {
        return Map.of("id", samples.createCatalogItem(body.name()));
    }

    @PostMapping("/sample-catalog/{id}/deactivate")
    public void deactivate(@PathVariable long id) {
        samples.deactivate(id);
    }

    @PostMapping("/sample-catalog/{id}/reactivate")
    public void reactivate(@PathVariable long id) {
        samples.reactivate(id);
    }

    @DeleteMapping("/sample-catalog/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        samples.deleteCatalogItem(id);
    }

    @PostMapping("/sample-cases")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createCase(@RequestBody CaseBody body) {
        return Map.of("id", samples.createCase(body.catalogId(), 1));
    }

    @GetMapping("/sample-cases/{id}")
    public Map<String, Object> readCase(@PathVariable long id) {
        return samples.readCase(id);
    }

    @PostMapping("/sample-cases/{id}/cancel")
    public void cancel(@PathVariable long id, @RequestBody ReasonBody body) {
        samples.cancelCase(id, body.reason());
    }
}
