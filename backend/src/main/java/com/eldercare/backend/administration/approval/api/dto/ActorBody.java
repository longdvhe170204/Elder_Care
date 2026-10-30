package com.eldercare.backend.administration.approval.api.dto;

import java.util.function.LongFunction;

/** Người thực hiện theo schema {@code Actor}; họ tên theo hồ sơ tài khoản (002 T032a). */
public record ActorBody(ActorType type, Long id, String fullName, String role) {

    public static ActorBody user(Long id, LongFunction<String> names) {
        return id == null ? null : new ActorBody(ActorType.USER, id, names.apply(id), null);
    }
}
