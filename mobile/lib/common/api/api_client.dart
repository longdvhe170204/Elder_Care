import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart' show MediaType;

/// Lỗi theo Problem Details của backend (RFC 9457; như `common/problem.js` của web).
class ApiProblem implements Exception {
  ApiProblem(this.status, this.errorCode, this.detail, [this.properties = const {}]);

  final int status;
  final String? errorCode;
  final String? detail;

  /// Toàn bộ Problem Details backend trả, gồm thuộc tính mở rộng (ví dụ `rowErrors` của lô lập công việc 005).
  final Map<String, dynamic> properties;

  @override
  String toString() => 'ApiProblem($status, $errorCode)';
}

/// Không gọi được backend (mất mạng, máy chủ không trả lời).
class ApiUnreachable implements Exception {}

/// Thân JSON đã giải mã và header phản hồi (tên header viết thường), ví dụ `etag` để gửi lại `If-Match`.
class ApiResponse {
  ApiResponse(this.body, this.headers);

  final Object? body;
  final Map<String, String> headers;
}

/// Nghe mọi phản hồi của backend (002 T047c, như `onResponse` của web): `background` là yêu cầu gửi kèm
/// `X-Background-Refresh`, không đếm lại thời gian không thao tác của phiên (FR-013a).
typedef ResponseListener = void Function(int status, String? errorCode, bool background);

/// Client REST `/api/v1` dùng chung cho mọi phần của app (mobile/README.md luật 2). Viết tay cho các endpoint đã dùng;
/// script sinh client từ hợp đồng (000 T010b) làm sau.
class ApiClient {
  ApiClient({required this.baseUrl, http.Client? httpClient}) : _http = httpClient ?? http.Client();

  /// Địa chỉ backend, ví dụ `http://10.0.2.2:8080` (máy ảo Android gọi máy chủ trên máy tính).
  final String baseUrl;
  final http.Client _http;

  /// Token phiên; `null` khi chưa đăng nhập.
  String? token;

  /// Mã thiết bị gửi kèm mọi yêu cầu (`X-Device-Id`, 002 FR-014a, FR-070).
  String? deviceId;

  /// Lần cuối backend trả lời (kể cả trả lỗi); hàng đợi ngoại tuyến gửi kèm khi đồng bộ (002 FR-041c).
  DateTime? lastConnectedAt;

  final List<ResponseListener> _listeners = [];

  void addResponseListener(ResponseListener listener) => _listeners.add(listener);

  void removeResponseListener(ResponseListener listener) => _listeners.remove(listener);

  Future<Map<String, dynamic>?> get(String path) async {
    final decoded = await requestJson('GET', path);
    return decoded is Map<String, dynamic> ? decoded : null;
  }

  /// Endpoint trả mảng JSON.
  Future<List<dynamic>> getList(String path) async {
    final decoded = await requestJson('GET', path);
    return decoded is List<dynamic> ? decoded : const [];
  }

  /// `headers`: ví dụ `If-Match` của lệnh trên đối tượng có phiên bản (000 FR-040).
  Future<Map<String, dynamic>?> post(String path, Map<String, dynamic> body, {Map<String, String>? headers}) async {
    final decoded = await requestJson('POST', path, body: body, headers: headers);
    return decoded is Map<String, dynamic> ? decoded : null;
  }

  /// Gửi thân JSON bất kỳ (Map, mảng) và trả JSON đã giải mã, `null` khi phản hồi rỗng. `headers`: ví dụ
  /// `Idempotency-Key` của lệnh ghi, `If-Match` của lệnh trên đối tượng có phiên bản.
  Future<Object?> requestJson(String method, String path, {Object? body, Map<String, String>? headers}) async =>
      (await requestResponse(method, path, body: body, headers: headers)).body;

  /// Như [requestJson] nhưng trả cả header phản hồi (001 T042: `ETag` của `GET /assessments/{id}` làm `If-Match` của PUT).
  Future<ApiResponse> requestResponse(String method, String path, {Object? body, Map<String, String>? headers}) async {
    final request = http.Request(method, Uri.parse('$baseUrl/api/v1$path'));
    _prepare(request, headers);
    if (body != null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = jsonEncode(body);
    }
    return _dispatch(request);
  }

  /// Tải tệp lên bằng multipart/form-data (001 T082: `POST /files`, trường `file`); trả JSON, ví dụ `{id}`.
  Future<Map<String, dynamic>?> uploadFile(
    String path,
    String field,
    List<int> bytes,
    String filename,
    String contentType, {
    Map<String, String>? headers,
  }) async {
    final request = http.MultipartRequest('POST', Uri.parse('$baseUrl/api/v1$path'));
    _prepare(request, headers);
    request.files.add(
      http.MultipartFile.fromBytes(field, bytes, filename: filename, contentType: MediaType.parse(contentType)),
    );
    final decoded = (await _dispatch(request)).body;
    return decoded is Map<String, dynamic> ? decoded : null;
  }

  void _prepare(http.BaseRequest request, Map<String, String>? headers) {
    request.headers['Accept'] = 'application/json, application/problem+json';
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    if (deviceId != null) request.headers['X-Device-Id'] = deviceId!;
    if (headers != null) request.headers.addAll(headers);
  }

  Future<ApiResponse> _dispatch(http.BaseRequest request) async {
    final http.Response response;
    try {
      response = await http.Response.fromStream(await _http.send(request));
    } on Exception {
      throw ApiUnreachable();
    }
    lastConnectedAt = DateTime.now().toUtc();
    final text = utf8.decode(response.bodyBytes);
    final decoded = text.isEmpty ? null : jsonDecode(text);
    final errorCode = decoded is Map<String, dynamic> ? decoded['errorCode'] as String? : null;
    final background = request.headers['X-Background-Refresh'] == 'true';
    for (final listener in List.of(_listeners)) {
      listener(response.statusCode, errorCode, background);
    }
    if (response.statusCode >= 400) {
      final problem = decoded is Map<String, dynamic> ? decoded : const <String, dynamic>{};
      throw ApiProblem(response.statusCode, problem['errorCode'] as String?, problem['detail'] as String?, problem);
    }
    return ApiResponse(decoded, response.headers);
  }
}
