import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/family/balance/family_documents_screen.dart';

// 017 T036, T039 phần app người thân (FR-026, FR-037, FR-038): danh sách phiếu báo phí, bảng quyết toán; tải đúng file đã
// lưu rồi đưa vào bảng chia sẻ; sao kê theo khoảng ngày ở PDF hoặc Excel; không có quyền xem chi phí thì nói rõ.

class _Shared {
  final List<(Uint8List, String, String)> files = [];

  Future<void> call(Uint8List bytes, String name, String mime) async => files.add((bytes, name, mime));
}

final _pdf = Uint8List.fromList(utf8.encode('%PDF-demo'));

http.Response _json(int status, Object body) => http.Response.bytes(
  utf8.encode(jsonEncode(body)),
  status,
  headers: {'content-type': status >= 400 ? 'application/problem+json' : 'application/json'},
);

Future<(List<http.Request>, _Shared)> pump(WidgetTester tester, http.Response Function(http.Request) handler) async {
  final requests = <http.Request>[];
  final shared = _Shared();
  final api = ApiClient(
    baseUrl: 'http://backend.test',
    httpClient: MockClient((request) async {
      requests.add(request);
      return handler(request);
    }),
  );
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: FamilyDocumentsScreen(
          api: api,
          residentId: 7,
          residentName: 'Nguyễn Thị Lan',
          share: shared.call,
          today: DateTime(2026, 11, 8),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return (requests, shared);
}

void main() {
  testWidgets('FR_038_danhSachChungTu_taiDungFile_chiaSe', (tester) async {
    final (requests, shared) = await pump(tester, (request) {
      final path = request.url.path.replaceFirst('/api/v1', '');
      if (path == '/family-portal/residents/7/documents') {
        return _json(200, [
          {
            'type': 'PAYMENT_NOTICE',
            'id': 5,
            'code': 'PBP-202610-42',
            'date': '2026-11-03',
            'period': '10/2026',
            'formats': ['PDF', 'XLSX'],
          },
          {
            'type': 'SETTLEMENT_STATEMENT',
            'id': 9,
            'code': 'v1',
            'date': '2026-11-01',
            'period': null,
            'formats': ['PDF'],
          },
        ]);
      }
      return http.Response.bytes(_pdf, 200, headers: {'content-type': 'application/pdf'});
    });

    expect(find.text('Phiếu báo phí PBP-202610-42'), findsOneWidget);
    expect(find.text('03/11/2026, kỳ 10/2026'), findsOneWidget);
    expect(find.text('Bảng quyết toán v1'), findsOneWidget);
    expect(find.text('Tải XLSX'), findsOneWidget);

    await tester.tap(find.text('Tải XLSX'));
    await tester.pumpAndSettle();
    final download = requests.last;
    expect(download.url.path, '/api/v1/family-portal/documents/PAYMENT_NOTICE/5/file');
    expect(download.url.queryParameters['format'], 'XLSX');
    expect(shared.files.single.$2, 'PBP-202610-42.xlsx');
    expect(shared.files.single.$3, contains('spreadsheetml'));
    expect(shared.files.single.$1, _pdf);
  });

  testWidgets('FR_026_saoKe_theoKhoangNgay_baoLoiKhiVuotGioiHan', (tester) async {
    var exceeded = false;
    final (requests, shared) = await pump(tester, (request) {
      final path = request.url.path.replaceFirst('/api/v1', '');
      if (path == '/family-portal/residents/7/documents') return _json(200, <Object>[]);
      if (exceeded) {
        return _json(422, {'errorCode': 'EXPORT_RANGE_EXCEEDED', 'detail': 'Khoảng xuất dài hơn giới hạn P12M'});
      }
      return http.Response.bytes(_pdf, 200, headers: {'content-type': 'application/pdf'});
    });

    expect(find.text('Chưa có phiếu báo phí hay bảng quyết toán nào.'), findsOneWidget);
    expect(find.text('Từ 09/10/2026'), findsOneWidget);
    expect(find.text('Đến 08/11/2026'), findsOneWidget);

    await tester.tap(find.text('Excel'));
    await tester.pump();
    await tester.tap(find.text('Tải sao kê'));
    await tester.pumpAndSettle();
    final post = requests.last;
    expect(post.method, 'POST');
    expect(post.url.path, '/api/v1/family-portal/residents/7/balance-statement');
    expect(post.headers['Idempotency-Key'], isNotEmpty);
    expect(jsonDecode(post.body), {'from': '2026-10-09', 'to': '2026-11-08', 'format': 'XLSX'});
    expect(shared.files.single.$2, 'sao-ke-2026-10-09-2026-11-08.xlsx');

    exceeded = true;
    await tester.tap(find.text('Tải sao kê'));
    await tester.pumpAndSettle();
    expect(find.textContaining('P12M'), findsOneWidget);
    expect(shared.files, hasLength(1));
  });

  testWidgets('FR_038_khongCoQuyenXemChiPhi_noiRo', (tester) async {
    await pump(tester, (request) => _json(403, {'errorCode': 'PERMISSION_DENIED', 'detail': 'Không có quyền'}));
    expect(find.textContaining('chưa có quyền xem chi phí của Nguyễn Thị Lan'), findsOneWidget);
    expect(find.text('Tải sao kê'), findsNothing);
  });
}
