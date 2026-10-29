import 'package:flutter/material.dart';

import '../../common/widgets/page_body.dart';

const _weekdays = ['thứ Hai', 'thứ Ba', 'thứ Tư', 'thứ Năm', 'thứ Sáu', 'thứ Bảy', 'Chủ nhật'];

/// "Hôm nay, thứ Hai, 19/10/2026" theo giờ Việt Nam (DESIGN.md 15.6; mobile/README.md luật 4).
String todayTitle(DateTime now) {
  final vn = now.toUtc().add(const Duration(hours: 7));
  String two(int n) => n.toString().padLeft(2, '0');
  return 'Hôm nay, ${_weekdays[vn.weekday - 1]}, ${two(vn.day)}/${two(vn.month)}/${vn.year}';
}

/// Màn đầu "Hôm nay" của người cao tuổi (DESIGN.md 15.6). Các thẻ Lịch hôm nay, Bữa ăn, Hoạt động, Người đến thăm,
/// Đồ của tôi, Góp ý với viện và nút SOS thêm khi các feature 012, 007 có phần app.
class TodayScreen extends StatelessWidget {
  const TodayScreen({super.key, this.now});

  final DateTime? now;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: PageBody(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(todayTitle(now ?? DateTime.now()), style: text.headlineMedium),
            const SizedBox(height: 16),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Text('Chưa có mục nào để xem.', style: text.bodyLarge),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
