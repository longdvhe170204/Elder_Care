import 'dart:async';

import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';
import 'package:mobile/common/notifications/push_coordinator.dart';
import 'package:mobile/common/notifications/push_messaging.dart';

import 'fake_backend.dart';

/// Bản giả nhận đẩy: test tự phát sự kiện, không cần Firebase.
class FakePush implements PushMessaging {
  FakePush({this.currentToken = 'token-1', this.initial});

  String? currentToken;
  int? initial;
  int permissionRequests = 0;
  final refresh = StreamController<String>.broadcast();
  final received = StreamController<String?>.broadcast();
  final opened = StreamController<int>.broadcast();

  @override
  Future<bool> start() async => true;

  @override
  Future<void> requestPermission() async => permissionRequests++;

  @override
  Future<String?> token() async => currentToken;

  @override
  Stream<String> get tokenRefresh => refresh.stream;

  @override
  Stream<String?> get onMessage => received.stream;

  @override
  Stream<int> get onOpened => opened.stream;

  @override
  Future<int?> initialOpened() async => initial;
}

/// Điều phối đẩy nối với phiên đã đăng nhập, cho test màn: `push.opened.add(id)` như người dùng bấm thông báo.
Future<PushCoordinator> startedPush(FakeBackend backend, FakePush push, String accountType) async {
  backend.on('POST', '/push-devices', 200);
  backend.on('GET', '/me', 200, user(accountType));
  final store = MemoryStore();
  await store.write(SessionController.tokenKey, 'session-1');
  final s = session(backend, store, FakeBiometric(available: false));
  final coordinator = PushCoordinator(push);
  await coordinator.start(s);
  await s.start();
  return coordinator;
}
