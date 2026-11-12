import 'dart:convert';

import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/care_tasks/rejected_care_record_screen.dart';

import '../../support/fake_backend.dart';

// 005 T040 (tên logic ban_ghi_bi_tu_choi_test; BR-M15-20, FR-036; chủ dự án duyệt 2026-11-11): kết quả công việc bị từ chối
// lúc đồng bộ giữ nội dung; người ghi đính chính kết quả đang có hoặc chuyển thành Ghi nhận phát sinh với người cao tuổi,
// loại công việc lấy từ CareTask.taskTypeId.

void main() {
  late FakeBackend backend;
  late OfflineQueue queue;
  late QueuedRecord rejected;

  Future<void> pump(WidgetTester tester) async {
    final api = backend.client();
    final db = OfflineDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    queue = OfflineQueue(db, api, newKey: () => 'key-1');
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': 'key-1', 'result': 'REJECTED', 'reason': 'Công việc đã được đóng'},
      ],
    });
    rejected = (await tester.runAsync(() async {
      await queue.enqueue(
        accountId: 7,
        recordType: 'M04.CARE_TASK_RESULT',
        content: {
          'taskId': 41,
          'action': 'RECORD',
          'recording': {'numericValue': 200, 'note': 'Uống chậm', 'performedAt': '2026-11-11T03:00:00Z'},
        },
      );
      await queue.sync(7);
      return (await queue.synced(7)).single;
    }))!;
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: RejectedCareRecordScreen(api: api, queue: queue, accountId: 7, record: rejected),
      ),
    );
    await tester.pumpAndSettle();
  }

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/care-tasks/41', 200, {
      'id': 41,
      'residentId': 9,
      'taskType': 'Uống nước',
      'taskTypeId': 3,
      'resultType': 'ML',
      'status': 'COMPLETED',
      'result': {'id': 41, 'taskId': 41},
    });
  });

  testWidgets('FR_036_banGhiBiTuChoi_dinhChinhKetQuaDangCo_guiNoiDungGoc', (tester) async {
    await pump(tester);
    expect(find.text('Lý do từ chối: Công việc đã được đóng'), findsOneWidget);
    expect(find.text('Giá trị: 200'), findsOneWidget);

    await tester.tap(find.text('Đính chính kết quả đã ghi'));
    await tester.pumpAndSettle();
    expect(find.text('Nhập lý do đính chính.'), findsOneWidget);

    backend.on('POST', '/care-results/41/correct', 201);
    await tester.enterText(find.widgetWithText(TextField, 'Lý do đính chính'), 'Ghi trùng lúc mất mạng');
    await tester.tap(find.text('Đính chính kết quả đã ghi'));
    await tester.pumpAndSettle();

    expect(find.text('Đã lưu đính chính.'), findsOneWidget);
    final body = jsonDecode(backend.calls('POST', '/care-results/41/correct').single.body) as Map<String, dynamic>;
    expect(body['type'], 'AMEND_CONTENT');
    expect(body['correctedContent'], {'numericValue': 200, 'note': 'Uống chậm'});
    expect(body['reason'], 'Ghi trùng lúc mất mạng');
  });

  testWidgets('FR_036_banGhiBiTuChoi_chuyenThanhGhiNhanPhatSinh_chonSanNguoiVaLoai', (tester) async {
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
    await pump(tester);

    await tester.tap(find.text('Chuyển thành Ghi nhận phát sinh'));
    await tester.pumpAndSettle();

    expect(find.text('Ghi nhận phát sinh'), findsOneWidget);
    expect(find.text('Người kiểm thử'), findsOneWidget);
    expect(find.widgetWithText(TextField, 'Lượng nước (ml)'), findsOneWidget);
  });
}
