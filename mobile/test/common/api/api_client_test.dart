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

  test('requestResponse trả thân và header phản hồi (ETag cho If-Match, 001 T042)', () async {
    final client = ApiClient(
      baseUrl: 'http://api',
      httpClient: MockClient(
        (request) async =>
            http.Response(jsonEncode({'id': 5}), 200, headers: {'content-type': 'application/json', 'etag': '"3"'}),
      ),
    );

    final response = await client.requestResponse('GET', '/assessments/5');

    expect(response.body, {'id': 5});
    expect(response.headers['etag'], '"3"');
  });

  test('uploadFile gửi multipart đúng tên trường, kèm token, và trả JSON (001 T082)', () async {
    late http.BaseRequest sent;
    List<int> sentBytes = const [];
    final client = ApiClient(
      baseUrl: 'http://api',
      httpClient: MockClient.streaming((request, bodyStream) async {
        sent = request;
        sentBytes = await bodyStream.toBytes();
        return http.StreamedResponse(
          Stream.value(utf8.encode(jsonEncode({'id': 77}))),
          201,
          headers: {'content-type': 'application/json'},
        );
      }),
    )..token = 'tok';

    final result = await client.uploadFile('/files', 'file', utf8.encode('%PDF'), 'giay.pdf', 'application/pdf');

    expect(sent.method, 'POST');
    expect(sent.url.toString(), 'http://api/api/v1/files');
    expect(sent.headers['Authorization'], 'Bearer tok');
    expect(sent.headers['content-type'], startsWith('multipart/form-data'));
    final text = utf8.decode(sentBytes);
    expect(text, contains('name="file"'));
    expect(text, contains('filename="giay.pdf"'));
    expect(text, contains('application/pdf'));
    expect(result, {'id': 77});
  });

  test('uploadFile báo ApiProblem khi backend từ chối (FILE_TOO_LARGE)', () async {
    final client = ApiClient(
      baseUrl: 'http://api',
      httpClient: MockClient.streaming(
        (request, bodyStream) async => http.StreamedResponse(
          Stream.value(utf8.encode(jsonEncode({'errorCode': 'FILE_TOO_LARGE', 'detail': 'Tệp quá lớn'}))),
          422,
          headers: {'content-type': 'application/problem+json'},
        ),
      ),
    );

    expect(
      client.uploadFile('/files', 'file', const [1], 'a.pdf', 'application/pdf'),
      throwsA(isA<ApiProblem>().having((p) => p.errorCode, 'errorCode', 'FILE_TOO_LARGE')),
    );
  });
}
