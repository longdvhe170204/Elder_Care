import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Kho khóa, giá trị của phiên (002 T001, T097): token phiên, token thiết bị, mã thiết bị, lựa chọn mở khóa sinh trắc
/// học. Bản chạy thật dùng `flutter_secure_storage`; test dùng [MemoryStore].
abstract class SecureStore {
  Future<String?> read(String key);

  Future<void> write(String key, String value);

  Future<void> delete(String key);
}

class DeviceSecureStore implements SecureStore {
  const DeviceSecureStore([this._storage = const FlutterSecureStorage()]);

  final FlutterSecureStorage _storage;

  @override
  Future<String?> read(String key) => _storage.read(key: key);

  @override
  Future<void> write(String key, String value) => _storage.write(key: key, value: value);

  @override
  Future<void> delete(String key) => _storage.delete(key: key);
}

class MemoryStore implements SecureStore {
  final Map<String, String> values = {};

  @override
  Future<String?> read(String key) async => values[key];

  @override
  Future<void> write(String key, String value) async => values[key] = value;

  @override
  Future<void> delete(String key) async => values.remove(key);
}
