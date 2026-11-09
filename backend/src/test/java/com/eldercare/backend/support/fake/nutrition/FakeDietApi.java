package com.eldercare.backend.support.fake.nutrition;

import com.eldercare.backend.nutrition.api.DietApi;
import com.eldercare.backend.nutrition.api.dto.CurrentDietView;
import com.eldercare.backend.nutrition.api.dto.DietAssignmentView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link DietApi}: trả chế độ ăn hiện hành và lịch sử do test đặt. */
public class FakeDietApi implements DietApi {

    private final Map<Long, CurrentDietView> current = new HashMap<>();
    private final List<DietAssignmentView> history = new ArrayList<>();

    public FakeDietApi current(CurrentDietView diet) {
        current.put(diet.residentId(), diet);
        return this;
    }

    public FakeDietApi history(DietAssignmentView assignment) {
        history.add(assignment);
        return this;
    }

    @Override
    public Optional<CurrentDietView> current(long residentId) {
        return Optional.ofNullable(current.get(residentId));
    }

    @Override
    public List<DietAssignmentView> history(long residentId) {
        return history.stream().filter(a -> a.residentId() == residentId).toList();
    }
}
