import 'dart:math';

import 'package:flutter/foundation.dart';

import '../api/api_client.dart';
import 'biometric_auth.dart';
import 'secure_store.dart';

/// Loại tài khoản (api-002 `UserInfo.type`).
enum AccountType { staff, familyMember, resident }

/// Người dùng đang đăng nhập (api-002 `UserInfo`).
class UserInfo {
  const UserInfo({required this.accountId, required this.type, this.fullName, required this.mustChangePassword});

  factory UserInfo.fromJson(Map<String, dynamic> json) => UserInfo(
    accountId: (json['accountId'] as num).toInt(),
    type: switch (json['type']) {
      'STAFF' => AccountType.staff,
      'FAMILY_MEMBER' => AccountType.familyMember,
      _ => AccountType.resident,
    },
    fullName: json['fullName'] as String?,
    mustChangePassword: json['mustChangePassword'] == true,
  );

  final int accountId;
  final AccountType type;
  final String? fullName;
  final bool mustChangePassword;
}

enum SessionStatus {
  /// Đang đọc phiên đã lưu.
  starting,
  signedOut,

  /// Phải đổi mật khẩu trước khi dùng (002 FR-006).
  mustChangePassword,

  /// Vừa đăng nhập lần đầu trên máy: hỏi một lần có mở bằng sinh trắc học không (DESIGN.md 15.6).
  offerBiometric,

  /// Đã bật mở khóa sinh trắc học: chờ vân tay hoặc khuôn mặt (002 FR-074).
  locked,
  signedIn,
}

/// Phiên đăng nhập của app (002 T046, T047 phần chung; T097): token phiên và token thiết bị trong kho an toàn, mã
/// thiết bị UUID tạo một lần trên máy. Phiên của người cao tuổi không hết theo thời gian (002 FR-010, FR-013a), nên
/// app chỉ quay về màn đăng nhập khi backend trả 401.
class SessionController extends ChangeNotifier {
  SessionController({required this.api, required this.store, required this.biometric});

  static const tokenKey = 'session.token';
  static const deviceTokenKey = 'session.deviceToken';
  static const deviceIdKey = 'device.id';
  static const userTypeKey = 'session.userType';
  static const biometricKey = 'resident.biometric';
  static const biometricOfferedKey = 'resident.biometricOffered';

  final ApiClient api;
  final SecureStore store;
  final BiometricAuth biometric;

  SessionStatus status = SessionStatus.starting;
  UserInfo? user;

  /// Câu báo cho màn đăng nhập, ví dụ sau khi đổi mật khẩu.
  String? notice;

  /// Mở app: lấy mã thiết bị, đọc phiên đã lưu và kiểm lại với backend.
  Future<void> start() async {
    api.deviceId = await _deviceId();
    api.token = await store.read(tokenKey);
    if (api.token == null) {
      return _set(SessionStatus.signedOut);
    }
    try {
      user = UserInfo.fromJson((await api.get('/me'))!);
      await store.write(userTypeKey, user!.type.name);
    } on ApiProblem catch (p) {
      if (p.status == 401) {
        await _clear();
        return _set(SessionStatus.signedOut);
      }
      rethrow;
    } on ApiUnreachable {
      // Mất mạng lúc mở app: phiên vẫn còn, dùng loại tài khoản đã lưu (DESIGN.md 15.6: màn xem dùng dữ liệu gần nhất)
      final type = await store.read(userTypeKey);
      user = UserInfo(
        accountId: 0,
        type: AccountType.values.firstWhere((t) => t.name == type, orElse: () => AccountType.resident),
        mustChangePassword: false,
      );
    }
    if (user!.mustChangePassword) {
      return _set(SessionStatus.mustChangePassword);
    }
    _set(await store.read(biometricKey) == 'on' ? SessionStatus.locked : SessionStatus.signedIn);
  }

  /// Quét mã QR nhân viên đưa (002 FR-069): gắn máy này và nhận token thiết bị cho nút SOS.
  Future<void> qrLogin(String code) async {
    await _signedIn(await api.post('/auth/qr-login', {'code': code}));
  }

  /// Mật khẩu dự phòng (002 FR-069), kênh app người cao tuổi cũng gắn máy.
  Future<void> passwordLogin(String username, String password) async {
    await _signedIn(
      await api.post('/auth/login', {'username': username, 'password': password, 'channel': 'RESIDENT_APP'}),
    );
  }

  /// Đổi mật khẩu bắt buộc lần đầu (002 FR-006); đổi xong backend kết thúc mọi phiên của tài khoản người cao tuổi
  /// (BR-M15-24), nên app về màn đăng nhập.
  Future<void> changePassword(String currentPassword, String newPassword) async {
    await api.post('/auth/change-password', {'currentPassword': currentPassword, 'newPassword': newPassword});
    await _clear();
    notice = 'Đã đổi mật khẩu. Quét mã mới hoặc dùng mật khẩu mới để vào.';
    _set(SessionStatus.signedOut);
  }

  Future<void> answerBiometricOffer({required bool enable}) async {
    await store.write(biometricOfferedKey, 'yes');
    if (enable && await biometric.authenticate('Mở ứng dụng bằng vân tay hoặc khuôn mặt')) {
      await store.write(biometricKey, 'on');
    }
    _set(SessionStatus.signedIn);
  }

  Future<void> unlock() async {
    if (await biometric.authenticate('Mở ứng dụng')) {
      _set(SessionStatus.signedIn);
    }
  }

  Future<void> _signedIn(Map<String, dynamic>? response) async {
    final body = response!;
    api.token = body['token'] as String;
    await store.write(tokenKey, api.token!);
    final deviceToken = body['deviceToken'] as String?;
    if (deviceToken != null) {
      await store.write(deviceTokenKey, deviceToken);
    }
    user = UserInfo.fromJson(body['user'] as Map<String, dynamic>);
    await store.write(userTypeKey, user!.type.name);
    if (user!.mustChangePassword || body['mustChangePassword'] == true) {
      return _set(SessionStatus.mustChangePassword);
    }
    final offered = await store.read(biometricOfferedKey) == 'yes';
    _set(!offered && await biometric.isAvailable() ? SessionStatus.offerBiometric : SessionStatus.signedIn);
  }

  Future<String> _deviceId() async {
    final saved = await store.read(deviceIdKey);
    if (saved != null) return saved;
    final id = _uuidV4();
    await store.write(deviceIdKey, id);
    return id;
  }

  Future<void> _clear() async {
    api.token = null;
    user = null;
    await store.delete(tokenKey);
  }

  void _set(SessionStatus next) {
    status = next;
    notifyListeners();
  }

  static String _uuidV4() {
    final random = Random.secure();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final hex = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-'
        '${hex.substring(16, 20)}-${hex.substring(20)}';
  }
}
