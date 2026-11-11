import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';

// Hoạt động trên app người thân (014 FR-068, loại thông tin chung; api-014 FamilyActivities, chủ dự án duyệt
// 2026-11-10), cùng chữ với cổng web: buổi đã đăng ký, chuyến đi, số buổi đã tham gia. Người đại diện ghi, rút đồng ý về
// muộn cho buổi đủ điều kiện (FR-016a). Không có mức tham gia, giao tiếp, cảnh báo hay kết quả kiểm tra.

const _tripStatus = {'SCHEDULED': 'Đã lên lịch', 'IN_PROGRESS': 'Đang đi', 'RETURNED': 'Đã về', 'CANCELLED': 'Đã hủy'};

String _date(String iso) => iso.split('-').reversed.join('/');

String _key() => List.generate(16, (_) => Random.secure().nextInt(256).toRadixString(16).padLeft(2, '0')).join();

class FamilyActivitiesScreen extends StatefulWidget {
  const FamilyActivitiesScreen({super.key, required this.api, required this.residentId, required this.representative});

  final ApiClient api;
  final int residentId;
  final bool representative;

  @override
  State<FamilyActivitiesScreen> createState() => _FamilyActivitiesScreenState();
}

class _FamilyActivitiesScreenState extends State<FamilyActivitiesScreen> {
  Map<String, dynamic>? _data;
  String? _error;
  String? _message;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final data = await widget.api.get('/family-portal/residents/${widget.residentId}/activities');
      if (mounted) setState(() => _data = data);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Future<void> _run(String path, Map<String, dynamic> body, String done) async {
    setState(() => _busy = true);
    try {
      await widget.api.post(path, body, headers: {'Idempotency-Key': _key()});
      _message = done;
      await _load();
    } on Exception catch (e) {
      _error = backendMessage(e);
    }
    if (mounted) setState(() => _busy = false);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final data = _data;
    final sessions = (data?['registeredSessions'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
    final trips = (data?['trips'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
    return Scaffold(
      appBar: AppBar(title: const Text('Hoạt động')),
      body: data == null
          ? Padding(
              padding: const EdgeInsets.all(16),
              child: _error != null ? Text(_error!) : const Center(child: CircularProgressIndicator()),
            )
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (_message != null) Semantics(liveRegion: true, child: Text(_message!)),
                if (_error != null) Semantics(liveRegion: true, child: Text(_error!)),
                if (sessions.isEmpty) const Text('Chưa đăng ký buổi nào trong khoảng này.'),
                for (final s in sessions)
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(s['activityName'] as String),
                    subtitle: Text(
                      [
                        formatDateTime(DateTime.parse(s['startsAt'] as String)),
                        if (s['activeLateReturnConsentId'] != null) 'Đã đồng ý về muộn',
                      ].join(', '),
                    ),
                    trailing: !widget.representative || s['lateReturnConsentEligible'] != true
                        ? null
                        : s['activeLateReturnConsentId'] == null
                        ? OutlinedButton(
                            onPressed: _busy
                                ? null
                                : () => _run('/family-portal/late-return-consents', {
                                    'residentId': widget.residentId,
                                    'sessionId': s['sessionId'],
                                  }, 'Đã đồng ý về muộn cho buổi ${s['activityName']}'),
                            child: const Text('Đồng ý về muộn'),
                          )
                        : TextButton(
                            onPressed: _busy
                                ? null
                                : () => _run(
                                    '/family-portal/late-return-consents/${s['activeLateReturnConsentId']}/withdraw',
                                    {},
                                    'Đã rút đồng ý về muộn cho buổi ${s['activityName']}',
                                  ),
                            child: const Text('Rút đồng ý'),
                          ),
                  ),
                if (trips.isNotEmpty) ...[
                  const SizedBox(height: 16),
                  Text('Chuyến đi', style: theme.textTheme.titleMedium),
                  for (final t in trips)
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text(t['destination'] as String),
                      subtitle: Text(
                        '${formatDateTime(DateTime.parse(t['departureAt'] as String))} → '
                        '${formatDateTime(DateTime.parse(t['expectedReturnAt'] as String))}, '
                        '${_tripStatus[t['status']] ?? t['status']}',
                      ),
                    ),
                ],
                const SizedBox(height: 16),
                Text(
                  'Đã tham gia ${data['attendedSessionCount']} buổi từ ${_date(data['from'] as String)} '
                  'đến ${_date(data['to'] as String)}',
                ),
              ],
            ),
    );
  }
}
