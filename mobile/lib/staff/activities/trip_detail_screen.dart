import 'dart:convert';
import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/offline/offline_queue.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/page_body.dart';
import '../../common/widgets/status_pill.dart';
import '../care_tasks/care_offline_recording.dart';
import 'trip_participant_info.dart';

const tripStatuses = {
  'SCHEDULED': 'Đã lên lịch',
  'IN_PROGRESS': 'Đang đi',
  'RETURNED': 'Đã trở về',
  'CANCELLED': 'Đã hủy',
};
const _participantStatuses = {
  'REGISTERED': 'Đã đăng ký',
  'ELIGIBLE': 'Được đi',
  'NOT_GOING': 'Không đi',
  'DEPARTED': 'Đã rời viện',
  'RETURNED': 'Đã về',
  'LEFT_GROUP': 'Rời đoàn',
  'MISSING_ON_RETURN': 'Thiếu khi về',
};

/// FR-039/040, FR-044: màn dài có nút chính cố định. Điểm danh rời viện, về lưu phong bì qua hàng đợi ngoại tuyến chung
/// (Q24, Q-01); mất mạng thì giữ trên máy, gửi khi có mạng.
class TripDetailScreen extends StatefulWidget {
  const TripDetailScreen({
    super.key,
    required this.api,
    required this.queue,
    required this.accountId,
    required this.tripId,
  });
  final ApiClient api;
  final OfflineQueue queue;
  final int accountId;
  final int tripId;
  @override
  State<TripDetailScreen> createState() => _TripDetailScreenState();
}

class _TripDetailScreenState extends State<TripDetailScreen> {
  Map<String, dynamic>? _trip;
  Map<int, String> _names = {};
  final _selected = <int>{};
  final _reason = TextEditingController();
  final _keys = <String, String>{};
  bool _canCreate = false, _permitted = true, _busy = false;
  String? _error, _message;
  late final CareOfflineRecording _recorder = CareOfflineRecording(widget.api, widget.queue, widget.accountId);

