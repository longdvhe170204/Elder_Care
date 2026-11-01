import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/notifications/notification_api.dart';
import '../../common/widgets/page_body.dart';
import '../notifications/resident_notifications_screen.dart';

const _weekdays = ['thứ Hai', 'thứ Ba', 'thứ Tư', 'thứ Năm', 'thứ Sáu', 'thứ Bảy', 'Chủ nhật'];

/// "Hôm nay, thứ Hai, 19/10/2026" theo giờ Việt Nam (DESIGN.md 15.6; mobile/README.md luật 4).
String todayTitle(DateTime now) {
  final vn = now.toUtc().add(const Duration(hours: 7));
  String two(int n) => n.toString().padLeft(2, '0');
  return 'Hôm nay, ${_weekdays[vn.weekday - 1]}, ${two(vn.day)}/${two(vn.month)}/${vn.year}';
}

/// Màn đầu "Hôm nay" của người cao tuổi (DESIGN.md 15.6). Các thẻ Lịch hôm nay, Bữa ăn, Hoạt động, Người đến thăm,
/// Đồ của tôi, Góp ý với viện và nút SOS thêm khi các feature 012, 007 có phần app; thẻ Thông báo của 009 T042.
class TodayScreen extends StatefulWidget {
  const TodayScreen({super.key, required this.api, this.now});

  final ApiClient api;
  final DateTime? now;

  @override
  State<TodayScreen> createState() => _TodayScreenState();
}

class _TodayScreenState extends State<TodayScreen> {
  int? _unread;

  @override
  void initState() {
    super.initState();
    _loadUnread();
  }

  /// Không lấy được số thì thẻ vẫn mở được, chỉ không có dòng số (mất mạng).
  Future<void> _loadUnread() async {
    try {
      final page = await NotificationApi(widget.api).page();
      if (mounted) setState(() => _unread = page.unreadCount);
    } on Exception {
      if (mounted) setState(() => _unread = null);
    }
  }

  Future<void> _openNotifications() async {
    await Navigator.of(
      context,
    ).push<void>(MaterialPageRoute(builder: (_) => ResidentNotificationsScreen(api: widget.api)));
    await _loadUnread();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final unread = _unread;
    return Scaffold(
      body: PageBody(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(todayTitle(widget.now ?? DateTime.now()), style: text.headlineMedium),
            const SizedBox(height: 16),
            Card(
              clipBehavior: Clip.antiAlias,
              child: InkWell(
                onTap: _openNotifications,
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Thông báo', style: text.titleLarge),
                      if (unread != null) ...[
                        const SizedBox(height: 4),
                        Text(
                          unread > 0 ? '$unread thông báo chưa xem' : 'Không có thông báo mới',
                          style: text.bodyLarge,
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
