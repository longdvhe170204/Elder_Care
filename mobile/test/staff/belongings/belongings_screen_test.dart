import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/belongings/belongings_screen.dart';

import '../../support/fake_backend.dart';

// 013 T011 (tên logic nhan-vien/do-gui; US1; FR-003 → FR-008, FR-031): app nhân viên xem đồ gửi theo người cao tuổi và
// tiếp nhận đồ có chụp ảnh; đồ có giá trị bắt buộc ảnh; lệnh đồ gửi chỉ trực tuyến, mất mạng thì báo, không xếp hàng.

const types = [
  {
    'id': 1,
    'name': 'Quần áo',
    'hasCashAmount': false,
    'requiresConsentForUse': true,
    'valuable': false,
    'status': 'ACTIVE',
  },
  {
    'id': 2,
    'name': 'Điện thoại',
    'hasCashAmount': false,
    'requiresConsentForUse': true,
    'valuable': true,
    'status': 'ACTIVE',
  },
];

FakeBackend backend() {
  final b = FakeBackend()
    ..on('GET', '/belongings/types', 200, types)
    ..on('GET', '/belongings/condition-levels', 200, [
      {'id': 11, 'name': 'Nguyên vẹn', 'order': 1, 'unusable': false, 'status': 'ACTIVE'},
    ])
    ..on('GET', '/belongings/storage-locations', 200, [
      {'id': 21, 'name': 'Kho tầng 2', 'status': 'ACTIVE'},
    ])
    ..on('GET', '/residents', 200, {
      'items': [
        {
          'id': 7,
          'profileCode': 'NCT-0007',
          'personalInfo': {'fullName': 'Nguyễn Thị Lan'},
        },
      ],
      'nextCursor': null,
    })
    ..on('GET', '/residents/7/belongings', 200, [
      {
        'id': 90,
        'residentId': 7,
        'typeId': 2,
        'description': 'Điện thoại Nokia',
        'quantity': 1,
        'status': 'HELD',
        'holder': 'STAFF',
        'version': 1,
      },
    ]);
  return b;
}

Future<void> pump(WidgetTester tester, ApiClient api, {PhotoPicker? picker}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: BelongingsScreen(api: api, pickPhoto: picker ?? () async => null),
      ),
    ),
  );
  await tester.pumpAndSettle();
  await tester.enterText(find.byKey(const Key('belongings-search')), 'Lan');
  await tester.tap(find.text('Tìm'));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Nguyễn Thị Lan'));
  await tester.pumpAndSettle();
}

Future<void> fillIntake(WidgetTester tester, {required String type}) async {
  await tester.tap(find.text('Tiếp nhận đồ'));
  await tester.pumpAndSettle();
  await tester.tap(find.byKey(const Key('intake-type')));
  await tester.pumpAndSettle();
  await tester.tap(find.text(type).last);
  await tester.pumpAndSettle();
  await tester.enterText(find.byKey(const Key('intake-description')), 'Điện thoại cũ');
  await tester.enterText(find.byKey(const Key('intake-amount')), '1');
  await tester.tap(find.byKey(const Key('intake-level')));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Nguyên vẹn').last);
  await tester.pumpAndSettle();
  await tester.tap(find.byKey(const Key('intake-giver')));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Người khác').last);
  await tester.pumpAndSettle();
  await tester.enterText(find.byKey(const Key('intake-giver-name')), 'Trần Văn Bình');
  await tester.enterText(find.byKey(const Key('intake-giver-doc')), 'CCCD 001');
  await tester.ensureVisible(find.byKey(const Key('intake-location')));
  await tester.tap(find.byKey(const Key('intake-location')));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Kho tầng 2').last);
  await tester.pumpAndSettle();
}

