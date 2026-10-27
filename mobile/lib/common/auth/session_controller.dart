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
/// thiết bị UUID tạo một lần trên máy. Một máy một phiên (FR-014a): nhân viên đăng nhập trên máy dùng chung kết thúc
/// phiên của người trước ở backend. Phiên của người cao tuổi không hết theo thời gian (002 FR-010, FR-013a), nên app chỉ
/// quay về màn đăng nhập khi backend trả 401.
class SessionController extends ChangeNotifier {
  SessionController({required this.api, required this.store, required this.biometric});

  static const tokenKey = 'session.token';
  static const deviceTokenKey = 'session.deviceToken';
  static const deviceIdKey = 'device.id';
  static const userTypeKey = 'session.userType';
  static const accountIdKey = 'session.accountId';
  static const biometricKey = 'resident.biometric';
  static const biometricOfferedKey = 'resident.biometricOffered';

  final ApiClient api;
  final SecureStore store;
  final BiometricAuth biometric;

  SessionStatus status = SessionStatus.starting;
  UserInfo? user;

  /// Máy đã gắn với tài khoản người cao tuổi (có token thiết bị): chỉ đăng nhập lại bằng mã QR, không đăng nhập được
  /// tài khoản khác (002 FR-014a, Q-370).
  bool boundToResident = false;

  /// Câu báo cho màn đăng nhập, ví dụ sau khi đổi mật khẩu.
  String? notice;

  /// Mở app: lấy mã thiết bị, đọc phiên đã lưu và kiểm lại với backend.
  Future<void> start() async {
    api.deviceId = await _deviceId();
    boundToResident = await store.read(deviceTokenKey) != null;
    api.token = await store.read(tokenKey);
    if (api.token == null) {
      return _set(SessionStatus.signedOut);
    }
    try {
      user = UserInfo.fromJson((await api.get('/me'))!);
      await _remember(user!);
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
        // Hàng đợi ngoại tuyến gắn bản ghi với tài khoản của phiên (002 FR-014a)
        accountId: int.tryParse(await store.read(accountIdKey) ?? '') ?? 0,
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

  /// Nhân viên, người thân đăng nhập ở màn đầu chung (002 T046, T047; FR-014a): kênh `MOBILE_APP`, backend gán kênh
  /// phiên STAFF_APP hoặc FAMILY_APP theo loại tài khoản (đổi api-002 2026-10-28; FR-013a).
  Future<void> appLogin(String username, String password) async {
    await _signedIn(
      await api.post('/auth/login', {'username': username, 'password': password, 'channel': 'MOBILE_APP'}),
    );
  }

  /// Đăng xuất (002 FR-013): báo backend kết thúc phiên nếu còn mạng, rồi về màn đăng nhập. [notice] là câu báo cho màn
  /// đăng nhập, ví dụ khi phiên bị kết thúc vì tài khoản không còn Hoạt động.
  Future<void> signOut({String? notice, bool callBackend = true}) async {
    if (callBackend) {
      try {
        await api.post('/auth/logout', const {});
      } on ApiProblem {
        // Phiên đã hết ở backend: vẫn đăng xuất trên máy
      } on ApiUnreachable {
        // Mất mạng: phiên ở backend kết thúc khi người khác đăng nhập trên máy này (FR-014a)
      }
    }
    await _clear();
    this.notice = notice;
    _set(SessionStatus.signedOut);
  }

  /// Đổi mật khẩu bắt buộc lần đầu (002 FR-006). Tài khoản người cao tuổi: backend kết thúc mọi phiên (BR-M15-24), nên
  /// app về màn đăng nhập; nhân viên, người thân giữ phiên và vào app.
  Future<void> changePassword(String currentPassword, String newPassword) async {
    await api.post('/auth/change-password', {'currentPassword': currentPassword, 'newPassword': newPassword});
    final current = user!;
    if (current.type != AccountType.resident) {
      user = UserInfo(
        accountId: current.accountId,
        type: current.type,
        fullName: current.fullName,
        mustChangePassword: false,
      );
      return _set(SessionStatus.signedIn);
    }
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
      boundToResident = true;
    }
    user = UserInfo.fromJson(body['user'] as Map<String, dynamic>);
    await _remember(user!);
    if (user!.mustChangePassword || body['mustChangePassword'] == true) {
      return _set(SessionStatus.mustChangePassword);
    }
    // Mở bằng sinh trắc học chỉ cho người cao tuổi (002 FR-074); máy dùng chung của nhân viên không giữ khóa riêng
    if (user!.type != AccountType.resident) {
      return _set(SessionStatus.signedIn);
    }
    final offered = await store.read(biometricOfferedKey) == 'yes';
    _set(!offered && await biometric.isAvailable() ? SessionStatus.offerBiometric : SessionStatus.signedIn);
  }

  Future<void> _remember(UserInfo info) async {
    await store.write(userTypeKey, info.type.name);
    await store.write(accountIdKey, info.accountId.toString());
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
