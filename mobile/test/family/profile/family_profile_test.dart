import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/profile/family_profile_screen.dart';

import '../../support/fake_backend.dart';

// 001 T023 (tên logic ho_so_nguoi_than_test; US1, US2; FR-022): app người thân xem hồ sơ người cao tuổi (GET /residents,
// GET /residents/{id}; dạng tập con FamilyProfile). Phần sức khỏe chỉ có khi thỏa chú thích ¹: backend bỏ activeHealthItems
// và ghi vào hiddenFields thì app không hiện phần này; có thì chỉ hiện mục đang Hiệu lực. Chỉ đọc, chỉ trực tuyến.

Map<String, dynamic> familyProfile({
  int id = 7,
  String name = '[Họ tên 1]',
  String status = 'IN_RESIDENCE',
  List<Map<String, dynamic>>? health,
  List<String> hidden = const [],
}) => {
  'id': id,
  'personalInfo': {'fullName': name, 'dateOfBirth': '1945-03-09', 'gender': 'FEMALE'},
  'status': status,
  'currentCareLevel': 'SPECIAL',
  'activeHealthItems': ?health,
  'hiddenFields': hidden,
};

Map<String, dynamic> item(
  String type, {
  String? allergen,
  String? freeText,
  String? severity,
  String status = 'ACTIVE',
}) => {
  'id': 1,
  'type': type,
  'allergen': allergen == null ? null : {'id': 3, 'name': allergen},
  'freeText': freeText,
  'severity': severity,
  'status': status,
};

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: FamilyProfileScreen(api: backend.client())),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> openProfile(WidgetTester tester) async {
  await tester.tap(find.text('[Họ tên 1]'));
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/residents', 200, {
      'items': [familyProfile()],
      'nextCursor': null,
    });
    backend.on('GET', '/residents/7', 200, familyProfile());
  });

  testWidgets('FR_007_list_showsNameCareLevelAndStatus', (tester) async {
    await pumpScreen(tester, backend);

    expect(find.text('[Họ tên 1]'), findsOneWidget);
    expect(find.text('Chăm sóc đặc biệt'), findsOneWidget);
    expect(find.text('Đang lưu trú'), findsOneWidget);
  });

  testWidgets('FR_007_list_empty_saysNoProfileToShow', (tester) async {
    backend.on('GET', '/residents', 200, {'items': <Object>[], 'nextCursor': null});
    await pumpScreen(tester, backend);

    expect(find.text('Chưa có hồ sơ nào để xem'), findsOneWidget);
  });

  testWidgets('FR_007_list_offline_showsMessageAndRetries', (tester) async {
    backend.down = true;
    await pumpScreen(tester, backend);
    expect(find.textContaining('Không kết nối được máy chủ'), findsOneWidget);

    backend.down = false;
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(find.text('[Họ tên 1]'), findsOneWidget);
  });

  testWidgets('FR_022_detail_withConsent_showsOnlyActiveHealthItemsGrouped', (tester) async {
    backend.on(
      'GET',
      '/residents/7',
      200,
      familyProfile(
        health: [
          item('MEDICAL_HISTORY', freeText: 'Mổ ruột thừa'),
          item('ALLERGY', allergen: 'Penicillin', severity: 'Nặng'),
          item('CHRONIC_CONDITION', freeText: 'Tăng huyết áp'),
          item('ALLERGY', freeText: 'Phấn hoa', status: 'EXCLUDED'),
        ],
      ),
    );
    await pumpScreen(tester, backend);
    await openProfile(tester);

    expect(find.text('Sức khỏe'), findsOneWidget);
    expect(find.text('Dị ứng: Penicillin (Nặng)'), findsOneWidget);
    expect(find.text('Tăng huyết áp'), findsOneWidget);
    expect(find.text('Mổ ruột thừa'), findsOneWidget);
    expect(find.textContaining('Phấn hoa'), findsNothing);
  });

  testWidgets('FR_022_detail_withoutConsent_hidesHealthSectionAndSaysNothingElse', (tester) async {
    backend.on('GET', '/residents/7', 200, familyProfile(hidden: ['activeHealthItems']));
    await pumpScreen(tester, backend);
    await openProfile(tester);

    expect(find.text('Sức khỏe'), findsNothing);
    expect(find.textContaining('Dị ứng'), findsNothing);
  });

  testWidgets('FR_022_detail_consentWithNoItems_saysNotRecorded', (tester) async {
    backend.on('GET', '/residents/7', 200, familyProfile(health: []));
    await pumpScreen(tester, backend);
    await openProfile(tester);

    expect(find.text('Sức khỏe'), findsOneWidget);
    expect(find.text('Chưa ghi nhận'), findsOneWidget);
  });

  testWidgets('FR_007_detail_showsBasicInfoWithoutAddressOrIdDocument', (tester) async {
    await pumpScreen(tester, backend);
    await openProfile(tester);

    expect(find.text('09/03/1945'), findsOneWidget);
    expect(find.text('Nữ'), findsOneWidget);
    expect(find.text('Chăm sóc đặc biệt'), findsOneWidget);
    expect(find.text('Lưu ý chăm sóc'), findsNothing);
    expect(find.text('Địa chỉ thường trú'), findsNothing);
  });

  testWidgets('FR_007_detail_outOfScope404_showsBackendMessage', (tester) async {
    backend.on('GET', '/residents/7', 404, {'errorCode': 'NOT_FOUND', 'detail': 'Không tìm thấy hồ sơ.'});
    await pumpScreen(tester, backend);
    await openProfile(tester);

    expect(find.text('Không tìm thấy hồ sơ.'), findsOneWidget);
  });

  testWidgets('FR_007_screen_doesNotWriteAnything', (tester) async {
    await pumpScreen(tester, backend);
    await openProfile(tester);

    expect(backend.requests.every((r) => r.method == 'GET'), isTrue);
  });
}
