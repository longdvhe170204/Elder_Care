import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/residents/resident_lookup_screen.dart';

import '../../support/fake_backend.dart';

// 001 T023 (tên logic tra_cuu_nguoi_cao_tuoi_test; US1; FR-007, FR-008, FR-009): app nhân viên tra cứu hồ sơ trong phạm vi
// (GET /residents: ô tìm q, lọc status, phân trang cursor) và mở thẻ người cao tuổi (GET /residents/{id}). Chỉ đọc, chỉ
// trực tuyến. Trường bị bỏ theo quyền nằm ở hiddenFields. Lệnh (cập nhật, trạng thái, sức khỏe, đánh giá) thuộc task khác.

Map<String, dynamic> profile({
  int id = 7,
  String name = '[Họ tên 1]',
  String code = 'HS-0007',
  String status = 'IN_RESIDENCE',
  String? careLevel = 'REGULAR',
  String? careNote,
  List<Map<String, dynamic>>? health,
  List<String> hidden = const [],
  List<String> careNeeds = const [],
}) => {
  'id': id,
  'profileCode': code,
  'personalInfo': {
    'fullName': name,
    'dateOfBirth': '1945-03-09',
    'gender': 'FEMALE',
    'idDocumentType': 'CITIZEN_ID',
    'idDocumentNumber': '000000000001',
    'permanentAddress': '[Địa chỉ 1]',
    'contact': {'phoneNumber': '0900000001'},
  },
  'status': status,
  'statusSince': '2026-10-01T02:00:00Z',
  'currentResidenceType': 'LONG_TERM_RESIDENTIAL',
  'currentCareLevel': ?careLevel,
  'careNeeds': careNeeds,
  'careNote': careNote,
  'activeHealthItems': ?health,
  'hasActiveConsent': true,
  'allowedCommands': <String>[],
  'hiddenFields': hidden,
};

Map<String, dynamic> allergy(String name) => {
  'id': 1,
  'type': 'ALLERGY',
  'allergen': {'id': 3, 'name': name},
  'status': 'ACTIVE',
};

