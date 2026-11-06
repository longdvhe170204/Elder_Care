import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/resident_login_qr/resident_login_qr_screen.dart';
import 'package:mobile/staff/staff_root.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../support/fake_backend.dart';

// 002 T096, T104 (US8; FR-069; bố cục duyệt 2026-11-06, cùng chữ với khối web duyệt 2026-11-02): Điều dưỡng chọn người cao
// tuổi trong phạm vi, ghi xác nhận của chính người đó (tự yêu cầu hay đồng ý, với nhân viên nào), tạo mã QR dùng một lần
// kèm đếm ngược; có máy đang gắn thì cảnh báo quét mã sẽ gỡ máy đó; hết hạn thì tạo mã mới với xác nhận điền sẵn. Mục
// "Thêm" của thanh dưới mở Phê duyệt và màn này (DESIGN.md 15.4).

const _residents = {
  'items': [
    {
      'id': 5,
      'profileCode': 'NCT-0005',
      'personalInfo': {'fullName': 'Nguyễn Thị Lan'},
      'status': 'IN_RESIDENCE',
    },
  ],
  'nextCursor': null,
};

void main() {
  late FakeBackend backend;
  late DateTime now;

  setUp(() {
    now = DateTime.utc(2026, 11, 6, 1);
    backend = FakeBackend()
      ..on('GET', '/residents', 200, _residents)
      ..on('GET', '/resident-accounts/5', 200, {
        'accountId': 41,
        'status': 'ACTIVE',
        'boundDevice': {'boundAt': '2026-11-01T02:30:00Z'},
      })
      ..on('POST', '/resident-accounts/5/login-qr-code', 201, {
        'code': 'QR-ABC',
        'expiresAt': now.add(const Duration(minutes: 5)).toIso8601String(),
      });
  });

  Future<void> pumpScreen(WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: ResidentLoginQrScreen(api: backend.client(), now: () => now),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> tapText(WidgetTester tester, String text) async {
    await tester.ensureVisible(find.text(text).first);
    await tester.pumpAndSettle();
    await tester.tap(find.text(text).first);
    await tester.pumpAndSettle();
  }

  Future<void> fillConsent(WidgetTester tester) async {
    await tapText(tester, 'Tự yêu cầu');
    await tester.enterText(find.widgetWithText(TextField, 'Mã hồ sơ nhân viên đã nghe'), ' 9003 ');
  }

  testWidgets('FR_069_coMayGan_canhBao_thieuXacNhan_khongGui', (tester) async {
    await pumpScreen(tester);
    await tapText(tester, 'Nguyễn Thị Lan');

    expect(find.text('Mã đăng nhập cho Nguyễn Thị Lan'), findsOneWidget);
    expect(find.text('Đang gắn máy từ 01/11/2026 09:30. Quét mã mới sẽ gỡ máy đó.'), findsOneWidget);

    await tapText(tester, 'Tạo mã QR');

    expect(find.text('Chọn người cao tuổi tự yêu cầu hay đồng ý'), findsOneWidget);
    expect(find.text('Nhập mã hồ sơ nhân viên'), findsOneWidget);
    expect(backend.calls('POST', '/resident-accounts/5/login-qr-code'), isEmpty);
  });

  testWidgets('FR_069_taoMa_hienQr_demNguoc_hetHan_taoMaMoiDienSanXacNhan', (tester) async {
    await pumpScreen(tester);
    await tapText(tester, 'Nguyễn Thị Lan');
    await fillConsent(tester);
    await tapText(tester, 'Tạo mã QR');

    final first = backend.calls('POST', '/resident-accounts/5/login-qr-code').single;
    expect(jsonDecode(first.body), {
      'consent': {'method': 'REQUESTED', 'withStaffId': 9003},
    });
    expect(first.headers['Idempotency-Key'], isNotEmpty);
    expect(tester.widget<QrImageView>(find.byType(QrImageView)), isNotNull);
    expect(find.bySemanticsLabel('Mã QR đăng nhập'), findsOneWidget);
    expect(find.text('Còn 5:00'), findsOneWidget);
    expect(find.text('Quét mã sẽ gỡ máy đang gắn.'), findsOneWidget);

    now = now.add(const Duration(seconds: 61));
    await tester.pump(const Duration(seconds: 1));
    expect(find.text('Còn 3:59'), findsOneWidget);

    now = now.add(const Duration(minutes: 4));
    await tester.pump(const Duration(seconds: 1));
    await tester.pumpAndSettle();
    expect(find.byType(QrImageView), findsNothing);
    expect(find.text('Mã đã hết hạn.'), findsOneWidget);

    await tapText(tester, 'Tạo mã mới');
    final staff = tester.widget<TextField>(find.widgetWithText(TextField, 'Mã hồ sơ nhân viên đã nghe'));
    expect(staff.controller!.text.trim(), '9003');
    await tapText(tester, 'Tạo mã QR');

    final calls = backend.calls('POST', '/resident-accounts/5/login-qr-code');
    expect(calls, hasLength(2));
    expect(calls[1].headers['Idempotency-Key'], isNot(first.headers['Idempotency-Key']));
  });

  testWidgets('FR_069_daKetThucLuuTru_409_baoCauBackend', (tester) async {
    backend.on('POST', '/resident-accounts/5/login-qr-code', 409, {
      'status': 409,
      'errorCode': 'RESIDENCY_ENDED',
      'detail': 'Người cao tuổi đã kết thúc lưu trú',
    });
    await pumpScreen(tester);
    await tapText(tester, 'Nguyễn Thị Lan');
    await fillConsent(tester);
    await tapText(tester, 'Tạo mã QR');

    expect(find.text('Người cao tuổi đã kết thúc lưu trú'), findsOneWidget);
    expect(find.byType(QrImageView), findsNothing);
  });

  testWidgets('FR_069_chuaCoTaiKhoan_404_baoKhongChoTaoMa', (tester) async {
    backend.on('GET', '/resident-accounts/5', 404, {'status': 404, 'errorCode': 'NOT_FOUND'});
    await pumpScreen(tester);
    await tapText(tester, 'Nguyễn Thị Lan');

    expect(
      find.text(
        'Người cao tuổi này chưa có tài khoản đăng nhập trên app. Hành chính tạo tài khoản theo lời người '
        'cao tuổi.',
      ),
      findsOneWidget,
    );
    expect(find.text('Tạo mã QR'), findsNothing);
  });

  testWidgets('DESIGN_15_4_mucThem_moPheDuyetVaMaDangNhap', (tester) async {
    final s = session(backend, MemoryStore(), FakeBiometric());
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: StaffRoot(session: s, queue: memoryQueue(s)),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.widgetWithText(NavigationDestination, 'Phê duyệt'), findsNothing);
    await tester.tap(find.widgetWithText(NavigationDestination, 'Thêm'));
    await tester.pumpAndSettle();
    expect(find.text('Phê duyệt'), findsOneWidget);

    await tester.tap(find.text('Mã đăng nhập cho người cao tuổi'));
    await tester.pumpAndSettle();
    expect(find.text('Nguyễn Thị Lan'), findsOneWidget);

    await tester.tap(find.byTooltip('Quay lại'));
    await tester.pumpAndSettle();
    expect(find.text('Mã đăng nhập cho người cao tuổi'), findsOneWidget);
  });
}
