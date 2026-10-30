import 'dart:convert';

import 'package:http/http.dart' as http;

/// Lỗi theo Problem Details của backend (RFC 9457; như `common/problem.js` của web).
class ApiProblem implements Exception {
  ApiProblem(this.status, this.errorCode, this.detail);

  final int status;
  final String? errorCode;
  final String? detail;

  @override
  String toString() => 'ApiProblem($status, $errorCode)';
}

/// Không gọi được backend (mất mạng, máy chủ không trả lời).
class ApiUnreachable implements Exception {}

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

  Future<Map<String, dynamic>?> get(String path) => _send('GET', path, null);

  /// `headers`: ví dụ `If-Match` của lệnh trên đối tượng có phiên bản (000 FR-040).
  Future<Map<String, dynamic>?> post(String path, Map<String, dynamic> body, {Map<String, String>? headers}) =>
      _send('POST', path, body, headers);

  Future<Map<String, dynamic>?> _send(
    String method,
    String path,
    Map<String, dynamic>? body, [
    Map<String, String>? headers,
  ]) async {
    final request = http.Request(method, Uri.parse('$baseUrl/api/v1$path'));
    request.headers['Accept'] = 'application/json, application/problem+json';
    if (token != null) request.headers['Authorization'] = 'Bearer $token';
    if (deviceId != null) request.headers['X-Device-Id'] = deviceId!;
    if (headers != null) request.headers.addAll(headers);
    if (body != null) {
      request.headers['Content-Type'] = 'application/json';
      request.body = jsonEncode(body);
    }
    final http.Response response;
    try {
      response = await http.Response.fromStream(await _http.send(request));
    } on Exception {
      throw ApiUnreachable();
    }
    lastConnectedAt = DateTime.now().toUtc();
    final text = utf8.decode(response.bodyBytes);
    final decoded = text.isEmpty ? null : jsonDecode(text);
    if (response.statusCode >= 400) {
      final problem = decoded is Map<String, dynamic> ? decoded : const <String, dynamic>{};
      throw ApiProblem(response.statusCode, problem['errorCode'] as String?, problem['detail'] as String?);
    }
    return decoded is Map<String, dynamic> ? decoded : null;
  }
}
