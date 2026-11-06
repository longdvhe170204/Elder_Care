import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/resident/feedback/resident_feedback_screen.dart';

import '../../support/fake_backend.dart';

// 012 T070 (tên logic phan_hoi_test; US9, FR-058, FR-058a, FR-058b; DESIGN.md 15.6): "Góp ý với viện": chọn một trong bốn
// loại có dòng giải thích; Mong muốn hỏi thêm về người đến thăm, người đón hay việc khác; loại khác hỏi về việc gì (nhóm
// nội dung bắt buộc theo FR-058, bổ sung so với DESIGN.md 15.6, chờ duyệt); viết rồi "Gửi tới viện". Danh sách góp ý đã
// gửi có trạng thái và trả lời; trả lời về sức khỏe chỉ phần chung.

Map<String, dynamic> feedback({String status = 'REPLIED', String group = 'HEALTH_MEDICATION'}) => {
  'id': 9,
  'residentId': 5,
  'senderType': 'RESIDENT',
  'type': 'QUESTION',
  'group': group,
  'content': 'Vì sao đổi thuốc tối?',
  'fileIds': <int>[],
  'status': status,
  'priority': 'NORMAL',
  'dueAt': '2026-11-08T02:00:00Z',
  'overdue': false,
  'assignee': 'Lê Thu Trang',
  'submittedAt': '2026-11-05T02:10:00Z',
};

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.resident(Brightness.light),
      home: ResidentFeedbackScreen(api: api),
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

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend()
      ..on('GET', '/resident-channel/feedback', 200, [feedback()])
      ..on('POST', '/resident-channel/feedback', 201, {'id': 10});
  });

  testWidgets('FR_058a_sendComplaintInSteps', (tester) async {
    await pumpScreen(tester, backend.client());

    await tapText(tester, 'Gửi góp ý mới');
    expect(find.text('Có việc không đúng'), findsOneWidget);
    await tapText(tester, 'Khiếu nại');
    await tapText(tester, 'Chăm sóc, sinh hoạt');
    await tapText(tester, 'Gửi tới viện');
    expect(find.text('Viết điều muốn nói'), findsOneWidget);
    expect(backend.calls('POST', '/resident-channel/feedback'), isEmpty);

    await tester.enterText(find.byType(TextField), 'Phòng chưa được dọn');
    await tapText(tester, 'Gửi tới viện');

    final post = backend.calls('POST', '/resident-channel/feedback').single;
    expect(jsonDecode(post.body), {'type': 'COMPLAINT', 'group': 'CARE', 'content': 'Phòng chưa được dọn'});
    expect(post.headers['Idempotency-Key'], matches(RegExp(r'^[0-9a-f-]{36}$')));
    expect(find.text('Đã gửi tới viện. Viện sẽ trả lời trong ứng dụng.'), findsOneWidget);
  });

  testWidgets('FR_058b_wishAsksTopic', (tester) async {
    await pumpScreen(tester, backend.client());

    await tapText(tester, 'Gửi góp ý mới');
    await tapText(tester, 'Mong muốn');
    expect(find.text('Chăm sóc, sinh hoạt'), findsNothing);
    await tapText(tester, 'Về người đến thăm, người đón tôi');
    await tester.enterText(find.byType(TextField), 'Không muốn gặp ông X');
    await tapText(tester, 'Gửi tới viện');

    expect(jsonDecode(backend.calls('POST', '/resident-channel/feedback').single.body), {
      'type': 'WISH',
      'group': 'OTHER',
      'wishTopic': 'VISITORS_PICKERS',
      'content': 'Không muốn gặp ông X',
    });
  });

  testWidgets('FR_058a_healthReplyGeneralOnly_agreeToClose', (tester) async {
    backend
      ..on('GET', '/feedback/9', 200, {
        ...feedback(),
        'items': [
          {'id': 1, 'type': 'CONTENT', 'content': 'Vì sao đổi thuốc tối?', 'at': '2026-11-05T02:10:00Z'},
        ],
        'healthReplyHidden': true,
        'allowedCommands': ['agree-to-close', 'reopen'],
      })
      ..on('POST', '/feedback/9/agree-to-close', 200, {...feedback(status: 'CLOSED')});
    await pumpScreen(tester, backend.client());

    expect(find.text('Đã có trả lời'), findsOneWidget);
    await tapText(tester, 'Xem trả lời');
    expect(find.text('Câu trả lời về sức khỏe sẽ được nhân viên nói trực tiếp.'), findsOneWidget);
    await tapText(tester, 'Đồng ý đóng');
    expect(backend.calls('POST', '/feedback/9/agree-to-close'), hasLength(1));
  });
}
