import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/residents/resident_health_screen.dart';

import '../../support/fake_backend.dart';

// 001 T033 (tên logic suc_khoe_test; US2; FR-018 → FR-021, FR-022; DBR-04): app nhân viên xem và ghi mục sức khỏe (dị ứng,
// bệnh nền, tiền sử bệnh) của một người cao tuổi, chỉ trực tuyến. GET, POST /residents/{id}/health-items, POST
// /health-items/{id}/exclude, GET /catalogs/{type}. Nút theo allowedCommands của hồ sơ; không có nút Sửa, Xóa.

Map<String, dynamic> item({
  int id = 1,
  String type = 'ALLERGY',
  String? allergen = 'Penicillin',
  String? freeText,
  String? severity = 'Nặng',
  String status = 'ACTIVE',
  bool notAutoChecked = false,
  bool pendingCheck = false,
  bool unverified = false,
  Map<String, dynamic>? exclusion,
}) => {
  'id': id,
  'type': type,
  'allergen': allergen == null ? null : {'id': 3, 'name': allergen},
  'freeText': freeText,
  'severity': severity,
  'notAutoChecked': notAutoChecked,
  'pendingCheck': pendingCheck,
  'source': 'Người cao tuổi nói',
  'recordedBy': '[Họ tên 2]',
  'at': '2026-10-20T02:00:00Z',
  'status': status,
  'exclusion': exclusion,
  'unverified': unverified,
};

const both = ['RECORD_HEALTH_ITEM', 'EXCLUDE_HEALTH_ITEM'];

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend, {List<String> commands = both}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: ResidentHealthScreen(api: backend.client(), residentId: 7, allowedCommands: commands),
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

/// Mở ô chọn có nhãn `label` rồi chọn dòng `option`.
Future<void> pick(WidgetTester tester, String label, String option) async {
  final field = find.widgetWithText(DropdownButtonFormField<int>, label);
  await tester.ensureVisible(field);
  await tester.pumpAndSettle();
  await tester.tap(field);
  await tester.pumpAndSettle();
  await tester.tap(find.text(option).last);
  await tester.pumpAndSettle();
}

