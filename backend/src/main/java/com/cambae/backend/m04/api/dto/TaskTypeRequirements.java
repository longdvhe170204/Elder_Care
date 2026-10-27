package com.cambae.backend.m04.api.dto;

import com.cambae.backend.common.Role;
import java.time.*;
import java.util.*;

/** T003: projection theo contracts/module-interfaces mục 1.2–1.10a. */
public record TaskTypeRequirements(Role role, Set<String> certificates) {}
