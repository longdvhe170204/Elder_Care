package com.cambae.backend.m03.api;

import com.cambae.backend.m03.api.dto.*;
import java.util.*;

/** Phần CoCauViTriApi cần cho 008; C review. */
public interface LocationStructureApi {
    boolean active(LocationRef location);

    Optional<RoomView> room(long id);

    Optional<FloorView> floor(long id);

    Optional<AreaView> area(long id);

    Set<Long> floorsInArea(long areaId);
}
