package com.cambae.backend.support.fake.m04;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Chỉ được đăng ký qua FakeM04Config; không quét vào ứng dụng test/demo mặc định. */
@TestComponent
@RestController
@Profile({"test", "demo"})
@RequestMapping("/api/v1/testing/fakes")
public class FakeM04Controller {
    private final FakeM04Switches switches;

    public FakeM04Controller(FakeM04Switches switches) {
        this.switches = switches;
    }

    @PutMapping("/{module}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(@PathVariable String module, @Valid @RequestBody SwitchRequest request) {
        try {
            switches.enabled(module, request.enabled());
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage(), error);
        }
    }

    public record SwitchRequest(@NotNull Boolean enabled) {}
}
