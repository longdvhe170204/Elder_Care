import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

/// Nhận đẩy của thiết bị (009 FR-016, Q-305); test và máy chưa cấu hình Firebase dùng bản khác.
abstract class PushMessaging {
  /// Khởi tạo; `false` là máy này không nhận đẩy được.
  Future<bool> start();

  Future<void> requestPermission();

  Future<String?> token();

  Stream<String> get tokenRefresh;

  /// Có thông báo khi app đang mở, kèm mức (`data.level` của `InAppSender`).
  Stream<String?> get onMessage;

  /// Mã thông báo người dùng bấm khi app đang chạy nền.
  Stream<int> get onOpened;

  /// Mã thông báo đã mở app từ trạng thái tắt hẳn.
  Future<int?> initialOpened();
}

/// Không nhận đẩy: thiếu cấu hình Firebase, hoặc nền tảng không hỗ trợ.
class NoPushMessaging implements PushMessaging {
  const NoPushMessaging();

  @override
  Future<bool> start() async => false;

  @override
  Future<void> requestPermission() async {}

  @override
  Future<String?> token() async => null;

  @override
  Stream<String> get tokenRefresh => const Stream.empty();

  @override
  Stream<String?> get onMessage => const Stream.empty();

  @override
  Stream<int> get onOpened => const Stream.empty();

  @override
  Future<int?> initialOpened() async => null;
}

int? _notificationId(RemoteMessage message) => int.tryParse(message.data['notificationId'] as String? ?? '');

/// Firebase Cloud Messaging (research B-05). Cấu hình `google-services.json`, `GoogleService-Info.plist` không có trong kho
/// mã; thiếu thì [start] trả `false` và app chạy không có đẩy.
class FirebasePushMessaging implements PushMessaging {
  FirebaseMessaging get _messaging => FirebaseMessaging.instance;

  @override
  Future<bool> start() async {
    try {
      await Firebase.initializeApp();
      return true;
    } on Object {
      return false;
    }
  }

  @override
  Future<void> requestPermission() async {
    await _messaging.requestPermission();
  }

  // Backend gửi theo registration token (`FcmPushGateway`, `Message.setToken`); register(), onRegistered thay thế trả
  // Firebase Installation ID, đổi cần sửa cả backend.
  @override
  // ignore: deprecated_member_use
  Future<String?> token() => _messaging.getToken();

  @override
  // ignore: deprecated_member_use
  Stream<String> get tokenRefresh => _messaging.onTokenRefresh;

  @override
  Stream<String?> get onMessage => FirebaseMessaging.onMessage.map((message) => message.data['level'] as String?);

  @override
  Stream<int> get onOpened =>
      FirebaseMessaging.onMessageOpenedApp.map(_notificationId).where((id) => id != null).cast();

  @override
  Future<int?> initialOpened() async {
    final message = await _messaging.getInitialMessage();
    return message == null ? null : _notificationId(message);
  }
}
