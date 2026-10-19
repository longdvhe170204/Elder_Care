package com.cambae.backend.common.notification;

/** Cách thay người nhận của nhóm loại (a) khi không có người (009 FR-010, Q-110). */
public sealed interface Substitution {

    record Default() implements Substitution {}

    record NoSubstitute() implements Substitution {}

    record Designated(RecipientGroup substitute) implements Substitution {}
}
