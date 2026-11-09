import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../support/fake_backend.dart';
import 'resident_status_test.dart'
    show entry, fillGrantForm, lastBody, pickupRecords, profile, pumpScreen, stubGrantSources, tapButton, tapText;

// 004 T056 (tên logic cho_tam_vang_test; US6; FR-048, FR-049; Q-395): lệnh Cho tạm vắng trên app nhân viên mang chi tiết lượt
// vắng. Người đón và bản ghi đón lấy từ bản ghi Chưa dùng của 012 (GET /residents/{id}/pickup-records), nhân viên bàn giao từ
// GET /staff. Chỉ trực tuyến; mọi kiểm tra thật (bản ghi hợp lệ, hết hạn, đã dùng) do backend. Ghi nhận trở về của lượt vắng không
// cần chi tiết (ngày trở về mặc định là lúc ghi), đã có test ở resident_status_test.dart.

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/residents/7', 200, profile());
    backend.on('GET', '/residents/7/status-history', 200, [entry()]);
    backend.on('GET', '/residents/7/conditions/GRANT_TEMPORARY_ABSENCE', 200, <Object>[]);
    backend.on('POST', '/residents/7/grant-temporary-absence', 200, {
      'status': 'TEMPORARILY_ABSENT',
      'warnings': <Object>[],
    });
    stubGrantSources(backend);
  });

  testWidgets('FR_048_grantForm_listsOnlyUnusedPickupRecordsOfAbsenceTypes', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 2400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');

    expect(backend.calls('GET', '/residents/7/pickup-records'), hasLength(1));
    expect(find.text('Tạm vắng · 09/11/2026 09:00'), findsOneWidget);
    expect(find.text('Đi chơi với gia đình · 09/11/2026 10:00'), findsWidgets);
    expect(find.text('Bán trú về · 09/11/2026 11:00'), findsNothing);
    expect(find.text('Tạm vắng · 08/11/2026 09:00'), findsNothing);
  });

  testWidgets('FR_048_grantMissingFields_showsErrorsAndSendsNothing', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 2400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Về thăm nhà');
    await tapButton(tester, 'Xác nhận cho tạm vắng');

    expect(find.text('Chọn loại vắng'), findsOneWidget);
    expect(find.text('Nhập ngày giờ dự kiến về dạng dd/MM/yyyy HH:mm'), findsOneWidget);
    expect(find.text('Chọn bản ghi đón của người đón'), findsOneWidget);
    expect(find.text('Chọn nhân viên bàn giao'), findsOneWidget);
    expect(backend.calls('POST', '/residents/7/grant-temporary-absence'), isEmpty);
  });

  testWidgets('FR_049_invalidExpectedReturn_showsErrorAndSendsNothing', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 2400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Về thăm nhà');
    await fillGrantForm(tester, expectedReturn: '31/02/2026 17:00');
    await tapButton(tester, 'Xác nhận cho tạm vắng');

    expect(find.text('Nhập ngày giờ dự kiến về dạng dd/MM/yyyy HH:mm'), findsOneWidget);
    expect(backend.calls('POST', '/residents/7/grant-temporary-absence'), isEmpty);
  });

  testWidgets('FR_048_grantForm_sendsAbsenceDetailsFromChosenPickupRecord', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 2400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Về thăm nhà');
    await fillGrantForm(tester, takesMedication: true);
    await tapButton(tester, 'Xác nhận cho tạm vắng');

    expect(lastBody(backend, '/residents/7/grant-temporary-absence'), {
      'reason': 'Về thăm nhà',
      'details': {
        'absenceType': 'HOME_VISIT',
        'expectedReturnAt': '2026-11-10T10:00:00.000Z',
        'pickedUpById': pickupRecords.first['familyMemberId'],
        'pickupRecordId': pickupRecords.first['id'],
        'handoverStaffId': 4,
        'takesMedication': true,
      },
    });
  });

  testWidgets('FR_048_pickupRecordPreviewCondition_blocksUntilRecordChosen', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 2400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    backend.on('GET', '/residents/7/conditions/GRANT_TEMPORARY_ABSENCE', 200, [
      {'code': 'ABS_PICKUP_RECORD', 'description': 'Người đón chưa có bản ghi đón hợp lệ', 'basis': 'FR-048'},
    ]);
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    FilledButton confirm() => tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Xác nhận cho tạm vắng'));
    expect(confirm().onPressed, isNull);

    await tapText(tester, 'Tạm vắng · 09/11/2026 09:00');
    expect(find.text('Người đón chưa có bản ghi đón hợp lệ'), findsNothing);
    expect(confirm().onPressed, isNotNull);
  });
}
