package com.eldercare.backend.incident.internal.alert.domain;

/** Nơi đang ở của một lần bấm (FR-047f): dòng con kind {@code M05.SOS_PLACE}, khóa duy nhất theo lần bấm nên đặt một lần. */
public record SosPlaceContent(SosPlace place) {}
