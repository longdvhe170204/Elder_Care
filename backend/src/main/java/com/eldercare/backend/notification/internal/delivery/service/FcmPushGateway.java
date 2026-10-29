package com.eldercare.backend.notification.internal.delivery.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Đẩy qua Firebase Cloud Messaging bằng Firebase Admin SDK (research B-05, T008). */
public class FcmPushGateway implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(FcmPushGateway.class);

    private final FirebaseMessaging messaging;

    public FcmPushGateway(FirebaseMessaging messaging) {
        this.messaging = messaging;
    }

    @Override
    public Outcome push(String token, Message message) {
        com.google.firebase.messaging.Message fcm = com.google.firebase.messaging.Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.data())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(message.urgent() ? AndroidConfig.Priority.HIGH : AndroidConfig.Priority.NORMAL)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-priority", message.urgent() ? "10" : "5")
                        .setAps(Aps.builder().build())
                        .build())
                .build();
        try {
            messaging.send(fcm);
            return Outcome.DELIVERED;
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode code = e.getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                return Outcome.INVALID_TOKEN;
            }
            log.warn("FCM gửi lỗi ({})", code, e);
            return Outcome.FAILED;
        }
    }
}
