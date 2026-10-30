package com.eldercare.backend.roombedallocation.internal.isolation.domain;

import com.eldercare.backend.roombedallocation.internal.structure.domain.Room;

/** Ảnh danh mục kèm chặn theo vùng, không lẫn quyền cấu hình với quyền cách ly. */
public record IsolationRoom(Room room, Boolean blocked) {}
