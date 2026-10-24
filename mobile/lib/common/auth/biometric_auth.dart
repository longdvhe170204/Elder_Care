import 'package:local_auth/local_auth.dart';

/// Mở khóa bằng vân tay hoặc khuôn mặt của máy (002 FR-074); không có mã PIN. Test dùng bản giả.
abstract class BiometricAuth {
  /// Máy có và đã cài vân tay hoặc khuôn mặt.
  Future<bool> isAvailable();

  Future<bool> authenticate(String reason);
}

class DeviceBiometricAuth implements BiometricAuth {
  DeviceBiometricAuth([LocalAuthentication? auth]) : _auth = auth ?? LocalAuthentication();

  final LocalAuthentication _auth;

  @override
  Future<bool> isAvailable() async {
    try {
      return await _auth.isDeviceSupported() && (await _auth.getAvailableBiometrics()).isNotEmpty;
    } on Exception {
      return false;
    }
  }

  @override
  Future<bool> authenticate(String reason) async {
    try {
      return await _auth.authenticate(localizedReason: reason, biometricOnly: true);
    } on Exception {
      return false;
    }
  }
}
