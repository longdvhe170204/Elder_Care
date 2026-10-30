package com.cambae.backend.common.notification;

/** Cách thay người nhận của nhóm loại (a) khi không có người (009 FR-010, Q-110). */
@com.fasterxml.jackson.annotation.JsonTypeInfo(
        use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME,
        property = "kind")
@com.fasterxml.jackson.annotation.JsonSubTypes({
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = Substitution.Default.class, name = "DEFAULT"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(
            value = Substitution.NoSubstitute.class,
            name = "NO_SUBSTITUTE"),
    @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = Substitution.Designated.class, name = "DESIGNATED")
})
public sealed interface Substitution {

    record Default() implements Substitution {}

    record NoSubstitute() implements Substitution {}

    record Designated(RecipientGroup substitute) implements Substitution {}
}
