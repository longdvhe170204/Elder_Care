import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/resident/today/today_screen.dart';

import '../../support/fake_backend.dart';

// 012 T070 (tên logic hom_nay_test; US9, FR-083, FR-084; DESIGN.md 15.6): màn đầu "Hôm nay" có các thẻ lớn một cột, mỗi
// thẻ hiện việc gần nhất và mở màn tương ứng (chữ lớn, nút "Quay lại" có chữ); nút "Gọi người giúp" ở đáy. Không có thẻ
// sức khỏe, thuốc, chi phí. Thực đơn: 011 chưa có nguồn nên báo chưa có.

const visit = {
  'visitId': 31,
  'date': '2026-11-06',
  'startTime': '15:00',
  'endTime': '17:00',
  'visitors': ['Lê M', 'Lê Bé'],
};
const session = {
  'sessionId': 41,
  'name': 'Tập dưỡng sinh',
  'startAt': '2026-11-06T03:00:00Z',
  'endAt': '2026-11-06T04:00:00Z',
  'facilityWide': true,
  'registered': true,
};

FakeBackend channel() => FakeBackend()
  ..on('GET', '/me/notifications', 200, {
    'items': <dynamic>[],
    'unreadCount': 0,
    'unacknowledgedUrgentCount': 0,
    'nextCursor': null,
  })
  ..on('GET', '/resident-channel/today', 200, {
    'date': '2026-11-06',
    'visits': [visit],
    'activities': [session],
    'dayCareSchedule': <dynamic>[],
    'unavailable': <String>[],
  })
  ..on('GET', '/resident-channel/activities', 200, {
    'available': true,
    'note': null,
    'items': [session],
  })
  ..on('GET', '/resident-channel/visits', 200, {
    'available': true,
    'note': null,
    'items': [visit],
  })
  ..on('GET', '/resident-channel/belongings', 200, {
    'available': true,
    'note': null,
    'items': [
      {'belongingId': 51, 'name': 'Đài radio', 'status': 'HELD', 'photoFileId': null},
    ],
  })
  ..on('GET', '/resident-channel/menu', 200, {'available': false, 'note': 'Chưa có dữ liệu', 'items': <dynamic>[]})
  ..on('GET', '/resident-channel/feedback', 200, <dynamic>[]);

Future<void> pumpToday(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.resident(Brightness.light),
      home: TodayScreen(api: api, now: DateTime.utc(2026, 11, 6, 1)),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> openCard(WidgetTester tester, String title) async {
  await tester.ensureVisible(find.text(title));
  await tester.pumpAndSettle();
  await tester.tap(find.text(title));
  await tester.pumpAndSettle();
}

Future<void> back(WidgetTester tester) async {
  await tester.tap(find.text('Quay lại'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_083_cardsShowNearestItem', (tester) async {
    await pumpToday(tester, channel().client());

    for (final title in ['Lịch hôm nay', 'Bữa ăn', 'Hoạt động', 'Người đến thăm', 'Đồ của tôi', 'Góp ý với viện']) {
      expect(find.text(title), findsOneWidget, reason: title);
    }
    expect(find.text('10:00 Tập dưỡng sinh'), findsWidgets);
    expect(find.text('15:00 Lê M đến thăm'), findsWidgets);
    expect(find.text('1 món đang giữ'), findsOneWidget);
    expect(find.text('Chưa có thực đơn'), findsOneWidget);
    expect(find.text('Gọi người giúp'), findsOneWidget);
    expect(find.textContaining('sức khỏe'), findsNothing);
  });

  testWidgets('FR_083_cardsOpenSections', (tester) async {
    await pumpToday(tester, channel().client());

    await openCard(tester, 'Hoạt động');
    expect(find.text('Tập dưỡng sinh'), findsOneWidget);
    expect(find.text('Đã có tên'), findsOneWidget);
    expect(find.textContaining('Đăng ký'), findsNothing);
    await back(tester);

    await openCard(tester, 'Người đến thăm');
    expect(find.text('Lê M, Lê Bé'), findsOneWidget);
    expect(find.text('06/11/2026, 15:00–17:00'), findsOneWidget);
    await back(tester);

    await openCard(tester, 'Đồ của tôi');
    expect(find.text('Đài radio'), findsOneWidget);
    expect(find.text('Đang giữ'), findsOneWidget);
    await back(tester);

    await openCard(tester, 'Bữa ăn');
    expect(find.text('Chưa có thực đơn.'), findsOneWidget);
    await back(tester);

    await openCard(tester, 'Lịch hôm nay');
    expect(find.text('10:00 Tập dưỡng sinh'), findsOneWidget);
    expect(find.text('15:00 Lê M, Lê Bé đến thăm'), findsOneWidget);
  });
}
