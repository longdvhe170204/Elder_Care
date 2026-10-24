package com.cambae.backend.m01.web.response;

import com.cambae.backend.m01.api.dto.Warning;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "CreateProfileResult")
public record CreateProfileResultBody(ProfileBody profile, List<Warning> warnings) {}
