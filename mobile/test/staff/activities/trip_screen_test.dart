import 'dart:convert';

import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/activities/trip_list_screen.dart';

import '../../support/fake_backend.dart';

/// 014 T018 (FR-039, FR-040, FR-044; Q24, Q-01): màn chuyến trên app; điểm danh rời viện, về lưu phong bì
/// `M04.TRIP_DEPARTURE`, `M04.TRIP_RETURN` qua hàng đợi ngoại tuyến chung, mất mạng thì giữ trên máy.
void main() {
  late FakeBackend backend;
  late OfflineDatabase db;
  late OfflineQueue queue;
  late bool writable;

  Map<String, dynamic> participant(String status) => {
    'id': 91,
    'residentId': 701,
    'status': status,
    'reason': null,
    'info': {
      'medicationDoses': {
        'availability': 'AVAILABLE',
        'items': [
          {'doseKey': 'd1', 'medicationName': 'Amlodipin', 'strength': '5 mg', 'scheduledTime': '2026-11-05T02:00:00Z'},
        ],
      },
      'carriedMedication': {'availability': 'NO_DATA'},
      'health': {'availability': 'NOT_PERMITTED'},
      'careNotes': {'availability': 'AVAILABLE', 'text': 'Đi chậm, cần người dìu'},
      'primaryContact': {'availability': 'AVAILABLE', 'fullName': 'Nguyễn Văn Nam', 'phoneNumber': '0900000000'},
      'emergencyCard': {'availability': 'SOURCE_UNAVAILABLE'},
    },
  };

  void trip(String status, String participantStatus) => backend.on('GET', '/trips/41', 200, {
    'id': 41,
    'status': status,
    'destination': 'Chùa',
    'transport': 'Xe thuê',
    'facilityVehicle': false,
    'plannedDepartureAt': '2026-11-05T00:00:00Z',
    'plannedReturnAt': '2026-11-05T06:00:00Z',
    'leaderId': 21,
    'companionIds': [22],
    'participants': [participant(participantStatus)],
  });

  setUp(() {
    backend = FakeBackend();
    db = OfflineDatabase(NativeDatabase.memory());
    queue = OfflineQueue(db, backend.client(), newKey: () => 'trip-key');
    writable = true;
  });
  tearDown(() => db.close());

  Future<void> open(WidgetTester t, {double textScale = 1}) async {
    backend.on('GET', '/me/permissions', 200, [
      for (final op in ['VIEW', if (writable) 'CREATE']) {'function': 'ACTIVITY_OUTING', 'operation': op},
    ]);
    backend.on('GET', '/activity-sessions', 200, [
      {'id': 41, 'activityId': 3, 'isTrip': true, 'startsAt': '2026-11-05T00:00:00Z', 'status': 'SCHEDULED'},
      {'id': 42, 'activityId': 4, 'isTrip': false, 'startsAt': '2026-11-05T00:00:00Z', 'status': 'SCHEDULED'},
    ]);
    backend.on('GET', '/activities', 200, [
      {'id': 3, 'name': 'Tham quan'},
    ]);
    backend.on('GET', '/residents', 200, {
      'items': [
        {'id': 701, 'fullName': '[Người kiểm thử]'},
      ],
    });
    await t.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(textScale)),
          child: child!,
        ),
        home: TripListScreen(api: backend.client(), queue: queue, accountId: 7, date: '2026-11-05'),
      ),
    );
    await t.pumpAndSettle();
    expect(find.textContaining('Buổi 42'), findsNothing);
    await t.tap(find.text('Tham quan'));
    await t.pumpAndSettle();
  }

  Future<void> choose(WidgetTester t, String button, String confirm) async {
    await t.ensureVisible(find.byType(CheckboxListTile));
    await t.tap(find.byType(CheckboxListTile));
    await t.pumpAndSettle();
    await t.tap(find.widgetWithText(FilledButton, button));
    await t.pumpAndSettle();
    await t.tap(find.widgetWithText(FilledButton, confirm));
    await t.pumpAndSettle();
  }

  Map<String, dynamic> envelope() {
    final sync = jsonDecode(backend.calls('POST', '/sync').last.body) as Map<String, dynamic>;
    return (sync['records'] as List).single as Map<String, dynamic>;
  }

  testWidgets('FR_039_danhSachChuyenMoChiTietVaKhongTuGuiLenh', (t) async {
    trip('SCHEDULED', 'ELIGIBLE');
    await open(t);
    expect(find.text('Điểm đến: Chùa'), findsOneWidget);
    expect(find.text('Được đi'), findsOneWidget);
    expect(backend.calls('POST', '/sync'), isEmpty);
  });

  testWidgets('FR_039_nguoiThamGia_sauPhanThongTin_theoQuyenVaNguon', (t) async {
    trip('SCHEDULED', 'ELIGIBLE');
    await open(t);
    await t.scrollUntilVisible(find.text('Đi chậm, cần người dìu'), 200);
    expect(find.text('Amlodipin 5 mg, 09:00'), findsOneWidget);
    expect(find.text('Chưa giao thuốc mang theo'), findsOneWidget);
    expect(find.text('Không có quyền xem'), findsOneWidget);
    expect(find.text('Nguyễn Văn Nam, 0900000000'), findsOneWidget);
    expect(find.text('Chưa lấy được dữ liệu từ nguồn'), findsOneWidget);
  });

  testWidgets('FR_040_roiVienQuaHangDoi_biTuChoiGiuLuaChon', (t) async {
    trip('SCHEDULED', 'ELIGIBLE');
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': 'trip-key', 'result': 'REJECTED', 'reason': 'Chưa chuẩn bị thuốc mang theo'},
      ],
    });
    await open(t);
    await choose(t, 'Điểm danh rời viện', 'Xác nhận rời viện');

    expect(find.text('Chưa chuẩn bị thuốc mang theo'), findsOneWidget);
    expect(t.widget<CheckboxListTile>(find.byType(CheckboxListTile)).value, isTrue);
    expect(backend.calls('POST', '/trips/41/depart'), isEmpty);
    expect(envelope()['recordType'], 'M04.TRIP_DEPARTURE');
    expect(envelope()['content'], {
      'tripId': 41,
      'residentIds': [701],
    });
  });

  testWidgets('Q24_matMang_roiVienLuuTrenMay', (t) async {
    trip('SCHEDULED', 'ELIGIBLE');
    await open(t);
    backend.down = true;
    await choose(t, 'Điểm danh rời viện', 'Xác nhận rời viện');

    expect(find.text('Đã lưu trên máy, sẽ gửi khi có mạng'), findsOneWidget);
    final pending = await queue.unsynced(7);
    expect(pending.single.recordType, 'M04.TRIP_DEPARTURE');
    expect(jsonDecode(pending.single.content), {
      'tripId': 41,
      'residentIds': [701],
    });
  });

  testWidgets('Q24_FR_044_diemDanhVeQuaHangDoi', (t) async {
    trip('IN_PROGRESS', 'DEPARTED');
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': 'trip-key', 'result': 'ACCEPTED'},
      ],
    });
    await open(t);
    expect(find.widgetWithText(FilledButton, 'Điểm danh rời viện'), findsNothing);
    await choose(t, 'Điểm danh về', 'Xác nhận về');

    expect(find.text('Đã điểm danh về'), findsOneWidget);
    expect(envelope()['recordType'], 'M04.TRIP_RETURN');
    expect(envelope()['content'], {
      'tripId': 41,
      'residentIds': [701],
    });
  });

  testWidgets('FR_049_quyenXemKhongCoLenhRoiVien', (t) async {
    writable = false;
    trip('SCHEDULED', 'ELIGIBLE');
    await open(t);
    expect(find.widgetWithText(FilledButton, 'Điểm danh rời viện'), findsNothing);
    expect(find.byType(CheckboxListTile), findsNothing);
  });

  testWidgets('NFR_10_dienThoaiChu200PhanTramVanDocVaChonDuocNguoi', (t) async {
    t.view.devicePixelRatio = 1;
    t.view.physicalSize = const Size(360, 800);
    addTearDown(t.view.resetPhysicalSize);
    addTearDown(t.view.resetDevicePixelRatio);
    trip('SCHEDULED', 'ELIGIBLE');
    await open(t, textScale: 2);
    await t.scrollUntilVisible(find.byType(CheckboxListTile), 200);
    await t.ensureVisible(find.byType(CheckboxListTile));
    await t.pumpAndSettle();
    await t.tap(find.byType(CheckboxListTile));
    await t.pumpAndSettle();
    expect(t.takeException(), isNull);
    expect(t.widget<CheckboxListTile>(find.byType(CheckboxListTile)).value, isTrue);
    expect(find.widgetWithText(FilledButton, 'Điểm danh rời viện'), findsOneWidget);
  });
}
