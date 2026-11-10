import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/care_tasks/ad_hoc_care_recording_screen.dart';
import '../../support/fake_backend.dart';

void main() {
  testWidgets('FR_025_reviewConfirmationChangesEnvelopeKeyWithoutLosingResult', (tester) async {
    final backend = FakeBackend();
    backend.on('GET', '/residents', 200, {
      'items': [
        {
          'id': 9,
          'personalInfo': {'fullName': 'Người kiểm thử'},
        },
      ],
    });
    backend.on('GET', '/task-types', 200, [
      {'id': 3, 'name': 'Uống nước', 'status': 'ACTIVE', 'resultType': 'ML'},
    ]);
    backend.on('GET', '/result-values', 200, []);
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': 'key-1', 'result': 'REJECTED', 'reason': 'Còn công việc cùng loại đang mở'},
      ],
    });
    final api = backend.client();
    final db = OfflineDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    int sequence = 0;
    final queue = OfflineQueue(db, api, newKey: () => 'key-${++sequence}');
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: AdHocCareRecordingScreen(api: api, queue: queue, accountId: 7),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Người cao tuổi'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Người kiểm thử').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Loại công việc'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Uống nước').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Lượng nước (ml)'), '200');
    await tester.ensureVisible(find.text('Lưu kết quả'));
    await tester.tap(find.text('Lưu kết quả'));
    await tester.pumpAndSettle();
    expect(find.text('Còn công việc cùng loại đang mở'), findsOneWidget);
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': 'key-2', 'result': 'ACCEPTED'},
      ],
    });
    await tester.ensureVisible(find.byType(CheckboxListTile));
    await tester.tap(find.byType(CheckboxListTile));
    await tester.pumpAndSettle();
    expect(find.text('200'), findsOneWidget);
    await tester.ensureVisible(find.text('Lưu kết quả'));
    await tester.tap(find.text('Lưu kết quả'));
    await tester.pumpAndSettle();
    expect(find.text('Đã lưu ghi nhận phát sinh.'), findsOneWidget);
    expect((await queue.synced(7)).map((r) => r.idempotencyKey).toSet(), {'key-1', 'key-2'});
  });
  testWidgets('FR_025_adHocFormRecordsOnlineUsingSelectedResidentAndType', (tester) async {
    final backend = FakeBackend();
    backend.on('GET', '/residents', 200, {
      'items': [
        {
          'id': 9,
          'personalInfo': {'fullName': 'Người kiểm thử'},
        },
      ],
    });
    backend.on('GET', '/task-types', 200, [
      {'id': 3, 'name': 'Uống nước', 'status': 'ACTIVE', 'resultType': 'ML'},
    ]);
    backend.on('GET', '/result-values', 200, []);
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': 'ad-hoc-key', 'result': 'ACCEPTED'},
      ],
    });
    final api = backend.client();
    final db = OfflineDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: AdHocCareRecordingScreen(
          api: api,
          queue: OfflineQueue(db, api, newKey: () => 'ad-hoc-key'),
          accountId: 7,
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Người cao tuổi'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Người kiểm thử').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Loại công việc'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Uống nước').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Lượng nước (ml)'), '200');
    await tester.ensureVisible(find.text('Lưu kết quả'));
    await tester.tap(find.text('Lưu kết quả'));
    await tester.pumpAndSettle();
    expect(backend.calls('POST', '/sync'), hasLength(1));
    expect(find.text('Đã lưu ghi nhận phát sinh.'), findsOneWidget);
  });
}
