import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/page_body.dart';
import 'bed_command.dart';

/// T056: đặt/gỡ cách ly phòng hoặc Bác sĩ xác nhận ở lại vùng cách ly, đều có lý do.
class RoomIsolationScreen extends StatefulWidget {
  const RoomIsolationScreen({super.key, required this.api, required this.roomId, this.allocationId});
  final ApiClient api;
  final int roomId;
  final int? allocationId;
  @override
  State<RoomIsolationScreen> createState() => _RoomIsolationScreenState();
}

class _RoomIsolationScreenState extends State<RoomIsolationScreen> {
  final _reason = TextEditingController();
  Map<String, dynamic>? _room;
  List<Map<String, dynamic>> _history = [];
  bool _canWrite = false, _busy = false, _done = false;
  String? _error, _message, _key;
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
      final rooms = (await widget.api.getList('/room-isolation/rooms')).whereType<Map<String, dynamic>>();
      final room = rooms.where((r) => r['id'] == widget.roomId).firstOrNull;
      if (room == null) {
        throw ApiProblem(404, 'NOT_FOUND', 'Không tìm thấy phòng trong phạm vi được xem.');
      }
      final history = await widget.api.getList('/rooms/${widget.roomId}/isolation-history');
      bool doctor = false;
      if (widget.allocationId != null) {
        doctor = ((await widget.api.get('/me'))?['roles'] as List<dynamic>? ?? []).contains('AC-03');
      }
      final operation = widget.allocationId != null || room['isolated'] == true ? 'UPDATE' : 'CREATE';
      if (mounted) {
        setState(() {
          _room = room;
          _history = history.whereType<Map<String, dynamic>>().toList();
          _canWrite =
              (widget.allocationId == null || doctor) &&
              permissions.any((p) => p['function'] == 'ROOM_ISOLATION' && p['operation'] == operation);
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = bedProblem(e));
    }
  }

  Future<void> _save() async {
    if (_busy || _done) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final path = widget.allocationId != null
          ? '/bed-allocations/${widget.allocationId}/confirm-isolated-stay'
          : '/rooms/${widget.roomId}/${_room!['isolated'] == true ? 'lift-isolation' : 'isolate'}';
      await widget.api.post(
        path,
        {'reason': _reason.text.trim()},
        headers: {'Idempotency-Key': _key ??= bedCommandKey()},
      );
      if (mounted) {
        setState(() {
          if (widget.allocationId == null) {
            _room = {..._room!, 'isolated': _room!['isolated'] != true};
          }
          _done = true;
          _message = 'Đã ghi nhận.';
        });
      }
    } catch (e) {
      if (e is ApiProblem && e.status < 500) _key = null;
      if (mounted) setState(() => _error = bedProblem(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final label = widget.allocationId != null
        ? 'Xác nhận ở lại giường'
        : _room?['isolated'] == true
        ? 'Gỡ cách ly'
        : 'Đặt cách ly';
    return Scaffold(
      appBar: headerAppBar(
        context,
        title: widget.allocationId != null ? 'Xác nhận ở lại vùng cách ly' : 'Cách ly phòng',
      ),
      body: PageBody(
        child: ListView(
          children: [
            if (_room == null && _error == null) const LinearProgressIndicator(),
            if (_error != null) Semantics(liveRegion: true, child: Text(_error!)),
            if (_message != null) Semantics(liveRegion: true, child: Text(_message!)),
            if (_room != null) ...[
              Text(_room!['name'] as String? ?? 'Phòng ${widget.roomId}'),
              Text(_room!['isolated'] == true ? 'Đang cách ly' : 'Không cách ly'),
              const SizedBox(height: 16),
              if (_canWrite)
                TextField(
                  controller: _reason,
                  enabled: !_busy && !_done,
                  maxLength: 2000,
                  minLines: 2,
                  maxLines: 4,
                  decoration: const InputDecoration(labelText: 'Lý do (bắt buộc)'),
                  onChanged: (_) => setState(() => _key = null),
                ),
              if (_history.isNotEmpty) const Text('Lịch sử cách ly'),
              for (final row in _history)
                ListTile(
                  title: Text(row['action'] == 'ISOLATE' ? 'Đặt cách ly' : 'Gỡ cách ly'),
                  subtitle: Text(
                    '${formatDateTime(DateTime.parse(row['occurredAt'] as String))}\n${row['reason'] ?? ''}',
                  ),
                ),
            ],
          ],
        ),
      ),
      bottomNavigationBar: _canWrite
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: FilledButton(
                  onPressed: !_busy && !_done && _reason.text.trim().isNotEmpty ? _save : null,
                  child: Text(_busy ? 'Đang ghi nhận…' : label),
                ),
              ),
            )
          : null,
    );
  }
}
