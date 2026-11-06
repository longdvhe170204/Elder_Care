import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/assessments/assessment_screen.dart';

import '../../support/fake_backend.dart';

// 001 T042 (US3; FR-025, FR-026, FR-029; BR-M01-13): app nhân viên lập nháp đánh giá, lưu nháp kèm If-Match lấy từ ETag
// của lần đọc gần nhất, hủy nháp kèm lý do, xem lịch sử. Không có ô điểm, không có mức đề xuất (FR-024). Chỉ trực tuyến.

Map<String, dynamic> assessment({
  int id = 5,
  String status = 'DRAFT',
  String type = 'INITIAL',
  String? comment,
  String? careLevel,
}) => {
  'id': id,
  'type': type,
  'basis': 'Tiếp nhận',
  'createdBy': '[Họ tên 2]',
  'comment': comment,
  'careNote': null,
  'careLevel': careLevel,
  'careNeeds': <Object>[],
  'status': status,
  'corrected': false,
};

Map<String, dynamic> detail([Map<String, dynamic>? base]) => {
  ...(base ?? assessment()),
  'reference': <String, dynamic>{},
};

Future<void> pumpScreen(
  WidgetTester tester,
  FakeBackend backend, {
  List<String> commands = const ['CREATE_ASSESSMENT'],
}) async {
  await tester.binding.setSurfaceSize(const Size(800, 1600));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: AssessmentScreen(api: backend.client(), residentId: 7, allowedCommands: commands),
    ),
  );
  await tester.pumpAndSettle();
}

Finder button(String label) =>
    find.ancestor(of: find.text(label), matching: find.byWidgetPredicate((w) => w is ButtonStyleButton));

Future<void> tapButton(WidgetTester tester, String label) async {
  await tester.ensureVisible(button(label));
  await tester.pumpAndSettle();
  await tester.tap(button(label));
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/residents/7/assessments', 200, [assessment(id: 4, status: 'CONFIRMED', careLevel: 'BASIC')]);
  });

  testWidgets('FR_025_noDraft_listsHistoryAndOffersCreate', (tester) async {
    await pumpScreen(tester, backend);

    expect(find.text('Đã xác nhận'), findsOneWidget);
    expect(button('Lập đánh giá'), findsOneWidget);
    expect(find.textContaining('điểm'), findsNothing);
  });

  testWidgets('FR_025_withoutCreateCommand_hasNoCreateButton', (tester) async {
    await pumpScreen(tester, backend, commands: const []);

    expect(button('Lập đánh giá'), findsNothing);
  });

  testWidgets('FR_025_create_postsTypeAndBasisThenOpensDraft', (tester) async {
    backend.on('POST', '/residents/7/assessments', 201, detail(), {'etag': '"1"'});
    backend.on('GET', '/assessments/5', 200, detail(), {'etag': '"1"'});
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Lập đánh giá');
    await tester.enterText(find.widgetWithText(TextField, 'Căn cứ đánh giá'), 'Tiếp nhận');
    await tapButton(tester, 'Tạo bản nháp');

    final sent = jsonDecode(backend.calls('POST', '/residents/7/assessments').single.body) as Map<String, dynamic>;
    expect(sent, {'type': 'INITIAL', 'basis': 'Tiếp nhận'});
    expect(find.widgetWithText(TextField, 'Nhận xét đánh giá'), findsOneWidget);
  });

  testWidgets('FR_029_saveDraft_sendsIfMatchFromEtagAndKeepsNewEtag', (tester) async {
    backend.on('GET', '/residents/7/assessments', 200, [assessment()]);
    backend.on('GET', '/assessments/5', 200, detail(), {'etag': '"3"'});
    backend.on('PUT', '/assessments/5', 200, detail(assessment(comment: 'Đi lại khó')), {'etag': '"4"'});
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Sửa bản nháp');
    await tester.enterText(find.widgetWithText(TextField, 'Nhận xét đánh giá'), 'Đi lại khó');
    await tapButton(tester, 'Lưu bản nháp');
    await tapButton(tester, 'Lưu bản nháp');

    final puts = backend.calls('PUT', '/assessments/5');
    expect(puts, hasLength(2));
    expect(puts.first.headers['If-Match'], '"3"');
    expect(puts.last.headers['If-Match'], '"4"');
    expect((jsonDecode(puts.first.body) as Map<String, dynamic>)['comment'], 'Đi lại khó');
  });

  testWidgets('FR_029_saveDraft_412_showsMessageAndReloads', (tester) async {
    backend.on('GET', '/residents/7/assessments', 200, [assessment()]);
    backend.on('GET', '/assessments/5', 200, detail(), {'etag': '"3"'});
    backend.on('PUT', '/assessments/5', 412, {
      'status': 412,
      'errorCode': 'PRECONDITION_FAILED',
      'detail': 'Bản nháp đã được sửa ở nơi khác',
    });
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Sửa bản nháp');
    await tapButton(tester, 'Lưu bản nháp');

    expect(find.text('Bản nháp đã được sửa ở nơi khác'), findsOneWidget);
    expect(backend.calls('GET', '/assessments/5'), hasLength(2));
  });

  testWidgets('FR_026_discardDraft_requiresReasonThenPosts', (tester) async {
    backend.on('GET', '/residents/7/assessments', 200, [assessment()]);
    backend.on('GET', '/assessments/5', 200, detail(), {'etag': '"3"'});
    backend.on('POST', '/assessments/5/discard-draft', 200, assessment(status: 'DISCARDED'));
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Sửa bản nháp');
    await tapButton(tester, 'Hủy bản nháp');
    await tapButton(tester, 'Xác nhận hủy');
    expect(find.text('Nhập lý do hủy'), findsOneWidget);
    expect(backend.calls('POST', '/assessments/5/discard-draft'), isEmpty);

    await tester.enterText(find.widgetWithText(TextField, 'Lý do hủy'), 'Lập nhầm');
    await tapButton(tester, 'Xác nhận hủy');

    final sent = jsonDecode(backend.calls('POST', '/assessments/5/discard-draft').single.body);
    expect(sent, {'reason': 'Lập nhầm'});
  });

  testWidgets('FR_025_offline_saysNeedsConnection', (tester) async {
    backend.down = true;
    await pumpScreen(tester, backend);

    expect(find.textContaining('Không kết nối được máy chủ'), findsOneWidget);
  });
}
