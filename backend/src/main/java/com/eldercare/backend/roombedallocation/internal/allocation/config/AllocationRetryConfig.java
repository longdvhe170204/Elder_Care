package com.eldercare.backend.roombedallocation.internal.allocation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.resilience.annotation.EnableResilientMethods;

/** Retry bao ngoài transaction để mỗi lần thử mở transaction mới sau rollback. */
@Configuration(proxyBeanMethods = false)
@EnableResilientMethods(order = Ordered.LOWEST_PRECEDENCE - 1)
public class AllocationRetryConfig {}
