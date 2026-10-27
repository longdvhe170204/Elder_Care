import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/pending_review/pending_review_screen.dart';

import '../../support/fake_backend.dart';

// 002 T060 (tên logic cho_xem_lai_test; FR-041c): bản ghi chờ xem lại trên app nhân viên: thẻ có loại, người ghi, tầng,
// lý do chờ; chấp nhận gửi If-Match theo version; từ chối bắt buộc lý do; người khác đã quyết định thì bỏ thẻ.

Map<String, dynamic> record() => {
  'id': 31,
  'recordType': 'M07.KET_QUA_LIEU',
  'content': {'note': 'Uống hết'},
  'recorderId': 12,
  'recorderName': '[Họ tên NV 1]',
  'deviceTime': '2026-10-27T00:10:00Z',
  'syncedAt': '2026-10-28T02:00:00Z',
  'lastConnectedAt': '2026-10-26T23:00:00Z',
  'pendingReason': 'EXCEEDS_CFG_M15_08',
  'floorAreaId': 3,
  'floorAreaName': 'Tầng 2',
  'status': 'PENDING_REVIEW',
  'version': 0,
};

Future<void> pumpScreen(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(body: PendingReviewScreen(api: api)),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> openCard(WidgetTester tester) async {
  await tester.tap(find.text('[Họ tên NV 1], Tầng 2'));
  await tester.pumpAndSettle();
}

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/pending-review-records', 200, {
      'items': [record()],
      'nextCursor': null,
    });
  });

  testWidgets('FR_041c_the_hienLoai_nguoiGhi_tang_lyDoCho', (tester) async {
    await pumpScreen(tester, backend.client());

    expect(find.text('Kết quả liều thuốc'), findsOneWidget);
    expect(find.text('[Họ tên NV 1], Tầng 2'), findsOneWidget);
    expect(find.text('Quá CFG-M15-08 kể từ lần kết nối cuối'), findsOneWidget);
    expect(find.text('Chờ xem lại'), findsOneWidget);
  });

  testWidgets('FR_041c_chapNhan_guiIfMatch_boThe', (tester) async {
    backend.on('POST', '/pending-review-records/31/accept', 200, {...record(), 'status': 'ACCEPTED', 'version': 1});
    await pumpScreen(tester, backend.client());
    await openCard(tester);

    expect(find.text('note: Uống hết'), findsOneWidget);
    await tester.tap(find.text('Chấp nhận bản ghi'));
    await tester.pumpAndSettle();

    expect(backend.calls('POST', '/pending-review-records/31/accept').single.headers['If-Match'], '"0"');
    expect(find.text('Đã chấp nhận bản ghi. Bản ghi có hiệu lực từ lúc chấp nhận.'), findsOneWidget);
    expect(find.text('[Họ tên NV 1], Tầng 2'), findsNothing);
  });

  testWidgets('FR_041c_tuChoiKhongLyDo_baoTaiO_coLyDo_guiLyDo', (tester) async {
    backend.on('POST', '/pending-review-records/31/reject', 200, {...record(), 'status': 'REJECTED', 'version': 1});
    await pumpScreen(tester, backend.client());
    await openCard(tester);

    await tester.tap(find.text('Từ chối'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xác nhận từ chối'));
    await tester.pumpAndSettle();
    expect(find.text('Nhập lý do từ chối'), findsOneWidget);
    expect(backend.calls('POST', '/pending-review-records/31/reject'), isEmpty);

    await tester.enterText(find.widgetWithText(TextField, 'Lý do từ chối'), 'Không khớp');
    await tester.tap(find.text('Xác nhận từ chối'));
    await tester.pumpAndSettle();

    expect(jsonDecode(backend.calls('POST', '/pending-review-records/31/reject').single.body), {
      'reason': 'Không khớp',
    });
    expect(find.text('Đã từ chối bản ghi. Bản ghi được lưu nhưng không có hiệu lực.'), findsOneWidget);
  });

  testWidgets('FR_041c_nguoiKhacDaQuyetDinh_409_baoVaBoThe', (tester) async {
    backend.on('POST', '/pending-review-records/31/accept', 409, {'status': 409, 'errorCode': 'ALREADY_PROCESSED'});
    await pumpScreen(tester, backend.client());
    await openCard(tester);

    await tester.tap(find.text('Chấp nhận bản ghi'));
    await tester.pumpAndSettle();

    expect(find.text('Bản ghi đã được người khác quyết định.'), findsOneWidget);
    expect(find.text('[Họ tên NV 1], Tầng 2'), findsNothing);
  });
}
