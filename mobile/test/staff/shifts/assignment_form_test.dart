import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/shifts/assignment_form.dart';
import 'package:mobile/staff/shifts/assignments_controller.dart';
import '../../support/fake_backend.dart';
import 'assignments_controller_test.dart' show shiftFixture;

Map<String, dynamic> original() => {
  'id': 22,
  'version': 3,
  'staffId': 9,
  'targetType': 'FLOOR',
  'targetId': 2,
  'role': 'CAREGIVER',
  'assignmentKind': 'PRIMARY',
  'assignedNurse': false,
  'startsAt': '2026-11-07T15:00:00Z',
  'endsAt': '2026-11-07T23:00:00Z',
};
FakeBackend backend() => FakeBackend()
  ..on('GET', '/staff', 200, [
    {'id': 9, 'fullName': '[Nhân viên 1]'},
  ])
  ..on('GET', '/floors', 200, [
    {'id': 2, 'name': '[Tầng 1]'},
  ])
  ..on('POST', '/assignments/22/draft-update-preview', 200, {
    'warnings': [
      {'description': 'Chứng chỉ cần kiểm tra'},
    ],
  });
void main() {
  testWidgets('FR_025_chọn loại công việc từ danh mục đang hiệu lực của 005', (tester) async {
    final b = backend()
      ..on('GET', '/task-types', 200, [
        {'id': 80, 'name': '[Loại công việc kiểm thử]', 'status': 'ACTIVE'},
        {'id': 81, 'name': '[Loại đã ngừng]', 'status': 'INACTIVE'},
      ]);
    final c = AssignmentsController(b.client());
    c.shift = shiftFixture(commands: ['updateDraftAssignment']);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: AssignmentForm(controller: c, original: {...original(), 'taskTypeId': 80}),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Loại công việc'), 150);
    final field = tester.widget<DropdownButton<int>>(find.byType(DropdownButton<int>));
    expect(field.items!.map((item) => item.value), [null, 80]);
    expect(field.value, 80);
    expect(b.calls('GET', '/task-types'), hasLength(1));
    expect(find.textContaining('Chưa tải được loại công việc:'), findsNothing);
    c.dispose();
  });
  testWidgets('FR_031_xem trước lại phải xác nhận cảnh báo mới', (tester) async {
    final b = backend(), c = AssignmentsController(b.client());
    c.shift = shiftFixture(commands: ['updateDraftAssignment']);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: AssignmentForm(controller: c, original: original()),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Xem trước'), 200);
    await tester.tap(find.text('Xem trước'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Lưu phân công'), 200);
    await tester.tap(find.byType(Checkbox).last);
    await tester.pumpAndSettle();
    expect(tester.widget<Checkbox>(find.byType(Checkbox).last).value, true);
    expect(tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Lưu phân công')).onPressed, isNotNull);
    b.on('POST', '/assignments/22/draft-update-preview', 200, {
      'warnings': [
        {'description': 'Có thêm công việc chưa đủ điều kiện'},
      ],
    });
    await tester.scrollUntilVisible(find.text('Xem trước'), -200);
    await tester.tap(find.text('Xem trước'));
    await tester.pumpAndSettle();
    expect(find.text('Có thêm công việc chưa đủ điều kiện'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Lưu phân công'), 200);
    expect(tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Lưu phân công')).onPressed, isNull);
    c.dispose();
  });
  testWidgets('FR_025_xem trước lại lỗi phải bỏ kết quả cũ và khóa lưu', (tester) async {
    final b = backend()..on('POST', '/assignments/22/draft-update-preview', 200, {'warnings': []});
    final c = AssignmentsController(b.client());
    c.shift = shiftFixture(commands: ['updateDraftAssignment']);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: AssignmentForm(controller: c, original: original()),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Xem trước'), 200);
    await tester.tap(find.text('Xem trước'));
    await tester.pumpAndSettle();
    b.on('POST', '/assignments/22/draft-update-preview', 412, {'detail': 'Phiên bản đã đổi'});
    await tester.tap(find.text('Xem trước'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Lưu phân công'), 200);
    expect(tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Lưu phân công')).onPressed, isNull);
    expect(b.calls('POST', '/assignments/22/update-draft'), isEmpty);
    c.dispose();
  });
  testWidgets('FR_025_tìm người trả chậm không ghi đè danh sách tầng vừa chọn', (tester) async {
    final pending = Completer<http.Response>();
    final api = ApiClient(
      baseUrl: 'http://backend.test',
      httpClient: MockClient((request) async {
        if (request.url.path.endsWith('/residents') && request.url.queryParameters['q'] == 'tìm chậm') {
          return pending.future;
        }
        final body = switch (request.url.path) {
          '/api/v1/staff' => [
            {'id': 9, 'fullName': '[Nhân viên 1]'},
          ],
          '/api/v1/floors' => [
            {'id': 2, 'name': '[Tầng 1]'},
          ],
          '/api/v1/residents' => {
            'items': [
              {'id': 5, 'fullName': '[NCT 1]'},
            ],
          },
          _ => throw StateError('Yêu cầu ngoài test: ${request.url}'),
        };
        return http.Response.bytes(utf8.encode(jsonEncode(body)), 200);
      }),
    );
    final c = AssignmentsController(api);
    c.shift = shiftFixture(commands: ['updateDraftAssignment']);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: AssignmentForm(controller: c, original: {...original(), 'targetType': 'RESIDENT', 'targetId': 5}),
      ),
    );
    await tester.pumpAndSettle();
    final search = find.widgetWithText(TextFormField, 'Tìm người cao tuổi');
    await tester.enterText(search, 'tìm chậm');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pump();
    await tester.tap(find.text('Người cao tuổi').first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tầng').last);
    await tester.pumpAndSettle();
    pending.complete(
      http.Response.bytes(
        utf8.encode(
          jsonEncode({
            'items': [
              {'id': 77, 'fullName': '[NCT cũ]'},
            ],
          }),
        ),
        200,
      ),
    );
    await tester.pumpAndSettle();
    final target = tester.widget<DropdownButton<Object>>(
      find.descendant(
        of: find.byWidgetPredicate(
          (w) => w is DropdownButtonFormField<Object> && w.decoration.labelText == 'Đối tượng',
        ),
        matching: find.byType(DropdownButton<Object>),
      ),
    );
    expect(target.items!.map((item) => item.value), [2]);
    expect(tester.takeException(), isNull);
    c.dispose();
  });
  testWidgets('FR_025_đọc lại sau 412 hiển thị nội dung hiện hành, không chỉ đổi version', (tester) async {
    final b = backend()
      ..on('POST', '/assignments/22/draft-update-preview', 200, {'warnings': []})
      ..on('POST', '/assignments/22/update-draft', 412, {'detail': 'Phiên bản đã đổi'})
      ..on('GET', '/assignments/22', 200, {...original(), 'version': 4, 'assignmentKind': 'SUPPORT'});
    final c = AssignmentsController(b.client());
    c.shift = shiftFixture(commands: ['updateDraftAssignment']);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: AssignmentForm(controller: c, original: original()),
      ),
    );
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Xem trước'), 200);
    await tester.ensureVisible(find.widgetWithText(OutlinedButton, 'Xem trước'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xem trước'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Lưu phân công'), 200);
    await tester.tap(find.text('Lưu phân công'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Đọc lại dữ liệu hiện hành'), -200);
    await tester.tap(find.text('Đọc lại dữ liệu hiện hành'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Xem trước'), 200);
    await tester.ensureVisible(find.widgetWithText(OutlinedButton, 'Xem trước'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xem trước'));
    await tester.pumpAndSettle();
    final request = b.calls('POST', '/assignments/22/draft-update-preview').last;
    expect((jsonDecode(request.body) as Map<String, dynamic>)['assignmentKind'], 'SUPPORT');
    expect(request.headers['If-Match'], '"4"');
    c.dispose();
  });
  testWidgets('FR_031_cần xem trước, xác nhận cảnh báo, đổi form phải kiểm lại', (tester) async {
    final b = backend(), c = AssignmentsController(b.client());
    c.shift = shiftFixture(commands: ['updateDraftAssignment']);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: AssignmentForm(controller: c, original: original()),
      ),
    );
    await tester.pumpAndSettle();
    final save = find.widgetWithText(FilledButton, 'Lưu phân công');
    await tester.scrollUntilVisible(find.text('Lưu phân công'), 200);
    expect(tester.widget<FilledButton>(save).onPressed, isNull);
    await tester.scrollUntilVisible(find.text('Xem trước'), -200);
    await tester.tap(find.text('Xem trước'));
    await tester.pumpAndSettle();
    expect(find.text('Chứng chỉ cần kiểm tra'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Lưu phân công'), 200);
    expect(tester.widget<FilledButton>(save).onPressed, isNull);
    await tester.ensureVisible(find.text('Tôi đã đọc cảnh báo'));
    await tester.tap(find.text('Tôi đã đọc cảnh báo'));
    await tester.pumpAndSettle();
    expect(tester.widget<FilledButton>(save).onPressed, isNotNull);
    final request = b.calls('POST', '/assignments/22/draft-update-preview').single;
    expect((jsonDecode(request.body) as Map<String, dynamic>)['startsAt'], endsWith('+07:00'));
    await tester.scrollUntilVisible(find.text('Điều dưỡng phụ trách'), -200);
    await tester.tap(find.text('Điều dưỡng phụ trách'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Lưu phân công'), 200);
    expect(tester.widget<FilledButton>(save).onPressed, isNull);
    c.dispose();
  });
  for (final dark in [false, true]) {
    testWidgets('FR_031_form 200 phần trăm, tối=$dark', (tester) async {
      tester.view.physicalSize = const Size(320, 1000);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      final c = AssignmentsController(backend().client());
      c.shift = shiftFixture(commands: ['updateDraftAssignment']);
      await tester.pumpWidget(
        MaterialApp(
          theme: dark ? AppTheme.dark() : AppTheme.light(),
          builder: (context, child) => MediaQuery(
            data: MediaQuery.of(context).copyWith(textScaler: const TextScaler.linear(2)),
            child: child!,
          ),
          home: AssignmentForm(controller: c, original: original()),
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      final role = tester.renderObject<RenderParagraph>(find.text('Nhân viên chăm sóc').first);
      final boxes = role.getBoxesForSelection(const TextSelection(baseOffset: 0, extentOffset: 18));
      expect(
        boxes.last.bottom,
        lessThanOrEqualTo(role.size.height + 0.01),
        reason: 'Phải hiện đủ chữ của vai trò ở cỡ chữ lớn',
      );
      await tester.scrollUntilVisible(find.text('Xem trước'), 200);
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
      c.dispose();
    });
  }
}
