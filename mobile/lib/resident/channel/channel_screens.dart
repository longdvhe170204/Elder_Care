import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/status_pill.dart';

/// Nút "Quay lại" có chữ ở góc trái thanh tiêu đề (DESIGN.md 15.6); bề rộng theo cỡ chữ của máy.
AppBar residentBackBar(BuildContext context) => AppBar(
  automaticallyImplyLeading: false,
  leadingWidth: MediaQuery.textScalerOf(context).scale(120),
  leading: TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Quay lại')),
);

/// Lượt thăm Đã duyệt trên kênh (012 FR-083 (d)).
class ChannelVisit {
  ChannelVisit(Map<String, dynamic> json)
    : date = json['date'] as String,
      startTime = json['startTime'] as String,
      endTime = json['endTime'] as String,
      visitors = (json['visitors'] as List<dynamic>).cast<String>();

  final String date;
  final String startTime;
  final String endTime;
  final List<String> visitors;

  String get who => visitors.join(', ');
}

/// Buổi hoạt động của tầng mình hoặc toàn viện (012 FR-083 (c)).
class ChannelActivity {
  ChannelActivity(Map<String, dynamic> json)
    : name = json['name'] as String,
      startAt = DateTime.parse(json['startAt'] as String),
      endAt = DateTime.parse(json['endAt'] as String),
      registered = json['registered'] as bool;

  final String name;
  final DateTime startAt;
  final DateTime endAt;
  final bool registered;
}

const _belongingStatus = {'HELD': 'Đang giữ', 'IN_USE': 'Đang được sử dụng'};

/// Một phần của kênh: `available = false` khi nguồn chưa có (012 research F-18).
class ChannelSection {
  ChannelSection(Map<String, dynamic> json)
    : available = json['available'] as bool,
      items = (json['items'] as List<dynamic>).cast<Map<String, dynamic>>();

  final bool available;
  final List<Map<String, dynamic>> items;
}

/// Màn xem một phần của kênh người cao tuổi (DESIGN.md 15.6): tiêu đề lớn trong thân màn, chữ lớn, không thao tác ẩn;
/// mất mạng thì nói rõ bằng câu ngắn.
class _ChannelPage extends StatelessWidget {
  const _ChannelPage({required this.title, required this.children});

  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: residentBackBar(context),
    body: SafeArea(
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(title, style: Theme.of(context).textTheme.headlineMedium),
          const SizedBox(height: 16),
          ...children,
        ],
      ),
    ),
  );
}

class _Line extends StatelessWidget {
  const _Line({required this.title, this.subtitle, this.trailing});

  final String title;
  final String? subtitle;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: text.titleLarge),
            if (subtitle != null) ...[const SizedBox(height: 4), Text(subtitle!, style: text.bodyLarge)],
            if (trailing != null) ...[const SizedBox(height: 8), trailing!],
          ],
        ),
      ),
    );
  }
}

/// Tải một phần rồi dựng; lỗi kết nối thì câu ngắn.
class _Loaded extends StatefulWidget {
  const _Loaded({required this.load, required this.title, required this.build});

  final Future<Object?> Function() load;
  final String title;
  final List<Widget> Function(BuildContext context, Object? data) build;

  @override
  State<_Loaded> createState() => _LoadedState();
}

class _LoadedState extends State<_Loaded> {
  Object? _data;
  bool _loaded = false;
  bool _failed = false;

  @override
  void initState() {
    super.initState();
    widget
        .load()
        .then((data) {
          if (mounted) {
            setState(() {
              _data = data;
              _loaded = true;
            });
          }
        })
        .catchError((Object _) {
          if (mounted) setState(() => _failed = true);
        });
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return _ChannelPage(
      title: widget.title,
      children: _failed
          ? [Text('Máy chưa kết nối mạng.', style: text.bodyLarge)]
          : !_loaded
          ? [const Center(child: CircularProgressIndicator())]
          : widget.build(context, _data),
    );
  }
}

/// "Lịch hôm nay": lượt thăm, buổi hoạt động, giờ đến, về của người bán trú (012 FR-083 (a)); không có việc chăm sóc,
/// giờ thuốc.
class ScheduleScreen extends StatelessWidget {
  const ScheduleScreen({super.key, required this.api});

  final ApiClient api;

  @override
  Widget build(BuildContext context) => _Loaded(
    title: 'Lịch hôm nay',
    load: () => api.get('/resident-channel/today'),
    build: (context, data) {
      final lines = todayLines(data as Map<String, dynamic>);
      if (lines.isEmpty) return [Text('Hôm nay chưa có lịch.', style: Theme.of(context).textTheme.bodyLarge)];
      return [for (final line in lines) _Line(title: line)];
    },
  );
}

