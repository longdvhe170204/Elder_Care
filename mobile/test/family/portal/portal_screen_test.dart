import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/portal/portal_screen.dart';

import '../../support/fake_backend.dart';

// 012 T028 (US5; FR-046 → FR-048, BR-M10-01; Q-305 cùng chức năng với cổng web): app người thân có phần cổng. Danh sách
// người cao tuổi có quan hệ Hiệu lực; thông tin chung cho mọi người thân; mục Sức khỏe chỉ khi quyền có tác dụng (phần
// chưa có nguồn ghi rõ chưa có dữ liệu); nút đăng ký thăm khi có quyền đăng ký thăm.

Map<String, dynamic> rights({bool health = false, bool visits = false, bool costs = false}) => {
  'VIEW_HEALTH': health,
  'VIEW_COSTS': costs,
  'RECEIVE_URGENT': false,
  'REGISTER_VISITS': visits,
  'PICKUP_ALLOWED': false,
  'REQUEST_SERVICES': false,
};

Map<String, dynamic> resident({
  bool health = false,
  bool visits = false,
  bool belongings = false,
  bool costs = false,
  bool representative = false,
}) => {
  'residentId': 5,
  'fullName': 'Nguyễn Thị Lan',
  'relationship': 'CHILD',
  'representative': representative,
  'primaryContact': false,
  'rights': rights(health: health, visits: visits, costs: costs),
  'canViewBelongings': belongings,
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
  'unavailable': ['menu', 'incidents'],
};

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: PortalScreen(api: api)),
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
  testWidgets('FR_037_quyenXemChiPhi_moBangChiPhiCua010', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [resident(costs: true)])
      ..on('GET', '/family-portal/residents/5/general', 200, _general)
      ..on('GET', '/residents/5/costs/statements', 200, <Object>[]);
    await pumpScreen(tester, fake.client());

    await tapText(tester, 'Nguyễn Thị Lan');
    await tapText(tester, 'Xem bảng chi phí');
    expect(fake.calls('GET', '/residents/5/costs/statements'), hasLength(1));
  });

  testWidgets('BR_M10_10_nguoiDaiDien_moDeNghiMuaHoCanDongY', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [resident(representative: true)])
      ..on('GET', '/family-portal/residents/5/general', 200, _general)
      ..on('GET', '/purchase-assistance-requests', 200, <Object>[]);
    await pumpScreen(tester, fake.client());

    await tapText(tester, 'Nguyễn Thị Lan');
    await tapText(tester, 'Đề nghị mua hộ');
    expect(fake.calls('GET', '/purchase-assistance-requests'), hasLength(1));
  });

  testWidgets('BR_M10_10_khongPhaiNguoiDaiDien_khongCoDeNghiMuaHo', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [resident(costs: true)])
      ..on('GET', '/family-portal/residents/5/general', 200, _general);
    await pumpScreen(tester, fake.client());

    await tapText(tester, 'Nguyễn Thị Lan');
    expect(find.text('Đề nghị mua hộ'), findsNothing);
  });

  testWidgets('FR_046b_nguoiDuocPhepXemDoGui_moManDoGuiCua013', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [resident(belongings: true)])
      ..on('GET', '/family-portal/residents/5/general', 200, _general)
      ..on('GET', '/family-portal/residents/5/belongings', 200, <Object>[]);
    await pumpScreen(tester, fake.client());

    await tapText(tester, 'Nguyễn Thị Lan');
    await tapText(tester, 'Xem đồ gửi');
    expect(find.text('Đồ gửi'), findsOneWidget);
    expect(fake.calls('GET', '/family-portal/residents/5/belongings'), hasLength(1));
  });

  testWidgets('FR_046b_khongDuocXemDoGui_khongCoNut', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [resident()])
      ..on('GET', '/family-portal/residents/5/general', 200, _general);
    await pumpScreen(tester, fake.client());

    await tapText(tester, 'Nguyễn Thị Lan');
    expect(find.text('Xem đồ gửi'), findsNothing);
  });

  testWidgets('US5_2_khongCoQuyenSucKhoe_chiThayPhanChung', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [resident()])
      ..on('GET', '/family-portal/residents/5/general', 200, _general);
    await pumpScreen(tester, fake.client());

    await tapText(tester, 'Nguyễn Thị Lan');
    expect(find.text('Phòng 201, giường A'), findsOneWidget);
    expect(find.text('Đang lưu trú'), findsOneWidget);
    expect(find.text('Xem sức khỏe'), findsNothing);
    expect(find.text('Đăng ký thăm'), findsNothing);
    expect(find.textContaining('chỉ hiện với người thân được người cao tuổi đồng ý chia sẻ'), findsOneWidget);
  });

  testWidgets('US5_1_coQuyenSucKhoe_xemCacPhan', (tester) async {
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [resident(health: true, visits: true)])
      ..on('GET', '/family-portal/residents/5/general', 200, _general)
      ..on('GET', '/family-portal/residents/5/health/allergies-conditions', 200, {
        'section': 'allergies-conditions',
        'available': true,
        'items': [
          {'type': 'CHRONIC_CONDITION', 'text': 'Tăng huyết áp'},
        ],
      })
      ..on('GET', '/family-portal/residents/5/health/medications', 200, {
        'section': 'medications',
        'available': false,
        'note': 'Thuốc: chưa có dữ liệu, tính năng nguồn chưa hoạt động',
        'items': <Object>[],
      });
    await pumpScreen(tester, fake.client());

    await tapText(tester, 'Nguyễn Thị Lan');
    expect(find.text('Đăng ký thăm'), findsOneWidget);
    await tapText(tester, 'Xem sức khỏe');

    expect(find.text('Tăng huyết áp'), findsOneWidget);
    expect(find.text('Thuốc: chưa có dữ liệu, tính năng nguồn chưa hoạt động'), findsOneWidget);
    expect(fake.calls('GET', '/family-portal/residents/5/health/allergies-conditions'), hasLength(1));
  });

  testWidgets('FR_046_anUong_luongNuocTrungBinhVaSoNgayCoDuLieu', (tester) async {
    // Chủ dự án chốt 2026-11-12: trung bình theo ngày có ghi nước, kèm số ngày có dữ liệu
    final fake = FakeBackend()
      ..on('GET', '/family-portal/residents', 200, [resident(health: true, visits: true)])
      ..on('GET', '/family-portal/residents/5/general', 200, _general)
      ..on('GET', '/family-portal/residents/5/health/eating', 200, {
        'section': 'eating',
        'available': true,
        'items': [
          {'averageWaterMl': 1450, 'waterDays': 6},
        ],
      });
    await pumpScreen(tester, fake.client());

    await tapText(tester, 'Nguyễn Thị Lan');
    await tapText(tester, 'Xem sức khỏe');
    final water = find.text('Lượng nước trung bình 1450 ml/ngày, tính trên 6 ngày có ghi');
    await tester.scrollUntilVisible(water, 200);

    expect(water, findsOneWidget);
  });

  testWidgets('US5_7_khongCoNguoiCaoTuoi_baoRo', (tester) async {
    final fake = FakeBackend()..on('GET', '/family-portal/residents', 200, <Object>[]);
    await pumpScreen(tester, fake.client());
    expect(find.text('Chưa có người cao tuổi nào có quan hệ Hiệu lực với tài khoản này.'), findsOneWidget);
  });
}
