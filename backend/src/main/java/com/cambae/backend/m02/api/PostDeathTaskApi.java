package com.cambae.backend.m02.api;

import com.cambae.backend.m02.api.dto.PostDeathTaskView;

/** Danh sách việc sau qua đời (tên logic {@code DanhSachViecSauQuaDoiApi}; contracts mục 1.6). */
public interface PostDeathTaskApi {

    /** Module sở hữu mục gọi sau lệnh của mình (010, 013, 006, 007, 017); 004 hỏi lại {@code PostDeathItemPort}. */
    void refresh(long residentId);

    PostDeathTaskView view(long residentId);
}
