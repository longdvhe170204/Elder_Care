package com.eldercare.backend.resident.web.response;

import java.util.List;

public record ProfilePage(List<ProfileBody> items, String nextCursor) {}
