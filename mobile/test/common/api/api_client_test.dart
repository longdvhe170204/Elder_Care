import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';

/// Transport dùng chung (005 phase4-contract-proposal mục 6): thân, phản hồi dạng mảng JSON, Idempotency-Key và
/// thuộc tính mở rộng của Problem Details (rowErrors của lô lập công việc).
void main() {
  test('requestJson gửi mảng JSON kèm Idempotency-Key và trả mảng', () async {
    late http.Request sent;
    final client = ApiClient(
      baseUrl: 'http://api',
      httpClient: MockClient((request) async {
        sent = request;
        return http.Response(
          jsonEncode([
            {'id': 1},
            {'id': 2},
          ]),
          201,
          headers: {'content-type': 'application/json'},
        );
      }),
    );

    final result = await client.requestJson(
      'POST',
      '/shifts/7/care-tasks/plan',
      body: [
        {'taskTypeId': 3},
      ],
      headers: {'Idempotency-Key': 'k-1'},
    );

    expect(sent.method, 'POST');
    expect(sent.url.toString(), 'http://api/api/v1/shifts/7/care-tasks/plan');
    expect(sent.headers['Idempotency-Key'], 'k-1');
    expect(jsonDecode(sent.body), [
      {'taskTypeId': 3},
    ]);
    expect(result, [
      {'id': 1},
      {'id': 2},
    ]);
  });

  test('ApiProblem giữ thuộc tính mở rộng như rowErrors', () async {
    final client = ApiClient(
      baseUrl: 'http://api',
      httpClient: MockClient(
        (request) async => http.Response.bytes(
          utf8.encode(
            jsonEncode({
              'status': 409,
              'errorCode': 'ROW_ERRORS',
              'detail': 'Lô bị từ chối',
              'rowErrors': [
                {
                  'row': 1,
                  'codes': ['RESIDENT_ABSENT'],
                },
              ],
            }),
          ),
          409,
          headers: {'content-type': 'application/problem+json'},
        ),
      ),
    );

    final error = await client
        .requestJson('POST', '/shifts/7/care-tasks/plan', body: const [])
        .then<Object?>((_) => null, onError: (Object e) => e);

    expect(error, isA<ApiProblem>());
    final problem = error as ApiProblem;
    expect(problem.status, 409);
    expect(problem.errorCode, 'ROW_ERRORS');
    expect(problem.properties['rowErrors'], [
      {
        'row': 1,
        'codes': ['RESIDENT_ABSENT'],
      },
    ]);
  });

  test('get, post cũ giữ cách trả Map', () async {
    final client = ApiClient(
      baseUrl: 'http://api',
      httpClient: MockClient((request) async => http.Response(jsonEncode({'ok': true}), 200)),
    );

    expect(await client.get('/me'), {'ok': true});
    expect(await client.post('/x', {'a': 1}), {'ok': true});
    expect(ApiProblem(400, 'A', 'b').properties, isEmpty);
  });

  // G2 (b), A duyệt 2026-11-05: tải tệp lên POST /files dạng multipart (api-001 tag files; 013 T011 ảnh đồ gửi).
  test('G2_uploadFile_guiMultipart_kemToken_traMaTep', () async {
    late http.BaseRequest sent;
    late String body;
    final client = ApiClient(
      baseUrl: 'http://api',
      httpClient: MockClient((request) async {
        sent = request;
        body = latin1.decode(request.bodyBytes);
        return http.Response(jsonEncode({'fileId': 501}), 201, headers: {'content-type': 'application/json'});
      }),
    )..token = 't-1';

    final id = await client.uploadFile(utf8.encode('anh'), 'do-gui.jpg');

    expect(id, 501);
    expect(sent.method, 'POST');
    expect(sent.url.toString(), 'http://api/api/v1/files');
    expect(sent.headers['Authorization'], 'Bearer t-1');
    expect(sent.headers['content-type'], startsWith('multipart/form-data'));
    expect(body, contains('name="file"; filename="do-gui.jpg"'));
    expect(body, contains('content-type: image/jpeg'));
  });

  test('G2_uploadFile_loiThanhApiProblem_matMangThanhApiUnreachable', () async {
    final failing = ApiClient(
      baseUrl: 'http://api',
      httpClient: MockClient(
        (request) async => http.Response.bytes(
          utf8.encode(jsonEncode({'status': 422, 'errorCode': 'FILE_TYPE_NOT_ALLOWED', 'detail': 'Sai loại tệp'})),
          422,
          headers: {'content-type': 'application/problem+json'},
        ),
      ),
    );
    await expectLater(
      failing.uploadFile([1], 'a.png'),
      throwsA(isA<ApiProblem>().having((p) => p.errorCode, 'errorCode', 'FILE_TYPE_NOT_ALLOWED')),
    );

    final offline = ApiClient(baseUrl: 'http://api', httpClient: MockClient((request) async => throw Exception('mạng')));
    await expectLater(offline.uploadFile([1], 'a.png'), throwsA(isA<ApiUnreachable>()));
  });
}
