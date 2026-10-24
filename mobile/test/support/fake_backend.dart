import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/auth/biometric_auth.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';

/// Backend giả cho test app: trả theo (phương thức, đường dẫn sau `/api/v1`), ghi lại mọi yêu cầu.
class FakeBackend {
  final Map<String, http.Response Function(http.Request)> _routes = {};
  final List<http.Request> requests = [];

  /// Mất mạng: mọi yêu cầu lỗi kết nối.
  bool down = false;

  void on(String method, String path, int status, [Object? body]) {
    _routes['$method $path'] = (_) => http.Response.bytes(
      body == null ? const [] : utf8.encode(jsonEncode(body)),
      status,
      headers: {'content-type': status >= 400 ? 'application/problem+json' : 'application/json'},
    );
  }

  List<http.Request> calls(String method, String path) =>
      requests.where((r) => r.method == method && r.url.path == '/api/v1$path').toList();

  ApiClient client() => ApiClient(
    baseUrl: 'http://backend.test',
    httpClient: MockClient((request) async {
      requests.add(request);
      if (down) throw http.ClientException('offline');
      final route = _routes['${request.method} ${request.url.path.replaceFirst('/api/v1', '')}'];
      return route == null ? http.Response('', 404) : route(request);
    }),
  );
}

class FakeBiometric implements BiometricAuth {
  FakeBiometric({this.available = true, this.succeeds = true});

  bool available;
  bool succeeds;
  int prompts = 0;

  @override
  Future<bool> isAvailable() async => available;

  @override
  Future<bool> authenticate(String reason) async {
    prompts++;
    return succeeds;
  }
}

Map<String, dynamic> residentUser({bool mustChangePassword = false}) => {
  'accountId': 41,
  'type': 'RESIDENT',
  'fullName': 'Nguyễn Thị Hoa',
  'roles': ['AC-16'],
  'mustChangePassword': mustChangePassword,
};

Map<String, dynamic> loginResponse({bool mustChangePassword = false}) => {
  'token': 'session-1',
  'deviceToken': 'device-token-1',
  'mustChangePassword': mustChangePassword,
  'user': residentUser(mustChangePassword: mustChangePassword),
};

SessionController session(FakeBackend backend, MemoryStore store, FakeBiometric biometric) =>
    SessionController(api: backend.client(), store: store, biometric: biometric);