/// Các dòng của lịch hôm nay theo giờ: "10:00 Tập dưỡng sinh", "15:00 [người thăm] đến thăm".
List<String> todayLines(Map<String, dynamic> today) {
  final lines = <(String, String)>[];
  for (final v in (today['visits'] as List<dynamic>).cast<Map<String, dynamic>>().map(ChannelVisit.new)) {
    lines.add((v.startTime, '${v.startTime} ${v.who} đến thăm'));
  }
  for (final a in (today['activities'] as List<dynamic>).cast<Map<String, dynamic>>().map(ChannelActivity.new)) {
    final at = formatTime(a.startAt);
    lines.add((at, '$at ${a.name}'));
  }
  for (final d in (today['dayCareSchedule'] as List<dynamic>).cast<Map<String, dynamic>>()) {
    lines.add((d['arrivalTime'] as String, '${d['arrivalTime']} Đến viện'));
    lines.add((d['departureTime'] as String, '${d['departureTime']} Về nhà'));
  }
  lines.sort((a, b) => a.$1.compareTo(b.$1));
  return [for (final l in lines) l.$2];
}

/// "Bữa ăn": thực đơn tuần và suất ăn dự kiến (012 FR-083 (b)); không có tên chế độ ăn. 011 chưa có nguồn thì báo chưa có.
class MenuScreen extends StatelessWidget {
  const MenuScreen({super.key, required this.api});

  final ApiClient api;

  @override
  Widget build(BuildContext context) => _Loaded(
    title: 'Bữa ăn',
    load: () => api.get('/resident-channel/menu'),
    build: (context, data) {
      final section = ChannelSection(data as Map<String, dynamic>);
      final text = Theme.of(context).textTheme;
      if (!section.available || section.items.isEmpty) return [Text('Chưa có thực đơn.', style: text.bodyLarge)];
      return [for (final item in section.items) _Line(title: '${item['name'] ?? ''}')];
    },
  );
}

/// "Hoạt động": buổi sắp tới, giờ, đã có tên chưa (012 FR-083 (c)); không có nút đăng ký (FR-084).
class ActivitiesScreen extends StatelessWidget {
  const ActivitiesScreen({super.key, required this.api});

  final ApiClient api;

  @override
  Widget build(BuildContext context) => _Loaded(
    title: 'Hoạt động',
    load: () => api.get('/resident-channel/activities'),
    build: (context, data) {
      final section = ChannelSection(data as Map<String, dynamic>);
      final text = Theme.of(context).textTheme;
      if (!section.available) return [Text('Chưa có lịch hoạt động.', style: text.bodyLarge)];
      if (section.items.isEmpty) return [Text('Chưa có buổi hoạt động sắp tới.', style: text.bodyLarge)];
      return [
        for (final a in section.items.map(ChannelActivity.new))
          _Line(
            title: a.name,
            subtitle: '${formatDateTime(a.startAt)}–${formatTime(a.endAt)}',
            trailing: StatusPill(
              label: a.registered ? 'Đã có tên' : 'Chưa có tên',
              tone: a.registered ? StatusTone.settled : StatusTone.neutral,
            ),
          ),
      ];
    },
  );
}

/// "Người đến thăm": ngày, giờ, họ tên người thăm (012 FR-083 (d)).
class VisitsScreen extends StatelessWidget {
  const VisitsScreen({super.key, required this.api});

  final ApiClient api;

  @override
  Widget build(BuildContext context) => _Loaded(
    title: 'Người đến thăm',
    load: () => api.get('/resident-channel/visits'),
    build: (context, data) {
      final section = ChannelSection(data as Map<String, dynamic>);
      if (section.items.isEmpty) {
        return [Text('Chưa có lượt thăm sắp tới.', style: Theme.of(context).textTheme.bodyLarge)];
      }
      return [
        for (final v in section.items.map(ChannelVisit.new))
          _Line(title: v.who, subtitle: '${formatDate(v.date)}, ${v.startTime}–${v.endTime}'),
      ];
    },
  );
}

/// "Đồ của tôi": tên đồ, trạng thái (012 FR-083 (e)). Ảnh đồ gửi chưa hiện: chưa có đường tải tệp cho tài khoản người cao
/// tuổi.
class BelongingsScreen extends StatelessWidget {
  const BelongingsScreen({super.key, required this.api});

  final ApiClient api;

  @override
  Widget build(BuildContext context) => _Loaded(
    title: 'Đồ của tôi',
    load: () => api.get('/resident-channel/belongings'),
    build: (context, data) {
      final section = ChannelSection(data as Map<String, dynamic>);
      final text = Theme.of(context).textTheme;
      if (!section.available) return [Text('Chưa có thông tin đồ gửi.', style: text.bodyLarge)];
      if (section.items.isEmpty) return [Text('Viện đang không giữ đồ nào của bạn.', style: text.bodyLarge)];
      return [
        for (final b in section.items)
          _Line(
            title: '${b['name']}',
            trailing: StatusPill(label: _belongingStatus[b['status']] ?? '${b['status']}', tone: StatusTone.neutral),
          ),
      ];
    },
  );
}
