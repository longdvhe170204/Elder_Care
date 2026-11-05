import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/feedback/feedback_screen.dart';

import '../../support/fake_backend.dart';

// 012 T030 (US6; FR-058, FR-061, FR-066): người thân gửi phản hồi trên app cho người cao tuổi đầu tiên có quan hệ (như
// cổng web); loại, nhóm nội dung, nội dung bắt buộc; phản hồi Đã phản hồi thì Đồng ý đóng hoặc Mở lại có lý do. Chỉ trực
// tuyến; trả lời là thông tin sức khỏe mà người xem không có quyền thì hiện dòng khóa. Nút lệnh theo allowedCommands của
// chi tiết (đổi api 2026-11-05).

Map<String, dynamic> feedback({int id = 91, String status = 'REPLIED', String group = 'CARE'}) => {
  'id': id,
  'residentId': 5,
  'senderType': 'FAMILY_MEMBER',
  'familyMemberId': 31,
  'type': 'COMPLAINT',
  'group': group,
  'content': 'Phòng chưa được dọn',
  'fileIds': <int>[],
  'status': status,
  'priority': 'HIGH',
  'dueAt': '2026-11-06T02:00:00Z',
  'overdue': false,
  'assignee': 'Lê Thu Trang',
};

FakeBackend backendWith(List<Map<String, dynamic>> items) => FakeBackend()
  ..on('GET', '/family-portal/residents', 200, [
    {'residentId': 5, 'fullName': 'Nguyễn Thị Lan', 'rights': <String, dynamic>{}},
  ])
  ..on('GET', '/feedback', 200, items);

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: FeedbackScreen(api: api)),
    ),
  );
  await tester.pumpAndSettle();
}

Finder button(String label) =>
    find.ancestor(of: find.text(label), matching: find.byWidgetPredicate((w) => w is ButtonStyleButton));

Future<void> tapText(WidgetTester tester, String label) async {
  await tester.ensureVisible(find.text(label).first);
  await tester.pumpAndSettle();
  await tester.tap(find.text(label).first);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_058_gui_batBuocLoaiNhomNoiDung', (tester) async {
    final backend = backendWith([])..on('POST', '/feedback', 201, {'id': 92});
    await pumpScreen(tester, backend.client());

    await tapText(tester, 'Gửi phản hồi');
    expect(find.text('Chọn loại phản hồi'), findsOneWidget);
    expect(backend.calls('POST', '/feedback'), isEmpty);

    await tapText(tester, 'Khiếu nại');
    await tapText(tester, 'Chăm sóc, sinh hoạt');
    await tester.enterText(find.widgetWithText(TextField, 'Nội dung'), 'Phòng chưa được dọn');
    await tapText(tester, 'Gửi phản hồi');

    final post = backend.calls('POST', '/feedback').single;
    expect(jsonDecode(post.body), {
      'residentId': 5,
      'type': 'COMPLAINT',
      'group': 'CARE',
      'content': 'Phòng chưa được dọn',
    });
    expect(post.headers['Idempotency-Key'], matches(RegExp(r'^[0-9a-f-]{36}$')));
    expect(find.text('Đã gửi phản hồi. Viện sẽ báo bạn khi có trả lời.'), findsOneWidget);
  });

  testWidgets('FR_066_moLai_batBuocLyDo', (tester) async {
    final backend = backendWith([feedback()])
      ..on('GET', '/feedback/91', 200, {
        ...feedback(),
        'items': <dynamic>[],
        'allowedCommands': ['agree-to-close', 'reopen'],
      })
      ..on('POST', '/feedback/91/reopen', 200, {...feedback(status: 'REOPENED'), 'items': <dynamic>[]});
    await pumpScreen(tester, backend.client());

    expect(find.text('Phòng chưa được dọn'), findsOneWidget);
    expect(find.text('Đã phản hồi'), findsOneWidget);
    await tapText(tester, 'Mở lại');
    await tapText(tester, 'Gửi mở lại');
    expect(find.text('Nhập lý do mở lại'), findsOneWidget);
    expect(backend.calls('POST', '/feedback/91/reopen'), isEmpty);

    await tester.enterText(find.widgetWithText(TextField, 'Lý do mở lại'), 'Chưa ổn');
    await tapText(tester, 'Gửi mở lại');
    expect(jsonDecode(backend.calls('POST', '/feedback/91/reopen').single.body), {'reason': 'Chưa ổn'});
  });

  testWidgets('FR_066_khongCoLenhDuocPhep_khongHienNut', (tester) async {
    final backend = backendWith([feedback()])
      ..on('GET', '/feedback/91', 200, {...feedback(), 'items': <dynamic>[], 'allowedCommands': <String>[]});
    await pumpScreen(tester, backend.client());

    expect(find.text('Phòng chưa được dọn'), findsOneWidget);
    expect(backend.calls('GET', '/feedback/91'), hasLength(1));
    expect(find.text('Mở lại'), findsNothing);
    expect(find.text('Đồng ý đóng'), findsNothing);
  });

  testWidgets('FR_058_traLoiSucKhoe_khongCoQuyen_hienDongKhoa', (tester) async {
    final backend = backendWith([feedback(group: 'HEALTH_MEDICATION')])
      ..on('GET', '/feedback/91', 200, {
        ...feedback(group: 'HEALTH_MEDICATION'),
        'items': [
          {'id': 1, 'type': 'CONTENT', 'content': 'Phòng chưa được dọn', 'at': '2026-11-05T02:00:00Z'},
        ],
        'healthReplyHidden': true,
      });
    await pumpScreen(tester, backend.client());

    await tapText(tester, 'Xem trả lời');
    expect(find.textContaining('Thông tin sức khỏe chỉ hiện'), findsOneWidget);
  });
}
