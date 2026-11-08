import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/auth/login_screen.dart';
import 'package:mobile/common/theme/app_theme.dart';

/// 002 T105 (US3 #12, #13; FR-052a, Q-06; api-002 /auth/password-reset/*): "Quên mật khẩu" trên app chung chỉ hiện khi
/// kênh tin nhắn đã cấu hình; xin mã, nhập mã và mật khẩu mới; mã sai hiện câu của máy chủ.
void main() {
  ApiClient backend(bool available, List<http.Request> sent, {int confirmStatus = 204}) => ApiClient(
    baseUrl: 'http://api',
    httpClient: MockClient((request) async {
      sent.add(request);
      return switch (request.url.path) {
        '/api/v1/auth/password-reset/availability' => http.Response(
          jsonEncode({'available': available}),
          200,
          headers: {'content-type': 'application/json'},
        ),
        '/api/v1/auth/password-reset/request' => http.Response('', 202),
        '/api/v1/auth/password-reset/confirm' when confirmStatus == 204 => http.Response('', 204),
        _ => http.Response.bytes(
          utf8.encode(
            jsonEncode({
              'status': confirmStatus,
              'errorCode': 'RESET_CODE_INVALID',
              'detail': 'Mã không đúng, đã hết hạn hoặc không còn hiệu lực',
            }),
          ),
          confirmStatus,
          headers: {'content-type': 'application/problem+json; charset=utf-8'},
        ),
      };
    }),
  );

  Future<void> open(WidgetTester tester, ApiClient api) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: LoginScreen(onSubmit: (_, _) async {}, residentLogin: (_) => const SizedBox(), api: api),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> fillConfirm(WidgetTester tester, String code) async {
    await tester.enterText(find.widgetWithText(TextField, 'Mã trong tin nhắn'), code);
    await tester.enterText(find.widgetWithText(TextField, 'Mật khẩu mới'), 'moi12345');
    await tester.enterText(find.widgetWithText(TextField, 'Nhập lại mật khẩu mới'), 'moi12345');
    await tester.tap(find.text('Đặt mật khẩu mới'));
    await tester.pumpAndSettle();
  }

  testWidgets('Q_06_chuaCauHinh_anQuenMatKhau', (tester) async {
    await open(tester, backend(false, []));
    expect(find.text('Quên mật khẩu'), findsNothing);
  });

  testWidgets('FR_052a_xinMa_datMatKhauMoi_quayVeDangNhap', (tester) async {
    final sent = <http.Request>[];
    await open(tester, backend(true, sent));

    await tester.tap(find.text('Quên mật khẩu'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Tên đăng nhập'), ' nt-a ');
    await tester.tap(find.text('Gửi mã'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Nếu tài khoản có số điện thoại đã đăng ký'), findsOneWidget);
    await fillConfirm(tester, '123456');

    expect(find.text('Đã đặt mật khẩu mới. Đăng nhập bằng mật khẩu mới.'), findsOneWidget);
    final bodies = sent.where((r) => r.method == 'POST').map((r) => jsonDecode(r.body)).toList();
    expect(bodies, [
      {'username': 'nt-a'},
      {'username': 'nt-a', 'code': '123456', 'newPassword': 'moi12345'},
    ]);
  });

  testWidgets('FR_052a_maSai_hienCauMayChu_giuManNhapMa', (tester) async {
    await open(tester, backend(true, [], confirmStatus: 422));

    await tester.tap(find.text('Quên mật khẩu'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Tên đăng nhập'), 'nt-a');
    await tester.tap(find.text('Gửi mã'));
    await tester.pumpAndSettle();
    await fillConfirm(tester, '000000');

    expect(find.text('Mã không đúng, đã hết hạn hoặc không còn hiệu lực'), findsOneWidget);
    expect(find.widgetWithText(TextField, 'Mã trong tin nhắn'), findsOneWidget);
  });
}