Future<void> save(WidgetTester tester) async {
  await tester.ensureVisible(find.text('Lưu tiếp nhận'));
  await tester.tap(find.text('Lưu tiếp nhận'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_011_danhSachDoGuiTheoNguoiCaoTuoi', (tester) async {
    await pump(tester, backend().client());

    expect(find.text('Điện thoại Nokia'), findsOneWidget);
    expect(find.text('Đang giữ'), findsOneWidget);
    expect(find.textContaining('Điện thoại, 1'), findsOneWidget);
  });

  testWidgets('BR_M12_05_doCoGiaTriThieuAnh_baoTaiO_khongGui', (tester) async {
    final b = backend();
    await pump(tester, b.client());
    await fillIntake(tester, type: 'Điện thoại');
    await save(tester);

    expect(find.text('Đồ có giá trị bắt buộc có ít nhất một ảnh.'), findsOneWidget);
    expect(b.calls('POST', '/residents/7/belongings'), isEmpty);
  });

  testWidgets('FR_006_chupAnh_taiTepRoiTiepNhan_kemIdempotencyKey', (tester) async {
    final b = backend()
      ..on('POST', '/files', 201, {'fileId': 501})
      ..on('POST', '/residents/7/belongings', 201, {'id': 91, 'status': 'HELD'});
    await pump(tester, b.client(), picker: () async => (utf8.encode('jpg'), 'anh.jpg'));
    await fillIntake(tester, type: 'Điện thoại');
    await tester.ensureVisible(find.text('Chụp ảnh'));
    await tester.tap(find.text('Chụp ảnh'));
    await tester.pumpAndSettle();
    expect(find.text('Đã chụp 1 ảnh'), findsOneWidget);
    await save(tester);

    expect(b.calls('POST', '/files'), hasLength(1));
    final sent = b.calls('POST', '/residents/7/belongings').single;
    expect(sent.headers['Idempotency-Key'], isNotEmpty);
    final body = jsonDecode(sent.body) as Map<String, dynamic>;
    expect(body['typeId'], 2);
    expect(body['quantity'], 1);
    expect(body['photoFileIds'], [501]);
    expect(body['giver'], {'type': 'OTHER_PERSON', 'fullName': 'Trần Văn Bình', 'idDocument': 'CCCD 001'});
    expect(body['storageLocationId'], 21);
    expect(body['at'] as String, endsWith('+07:00'));
    expect(find.text('Đã tiếp nhận đồ gửi.'), findsOneWidget);
  });

  testWidgets('FR_031_matMang_baoCanMang_khongXepHang', (tester) async {
    final b = backend();
    await pump(tester, b.client());
    await fillIntake(tester, type: 'Quần áo');
    b.down = true;
    await save(tester);

    expect(find.text('Lệnh đồ gửi chỉ thực hiện được khi có mạng. Kiểm tra kết nối rồi lưu lại.'), findsOneWidget);
    expect(find.byKey(const Key('intake-description')), findsOneWidget);
  });

  testWidgets('FR_003_nguoiKhacThieuGiayTo_khongCoViTri_baoTaiO_khongCoNhanVien', (tester) async {
    final b = backend();
    await pump(tester, b.client());
    await fillIntake(tester, type: 'Quần áo');
    await tester.enterText(find.byKey(const Key('intake-giver-doc')), '');
    await tester.ensureVisible(find.byKey(const Key('intake-giver')));
    await tester.tap(find.byKey(const Key('intake-giver')));
    await tester.pumpAndSettle();
    expect(find.text('Nhân viên'), findsNothing);
    await tester.tap(find.text('Người khác').last);
    await tester.pumpAndSettle();
    await save(tester);

    expect(find.text('Nhập giấy tờ của người giao.'), findsOneWidget);
    expect(b.calls('POST', '/residents/7/belongings'), isEmpty);
  });

  testWidgets('FR_003_dangGiuPhaiCoViTri', (tester) async {
    final b = backend();
    await pump(tester, b.client());
    await tester.tap(find.text('Tiếp nhận đồ'));
    await tester.pumpAndSettle();
    await save(tester);
    expect(find.text('Chọn vị trí lưu giữ.'), findsOneWidget);
  });
}
