import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/portal/family_activities_screen.dart';

import '../../support/fake_backend.dart';

// 014 FR-068, FR-016a (T026 app; schema chủ dự án duyệt 2026-11-10): cùng chữ với cổng web. Buổi đã đăng ký, chuyến đi,
// số buổi đã tham gia; người đại diện ghi, rút đồng ý về muộn cho buổi đủ điều kiện.

const _activities = {
  'from': '2026-10-12',
  'to': '2026-12-11',
  'registeredSessions': [
    {
      'sessionId': 51,
      'activityName': 'Cờ tướng',
      'startsAt': '2026-11-12T09:00:00Z',
      'endsAt': '2026-11-12T10:00:00Z',
      'registrationStatus': 'REGISTERED',
      'lateReturnConsentEligible': true,
      'activeLateReturnConsentId': null,
    },
  ],
  'trips': [
    {
      'sessionId': 60,
      'destination': 'Chùa Một Cột',
      'departureAt': '2026-11-14T01:00:00Z',
      'expectedReturnAt': '2026-11-14T05:00:00Z',
      'status': 'SCHEDULED',
    },
  ],
  'attendedSessionCount': 4,
};

void main() {
  late FakeBackend backend;
  setUp(() {
    backend = FakeBackend()
      ..on('GET', '/family-portal/residents/5/activities', 200, _activities)
      ..on('POST', '/family-portal/late-return-consents', 201);
  });

  Future<void> open(WidgetTester t, {required bool representative}) async {
    await t.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: FamilyActivitiesScreen(api: backend.client(), residentId: 5, representative: representative),
      ),
    );
    await t.pumpAndSettle();
  }

  testWidgets('FR_068_buoiDaDangKy_chuyenDi_soBuoiDaThamGia', (t) async {
    await open(t, representative: false);
    expect(find.text('Cờ tướng'), findsOneWidget);
    expect(find.text('Chùa Một Cột'), findsOneWidget);
    expect(find.text('Đã tham gia 4 buổi từ 12/10/2026 đến 11/12/2026'), findsOneWidget);
    expect(find.text('Đồng ý về muộn'), findsNothing);
  });

  testWidgets('FR_016a_nguoiDaiDienGhiDongYVeMuon', (t) async {
    await open(t, representative: true);
    await t.tap(find.text('Đồng ý về muộn'));
    await t.pumpAndSettle();
    final calls = backend.calls('POST', '/family-portal/late-return-consents');
    expect(calls, hasLength(1));
    expect(jsonDecode(calls.single.body), {'residentId': 5, 'sessionId': 51});
    expect(calls.single.headers['Idempotency-Key'], isNotEmpty);
  });
}
