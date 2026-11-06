import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/portal/portal_screen.dart';

import '../../support/fake_backend.dart';

// 002 T072 (US6; FR-046 giới hạn trường, plan 002 "Chú thích ¹"): app người thân chỉ hiện phần sức khỏe khi backend trả.
// Backend (`FieldVisibilityService`) xét điều kiện đồng ý chia sẻ ở chính lần xem; app không tự suy quyền, không giữ dữ
// liệu của phần bị chặn: phản hồi không có quyền Xem sức khỏe thì không có lối vào, phần trả 403 thì chỉ hiện câu khóa.

const _lock = 'Thông tin sức khỏe chỉ hiện với người thân được người cao tuổi đồng ý chia sẻ.';

Map<String, dynamic> _resident(Map<String, dynamic> rights) => {
  'residentId': 5,
  'fullName': 'Nguyễn Thị Lan',
  'relationship': 'CHILD',
  'representative': false,
  'primaryContact': false,
  'rights': rights,
  'canViewBelongings': false,
};

const _general = {
  'residentId': 5,
  'fullName': 'Nguyễn Thị Lan',
  'status': 'IN_RESIDENCE',
  'location': 'Phòng 201, giường A',
  'visits': <Object>[],
  'absences': <Object>[],
  'dayCareSchedule': <Object>[],
  'incidents': <Object>[],
  'unavailable': <Object>[],
};

Future<void> _openResident(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: PortalScreen(api: api)),
    ),
  );
  await tester.pumpAndSettle();
  await tester.tap(find.text('Nguyễn Thị Lan').first);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_046_phanHoiKhongCoQuyenXemSucKhoe_khongCoLoiVao', (tester) async {
    // Backend không trả khóa VIEW_HEALTH: app coi như không có quyền
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [
        _resident({'REGISTER_VISITS': false, 'PICKUP_ALLOWED': true}),
      ])
      ..on('GET', '/family-portal/residents/5/general', 200, _general);

    await _openResident(tester, fake.client());

    expect(find.text('Phòng 201, giường A'), findsOneWidget);
    expect(find.text('Xem sức khỏe'), findsNothing);
    expect(find.text(_lock), findsOneWidget);
    expect(fake.requests.where((r) => r.url.path.contains('/health/')), isEmpty);
  });

  testWidgets('FR_046_phanBiChanLucXem_chiHienCauKhoa_phanKhacVanHien', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [
        _resident({'VIEW_HEALTH': true}),
      ])
      ..on('GET', '/family-portal/residents/5/general', 200, _general)
      // Đồng ý chia sẻ vừa bị rút: backend chặn phần dị ứng ở chính lần xem
      ..on('GET', '/family-portal/residents/5/health/allergies-conditions', 403, {
        'status': 403,
        'errorCode': 'PERMISSION_DENIED',
        'detail': 'Không có quyền',
      })
      ..on('GET', '/family-portal/residents/5/health/medications', 200, {
        'section': 'medications',
        'available': true,
        'items': [
          {'medicationName': 'Amlodipin 5 mg'},
        ],
      });

    await _openResident(tester, fake.client());
    await tester.tap(find.text('Xem sức khỏe'));
    await tester.pumpAndSettle();

    expect(find.text('Dị ứng, bệnh nền'), findsOneWidget);
    expect(find.text(_lock), findsOneWidget);
    expect(find.text('Không có quyền'), findsNothing);
    expect(find.text('Amlodipin 5 mg'), findsOneWidget);
  });
}
