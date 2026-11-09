import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/belongings/recipient_confirmation_screen.dart';

import '../../support/fake_backend.dart';

// 013 T019 (tên logic nguoi-than/do-gui; FR-017, FR-030): người đại diện xác nhận hoặc từ chối (bắt buộc lý do) yêu cầu
// xác nhận người nhận khác, mở từ thông báo RECIPIENT_CONFIRMATION_REQUESTED (nguồn là mã yêu cầu); chỉ trực tuyến.

Future<void> pump(WidgetTester tester, ApiClient api) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: RecipientConfirmationScreen(api: api, confirmationId: 61),
    ),
  );
  await tester.pumpAndSettle();
}

const _request = {
  'id': 61,
  'recipientName': 'Trần Thị N',
  'recipientIdDocument': '999',
  'belongingIds': [90, 91],
  'status': 'PENDING_CONFIRMATION',
};

FakeBackend backend() => FakeBackend()..on('GET', '/family-portal/recipient-confirmations/61', 200, _request);

void main() {
  testWidgets('FR_017_hienNguoiNhanVaDoTrongYeuCau_truocKhiQuyetDinh', (tester) async {
    final b = backend();
    await pump(tester, b.client());
    expect(find.text('Người nhận: Trần Thị N'), findsOneWidget);
    expect(find.text('Giấy tờ: 999'), findsOneWidget);
    expect(find.text('Đồ trong yêu cầu: 2 đồ (mã 90, 91)'), findsOneWidget);
    expect(find.text('Xác nhận người nhận'), findsOneWidget);
  });

  testWidgets('FR_030_khongPhaiNguoiDaiDien_403_anNutQuyetDinh', (tester) async {
    final b = FakeBackend()
      ..on('GET', '/family-portal/recipient-confirmations/61', 403, {
        'status': 403,
        'errorCode': 'FORBIDDEN',
        'detail': 'Chỉ người đại diện hiện tại xác nhận, từ chối (FR-017)',
      });
    await pump(tester, b.client());
    expect(find.text('Chỉ người đại diện hiện tại xác nhận, từ chối (FR-017)'), findsOneWidget);
    expect(find.text('Xác nhận người nhận'), findsNothing);
  });

  testWidgets('FR_017_xacNhan_guiLenhConfirm_kemIdempotencyKey', (tester) async {
    final b = backend()..on('POST', '/family-portal/recipient-confirmations/61/confirm', 200);
    await pump(tester, b.client());
    await tester.tap(find.text('Xác nhận người nhận'));
    await tester.pumpAndSettle();

    final sent = b.calls('POST', '/family-portal/recipient-confirmations/61/confirm').single;
    expect(sent.headers['Idempotency-Key'], isNotEmpty);
    expect(find.text('Đã xác nhận. Viện được trả đồ cho người này.'), findsOneWidget);
  });

  testWidgets('FR_017_tuChoi_batBuocLyDo', (tester) async {
    final b = backend()..on('POST', '/family-portal/recipient-confirmations/61/reject', 200);
    await pump(tester, b.client());
    await tester.tap(find.text('Từ chối'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Gửi từ chối'));
    await tester.pumpAndSettle();
    expect(find.text('Nhập lý do từ chối.'), findsOneWidget);
    expect(b.calls('POST', '/family-portal/recipient-confirmations/61/reject'), isEmpty);

    await tester.enterText(find.byKey(const Key('reject-reason')), 'Tôi không biết người này');
    await tester.tap(find.text('Gửi từ chối'));
    await tester.pumpAndSettle();
    final body = jsonDecode(b.calls('POST', '/family-portal/recipient-confirmations/61/reject').single.body);
    expect(body, {'reason': 'Tôi không biết người này'});
    expect(find.text('Đã từ chối. Viện không trả đồ cho người này.'), findsOneWidget);
  });

  testWidgets('FR_030_409_hienLyDo_matMangBaoCanMang', (tester) async {
    final b = backend()
      ..on('POST', '/family-portal/recipient-confirmations/61/confirm', 409, {
        'status': 409,
        'errorCode': 'ALREADY_REJECTED',
        'detail': 'Yêu cầu đã bị từ chối trước đó.',
      });
    await pump(tester, b.client());
    await tester.tap(find.text('Xác nhận người nhận'));
    await tester.pumpAndSettle();
    expect(find.text('Yêu cầu đã bị từ chối trước đó.'), findsOneWidget);

    b.down = true;
    await tester.tap(find.text('Xác nhận người nhận'));
    await tester.pumpAndSettle();
    expect(find.text('Cần có mạng để gửi quyết định. Kiểm tra kết nối rồi thử lại.'), findsOneWidget);
  });
}
