package com.cambae.backend.m01.web.response;

import java.util.List;

public record ProfilePage(List<ProfileBody> items, String nextCursor) {}
