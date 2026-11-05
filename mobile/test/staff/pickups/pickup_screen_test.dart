import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/pickups/pickup_screen.dart';

import '../../support/fake_backend.dart';

// 012 T021 (US2; FR-021, FR-023, FR-026, FR-028): nhân viên ghi đón trên app, chỉ trực tuyến. Chọn người cao tuổi, loại
// đón, người đón trong danh sách được phép đón (hoặc theo ngoại lệ), giấy tờ xuất trình, nhân viên bàn giao. Bị chặn thì
// hiện lý do và báo đã ghi lần chặn; mất mạng thì không xếp hàng gửi sau, giữ phần đang nhập.

const _residents = {
  'items': [
    {
      'id': 5,
      'profileCode': 'NCT-0005',
      'personalInfo': {'fullName': 'Nguyễn Thị Lan'},
      'status': 'IN_RESIDENCE',
    },
  ],
  'nextCursor': null,
};

const _pickupList = [
  {'familyMemberId': 32, 'fullName': 'Trần Thu Hà', 'phoneNumber': '0901000222', 'relationship': 'GRANDCHILD'},
];

const _staff = [
  {'id': 9003, 'fullName': 'Lê Thị Mai', 'position': 'Hành chính'},
];

FakeBackend backend() => FakeBackend()
  ..on('GET', '/residents', 200, _residents)
  ..on('GET', '/residents/5/pickup-list', 200, _pickupList)
  ..on('GET', '/staff', 200, _staff);

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: PickupScreen(api: api)),
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

Future<void> openResident(WidgetTester tester) async {
  await tapText(tester, 'Nguyễn Thị Lan');
  expect(find.text('Ghi đón cho Nguyễn Thị Lan'), findsOneWidget);
}

Future<void> enter(WidgetTester tester, String label, String value) async {
  final field = find.widgetWithText(TextField, label);
  await tester.ensureVisible(field);
  await tester.enterText(field, value);
  await tester.pumpAndSettle();
}

Future<void> chooseStaff(WidgetTester tester, String label) async {
  final dropdown = find.widgetWithText(DropdownButtonFormField<int>, label);
  await tester.ensureVisible(dropdown);
  await tester.tap(dropdown);
  await tester.pumpAndSettle();
  await tester.tap(find.text('Lê Thị Mai').last);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_021_ghiDonTrongDanhSach_guiDungThan', (tester) async {
    final fake = backend()
      ..on('POST', '/residents/5/pickups', 201, {
        'id': 53,
        'type': 'TEMPORARY_ABSENCE',
        'familyMemberId': 32,
        'basis': 'PICKUP_LIST',
        'at': '2026-11-05T02:00:00Z',
        'status': 'UNUSED',
        'expiresAt': '2026-11-05T02:30:00Z',
      });
    await pumpScreen(tester, fake.client());
    await openResident(tester);

    await tapText(tester, 'Ghi bản ghi đón');
    expect(find.text('Chọn loại đón'), findsOneWidget);
    expect(find.text('Chọn người đón'), findsOneWidget);
    expect(find.text('Nhập số giấy tờ xuất trình'), findsOneWidget);
    expect(find.text('Chọn nhân viên bàn giao'), findsOneWidget);
    expect(fake.calls('POST', '/residents/5/pickups'), isEmpty);

    await tapText(tester, 'Tạm vắng');
    await tapText(tester, 'Trần Thu Hà');
    await enter(tester, 'Loại giấy tờ xuất trình', 'CCCD');
    await enter(tester, 'Số giấy tờ xuất trình', ' 001234567890 ');
    await chooseStaff(tester, 'Nhân viên bàn giao');
    await tapText(tester, 'Ghi bản ghi đón');

    final call = fake.calls('POST', '/residents/5/pickups').single;
    expect(jsonDecode(call.body), {
      'type': 'TEMPORARY_ABSENCE',
      'familyMemberId': 32,
      'presentedIdDocument': {'type': 'CCCD', 'number': '001234567890'},
      'handoverStaffId': 9003,
    });
    expect(call.headers['Idempotency-Key'], matches(RegExp(r'^[0-9a-f-]{36}$')));
    expect(find.text('Đã ghi đón'), findsOneWidget);
  });

  testWidgets('FR_021_tuVe_khongCoNguoiDonVaGiayTo', (tester) async {
    final fake = backend()..on('POST', '/residents/5/pickups', 201, {'id': 54, 'type': 'SELF_RETURN'});
    await pumpScreen(tester, fake.client());
    await openResident(tester);

    await tapText(tester, 'Tự về');
    expect(find.text('Người đón'), findsNothing);
    expect(find.widgetWithText(TextField, 'Số giấy tờ xuất trình'), findsNothing);
    await chooseStaff(tester, 'Nhân viên tiễn');
    await tapText(tester, 'Ghi bản ghi đón');

    expect(jsonDecode(fake.calls('POST', '/residents/5/pickups').single.body), {
      'type': 'SELF_RETURN',
      'handoverStaffId': 9003,
    });
  });

  testWidgets('FR_023_biChan_hienLyDoVaLanChanDaGhi', (tester) async {
    final fake = backend()
      ..on('POST', '/residents/5/pickups', 409, {
        'status': 409,
        'errorCode': 'PICKUP_BLOCKED',
        'detail': 'Lượt đón bị chặn',
        'blockReason': 'NOT_ON_LIST',
      });
    await pumpScreen(tester, fake.client());
    await openResident(tester);

    await tapText(tester, 'Bán trú về');
    await tapText(tester, 'Trần Thu Hà');
    await enter(tester, 'Số giấy tờ xuất trình', '999');
    await chooseStaff(tester, 'Nhân viên bàn giao');
    await tapText(tester, 'Ghi bản ghi đón');

    expect(find.text('Người đón không thuộc danh sách được phép đón và không có ngoại lệ Hiệu lực.'), findsOneWidget);
    expect(find.textContaining('Đã ghi lần chặn đón'), findsOneWidget);
    expect(find.text('999'), findsOneWidget);
  });

  testWidgets('FR_028_matMang_khongXepHangGuiSau_giuPhanDangNhap', (tester) async {
    final fake = backend();
    await pumpScreen(tester, fake.client());
    await openResident(tester);

    await tapText(tester, 'Đi chơi với gia đình');
    await tapText(tester, 'Người đón theo ngoại lệ');
    await enter(tester, 'Mã yêu cầu ngoại lệ', '81');
    await enter(tester, 'Số giấy tờ xuất trình', '0011');
    await chooseStaff(tester, 'Nhân viên bàn giao');
    fake.down = true;
    await tapText(tester, 'Ghi bản ghi đón');

    expect(
      find.text('Ghi đón cần kết nối mạng, không lưu để gửi sau. Thông tin đang nhập được giữ lại.'),
      findsOneWidget,
    );
    expect(find.text('0011'), findsOneWidget);

    fake
      ..down = false
      ..on('POST', '/residents/5/pickups', 201, {'id': 55});
    await tapText(tester, 'Ghi bản ghi đón');
    final calls = fake.calls('POST', '/residents/5/pickups');
    expect(jsonDecode(calls.last.body), {
      'type': 'FAMILY_OUTING',
      'exceptionId': 81,
      'presentedIdDocument': {'number': '0011'},
      'handoverStaffId': 9003,
    });
    // Gửi lại cùng một lệnh dùng cùng khóa idempotency
    expect(calls.first.headers['Idempotency-Key'], calls.last.headers['Idempotency-Key']);
  });
}
