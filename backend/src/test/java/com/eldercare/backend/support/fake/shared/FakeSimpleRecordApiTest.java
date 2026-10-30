package com.eldercare.backend.support.fake.shared;

import com.eldercare.backend.administration.shared.SimpleRecordApiContract;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import java.util.function.Supplier;

/** Bản giả {@link FakeSimpleRecordApi} giữ đúng hợp đồng của bản thật (constitution XVI, L). */
class FakeSimpleRecordApiTest implements SimpleRecordApiContract {

    private final FakeSimpleRecordApi api = new FakeSimpleRecordApi();

    @Override
    public SimpleRecordApi records() {
        return api;
    }

    @Override
    public <R> R tx(Supplier<R> work) {
        return work.get();
    }
}
