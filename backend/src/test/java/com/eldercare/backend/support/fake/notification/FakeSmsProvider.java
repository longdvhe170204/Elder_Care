package com.eldercare.backend.support.fake.notification;

import com.eldercare.backend.notification.api.spi.SmsProvider;
import com.eldercare.backend.notification.api.spi.SmsResult;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Bản giả {@link SmsProvider} (009 T003; research B-13): ghi lại tin đã gửi; kết quả lần lượt theo kịch bản test đặt,
 * hết kịch bản thì trả kết quả mặc định ({@code SENT}).
 */
public class FakeSmsProvider implements SmsProvider {

    /** Một tin đã gửi tới nhà cung cấp giả. */
    public record SentSms(String phoneNumber, String content) {}

    private final List<SentSms> sent = new ArrayList<>();
    private final Deque<SmsResult> script = new ArrayDeque<>();
    private SmsResult fallback = SmsResult.of(SmsResult.Outcome.SENT);

    /** Kết quả cho các lần gửi kế tiếp, theo thứ tự. */
    public synchronized FakeSmsProvider thenReturn(SmsResult... results) {
        script.addAll(List.of(results));
        return this;
    }

    /** Kết quả khi hết kịch bản. */
    public synchronized FakeSmsProvider byDefault(SmsResult result) {
        fallback = result;
        return this;
    }

    public synchronized List<SentSms> sent() {
        return List.copyOf(sent);
    }

    public synchronized void reset() {
        sent.clear();
        script.clear();
        fallback = SmsResult.of(SmsResult.Outcome.SENT);
    }

    @Override
    public synchronized SmsResult send(String phoneNumber, String content) {
        sent.add(new SentSms(phoneNumber, content));
        return script.isEmpty() ? fallback : script.poll();
    }
}
