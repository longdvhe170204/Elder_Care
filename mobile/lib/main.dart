import 'package:flutter/material.dart';

import 'common/api/api_client.dart';
import 'common/auth/biometric_auth.dart';
import 'common/auth/secure_store.dart';
import 'common/auth/session_controller.dart';
import 'resident/resident_root.dart';

/// Địa chỉ backend: `flutter run --dart-define=API_BASE_URL=http://<máy chủ>:8080`; mặc định là máy tính chạy máy ảo
/// Android.
const apiBaseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8080');

/// App chung (000 T006): mở đăng nhập rồi chuyển theo loại tài khoản. Hiện mới có phần người cao tuổi (002 T097).
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final session = SessionController(
    api: ApiClient(baseUrl: apiBaseUrl),
    store: const DeviceSecureStore(),
    biometric: DeviceBiometricAuth(),
  );
  runApp(ResidentRoot(session: session));
  await session.start();
}
