import 'dart:math';

import '../api/api_client.dart';

/// Một trang hộp thông báo (`GET /me/notifications`, 009 FR-033).
class NotificationPage {
  const NotificationPage({
    required this.items,
    required this.unreadCount,
    required this.urgentCount,
    required this.nextCursor,
  });

  final List<Map<String, dynamic>> items;
  final int unreadCount;
  final int urgentCount;
  final String? nextCursor;
}

/// Lọc hộp thông báo, trường rỗng là không lọc (FR-033).
class NotificationFilter {
  const NotificationFilter({this.level, this.unread = false, this.eventType, this.residentId});

  final String? level;
  final bool unread;
  final String? eventType;
  final int? residentId;
}

/// Đọc hộp thông báo của tài khoản đang đăng nhập, dùng chung cho các phần của app (009 T031, T042). Xác nhận và ẩn cần
/// `Idempotency-Key`, giữ một khóa cho mỗi thao tác để bấm lại khi mạng chập chờn không ghi hai lần.
class NotificationApi {
  const NotificationApi(this._api);

  final ApiClient _api;

  Future<NotificationPage> page({NotificationFilter filter = const NotificationFilter(), String? cursor}) async {
    final query = Uri(
      queryParameters: {
        'level': ?filter.level,
        if (filter.unread) 'unread': 'true',
        'eventType': ?filter.eventType,
        if (filter.residentId != null) 'residentId': '${filter.residentId}',
        'cursor': ?cursor,
      },
    ).query;
    final body = await _api.get('/me/notifications${query.isEmpty ? '' : '?$query'}');
    return NotificationPage(
      items: (body?['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>(),
      unreadCount: body?['unreadCount'] as int? ?? 0,
      urgentCount: body?['unacknowledgedUrgentCount'] as int? ?? 0,
      nextCursor: body?['nextCursor'] as String?,
    );
  }

  /// Mở thông báo: backend ghi thời điểm xem lần đầu và trả nội dung theo quyền hiện hành (FR-034).
  Future<Map<String, dynamic>> open(int id) async => (await _api.get('/me/notifications/$id'))!;

  Future<void> hide(int id, String key) =>
      _api.post('/me/notifications/$id/hide', const {}, headers: {'Idempotency-Key': key});

  Future<void> acknowledge(int id, String key) =>
      _api.post('/notifications/$id/acknowledge', const {}, headers: {'Idempotency-Key': key});
}

/// UUID v4 cho `Idempotency-Key`.
String newIdempotencyKey() {
  final random = Random.secure();
  final bytes = List<int>.generate(16, (_) => random.nextInt(256));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  final hex = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-'
      '${hex.substring(16, 20)}-${hex.substring(20)}';
}
