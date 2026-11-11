import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/offline/offline_queue.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/page_body.dart';
import 'trip_detail_screen.dart';

/// T018: danh sách chuyến trực tuyến theo ngày, gắn vào điều hướng sau khi A duyệt.
class TripListScreen extends StatefulWidget {
  const TripListScreen({
    super.key,
    required this.api,
    required this.queue,
    required this.accountId,
    required this.date,
  });
  final ApiClient api;
  final OfflineQueue queue;
  final int accountId;
  final String date;
  @override
  State<TripListScreen> createState() => _TripListScreenState();
}

class _TripListScreenState extends State<TripListScreen> {
  late String _date = widget.date;
  List<Map<String, dynamic>>? _rows;
  Map<int, String> _names = {};
  String? _error;
  bool _permitted = true;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _rows = null;
      _error = null;
    });
    try {
      final permissions = await widget.api.getList('/me/permissions');
      final allowed = permissions.whereType<Map<String, dynamic>>().any(
        (p) => p['function'] == 'ACTIVITY_OUTING' && p['operation'] == 'VIEW',
      );
      if (!allowed) {
        if (mounted) {
          setState(() {
            _permitted = false;
            _rows = [];
          });
        }
        return;
      }
      final rows = await widget.api.getList('/activity-sessions?from=$_date&to=$_date');
      final activities = await widget.api.getList('/activities');
      if (mounted) {
        setState(() {
          _permitted = true;
          _rows = rows.whereType<Map<String, dynamic>>().where((s) => s['isTrip'] == true).toList();
          _names = {for (final a in activities.whereType<Map<String, dynamic>>()) a['id'] as int: a['name'] as String};
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = backendMessage(e);
          _rows = [];
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final tones = Theme.of(context).extension<Tones>()!;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Chuyến đi ngoài viện'),
        backgroundColor: tones.header,
        foregroundColor: tones.headerInkStrong,
      ),
      body: PageBody(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            OutlinedButton.icon(
              icon: const Icon(LucideIcons.calendarDays),
              label: Text(formatDate(_date)),
              onPressed: () async {
                final current = DateTime.parse(_date);
                final picked = await showDatePicker(
                  context: context,
                  initialDate: current,
                  firstDate: DateTime(2000),
                  lastDate: DateTime(2100),
                );
                if (picked != null && mounted) {
                  setState(
                    () => _date =
                        '${picked.year}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}',
                  );
                  await _load();
                }
              },
            ),
            const SizedBox(height: 16),
            if (_error != null) ...[
              Semantics(liveRegion: true, child: Text(_error!)),
              TextButton(onPressed: _load, child: const Text('Thử lại')),
            ],
            if (!_permitted) const Text('Bạn chưa có quyền xem chuyến đi.'),
            if (_rows == null)
              const Center(child: CircularProgressIndicator())
            else if (_rows!.isEmpty && _error == null && _permitted)
              const Text('Ngày này chưa có chuyến đi.')
            else
              Expanded(
                child: ListView.separated(
                  itemCount: _rows!.length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (context, i) {
                    final row = _rows![i];
                    return Material(
                      color: Theme.of(context).colorScheme.surface,
                      child: ListTile(
                        minTileHeight: 64,
                        title: Text(_names[row['activityId']] ?? 'Chuyến ${row['id']}'),
                        subtitle: Text(formatDateTime(DateTime.parse(row['startsAt'] as String))),
                        trailing: Text(tripStatuses[row['status']] ?? row['status'] as String),
                        onTap: () => Navigator.of(context).push<void>(
                          MaterialPageRoute(
                            builder: (_) => TripDetailScreen(
                              api: widget.api,
                              queue: widget.queue,
                              accountId: widget.accountId,
                              tripId: row['id'] as int,
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              ),
          ],
        ),
      ),
    );
  }
}
