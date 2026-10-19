package com.cambae.backend.common.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Mọi repository JPA của backend dùng {@link BaseJpaRepository} để có {@code append} (000 T023). */
@Configuration
@EnableJpaRepositories(basePackages = "com.cambae.backend", repositoryBaseClass = BaseJpaRepository.class)
public class JpaConfig {}
