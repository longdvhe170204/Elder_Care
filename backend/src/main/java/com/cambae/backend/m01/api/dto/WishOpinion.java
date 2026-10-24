package com.cambae.backend.m01.api.dto;

/** Ý kiến khác với lựa chọn trên phiếu, mỗi ý kiến một mục có nguồn (FR-023c, FR-023g). */
public record WishOpinion(OpinionSource source, String content) {}
