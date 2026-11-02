package com.eldercare.backend.administration.account.web.response;

import com.eldercare.backend.common.web.SecretResponse;

public record TemporaryPassword(String temporaryPassword) implements SecretResponse {}
