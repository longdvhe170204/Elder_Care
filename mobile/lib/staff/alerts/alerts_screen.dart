import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';

const _levelNames = {'CRITICAL': 'Khẩn cấp', 'MEDIUM': 'Trung bình', 'LOW': 'Nhẹ'};
const _statusNames = {
  'NEW': 'Mới',
  'ESCALATED': 'Leo thang',
  'RECEIVED': 'Đã tiếp nhận',
  'IN_PROGRESS': 'Đang xử lý',
  'CLOSED': 'Đã đóng',
  'CONVERTED_TO_INCIDENT': 'Chuyển sự cố',
};
const _commandNames = {
  'receive': 'Tiếp nhận',
  'start-handling': 'Bắt đầu xử lý',
  'close': 'Đóng cảnh báo',
  'convert-to-incident': 'Chuyển sự cố',
  'activate-emergency': 'Kích hoạt khẩn cấp',
};
// Lệnh app làm được; lệnh khác (giao người, đổi mức) làm trên web.
const _handled = ['receive', 'start-handling', 'convert-to-incident', 'activate-emergency', 'close'];

String _newKey() {
  final random = Random.secure();
  return List.generate(16, (_) => random.nextInt(256).toRadixString(16).padLeft(2, '0')).join();
}

/// Cảnh báo trên app nhân viên (007 T031; US3; FR-027, FR-029, FR-033, FR-036). Liệt kê cảnh báo đang mở theo phạm vi,
/// mở một cảnh báo xem nguồn và các lệnh được phép (`allowedCommands`): tiếp nhận, bắt đầu xử lý, chuyển sự cố, kích
/// hoạt khẩn cấp, đóng (kết quả bắt buộc). Mọi lệnh là trực tuyến, gửi kèm `Idempotency-Key`; kích hoạt khẩn cấp bị chặn
/// khi mất kết nối (hiện lỗi, không xếp hàng ngoại tuyến).
class AlertsScreen extends StatefulWidget {
  const AlertsScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<AlertsScreen> createState() => _AlertsScreenState();
}

class _AlertsScreenState extends State<AlertsScreen> {
  List<Map<String, dynamic>>? _alerts;
  Map<String, dynamic>? _selected;
  String? _error, _notice;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final rows = await widget.api.getList('/alerts?status=NEW&status=ESCALATED&status=RECEIVED&status=IN_PROGRESS');
      if (mounted) {
        setState(() {
          _alerts = rows.cast<Map<String, dynamic>>();
          _error = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Future<void> _open(Map<String, dynamic> alert) async {
    try {
      final detail = await widget.api.get('/alerts/${alert['id']}');
      if (mounted) setState(() => _selected = detail);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final selected = _selected;
    if (selected != null) {
      return _AlertDetail(
        api: widget.api,
        alert: selected,
        onBack: () {
          setState(() => _selected = null);
          _load();
        },
        onChanged: (message) {
          setState(() {
            _selected = null;
            _notice = message;
          });
          _load();
        },
      );
    }
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Cảnh báo đang mở', style: text.titleMedium),
          if (_notice != null)
            Semantics(
              liveRegion: true,
              child: Padding(padding: const EdgeInsets.only(top: 8), child: Text(_notice!, style: text.bodyMedium)),
            ),
          const SizedBox(height: 8),
          if (_error != null) ...[
            Text(_error!, style: text.bodyMedium),
            OutlinedButton(onPressed: _load, child: const Text('Tải lại')),
          ] else if (_alerts == null)
            const Center(child: CircularProgressIndicator())
          else if (_alerts!.isEmpty)
            Text('Không có cảnh báo đang mở.', style: text.bodyMedium)
          else
            for (final a in _alerts!)
              Card(
                child: ListTile(
                  minTileHeight: 64,
                  title: Text('${a['type'] ?? 'Cảnh báo'}'),
                  subtitle: Text(
                    '${_levelNames[a['level']] ?? a['level']} · ${_statusNames[a['status']] ?? a['status']}'
                    '${a['maxEscalationReached'] == true ? ' · Đã leo thang tối đa' : ''}',
                  ),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _open(a),
                ),
              ),
        ],
      ),
    );
  }
}

class _AlertDetail extends StatefulWidget {
  const _AlertDetail({required this.api, required this.alert, required this.onBack, required this.onChanged});

