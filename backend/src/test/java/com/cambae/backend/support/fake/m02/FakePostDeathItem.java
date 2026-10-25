package com.cambae.backend.support.fake.m02;

import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import com.cambae.backend.m02.api.dto.PostDeathItemStatusView;
import com.cambae.backend.m02.api.spi.PostDeathItemPort;
import java.util.HashMap;
import java.util.Map;

/**
 * Bản giả {@link PostDeathItemPort} cho một mã mục; {@link FakeM02Config} tạo một bean mỗi mã. Mặc định
 * {@code NOT_APPLICABLE}.
 */
public class FakePostDeathItem implements PostDeathItemPort {

    private final PostDeathItemCode code;
    private final Map<Long, PostDeathItemStatusView> statuses = new HashMap<>();

    public FakePostDeathItem(PostDeathItemCode code) {
        this.code = code;
    }

    public FakePostDeathItem status(long residentId, PostDeathItemStatus status) {
        statuses.put(residentId, new PostDeathItemStatusView(status, "fake:" + code));
        return this;
    }

    public FakePostDeathItem reset() {
        statuses.clear();
        return this;
    }

    @Override
    public PostDeathItemCode code() {
        return code;
    }

    @Override
    public PostDeathItemStatusView status(long residentId) {
        return statuses.getOrDefault(
                residentId, new PostDeathItemStatusView(PostDeathItemStatus.NOT_APPLICABLE, "fake:" + code));
    }
}
