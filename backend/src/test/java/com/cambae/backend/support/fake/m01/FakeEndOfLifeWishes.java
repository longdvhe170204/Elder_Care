package com.cambae.backend.support.fake.m01;

import com.cambae.backend.m01.api.EndOfLifeWishApi;
import com.cambae.backend.m01.api.dto.EndOfLifeWishView;
import com.cambae.backend.m01.api.dto.RecordEndOfLifeWish;
import com.cambae.backend.m01.api.dto.TreatmentWishView;
import com.cambae.backend.m01.api.dto.WishRecordSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả {@link EndOfLifeWishApi} (001 cài, 001 T083) cho 007, 006: test đặt phiếu Hiệu lực bằng {@link #endOfLife}
 * và {@link #treatment}; {@link #recordVersion} lưu lệnh vào {@link #recorded()} và trả mã tăng dần, phiếu ghi xong
 * thành phiếu Hiệu lực. Không nằm trong {@link FakeM01Config} vì {@code @Primary} thay bản thật cho cả context; test
 * cần thì {@code @Import(FakeEndOfLifeWishes.Config.class)}.
 */
public class FakeEndOfLifeWishes implements EndOfLifeWishApi {

    private final Map<Long, EndOfLifeWishView> endOfLife = new HashMap<>();
    private final Map<Long, TreatmentWishView> treatment = new HashMap<>();
    private final List<RecordEndOfLifeWish> recorded = new ArrayList<>();
    private long nextId = 1;

    public FakeEndOfLifeWishes endOfLife(EndOfLifeWishView view) {
        endOfLife.put(view.residentId(), view);
        return this;
    }

    public FakeEndOfLifeWishes treatment(TreatmentWishView view) {
        treatment.put(view.residentId(), view);
        return this;
    }

    /** Các lệnh ghi phiên bản đã nhận, theo thứ tự gọi. */
    public List<RecordEndOfLifeWish> recorded() {
        return List.copyOf(recorded);
    }

    public void reset() {
        endOfLife.clear();
        treatment.clear();
        recorded.clear();
    }

    @Override
    public Optional<EndOfLifeWishView> current(long residentId) {
        return Optional.ofNullable(endOfLife.get(residentId));
    }

    @Override
    public long recordVersion(RecordEndOfLifeWish command, WishRecordSource source) {
        recorded.add(command);
        long id = nextId++;
        endOfLife.put(
                command.residentId(),
                new EndOfLifeWishView(
                        id,
                        command.residentId(),
                        command.choice(),
                        command.firstContact(),
                        command.note(),
                        command.signerType(),
                        command.signedAt(),
                        source));
        return id;
    }

    @Override
    public Optional<TreatmentWishView> currentTreatmentWish(long residentId) {
        return Optional.ofNullable(treatment.get(residentId));
    }

    @TestConfiguration
    public static class Config {

        @Bean
        @Primary
        FakeEndOfLifeWishes fakeEndOfLifeWishes() {
            return new FakeEndOfLifeWishes();
        }
    }
}
