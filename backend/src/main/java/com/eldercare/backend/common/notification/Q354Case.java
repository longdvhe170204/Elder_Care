package com.eldercare.backend.common.notification;

/** Trường hợp Q-354 được gửi vượt phạm vi đồng ý (009 FR-014a); trường hợp mới cần Q mới. */
public enum Q354Case {
    PRIMARY_CONTACT_EMERGENCY,
    CONTACT_LIST,
    WISH_RECONFIRMATION,
    TRANSFER,
    DEATH
}
