import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/staff/care_tasks/day_care_attendance_gateway.dart';
import 'package:mobile/staff/care_tasks/day_care_attendance_screen.dart';

class FixtureAttendance implements DayCareAttendanceGateway {
  Future<DayCareAttendanceData> Function(String)? loader;
  bool writable = true, fail = false;
  final keys = <String>[];
  int? leftId, pickupId;
  @override
  Future<DayCareAttendanceData> load(String date) async => loader != null
      ? loader!(date)
      : DayCareAttendanceData(
          rows: [
            {'id': 5, 'residentId': 77, 'status': 'PRESENT', 'adHocSession': true},
          ],
          residents: [
            {'id': 77, 'fullName': '[Người kiểm thử]'},
          ],
          canCreate: writable,
        );
  @override
  Future<void> checkIn(int resident, String key) async {
    keys.add(key);
    if (fail) throw ApiProblem(409, 'ATTENDANCE_STATE_CONFLICT', 'Không thể điểm danh.');
  }

  @override
  Future<void> checkOut(int id, int pickup, String key) async {
    leftId = id;
    pickupId = pickup;
  }

  @override
  Future<void> correct(int id, String at, String reason, String key) async {}
}

void main() {
  Future<void> show(WidgetTester t, FixtureAttendance gateway) async {
    await t.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: DayCareAttendanceScreen(gateway: gateway, date: '2026-11-01'),
        ),
      ),
    );
    await t.pumpAndSettle();
  }

  testWidgets('FR_012_hienCoMatVaPhatSinhKhongTuGuiLenh', (t) async {
    final g = FixtureAttendance();
    await show(t, g);
    expect(find.text('Có mặt'), findsOneWidget);
    expect(find.text('Buổi phát sinh'), findsOneWidget);
    expect(g.keys, isEmpty);
  });
  testWidgets('FR_049_chiXemKhongHienNutDiemDanh', (t) async {
    final g = FixtureAttendance()..writable = false;
    await show(t, g);
    expect(find.text('Có mặt'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Điểm danh đến'), findsNothing);
  });
  testWidgets('FR_012_doiNgayKhongHienLaiTrangThaiCuKhiTaiHoacLoi', (t) async {
    final g = FixtureAttendance();
    await show(t, g);
    final pending = Completer<DayCareAttendanceData>();
    g.loader = (_) => pending.future;
    await t.enterText(find.widgetWithText(TextField, 'Ngày xem trạng thái (dd/MM/yyyy)'), '02/11/2026');
    await t.tap(find.text('Xem trạng thái'));
    await t.pump();
    expect(find.text('Có mặt'), findsNothing);
    pending.completeError(ApiProblem(403, 'PERMISSION_DENIED', 'Bạn không có quyền xem điểm danh.'));
    await t.pumpAndSettle();
    expect(find.text('Có mặt'), findsNothing);
    expect(find.text('Bạn không có quyền xem điểm danh.'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Điểm danh đến'), findsNothing);
  });
  testWidgets('BR_M04_02_loiGiuNguoiDaChonVaKhoaThuLai', (t) async {
    final g = FixtureAttendance()..fail = true;
    await show(t, g);
    await t.tap(find.byKey(const Key('attendance-resident')));
    await t.pumpAndSettle();
    await t.tap(find.text('[Người kiểm thử]').last);
    await t.pumpAndSettle();
    await t.tap(find.widgetWithText(FilledButton, 'Điểm danh đến'));
    await t.pumpAndSettle();
    expect(find.text('Không thể điểm danh.'), findsOneWidget);
    await t.tap(find.widgetWithText(FilledButton, 'Điểm danh đến'));
    await t.pumpAndSettle();
    expect(g.keys, hasLength(2));
    expect(g.keys[0], g.keys[1]);
  });
}
