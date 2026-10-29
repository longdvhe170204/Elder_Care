package com.eldercare.backend.resident.web.response;

import com.eldercare.backend.resident.api.dto.Warning;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "CreateProfileResult")
public record CreateProfileResultBody(ProfileBody profile, List<Warning> warnings) {}
