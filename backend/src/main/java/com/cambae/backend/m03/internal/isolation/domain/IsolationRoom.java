package com.cambae.backend.m03.internal.isolation.domain;

import com.cambae.backend.m03.internal.structure.domain.Room;

/** Ảnh danh mục kèm chặn theo vùng, không lẫn quyền cấu hình với quyền cách ly. */
public record IsolationRoom(Room room, Boolean blocked) {}
