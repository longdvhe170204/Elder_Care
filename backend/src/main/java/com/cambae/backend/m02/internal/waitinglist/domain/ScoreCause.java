package com.cambae.backend.m02.internal.waitinglist.domain;

/** Nguyên nhân một dòng lịch sử điểm (FR-010; data-model mục 4). */
public enum ScoreCause {
    CREATED,
    DAILY,
    CARE_LEVEL_CHANGED,
    CIRCUMSTANCES_CHANGED,
    ADJUSTMENT,
    SCORE_TABLE_CHANGED
}
