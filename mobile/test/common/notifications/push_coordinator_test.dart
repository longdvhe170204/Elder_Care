import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';
import 'package:mobile/common/notifications/push_coordinator.dart';
import 'package:mobile/common/notifications/push_messaging.dart';

import '../../support/fake_backend.dart';
import '../../support/fake_push.dart';

// 009 T031, T042 (FR-016, Q-305): nhận đẩy: đăng ký token khi đã đăng nhập, app theo loại tài khoản, cập nhật khi token đổi;
// có thông báo thì báo màn tải lại; bấm thông báo thì giữ mã để màn mở, kể cả khi app mở từ thông báo.

void main() {
  late FakeBackend backend;
  late MemoryStore store;

  setUp(() {
    backend = FakeBackend();
    store = MemoryStore();
    backend.on('POST', '/push-devices', 200);
    backend.on('GET', '/me', 200, user('FAMILY_MEMBER'));
  });

  Future<PushCoordinator> started(FakePush push) async {
    await store.write(SessionController.tokenKey, 'session-1');
    final s = session(backend, store, FakeBiometric(available: false));
    final coordinator = PushCoordinator(push);
    await coordinator.start(s);
    await s.start();
    await pumpEventQueue();
    return coordinator;
  }

  test('FR_016_signedIn_registersToken_appByAccountType', () async {
    final push = FakePush();
    await started(push);

    final call = backend.calls('POST', '/push-devices').single;
    expect(jsonDecode(call.body), {'app': 'FAMILY_APP', 'fcmToken': 'token-1'});
    expect(call.headers['Idempotency-Key'], isNotEmpty);
    expect(push.permissionRequests, 1);
  });

  test('FR_016_noToken_registersNothing', () async {
    await started(FakePush(currentToken: null));

    expect(backend.calls('POST', '/push-devices'), isEmpty);
  });

  test('FR_016_tokenRefreshed_registersNewToken', () async {
    final push = FakePush();
    await started(push);

    push.refresh.add('token-2');
    await pumpEventQueue();

    final calls = backend.calls('POST', '/push-devices');
    expect(calls, hasLength(2));
    expect((jsonDecode(calls.last.body) as Map<String, dynamic>)['fcmToken'], 'token-2');
  });

  test('FR_016_registrationFails_doesNotBreakApp', () async {
    backend.on('POST', '/push-devices', 500);

    await expectLater(started(FakePush()), completes);
  });

  test('T031_foregroundMessage_notifiesScreens', () async {
    final push = FakePush();
    final coordinator = await started(push);
    var notified = 0;
    coordinator.addListener(() => notified++);

    push.received.add(null);
    await pumpEventQueue();

    expect(notified, 1);
    expect(coordinator.takeOpened(), isNull);
  });

  test('T022_urgentMessage_reportedOnce_otherLevelsNot', () async {
    final push = FakePush();
    final coordinator = await started(push);

    push.received.add('LOW');
    await pumpEventQueue();
    expect(coordinator.takeUrgent(), isFalse);

    push.received.add('URGENT');
    await pumpEventQueue();
    expect(coordinator.takeUrgent(), isTrue);
    expect(coordinator.takeUrgent(), isFalse);
  });

  test('T031_tappedNotification_kept_takenOnce', () async {
    final push = FakePush();
    final coordinator = await started(push);

    push.opened.add(42);
    await pumpEventQueue();

    expect(coordinator.takeOpened(), 42);
    expect(coordinator.takeOpened(), isNull);
  });

  test('T031_appOpenedFromNotification_kept', () async {
    final coordinator = await started(FakePush(initial: 7));

    expect(coordinator.takeOpened(), 7);
  });

  test('T031_firebaseNotConfigured_noPush_noRegistration', () async {
    final coordinator = PushCoordinator(const NoPushMessaging());
    final s = session(backend, store, FakeBiometric(available: false));

    await coordinator.start(s);
    await s.start();
    await pumpEventQueue();

    expect(backend.calls('POST', '/push-devices'), isEmpty);
  });
}
