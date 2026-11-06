import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/app.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/auth/session_controller.dart';
import 'package:mobile/common/auth/session_guard.dart';

import '../../support/fake_backend.dart';

// 002 T047c (FR-013a, FR-041a; DESIGN.md 9.1, 9.3, 15.5; bố cục duyệt 2026-11-06): app phần nhân viên báo trước 2 phút khi
// sắp hết phiên theo `sessionIdleTimeoutSeconds` của GET /me; hết giờ hoặc backend trả 401 SESSION_EXPIRED thì che màn bằng
// màn đăng nhập lại đúng tài khoản; đăng nhập lại xong phần đang nhập còn nguyên.

Map<String, dynamic> staffMe(int? timeout) => {...user('STAFF'), 'sessionIdleTimeoutSeconds': timeout};

void main() {
  late FakeBackend backend;
  late MemoryStore store;
  late DateTime now;

  setUp(() {
    backend = FakeBackend();
    store = MemoryStore()
      ..values[SessionController.tokenKey] = 'staff-1'
      ..values[SessionController.usernameKey] = 'dieu-duong-1';
    now = DateTime.utc(2026, 11, 6, 8);
  });

  Future<SessionController> pumpGuard(WidgetTester tester, {int? timeout = 300}) async {
    backend.on('GET', '/me', 200, staffMe(timeout));
    backend.on('GET', '/residents', 401, {'status': 401, 'errorCode': 'SESSION_EXPIRED'});
    final s = session(backend, store, FakeBiometric());
    await s.start();
    await tester.pumpWidget(
      MaterialApp(
        home: SessionGuard(
          session: s,
          now: () => now,
          child: Scaffold(
            body: Column(
              children: [
                const TextField(decoration: InputDecoration(labelText: 'Ghi chú')),
                FilledButton(
                  onPressed: () => s.api.get('/residents').catchError((_) => null),
                  child: const Text('Tải danh sách'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    return s;
  }

  Future<void> wait(WidgetTester tester, int seconds) async {
    now = now.add(Duration(seconds: seconds));
    await tester.pump(const Duration(seconds: 1));
    await tester.pumpAndSettle();
  }

  testWidgets('FR_013a_con2Phut_baoSapHet_tiepTucLamViec_goiMe_demLai', (tester) async {
    await pumpGuard(tester);
    expect(backend.calls('GET', '/me'), hasLength(2)); // start() và guard đọc thời hạn

    await wait(tester, 170);
    expect(find.text('Phiên đăng nhập sắp hết'), findsNothing);

    await wait(tester, 11);
    expect(find.text('Phiên đăng nhập sắp hết'), findsOneWidget);
    expect(find.textContaining('1:59'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, 'Tiếp tục làm việc'));
    await tester.pumpAndSettle();
    expect(backend.calls('GET', '/me'), hasLength(3));
    expect(find.text('Phiên đăng nhập sắp hết'), findsNothing);

    await wait(tester, 170);
    expect(find.text('Phiên đăng nhập sắp hết'), findsNothing);
  });

  testWidgets('FR_013a_hetGio_dangNhapLaiDungTaiKhoan_phanDangNhapConNguyen', (tester) async {
    backend.on('POST', '/auth/login', 200, {'token': 'staff-2', 'user': user('STAFF')});
    final s = await pumpGuard(tester);
    await tester.enterText(find.widgetWithText(TextField, 'Ghi chú'), 'đo huyết áp phòng 203');

    await wait(tester, 301);
    expect(find.text('Phiên đăng nhập đã hết'), findsOneWidget);
    final username = tester.widget<TextField>(find.widgetWithText(TextField, 'Tên đăng nhập'));
    expect(username.controller!.text, 'dieu-duong-1');
    expect(username.readOnly, isTrue);

    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu'), 'matkhau123');
    await tester.tap(find.widgetWithText(FilledButton, 'Đăng nhập lại'));
    await tester.pumpAndSettle();

    final call = backend.calls('POST', '/auth/login').single;
    expect(jsonDecode(call.body), {'username': 'dieu-duong-1', 'password': 'matkhau123', 'channel': 'MOBILE_APP'});
    expect(s.api.token, 'staff-2');
    expect(store.values[SessionController.tokenKey], 'staff-2');
    expect(find.text('Phiên đăng nhập đã hết'), findsNothing);
    expect(find.text('đo huyết áp phòng 203'), findsOneWidget);
  });

  testWidgets('FR_013a_backendTra401SessionExpired_hienManDangNhapLai', (tester) async {
    await pumpGuard(tester);

    await tester.tap(find.text('Tải danh sách'));
    await tester.pumpAndSettle();

    expect(find.text('Phiên đăng nhập đã hết'), findsOneWidget);
  });

  testWidgets('FR_013a_saiMatKhau_oLaiManDangNhapLai_baoCauBackend', (tester) async {
    backend.on('POST', '/auth/login', 401, {
      'status': 401,
      'errorCode': 'LOGIN_FAILED',
      'detail': 'Tên đăng nhập hoặc mật khẩu không đúng',
    });
    await pumpGuard(tester);
    await wait(tester, 301);

    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu'), 'sai');
    await tester.tap(find.widgetWithText(FilledButton, 'Đăng nhập lại'));
    await tester.pumpAndSettle();

    expect(find.text('Phiên đăng nhập đã hết'), findsOneWidget);
    expect(find.text('Tên đăng nhập hoặc mật khẩu không đúng'), findsOneWidget);
  });

  testWidgets('FR_041a_dangXuatKhiHetPhien_banGhiNgoaiTuyenVanCon', (tester) async {
    backend.on('POST', '/auth/logout', 401, {'status': 401, 'errorCode': 'SESSION_EXPIRED'});
    final s = await pumpGuard(tester);
    final queue = memoryQueue(s);
    await tester.runAsync(() => queue.enqueue(accountId: 7, recordType: 'CARE_RESULT', content: const {'note': 'x'}));
    await wait(tester, 301);

    await tester.tap(find.widgetWithText(TextButton, 'Đăng xuất'));
    await tester.pumpAndSettle();

    expect(s.status, SessionStatus.signedOut);
    expect(await tester.runAsync(() => queue.unsynced(7)), hasLength(1));
  });

  testWidgets('FR_013a_khongCoThoiHan_khongBao', (tester) async {
    await pumpGuard(tester, timeout: null);

    await wait(tester, 3600);

    expect(find.text('Phiên đăng nhập sắp hết'), findsNothing);
    expect(find.text('Phiên đăng nhập đã hết'), findsNothing);
  });

  testWidgets('T047c_appNhanVien_boc_trongSessionGuard', (tester) async {
    backend.on('GET', '/me', 200, staffMe(300));
    final s = session(backend, store, FakeBiometric());
    await s.start();
    await tester.pumpWidget(ElderCareApp(session: s, queue: memoryQueue(s)));
    await tester.pumpAndSettle();

    expect(find.byType(SessionGuard), findsOneWidget);
  });
}
