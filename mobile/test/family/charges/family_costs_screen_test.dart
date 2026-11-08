import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/family/charges/family_costs_screen.dart';

void main() {
  testWidgets('FR_037_038_bang_chot_va_tam_tinh_thieu_gia', (tester) async {
    final api = ApiClient(
      baseUrl: 'http://localhost',
      httpClient: MockClient((request) async {
        final Object body = request.url.path.endsWith('provisional')
            ? {'label': 'tạm tính, chưa chốt', 'totalsByType': <Object>[], 'hasUnpricedItems': true}
            : [
                {'id': 1, 'period': '2026-10', 'total': 300, 'items': <Object>[], 'totalsByType': <Object>[]},
              ];
        return http.Response(jsonEncode(body), 200, headers: {'content-type': 'application/json; charset=utf-8'});
      }),
    );
    await tester.pumpWidget(MaterialApp(home: FamilyCostsScreen(api: api, residentId: 7)));
    await tester.pumpAndSettle();
    expect(find.textContaining('10/2026'), findsOneWidget);
    await tester.tap(find.text('Xem tạm tính'));
    await tester.pumpAndSettle();
    expect(find.text('Còn khoản chưa có đơn giá'), findsOneWidget);
  });

  testWidgets('FR_043_403_khong_hien_du_lieu_chi_phi', (tester) async {
    final api = ApiClient(
      baseUrl: 'http://localhost',
      httpClient: MockClient(
        (request) async => http.Response(
          jsonEncode({'errorCode': 'PERMISSION_DENIED'}),
          403,
          headers: {'content-type': 'application/json'},
        ),
      ),
    );
    await tester.pumpWidget(MaterialApp(home: FamilyCostsScreen(api: api, residentId: 7)));
    await tester.pumpAndSettle();
    expect(find.text('Bạn không có quyền xem chi phí của người cao tuổi này.'), findsOneWidget);
    expect(find.text('Xem tạm tính'), findsNothing);
  });
}
