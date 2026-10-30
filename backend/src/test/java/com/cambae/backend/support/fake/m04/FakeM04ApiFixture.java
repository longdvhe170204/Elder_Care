package com.cambae.backend.support.fake.m04;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Supplier;

/** T003: fixture hợp đồng API M04; thiếu cấu hình gây lỗi, không tự nhận lệnh thành công. */
public class FakeM04ApiFixture {
    private final Map<Key, Supplier<?>> replies = new HashMap<>();

    public void reply(Class<?> api, String method, List<?> arguments, Supplier<?> result) {
        replies.put(new Key(api, method, Collections.unmodifiableList(new ArrayList<>(arguments))), result);
    }

    public <T> T api(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class)
                return switch (method.getName()) {
                    case "toString" -> "Bản giả " + type.getSimpleName();
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new IllegalStateException(method.getName());
                };
            Key key = new Key(type, method.getName(), args == null ? List.of() : Arrays.asList(args));
            Supplier<?> result = replies.get(key);
            if (result == null)
                throw new IllegalStateException(
                        "Chưa đặt fixture API " + type.getSimpleName() + "." + method.getName());
            return result.get();
        }));
    }

    public void reset() {
        replies.clear();
    }

    private record Key(Class<?> api, String method, List<?> arguments) {}
}
