package com.eldercare.backend.belongings.internal.returns.domain;

/** Giá trị CFG-M12-06: số lần liên hệ tối thiểu với mỗi người đại diện và số kênh khác nhau (FR-017a, Q-157). */
public record ContactEvidenceRule(int attemptsPerRepresentative, int channels) {}
