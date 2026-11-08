import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/family/charges/purchase_consent_screen.dart';

void main() {
  testWidgets('FR_023_tu_choi_can_ly_do_va_403_xoa_du_lieu', (tester) async {
    Map<String, dynamic>? sent;
    final api = ApiClient(
      baseUrl: 'http://localhost',
      httpClient: MockClient((request) async {
        if (request.method == 'POST') {
          sent = jsonDecode(request.body) as Map<String, dynamic>;
          return http.Response(jsonEncode({'status': 403, 'errorCode': 'PERMISSION_DENIED'}), 403);
        }
        return http.Response(
          jsonEncode([
            {
              'id': 9,
              'residentId': 7,
              'description': 'Đồ dùng cá nhân',
              'quantity': 1,
              'estimatedAmount': 800000,
              'decisions': [],
            },
          ]),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      }),
    );
    await tester.pumpWidget(MaterialApp(home: PurchaseConsentScreen(api: api, residentId: 7)));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Từ chối'));
    await tester.pumpAndSettle();
    expect(tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Xác nhận từ chối')).onPressed, isNull);
    await tester.enterText(find.byType(TextField), 'Không còn nhu cầu');
    await tester.pump();
    await tester.tap(find.text('Xác nhận từ chối'));
    await tester.pumpAndSettle();
    expect(sent, {'reason': 'Không còn nhu cầu'});
    expect(find.text('Đồ dùng cá nhân'), findsNothing);
    expect(find.text('Bạn không còn quyền xem đề nghị mua hộ.'), findsOneWidget);
  });

  testWidgets('FR_023_409_tai_lai_va_bao_de_nghi_da_thay_doi', (tester) async {
    var reads = 0;
    final api = ApiClient(
      baseUrl: 'http://localhost',
      httpClient: MockClient((request) async {
        if (request.method == 'POST') {
          return http.Response(jsonEncode({'status': 409, 'errorCode': 'ALREADY_PROCESSED'}), 409);
        }
        reads++;
        return http.Response(
          jsonEncode(
            reads == 1
                ? [
                    {
                      'id': 9,
                      'residentId': 7,
                      'description': 'Đồ dùng cá nhân',
                      'quantity': 1,
                      'estimatedAmount': 800000,
                      'decisions': [],
                    },
                  ]
                : [],
          ),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      }),
    );
    await tester.pumpWidget(MaterialApp(home: PurchaseConsentScreen(api: api, residentId: 7)));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đồng ý'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xác nhận đồng ý'));
    await tester.pumpAndSettle();
    expect(reads, 2);
    expect(find.textContaining('Đề nghị đã thay đổi'), findsOneWidget);
  });

  testWidgets('FR_023_xem_lai_truoc_khi_dong_y', (tester) async {
    var posts = 0;
    final api = ApiClient(
      baseUrl: 'http://localhost',
      httpClient: MockClient((request) async {
        if (request.method == 'POST') {
          posts++;
          return http.Response('', 200);
        }
        return http.Response(
          jsonEncode([
            {
              'id': 9,
              'residentId': 7,
              'description': 'Đồ dùng cá nhân',
              'quantity': 1,
              'estimatedAmount': 800000,
              'status': 'AWAITING_CONSENT',
              'decisions': [],
            },
          ]),
          200,
          headers: {'content-type': 'application/json'},
        );
      }),
    );
    await tester.pumpWidget(MaterialApp(home: PurchaseConsentScreen(api: api, residentId: 7)));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Đồng ý'));
    await tester.pumpAndSettle();
    expect(posts, 0);
    await tester.tap(find.text('Xác nhận đồng ý'));
    await tester.pumpAndSettle();
    expect(posts, 1);
  });
}