Map<String, dynamic> lastBody(FakeBackend backend, String path) =>
    jsonDecode(backend.calls('POST', path).last.body) as Map<String, dynamic>;

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/residents/7/health-items', 200, [
      item(),
      item(id: 2, type: 'CHRONIC_CONDITION', allergen: null, freeText: 'Tăng huyết áp', severity: null),
      item(id: 3, type: 'MEDICAL_HISTORY', allergen: null, freeText: 'Mổ ruột thừa', severity: null),
    ]);
    backend.on('GET', '/catalogs/allergens', 200, [
      {'id': 3, 'name': 'Penicillin'},
    ]);
    backend.on('GET', '/catalogs/allergy-severities', 200, [
      {'id': 4, 'name': 'Nặng'},
    ]);
    backend.on('GET', '/catalogs/information-sources', 200, [
      {'id': 5, 'name': 'Người cao tuổi nói'},
    ]);
    backend.on('POST', '/residents/7/health-items', 201, {'items': item(), 'warnings': <Object>[]});
    backend.on('POST', '/health-items/1/exclude', 200, item(status: 'EXCLUDED'));
  });

  testWidgets('FR_018_list_groupsAllergyFirstAndShowsSourceRecorder', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await pumpScreen(tester, backend);

    expect(backend.requests.first.url.queryParameters['status'], 'ACTIVE');
    expect(find.text('Dị ứng: Penicillin (Nặng)'), findsOneWidget);
    expect(find.text('Tăng huyết áp'), findsOneWidget);
    expect(find.text('Mổ ruột thừa'), findsOneWidget);
    expect(find.text('Nguồn: Người cao tuổi nói. Ghi bởi [Họ tên 2], 20/10/2026'), findsWidgets);
    expect(find.text('Mức độ: Nặng'), findsOneWidget);
  });

  testWidgets('FR_018a_item_showsNotAutoCheckedAndPendingCheckTags', (tester) async {
    backend.on('GET', '/residents/7/health-items', 200, [
      item(allergen: null, freeText: 'Phấn hoa', notAutoChecked: true),
      item(id: 2, pendingCheck: true),
    ]);
    await pumpScreen(tester, backend);

    expect(find.text('Không kiểm tra tự động'), findsOneWidget);
    expect(find.text('Chờ kiểm tra thuốc và chế độ ăn'), findsOneWidget);
  });

  testWidgets('FR_019_filterExcluded_sendsStatusAndShowsExclusion', (tester) async {
    await pumpScreen(tester, backend);
    backend.on('GET', '/residents/7/health-items', 200, [
      item(status: 'EXCLUDED', exclusion: {'at': '2026-10-25T02:00:00Z', 'by': '[Họ tên 3]', 'reason': 'Ghi nhầm'}),
    ]);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Đã loại trừ'));
    await tester.pumpAndSettle();

    expect(backend.requests.last.url.queryParameters['status'], 'EXCLUDED');
    expect(find.text('Đã loại trừ'), findsWidgets);
    expect(find.text('Loại trừ bởi [Họ tên 3], 25/10/2026. Lý do: Ghi nhầm'), findsOneWidget);
    expect(button('Loại trừ'), findsNothing);
  });

  testWidgets('DBR_04_noPermission_hasNoRecordExcludeEditOrDeleteButtons', (tester) async {
    await pumpScreen(tester, backend, commands: const []);

    expect(find.text('Ghi nhận mục'), findsNothing);
    expect(find.text('Loại trừ'), findsNothing);
    expect(find.text('Sửa'), findsNothing);
    expect(find.text('Xóa'), findsNothing);
  });

  testWidgets('FR_018_empty_saysNothingRecorded', (tester) async {
    backend.on('GET', '/residents/7/health-items', 200, <Object>[]);
    await pumpScreen(tester, backend);

    expect(find.text('Chưa ghi nhận dị ứng, bệnh nền hay tiền sử bệnh'), findsOneWidget);
  });

  testWidgets('FR_018_record_catalogAllergy_sendsBodyKeyAndReloads', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Ghi nhận mục');
    await pick(tester, 'Dị nguyên', 'Penicillin');
    await pick(tester, 'Mức độ', 'Nặng');
    await pick(tester, 'Nguồn thông tin', 'Người cao tuổi nói');
    await tapButton(tester, 'Ghi nhận');

    expect(lastBody(backend, '/residents/7/health-items'), {
      'type': 'ALLERGY',
      'allergenId': 3,
      'severityId': 4,
      'sourceId': 5,
    });
    expect(backend.calls('POST', '/residents/7/health-items').last.headers['Idempotency-Key'], isNotEmpty);
    expect(backend.calls('GET', '/residents/7/health-items'), hasLength(2));
    expect(find.text('Dị nguyên'), findsNothing);
  });

  testWidgets('FR_018_record_missingFields_showsErrorsAndSendsNothing', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Ghi nhận mục');
    await tapButton(tester, 'Ghi nhận');

    expect(find.text('Chọn dị nguyên'), findsOneWidget);
    expect(find.text('Chọn mức độ'), findsOneWidget);
    expect(find.text('Chọn nguồn thông tin'), findsOneWidget);
    expect(backend.calls('POST', '/residents/7/health-items'), isEmpty);
  });

  testWidgets('FR_018a_record_otherAllergy_warnsNotAutoCheckedAndSendsFreeText', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Ghi nhận mục');
    await tester.tap(find.widgetWithText(ChoiceChip, 'Dị ứng khác, nhập tay'));
    await tester.pumpAndSettle();
    expect(find.text('Dị ứng nhập tay không kiểm tra tự động'), findsOneWidget);

    await tester.enterText(find.widgetWithText(TextField, 'Nội dung'), ' Phấn hoa ');
    await pick(tester, 'Mức độ', 'Nặng');
    await pick(tester, 'Nguồn thông tin', 'Người cao tuổi nói');
    await tapButton(tester, 'Ghi nhận');

    expect(lastBody(backend, '/residents/7/health-items'), {
      'type': 'ALLERGY',
      'freeText': 'Phấn hoa',
      'severityId': 4,
      'sourceId': 5,
    });
  });

  testWidgets('FR_018_record_chronicCondition_hasNoSeverityField', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Ghi nhận mục');
    await tester.tap(find.widgetWithText(ChoiceChip, 'Bệnh nền'));
    await tester.pumpAndSettle();

    expect(find.widgetWithText(DropdownButtonFormField<int>, 'Mức độ'), findsNothing);
    await tester.enterText(find.widgetWithText(TextField, 'Nội dung'), 'Đái tháo đường');
    await pick(tester, 'Nguồn thông tin', 'Người cao tuổi nói');
    await tapButton(tester, 'Ghi nhận');

    expect(lastBody(backend, '/residents/7/health-items'), {
      'type': 'CHRONIC_CONDITION',
      'freeText': 'Đái tháo đường',
      'sourceId': 5,
    });
  });

  testWidgets('FR_021_record_duplicate409_offersConfirmDuplicateAndResends', (tester) async {
    await pumpScreen(tester, backend);
    backend.on('POST', '/residents/7/health-items', 409, {'errorCode': 'DUPLICATE_HEALTH_ITEM', 'detail': 'Trùng'});

    await tapButton(tester, 'Ghi nhận mục');
    await pick(tester, 'Dị nguyên', 'Penicillin');
    await pick(tester, 'Mức độ', 'Nặng');
    await pick(tester, 'Nguồn thông tin', 'Người cao tuổi nói');
    await tapButton(tester, 'Ghi nhận');

    expect(find.text('Đã có mục đang hiệu lực trùng nội dung'), findsOneWidget);

    backend.on('POST', '/residents/7/health-items', 201, {'items': item(), 'warnings': <Object>[]});
    await tapButton(tester, 'Vẫn ghi nhận mục này');

    expect(lastBody(backend, '/residents/7/health-items')['confirmDuplicate'], true);
    expect(find.text('Dị nguyên'), findsNothing);
  });

  testWidgets('FR_018a_record_warnings_showAfterSaving', (tester) async {
    await pumpScreen(tester, backend);
    backend.on('POST', '/residents/7/health-items', 201, {
      'items': item(),
      'warnings': [
        {'code': 'ALLERGY_NOT_AUTO_CHECKED', 'description': 'Dị ứng nhập tay không được kiểm tra tự động'},
      ],
    });

    await tapButton(tester, 'Ghi nhận mục');
    await tester.tap(find.widgetWithText(ChoiceChip, 'Bệnh nền'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Nội dung'), 'Hen');
    await pick(tester, 'Nguồn thông tin', 'Người cao tuổi nói');
    await tapButton(tester, 'Ghi nhận');

    expect(find.text('Đã ghi nhận, cần chú ý'), findsOneWidget);
    expect(find.text('Dị ứng nhập tay không được kiểm tra tự động'), findsOneWidget);
  });

  testWidgets('FR_018b_unverifiedItem_showsSelfReportedTag_andVerifyButtonForDoctorOrNurse', (tester) async {
    backend.on('GET', '/residents/7/health-items', 200, [item(unverified: true)]);
    backend.on('POST', '/health-items/1/verify', 200, item());
    await pumpScreen(tester, backend, commands: const [...both, 'VERIFY_HEALTH_ITEM']);

    expect(find.text('Tự khai, chưa được Bác sĩ kiểm chứng'), findsOneWidget);
    await tapButton(tester, 'Kiểm chứng');

    expect(backend.requests.any((r) => r.method == 'POST' && r.url.path == '/api/v1/health-items/1/verify'), isTrue);
  });

  testWidgets('FR_018b_unverifiedItem_withoutVerifyCommand_hasNoVerifyButton', (tester) async {
    backend.on('GET', '/residents/7/health-items', 200, [item(unverified: true)]);
    await pumpScreen(tester, backend, commands: const ['RECORD_HEALTH_ITEM']);

    expect(find.text('Tự khai, chưa được Bác sĩ kiểm chứng'), findsOneWidget);
    expect(button('Kiểm chứng'), findsNothing);
  });

  testWidgets('FR_019_exclude_requiresReasonThenPostsAndReloads', (tester) async {
    await pumpScreen(tester, backend);

    await tester.tap(button('Loại trừ').first);
    await tester.pumpAndSettle();
    await tapButton(tester, 'Xác nhận loại trừ');
    expect(find.text('Nhập lý do loại trừ'), findsOneWidget);
    expect(backend.calls('POST', '/health-items/1/exclude'), isEmpty);

    await tester.enterText(find.widgetWithText(TextField, 'Lý do loại trừ'), ' Ghi nhầm ');
    await tapButton(tester, 'Xác nhận loại trừ');

    expect(lastBody(backend, '/health-items/1/exclude'), {'reason': 'Ghi nhầm'});
    expect(backend.calls('POST', '/health-items/1/exclude').last.headers['Idempotency-Key'], isNotEmpty);
    expect(backend.calls('GET', '/residents/7/health-items'), hasLength(2));
  });

  testWidgets('FR_007_loadForbidden_showsBackendMessageAndRetry', (tester) async {
    backend.on('GET', '/residents/7/health-items', 403, {
      'errorCode': 'FORBIDDEN',
      'detail': 'Bạn không xem được mục này.',
    });
    await pumpScreen(tester, backend);

    expect(find.text('Bạn không xem được mục này.'), findsOneWidget);
    expect(find.text('Thử lại'), findsOneWidget);
  });

  testWidgets('FR_018_record_offline_saysNeedsConnectionAndKeepsForm', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Ghi nhận mục');
    await pick(tester, 'Dị nguyên', 'Penicillin');
    await pick(tester, 'Mức độ', 'Nặng');
    await pick(tester, 'Nguồn thông tin', 'Người cao tuổi nói');
    backend.down = true;
    await tapButton(tester, 'Ghi nhận');

    expect(find.textContaining('Không kết nối được máy chủ'), findsOneWidget);
    expect(find.text('Dị nguyên'), findsWidgets);
  });
}