Map<String, dynamic> page(List<Map<String, dynamic>> items, {String? next}) => {'items': items, 'nextCursor': next};

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: ResidentLookupScreen(api: backend.client())),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on(
      'GET',
      '/residents',
      200,
      page([profile(), profile(id: 8, name: '[Họ tên 2]', code: 'HS-0008', status: 'HOSPITALIZED')]),
    );
    backend.on('GET', '/residents/7', 200, profile());
    backend.on('GET', '/residents/7/linked-profiles', 200, <Object>[]);
  });

  testWidgets('FR_007_lookup_showsNameCodeCareLevelAndStatus', (tester) async {
    await pumpScreen(tester, backend);

    expect(backend.requests.first.url.queryParameters, isEmpty);
    expect(find.text('[Họ tên 1]'), findsOneWidget);
    expect(find.text('HS-0007 · Chăm sóc thường xuyên'), findsOneWidget);
    expect(find.text('Đang lưu trú'), findsWidgets);
    expect(find.text('Điều trị tại bệnh viện'), findsWidgets);
  });

  testWidgets('FR_007_search_sendsQueryAndKeepsFilter', (tester) async {
    await pumpScreen(tester, backend);

    await tester.enterText(find.byType(TextField), 'Họ tên 2');
    await tester.testTextInput.receiveAction(TextInputAction.search);
    await tester.pumpAndSettle();

    expect(backend.requests.last.url.queryParameters['q'], 'Họ tên 2');
  });

  testWidgets('FR_007_statusFilter_sendsStatusQuery', (tester) async {
    await pumpScreen(tester, backend);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Tạm vắng'));
    await tester.pumpAndSettle();

    expect(backend.requests.last.url.queryParameters['status'], 'TEMPORARILY_ABSENT');
  });

  testWidgets('FR_007_emptyResult_saysNoMatchingProfile', (tester) async {
    backend.on('GET', '/residents', 200, page([]));
    await pumpScreen(tester, backend);

    expect(find.text('Không có hồ sơ khớp điều kiện'), findsOneWidget);
  });

  testWidgets('FR_007_loadMore_sendsCursorAndAppends', (tester) async {
    backend.on('GET', '/residents', 200, page([profile()], next: 'c2'));
    await pumpScreen(tester, backend);

    backend.on('GET', '/residents', 200, page([profile(id: 9, name: '[Họ tên 3]', code: 'HS-0009')]));
    await tester.tap(find.text('Xem thêm'));
    await tester.pumpAndSettle();

    expect(backend.requests.last.url.queryParameters['cursor'], 'c2');
    expect(find.text('[Họ tên 1]'), findsOneWidget);
    expect(find.text('[Họ tên 3]'), findsOneWidget);
    expect(find.text('Xem thêm'), findsNothing);
  });

  testWidgets('FR_007_forbiddenRole_showsBackendMessage', (tester) async {
    backend.on('GET', '/residents', 403, {'errorCode': 'FORBIDDEN', 'detail': 'Bạn không có quyền xem hồ sơ.'});
    await pumpScreen(tester, backend);

    expect(find.text('Bạn không có quyền xem hồ sơ.'), findsOneWidget);
  });

  testWidgets('FR_007_offline_saysNeedsConnectionAndRetries', (tester) async {
    backend.down = true;
    await pumpScreen(tester, backend);
    expect(find.textContaining('Không kết nối được máy chủ'), findsOneWidget);

    backend.down = false;
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(find.text('[Họ tên 1]'), findsOneWidget);
  });

  testWidgets('FR_008_card_showsAllergyCareNoteAndPersonalInfo', (tester) async {
    backend.on(
      'GET',
      '/residents/7',
      200,
      profile(careNote: 'Cần hỗ trợ khi đứng dậy', health: [allergy('Penicillin')], careNeeds: ['Hỗ trợ vệ sinh']),
    );
    await pumpScreen(tester, backend);

    await tester.tap(find.text('[Họ tên 1]'));
    await tester.pumpAndSettle();

    expect(backend.calls('GET', '/residents/7'), hasLength(1));
    expect(find.text('Dị ứng: Penicillin'), findsOneWidget);
    expect(find.text('Cần hỗ trợ khi đứng dậy'), findsOneWidget);
    expect(find.text('09/03/1945'), findsOneWidget);
    expect(find.text('Nữ'), findsOneWidget);
    expect(find.text('CCCD 000000000001'), findsOneWidget);
    expect(find.text('Chăm sóc thường xuyên'), findsOneWidget);
    expect(find.text('Nội trú dài hạn'), findsOneWidget);
    expect(find.text('từ 01/10/2026'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Hỗ trợ vệ sinh'), 200);
    expect(find.text('Hỗ trợ vệ sinh'), findsOneWidget);
  });

  testWidgets('FR_008_card_noAllergyOrNote_saysNotRecorded', (tester) async {
    backend.on('GET', '/residents/7', 200, profile(health: []));
    await pumpScreen(tester, backend);

    await tester.tap(find.text('[Họ tên 1]'));
    await tester.pumpAndSettle();

    expect(find.text('An toàn'), findsOneWidget);
    expect(find.text('Chưa ghi nhận'), findsWidgets);
  });

  testWidgets('FR_008_card_healthHiddenByRole_noSafetySectionAndNotice', (tester) async {
    backend.on('GET', '/residents/7', 200, profile(hidden: ['activeHealthItems']));
    await pumpScreen(tester, backend);

    await tester.tap(find.text('[Họ tên 1]'));
    await tester.pumpAndSettle();

    expect(find.text('An toàn'), findsNothing);
    expect(find.text('Một số thông tin không hiển thị theo quyền của bạn.'), findsOneWidget);
  });

  testWidgets('FR_004_card_showsLinkedPreviousProfiles', (tester) async {
    backend.on('GET', '/residents/7/linked-profiles', 200, [
      {'id': 2, 'profileCode': 'HS-0002', 'status': 'RESIDENCY_ENDED', 'viewable': true},
      {'id': 3, 'profileCode': 'HS-0003', 'viewable': false},
    ]);
    await pumpScreen(tester, backend);

    await tester.tap(find.text('[Họ tên 1]'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('HS-0003 · không có quyền xem'), 200);

    expect(find.text('HS-0002 · Kết thúc lưu trú'), findsOneWidget);
    expect(find.text('HS-0003 · không có quyền xem'), findsOneWidget);
  });

  testWidgets('FR_007_card_outOfScope404_showsMessageAndRetry', (tester) async {
    backend.on('GET', '/residents/7', 404, {'errorCode': 'NOT_FOUND', 'detail': 'Không tìm thấy hồ sơ.'});
    await pumpScreen(tester, backend);

    await tester.tap(find.text('[Họ tên 1]'));
    await tester.pumpAndSettle();

    expect(find.text('Không tìm thấy hồ sơ.'), findsOneWidget);
    expect(find.text('Thử lại'), findsOneWidget);
  });

  testWidgets('FR_007_screen_doesNotWriteAnything', (tester) async {
    await pumpScreen(tester, backend);
    await tester.tap(find.text('[Họ tên 1]'));
    await tester.pumpAndSettle();

    expect(backend.requests.every((r) => r.method == 'GET'), isTrue);
  });

  testWidgets('FR_008_card_parts_showHealthAndStatusButtonsAndOpenStatus', (tester) async {
    backend.on('GET', '/residents/7/status-history', 200, <Object>[]);
    await pumpScreen(tester, backend);

    await tester.tap(find.text('[Họ tên 1]'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.widgetWithText(OutlinedButton, 'Trạng thái'), 200);
    await tester.ensureVisible(find.widgetWithText(OutlinedButton, 'Trạng thái'));
    await tester.pumpAndSettle();
    expect(find.widgetWithText(OutlinedButton, 'Sức khỏe'), findsOneWidget);

    await tester.tap(find.widgetWithText(OutlinedButton, 'Trạng thái'));
    await tester.pumpAndSettle();

    expect(find.text('Trạng thái hiện tại'), findsOneWidget);
    expect(backend.calls('GET', '/residents/7/status-history'), hasLength(1));
  });

  testWidgets('FR_008_card_parts_healthHiddenByRole_hasNoHealthButton', (tester) async {
    backend.on('GET', '/residents/7', 200, profile(hidden: ['activeHealthItems']));
    await pumpScreen(tester, backend);

    await tester.tap(find.text('[Họ tên 1]'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.widgetWithText(OutlinedButton, 'Trạng thái'), 200);
    await tester.ensureVisible(find.widgetWithText(OutlinedButton, 'Trạng thái'));
    await tester.pumpAndSettle();

    expect(find.widgetWithText(OutlinedButton, 'Sức khỏe'), findsNothing);
  });

  testWidgets('FR_018_card_parts_openHealthWithAllowedCommands', (tester) async {
    backend.on('GET', '/residents/7', 200, {
      ...profile(),
      'allowedCommands': ['RECORD_HEALTH_ITEM'],
    });
    backend.on('GET', '/residents/7/health-items', 200, <Object>[]);
    await pumpScreen(tester, backend);

    await tester.tap(find.text('[Họ tên 1]'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.widgetWithText(OutlinedButton, 'Sức khỏe'), 200);
    await tester.ensureVisible(find.widgetWithText(OutlinedButton, 'Sức khỏe'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(OutlinedButton, 'Sức khỏe'));
    await tester.pumpAndSettle();

    expect(find.text('Dị ứng, bệnh nền, tiền sử bệnh'), findsOneWidget);
    expect(find.text('Ghi nhận mục'), findsOneWidget);
    expect(find.text('Loại trừ'), findsNothing);
  });
}
