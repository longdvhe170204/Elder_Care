import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/activities/trip_list_screen.dart';

void main() {
  late List<http.Request> commands;
  late bool writable;
  late ApiClient api;
  setUp(() {
    commands = [];
    writable = true;
    api = ApiClient(
      baseUrl: 'http://fixture',
      httpClient: MockClient((r) async {
        Object body;
        switch (r.url.path) {
          case '/api/v1/me/permissions':
            body = [
              for (final op in ['VIEW', if (writable) 'CREATE']) {'function': 'ACTIVITY_OUTING', 'operation': op},
            ];
          case '/api/v1/activity-sessions':
            body = [
              {'id': 41, 'activityId': 3, 'isTrip': true, 'startsAt': '2026-11-05T00:00:00Z', 'status': 'SCHEDULED'},
              {'id': 42, 'activityId': 4, 'isTrip': false, 'startsAt': '2026-11-05T00:00:00Z', 'status': 'SCHEDULED'},
            ];
          case '/api/v1/activities':
            body = [
              {'id': 3, 'name': 'Tham quan'},
            ];
          case '/api/v1/residents':
            body = {
              'items': [
                {'id': 701, 'fullName': '[Người kiểm thử]'},
              ],
            };
          case '/api/v1/trips/41':
            body = {
              'id': 41,
              'status': 'SCHEDULED',
              'destination': 'Chùa',
              'transport': 'Xe thuê',
              'facilityVehicle': false,
              'plannedDepartureAt': '2026-11-05T00:00:00Z',
              'plannedReturnAt': '2026-11-05T06:00:00Z',
              'leaderId': 21,
              'companionIds': [22],
              'participants': [
                {'id': 91, 'residentId': 701, 'status': 'ELIGIBLE', 'reason': null},
              ],
            };
          case '/api/v1/trips/41/depart':
            commands.add(r);
            return http.Response(
              jsonEncode({'detail': 'Chưa chuẩn bị thuốc mang theo', 'errorCode': 'MEDICATION_NOT_HANDLED'}),
              409,
              headers: {'content-type': 'application/problem+json; charset=utf-8'},
            );
          default:
            throw StateError('Không mong đợi ${r.method} ${r.url}');
        }
        return http.Response(jsonEncode(body), 200, headers: {'content-type': 'application/json; charset=utf-8'});
      }),
    );
  });
  Future<void> open(WidgetTester t, {double textScale = 1}) async {
    await t.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(textScale)),
          child: child!,
        ),
        home: TripListScreen(api: api, date: '2026-11-05'),
      ),
    );
    await t.pumpAndSettle();
    expect(find.textContaining('Buổi 42'), findsNothing);
    await t.tap(find.text('Tham quan'));
    await t.pumpAndSettle();
  }

  testWidgets('FR_039_danhSachChuyenMoChiTietVaKhongTuGuiLenh', (t) async {
    await open(t);
    expect(find.text('Điểm đến: Chùa'), findsOneWidget);
    expect(find.text('Được đi'), findsOneWidget);
    expect(commands, isEmpty);
  });
  testWidgets('FR_040_biChanGiuLuaChonVaKhoaThuLai', (t) async {
    await open(t);
    await t.ensureVisible(find.byType(CheckboxListTile));
    await t.tap(find.byType(CheckboxListTile));
    await t.pumpAndSettle();
    for (var i = 0; i < 2; i++) {
      await t.tap(find.widgetWithText(FilledButton, 'Điểm danh rời viện'));
      await t.pumpAndSettle();
      await t.tap(find.widgetWithText(FilledButton, 'Xác nhận rời viện'));
      await t.pumpAndSettle();
    }
    expect(find.text('Chưa chuẩn bị thuốc mang theo'), findsOneWidget);
    expect(t.widget<CheckboxListTile>(find.byType(CheckboxListTile)).value, isTrue);
    expect(commands, hasLength(2));
    expect(commands[0].headers['Idempotency-Key'], isNotEmpty);
    expect(commands[0].headers['Idempotency-Key'], commands[1].headers['Idempotency-Key']);
    expect(jsonDecode(commands[0].body), {
      'residentIds': [701],
    });
  });
  testWidgets('FR_049_quyenXemKhongCoLenhRoiVien', (t) async {
    writable = false;
    await open(t);
    expect(find.widgetWithText(FilledButton, 'Điểm danh rời viện'), findsNothing);
    expect(find.byType(CheckboxListTile), findsNothing);
  });
  testWidgets('NFR_10_dienThoaiChu200PhanTramVanDocVaChonDuocNguoi', (t) async {
    t.view.devicePixelRatio = 1;
    t.view.physicalSize = const Size(360, 800);
    addTearDown(t.view.resetPhysicalSize);
    addTearDown(t.view.resetDevicePixelRatio);
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
