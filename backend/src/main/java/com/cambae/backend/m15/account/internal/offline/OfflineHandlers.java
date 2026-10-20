package com.cambae.backend.m15.account.internal.offline;

import com.cambae.backend.m15.account.api.spi.OfflineHandler;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import tools.jackson.databind.json.JsonMapper;

/** Tra {@link OfflineHandler} theo loại bản ghi và đổi nội dung JSON sang kiểu nội dung của handler (research A-11). */
@Component
class OfflineHandlers {

    private final ObjectProvider<OfflineHandler<?>> handlers;
    private final JsonMapper json;

    OfflineHandlers(ObjectProvider<OfflineHandler<?>> handlers, JsonMapper json) {
        this.handlers = handlers;
        this.json = json;
    }

    Optional<OfflineHandler<Object>> byType(String recordType) {
        Map<String, OfflineHandler<?>> byType = handlers.orderedStream()
                .collect(Collectors.toMap(OfflineHandler::recordType, h -> h, (a, b) -> {
                    throw new IllegalStateException("Hai OfflineHandler cùng loại " + a.recordType());
                }));
        @SuppressWarnings("unchecked")
        OfflineHandler<Object> handler = (OfflineHandler<Object>) byType.get(recordType);
        return Optional.ofNullable(handler);
    }

    /** Nội dung theo kiểu {@code T} của handler; ném lỗi của Jackson khi nội dung không đúng kiểu. */
    Object content(OfflineHandler<Object> handler, String contentJson) {
        Class<?> type = ResolvableType.forClass(ClassUtils.getUserClass(handler))
                .as(OfflineHandler.class)
                .getGeneric(0)
                .resolve(Object.class);
        return json.readValue(contentJson, type);
    }
}
