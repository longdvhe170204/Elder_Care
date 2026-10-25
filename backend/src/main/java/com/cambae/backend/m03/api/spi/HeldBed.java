package com.cambae.backend.m03.api.spi;

import java.time.Instant;
import java.util.Optional;

/** Quyết định giữ giường và hạn giữ khi vắng, do 004 cung cấp. */
public record HeldBed(boolean held, Optional<Instant> until) {}
