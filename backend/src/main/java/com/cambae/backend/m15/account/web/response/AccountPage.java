package com.cambae.backend.m15.account.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(name = "AccountPage")
public record AccountPage(List<AccountBody> items, String nextCursor) {}