  final ApiClient api;
  final Map<String, dynamic> alert;
  final VoidCallback onBack;
  final void Function(String message) onChanged;

  @override
  State<_AlertDetail> createState() => _AlertDetailState();
}

class _AlertDetailState extends State<_AlertDetail> {
  final _resolution = TextEditingController();
  final _keys = <String, String>{};
  String? _error;
  bool _busy = false, _closing = false;

  @override
  void dispose() {
    _resolution.dispose();
    super.dispose();
  }

  Future<void> _run(String command, [Map<String, dynamic> body = const {}]) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.post(
        '/alerts/${widget.alert['id']}/$command',
        body,
        headers: {'Idempotency-Key': _keys.putIfAbsent(command, _newKey)},
      );
      widget.onChanged('${_commandNames[command]}: đã thực hiện.');
    } on ApiUnreachable {
      if (mounted) {
        setState(() => _error = 'Mất kết nối, lệnh chưa được gửi. Thử lại khi có mạng.');
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final a = widget.alert;
    final commands = ((a['allowedCommands'] as List?) ?? const []).cast<String>().where(_handled.contains).toList();
    final sources = ((a['sources'] as List?) ?? const []).cast<Map<String, dynamic>>();
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton(onPressed: widget.onBack, child: const Text('Quay lại danh sách')),
        ),
        Text('${a['type'] ?? 'Cảnh báo'}', style: text.titleMedium),
        const SizedBox(height: 4),
        Text('Mức: ${_levelNames[a['level']] ?? a['level']}', style: text.bodyMedium),
        Text('Trạng thái: ${_statusNames[a['status']] ?? a['status']}', style: text.bodyMedium),
        if (a['assignee'] != null) Text('Người phụ trách: ${a['assignee']}', style: text.bodyMedium),
        if (a['acknowledgementDeadline'] != null)
          Text('Hạn tiếp nhận: ${a['acknowledgementDeadline']}', style: text.bodyMedium),
        if (a['pressedBy'] != null) Text('Người bấm: ${a['pressedBy']}', style: text.bodyMedium),
        const SizedBox(height: 8),
        if (sources.isNotEmpty) Text('Nguồn', style: text.titleSmall),
        for (final s in sources) Text('• ${s['sourceType']} (${_levelNames[s['level']] ?? s['level']})'),
        const SizedBox(height: 12),
        if (commands.isEmpty) Text('Bạn không có lệnh nào với cảnh báo này.', style: text.bodyMedium),
        for (final c in commands.where((c) => c != 'close'))
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: FilledButton(onPressed: _busy ? null : () => _run(c), child: Text(_commandNames[c]!)),
          ),
        if (commands.contains('close'))
          if (!_closing)
            OutlinedButton(onPressed: () => setState(() => _closing = true), child: Text(_commandNames['close']!))
          else ...[
            TextField(
              controller: _resolution,
              maxLines: 3,
              decoration: const InputDecoration(labelText: 'Kết quả xử lý (bắt buộc)'),
            ),
            const SizedBox(height: 8),
            FilledButton(
              onPressed: _busy
                  ? null
                  : () {
                      final value = _resolution.text.trim();
                      if (value.isEmpty) {
                        setState(() => _error = 'Nhập kết quả xử lý.');
                        return;
                      }
                      _run('close', {'resolution': value});
                    },
              child: const Text('Xác nhận đóng'),
            ),
          ],
        if (_error != null)
          Semantics(
            liveRegion: true,
            child: Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Text(_error!, style: text.bodyMedium),
            ),
          ),
      ],
    );
  }
}
