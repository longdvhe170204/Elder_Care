import 'package:flutter/foundation.dart';

import '../api/api_client.dart';
import '../auth/session_controller.dart';
import 'notification_api.dart';
import 'push_messaging.dart';

/// Nối nhận đẩy với phiên (009 T031, T042; FR-016): đã đăng nhập thì đăng ký token với backend theo loại tài khoản, token đổi
/// thì đăng ký lại. Màn thông báo nghe lớp này: có thông báo thì tải lại, có thông báo được bấm thì lấy mã bằng
/// [takeOpened] rồi mở. Đăng ký lỗi không chặn app, lần mở sau đăng ký lại. Hợp đồng không có lệnh hủy token: tài khoản khác
/// đăng nhập trên cùng máy thì đăng ký lại token đó cho tài khoản mới.
class PushCoordinator extends ChangeNotifier {
  PushCoordinator(this._messaging);

  final PushMessaging _messaging;
  SessionController? _session;
  bool _permissionAsked = false;
  String? _registered;
  int? _opened;

  /// Lấy mã thông báo đang chờ mở, mỗi mã một lần.
  int? takeOpened() {
    final id = _opened;
    _opened = null;
    return id;
  }

  Future<void> start(SessionController session) async {
    _session = session;
    if (!await _messaging.start()) return;
    _messaging.tokenRefresh.listen((token) => _register(token));
    _messaging.onMessage.listen((_) => notifyListeners());
    _messaging.onOpened.listen(_open);
    final initial = await _messaging.initialOpened();
    if (initial != null) _open(initial);
    session.addListener(_onSession);
    await _onSession();
  }

  void _open(int id) {
    _opened = id;
    notifyListeners();
  }

  Future<void> _onSession() async {
    final session = _session!;
    if (session.status != SessionStatus.signedIn) {
      _registered = null;
      return;
    }
    if (_registered != null) return;
    try {
      if (!_permissionAsked) {
        _permissionAsked = true;
        await _messaging.requestPermission();
      }
      final token = await _messaging.token();
      if (token != null) await _register(token);
    } on Exception {
      // máy không có dịch vụ Google hoặc mất mạng: chạy không có đẩy, lần mở sau thử lại
    }
  }

  Future<void> _register(String token) async {
    final session = _session!;
    final app = switch (session.user?.type) {
      AccountType.staff => 'STAFF_APP',
      AccountType.familyMember => 'FAMILY_APP',
      AccountType.resident => 'RESIDENT_APP',
      null => null,
    };
    if (app == null || token == _registered) return;
    _registered = token;
    try {
      await session.api.post(
        '/push-devices',
        {'app': app, 'fcmToken': token},
        headers: {'Idempotency-Key': newIdempotencyKey()},
      );
    } on ApiProblem {
      _registered = null;
    } on ApiUnreachable {
      _registered = null;
    }
  }
}