  /// Người được chọn cùng một nhóm: Được đi (rời viện) hoặc Đã rời viện (về, khi chuyến Đang đi).
  bool _returning = false;
  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final permissions = (await widget.api.getList('/me/permissions')).whereType<Map<String, dynamic>>();
      final allowed = permissions.any((p) => p['function'] == 'ACTIVITY_OUTING' && p['operation'] == 'VIEW');
      if (!allowed) {
        if (mounted) setState(() => _permitted = false);
        return;
      }
      final trip = await widget.api.get('/trips/${widget.tripId}');
      Map<int, String> names = {};
      try {
        final residents = await widget.api.get('/residents');
        names = {
          for (final r in (residents?['items'] as List<dynamic>? ?? []).whereType<Map<String, dynamic>>())
            r['id'] as int: r['fullName'] as String,
        };
      } on Exception {
        /* Tên không có thì hiện mã, không chặn việc đọc chuyến. */
      }
      if (mounted) {
        setState(() {
          _trip = trip;
          _names = names;
          _permitted = true;
          _canCreate = permissions.any((p) => p['function'] == 'ACTIVITY_OUTING' && p['operation'] == 'CREATE');
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  String _name(int id) => _names[id] ?? 'Người cao tuổi $id';
  String _uuid() {
    final random = Random.secure(), bytes = List<int>.generate(16, (_) => random.nextInt(256));
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    final hex = bytes.map((n) => n.toRadixString(16).padLeft(2, '0')).join();
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
  }

  bool _selectable(Map<String, dynamic> p, Map<String, dynamic> trip) =>
      p['status'] == 'ELIGIBLE' || (p['status'] == 'DEPARTED' && trip['status'] == 'IN_PROGRESS');

  bool _anyEligible(Map<String, dynamic>? trip) => (trip?['participants'] as List<dynamic>? ?? [])
      .whereType<Map<String, dynamic>>()
      .any((p) => p['status'] == 'ELIGIBLE');

  Future<void> _record() async {
    if (_busy || _selected.isEmpty) return;
    final ids = _selected.toList()..sort();
    final returning = _returning;
    final reason = _reason.text.trim();
    final body = <String, dynamic>{'returning': returning, 'residentIds': ids, if (!returning) 'reason': reason};
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(returning ? 'Xác nhận về' : 'Xác nhận rời viện'),
        content: Text(
          'Ghi nhận ${ids.length} người ${returning ? 'đã về' : 'rời viện'}: ${ids.map(_name).join(', ')}. Hệ thống sẽ kiểm tra điều kiện trước khi ghi nhận.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Quay lại')),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(returning ? 'Xác nhận về' : 'Xác nhận rời viện'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    setState(() {
      _busy = true;
      _error = null;
      _message = null;
    });
    final signature = jsonEncode(body), key = _keys.putIfAbsent(signature, _uuid);
    try {
      final queued = await _recorder.recordTripAttendance(returning, widget.tripId, ids, reason, key);
      _keys.remove(signature);
      if (mounted) {
        setState(() {
          _selected.clear();
          _message = queued
              ? 'Đã lưu trên máy, sẽ gửi khi có mạng'
              : (returning ? 'Đã điểm danh về' : 'Đã điểm danh rời viện');
        });
      }
      if (!queued) await _load();
    } catch (e) {
      // Bị từ chối là kết quả cuối của phong bì; lần sau lập phong bì mới.
      _keys.remove(signature);
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final trip = _trip, tones = Theme.of(context).extension<Tones>()!;
    final active = trip != null && ['SCHEDULED', 'IN_PROGRESS'].contains(trip['status']);
    final participants = (trip?['participants'] as List<dynamic>? ?? []).whereType<Map<String, dynamic>>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Chi tiết chuyến'),
        backgroundColor: tones.header,
        foregroundColor: tones.headerInkStrong,
      ),
      body: PageBody(
        child: !_permitted
            ? const Text('Bạn chưa có quyền xem chuyến đi.')
            : ListView(
                children: [
                  if (_error != null && trip == null) Semantics(liveRegion: true, child: Text(_error!)),
                  // Có nút chính thì báo kết quả ở chân màn, cạnh nút.
                  if (_message != null && !(_canCreate && active)) Semantics(liveRegion: true, child: Text(_message!)),
                  if (trip == null) ...[
                    if (_error == null)
                      const Center(child: CircularProgressIndicator())
                    else
                      TextButton(onPressed: _load, child: const Text('Thử lại')),
                  ] else ...[
                    StatusPill(
                      label: tripStatuses[trip['status']] ?? trip['status'] as String,
                      tone: StatusTone.neutral,
                    ),
                    const SizedBox(height: 16),
                    Text('Điểm đến: ${trip['destination']}', style: Theme.of(context).textTheme.titleMedium),
                    Text('Rời dự kiến: ${formatDateTime(DateTime.parse(trip['plannedDepartureAt'] as String))}'),
                    Text('Về dự kiến: ${formatDateTime(DateTime.parse(trip['plannedReturnAt'] as String))}'),
                    Text('Phương tiện: ${trip['transport']}${trip['facilityVehicle'] == true ? ' · Xe của viện' : ''}'),
                    const Divider(height: 32),
                    Text('Phân công', style: Theme.of(context).textTheme.titleMedium),
                    Text('Trưởng đoàn: ${trip['leaderId'] ?? 'Chưa phân công'}'),
                    Text('Người đi cùng: ${(trip['companionIds'] as List<dynamic>).join(', ')}'),
                    const Divider(height: 32),
                    Text('Người tham gia', style: Theme.of(context).textTheme.titleMedium),
                    if (participants.isEmpty) const Text('Chưa có người tham gia.'),
                    for (final p in participants)
                      Material(
                        color: Theme.of(context).colorScheme.surface,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            if (_canCreate && active && _selectable(p, trip))
                              CheckboxListTile(
                                title: Text(_name(p['residentId'] as int)),
                                subtitle: Text(_participantStatuses[p['status']] ?? p['status'] as String),
                                value: _selected.contains(p['residentId']),
                                onChanged: _busy
                                    ? null
                                    : (value) => setState(() {
                                        final returning = p['status'] == 'DEPARTED';
                                        if (value == true && returning != _returning) {
                                          _selected.clear();
                                          _returning = returning;
                                        }
                                        value == true
                                            ? _selected.add(p['residentId'] as int)
                                            : _selected.remove(p['residentId']);
                                      }),
                              )
                            else
                              ListTile(
                                title: Text(_name(p['residentId'] as int)),
                                subtitle: Text(_participantStatuses[p['status']] ?? p['status'] as String),
                              ),
                            if (p['reason'] != null)
                              Padding(padding: const EdgeInsets.all(16), child: Text('Lý do: ${p['reason']}')),
                            if (p['info'] is Map<String, dynamic>)
                              TripParticipantInfo(info: p['info'] as Map<String, dynamic>),
                            const Divider(height: 1),
                          ],
                        ),
                      ),
                    if (_canCreate && active) ...[
                      const SizedBox(height: 16),
                      TextField(
                        controller: _reason,
                        maxLines: 3,
                        decoration: const InputDecoration(labelText: 'Lý do điểm danh thay (Trưởng tầng)'),
                      ),
                    ],
                  ],
                ],
              ),
      ),
      bottomNavigationBar: _canCreate && active && _permitted
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (_message != null) Semantics(liveRegion: true, child: Text(_message!)),
                    if (_error != null) Semantics(liveRegion: true, child: Text(_error!)),
                    FilledButton(
                      onPressed: _busy || _selected.isEmpty ? null : _record,
                      child: Text(
                        _busy
                            ? 'Đang ghi nhận…'
                            : (_selected.isEmpty ? !_anyEligible(_trip) : _returning)
                            ? 'Điểm danh về'
                            : 'Điểm danh rời viện',
                      ),
                    ),
                  ],
                ),
              ),
            )
          : null,
    );
  }
}
