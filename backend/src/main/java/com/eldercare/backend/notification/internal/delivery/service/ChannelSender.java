package com.eldercare.backend.notification.internal.delivery.service;

import com.eldercare.backend.notification.internal.delivery.domain.AttemptContext;
import com.eldercare.backend.notification.internal.delivery.domain.SendResult;
import java.util.Set;

/** Gửi một lần gửi trên kênh mình phụ trách; chạy ngoài transaction, không ném lỗi của kênh ra ngoài. */
public interface ChannelSender {

    Set<String> channels();

    SendResult send(AttemptContext attempt);
}
