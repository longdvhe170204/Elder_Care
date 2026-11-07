import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/medication/medication_round_gateway.dart';
import 'package:mobile/staff/medication/medication_round_repository.dart';
import 'package:mobile/staff/medication/medication_round_screen.dart';

import 'medication_fixtures.dart';

// 006 T028 (tên logic vong_phat_thuoc_ngoai_tuyen_test; quickstart 4 XacNhanLieuNgoaiTuyenTest, KsdbKhongVaoHangDoiTest):
// vòng phát thuốc tải, ghép trường FR-020, lưu bản đã tải; mất mạng thì xem bản đó kèm "Chưa cập nhật", ghi kết quả vào
// hàng đợi M07.DOSE_RESULT; thuốc kiểm soát đặc biệt không vào hàng đợi (FR-029, BR-M07-20).

Future<void> settle(WidgetTester tester) async {
  await tester.runAsync(() => Future<void>.delayed(const Duration(milliseconds: 100)));
  await tester.pumpAndSettle();
}

void main() {
  late RoundFixture f;

  setUp(() {
    f = RoundFixture();
    serveRound(f.backend);
  });

  tearDown(() => f.cache.close());

  Map<String, dynamic> itemOf(MedicationRoundLoad load, String key) =>
      load.round.items.firstWhere((i) => (i['dose'] as Map)['doseKey'] == key);

  test('FR_020_taiVong_ghepTenThuocLieuDuongDungHoTenDiUng', () async {
    final load = await f.repository.load(7);

    final item = itemOf(load, '10-202611050800');
    expect((item['medication'] as Map)['name'], 'Paracetamol');
    expect((item['prescription'] as Map)['route'], 'đường uống');
    expect((item['resident'] as Map)['fullName'], 'Nguyễn Thị Lan');
    expect((item['resident'] as Map)['allergies'], ['Penicillin']);
    expect(load.offline, isFalse);
    expect(f.backend.calls('GET', '/prescriptions/10'), hasLength(1), reason: 'mỗi đơn chỉ tải một lần');
  });

  test('FR_020_khongXemDuocPhanSucKhoe_khongHienDiUng', () async {
    f.backend.on('GET', '/residents/77', 200, {
      'id': 77,
      'personalInfo': {'fullName': 'Nguyễn Thị Lan'},
      'hiddenFields': ['activeHealthItems'],
    });

    final load = await f.repository.load(7);

    expect((itemOf(load, '10-202611050800')['resident'] as Map)['allergies'], isNull);
  });

  test('T028_matMang_traBanDaTai_daDauNgoaiTuyen', () async {
    await f.repository.load(7);
    f.backend.down = true;

    final load = await f.repository.load(7);

    expect(load.offline, isTrue);
    expect(load.round.items, hasLength(3));
    expect(load.round.loadedAt.toUtc(), DateTime.utc(2026, 11, 5, 1, 5));
  });

  test('T028_matQuyen_xoaBanDaTai', () async {
    await f.repository.load(7);
    f.backend.on('GET', '/medication-doses/rounds', 403, {'errorCode': 'FORBIDDEN', 'detail': 'Không có quyền'});
    await expectLater(f.repository.load(7), throwsA(isA<ApiProblem>()));

    f.backend.down = true;
    await expectLater(f.repository.load(7), throwsA(isA<ApiUnreachable>()));
  });

  test('FR_029_matMang_ghiDaDung_vaoHangDoiM07DoseResult', () async {
    final load = await f.repository.load(7);
    f.backend.down = true;

    final saved = await f.repository.record(
      accountId: 7,
      item: itemOf(load, '10-202611050800'),
      outcome: DoseOutcome.administered,
      body: {'administeredAt': '2026-11-05T08:05:00+07:00', 'basis': 'REGULAR', 'note': 'Uống hết'},
      key: 'k1',
    );

    expect(saved, RecordSaved.queued);
    final record = (await f.queue.unsynced(7)).single;
    expect(record.recordType, 'M07.DOSE_RESULT');
    expect(jsonDecode(record.content), {
      'doseKey': '10-202611050800',
      'residentId': 77,
      'resultType': 'ADMINISTERED',
      'administeredAt': '2026-11-05T08:05:00+07:00',
      'note': 'Uống hết',
    });
    expect((await f.repository.load(7)).queued, {'10-202611050800': 'ADMINISTERED'});
  });

  test('BR_M07_20_thuocKiemSoatDacBiet_matMang_khongVaoHangDoi', () async {
    final load = await f.repository.load(7);
    f.backend.down = true;

    await expectLater(
      f.repository.record(
        accountId: 7,
        item: itemOf(load, '11-202611050730'),
        outcome: DoseOutcome.administered,
        body: {'administeredAt': '2026-11-05T07:40:00+07:00', 'basis': 'REGULAR'},
        key: 'k2',
      ),
      throwsA(isA<RequiresOnline>()),
    );
    expect(await f.queue.unsynced(7), isEmpty);
  });

  Future<void> pumpRound(WidgetTester tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: MedicationRoundScreen(
          repository: f.repository,
          accountId: 7,
          newKey: () => 'cmd-1',
          clock: () => DateTime.utc(2026, 11, 5, 1, 5),
        ),
      ),
    );
    await settle(tester);
  }

  testWidgets('T028_vongPhatThuoc_nhomCanLamNgay_diUng_ksdb', (tester) async {
    await pumpRound(tester);

    expect(find.text('Cần làm ngay (1)'), findsOneWidget);
    expect(find.text('Sau đó'), findsOneWidget);
    expect(find.text('dị ứng Penicillin'), findsNWidgets(2));
    expect(find.text('08:00 Paracetamol 500 mg, 1 viên, đường uống'), findsOneWidget);
    expect(find.text('07:30 Morphin 10 mg/ml, 2.5 mg, tiêm dưới da'), findsOneWidget);
    expect(find.text('Kiểm soát đặc biệt, phải trực tuyến'), findsOneWidget);
    expect(find.text('Trễ'), findsOneWidget);
    expect(find.textContaining('Cập nhật lúc'), findsOneWidget);
  });

  testWidgets('T028_matMang_hienDaiNgoaiTuyen_vaChuaCapNhat', (tester) async {
    await tester.runAsync(() => f.repository.load(7));
    f.backend.down = true;

    await pumpRound(tester);

    expect(find.text('Đang ngoại tuyến. Bản ghi sẽ đồng bộ khi có mạng.'), findsOneWidget);
    expect(find.text('Chưa cập nhật, tải lúc 05/11/2026 08:05'), findsOneWidget);
    expect(find.text('Nguyễn Thị Lan'), findsNWidgets(2));
  });

  testWidgets('FR_021_tuChoi_batBuocLyDo_roiGuiLenhTrucTuyen', (tester) async {
    f.backend.on('POST', '/medication-doses/10-202611050800/refuse', 200);
    await pumpRound(tester);
    await tester.tap(find.text('08:00 Paracetamol 500 mg, 1 viên, đường uống'));
    await settle(tester);

    await tester.tap(find.text('Từ chối').at(1));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lưu kết quả'));
    await settle(tester);
    expect(find.text('Nhập lý do từ chối.'), findsOneWidget);
    expect(f.backend.calls('POST', '/medication-doses/10-202611050800/refuse'), isEmpty);

    await tester.enterText(find.widgetWithText(TextField, 'Lý do (bắt buộc)'), 'Người cao tuổi nói đã uống ở nhà');
    await tester.tap(find.text('Lưu kết quả'));
    await settle(tester);
    await settle(tester);

    final call = f.backend.calls('POST', '/medication-doses/10-202611050800/refuse').single;
    expect(call.headers['Idempotency-Key'], 'cmd-1');
    expect(jsonDecode(call.body), {'reason': 'Người cao tuổi nói đã uống ở nhà'});
    expect(find.text('Đã lưu 1 liều.'), findsOneWidget);
  });

  testWidgets('BR_M07_20_ksdbMatMang_baoCanMang_khongGhiTam', (tester) async {
    await pumpRound(tester);
    f.backend.down = true;
    await tester.tap(find.text('07:30 Morphin 10 mg/ml, 2.5 mg, tiêm dưới da'));
    await settle(tester);

    await tester.tap(find.text('Đã dùng').at(0));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lưu kết quả'));
    await settle(tester);

    expect(find.textContaining('chỉ ghi được khi có mạng'), findsOneWidget);
    expect(await tester.runAsync(() => f.queue.unsynced(7)), isEmpty);
  });
}
