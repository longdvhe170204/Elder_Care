import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';

const _levelNames = {'CRITICAL': 'Khẩn cấp', 'MEDIUM': 'Trung bình', 'LOW': 'Nhẹ'};
const _statusNames = {
  'NEW': 'Mới',
  'IN_PROGRESS': 'Đang xử lý',
  'MONITORING': 'Theo dõi',
  'CLOSED': 'Đã đóng',
  'VOIDED': 'Đã hủy',
};
const _commandNames = {
  'receive': 'Tiếp nhận',
  'move-to-monitoring': 'Chuyển sang theo dõi',
  'reopen': 'Mở lại',
  'close': 'Đóng sự cố',
};
// Lệnh app làm được; giao người, đổi mức, diễn biến, chuyển viện làm trên web.
const _handled = ['receive', 'move-to-monitoring', 'reopen', 'close'];

String _newKey() {
  final random = Random.secure();
  return List.generate(16, (_) => random.nextInt(256).toRadixString(16).padLeft(2, '0')).join();
}

/// Sự cố trên app nhân viên (007 T037, T042; US4, US5; FR-045, FR-047, FR-048). Liệt kê sự cố đang xử lý, mở một sự
/// cố xem nội dung, người xử lý, các lệnh được phép (tiếp nhận, theo dõi, mở lại, đóng kèm kết quả) và thẻ thông tin khẩn
/// cấp (bản chụp lúc kích hoạt; người không có quyền xem sức khỏe chỉ thấy nhận dạng). Thẻ cần trực tuyến: mất kết nối thì
/// báo lỗi, không dùng bản cũ.
class IncidentsScreen extends StatefulWidget {
  const IncidentsScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<IncidentsScreen> createState() => _IncidentsScreenState();
}

class _IncidentsScreenState extends State<IncidentsScreen> {
  List<Map<String, dynamic>>? _incidents;
  Map<String, dynamic>? _selected;
  String? _error, _notice;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final rows = await widget.api.getList('/incidents?status=NEW&status=IN_PROGRESS&status=MONITORING');
      if (mounted) {
        setState(() {
          _incidents = rows.cast<Map<String, dynamic>>();
          _error = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Future<void> _open(Map<String, dynamic> incident) async {
    try {
      final detail = await widget.api.get('/incidents/${incident['id']}');
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
      return _IncidentDetail(
        api: widget.api,
        incident: selected,
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
          Text('Sự cố đang xử lý', style: text.titleMedium),
          if (_notice != null)
            Semantics(
              liveRegion: true,
              child: Padding(padding: const EdgeInsets.only(top: 8), child: Text(_notice!, style: text.bodyMedium)),
            ),
          const SizedBox(height: 8),
          if (_error != null) ...[
            Text(_error!, style: text.bodyMedium),
            OutlinedButton(onPressed: _load, child: const Text('Tải lại')),
          ] else if (_incidents == null)
            const Center(child: CircularProgressIndicator())
          else if (_incidents!.isEmpty)
            Text('Không có sự cố đang xử lý.', style: text.bodyMedium)
          else
            for (final i in _incidents!)
              Card(
                child: ListTile(
                  minTileHeight: 64,
                  title: Text('${(i['content'] as Map?)?['description'] ?? 'Sự cố'}', maxLines: 2),
                  subtitle: Text('${_levelNames[i['level']] ?? i['level']} · ${_statusNames[i['status']] ?? i['status']}'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => _open(i),
                ),
              ),
        ],
      ),
    );
  }
}

class _IncidentDetail extends StatefulWidget {
  const _IncidentDetail({required this.api, required this.incident, required this.onBack, required this.onChanged});

  final ApiClient api;
  final Map<String, dynamic> incident;
  final VoidCallback onBack;
  final void Function(String message) onChanged;

  @override
  State<_IncidentDetail> createState() => _IncidentDetailState();
}

class _IncidentDetailState extends State<_IncidentDetail> {
  final _resolution = TextEditingController();
  final _keys = <String, String>{};
  Map<String, dynamic>? _card;
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
        '/incidents/${widget.incident['id']}/$command',
        body,
        headers: {'Idempotency-Key': _keys.putIfAbsent(command, _newKey)},
      );
      widget.onChanged('${_commandNames[command]}: đã thực hiện.');
    } on ApiUnreachable {
      if (mounted) setState(() => _error = 'Mất kết nối, lệnh chưa được gửi. Thử lại khi có mạng.');
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _loadCard() async {
    try {
      final card = await widget.api.get('/incidents/${widget.incident['id']}/emergency-card');
      if (mounted) {
        setState(() {
          _card = card;
          _error = null;
        });
      }
    } on ApiUnreachable {
      if (mounted) setState(() => _error = 'Mất kết nối, chưa mở được thẻ khẩn cấp.');
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final i = widget.incident;
    final commands = ((i['allowedCommands'] as List?) ?? const []).cast<String>().where(_handled.contains).toList();
    final handlers = ((i['handlers'] as List?) ?? const []).join(', ');
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Align(
          alignment: Alignment.centerLeft,
          child: TextButton(onPressed: widget.onBack, child: const Text('Quay lại danh sách')),
        ),
        Text('${(i['content'] as Map?)?['description'] ?? 'Sự cố'}', style: text.titleMedium),
        const SizedBox(height: 4),
        Text('Mức: ${_levelNames[i['level']] ?? i['level']}', style: text.bodyMedium),
        Text('Trạng thái: ${_statusNames[i['status']] ?? i['status']}', style: text.bodyMedium),
        if (i['assignee'] != null) Text('Người phụ trách: ${i['assignee']}', style: text.bodyMedium),
        if (handlers.isNotEmpty) Text('Người xử lý: $handlers', style: text.bodyMedium),
        if (i['wasEmergency'] == true) ...[
          const SizedBox(height: 12),
          if (_card == null)
            OutlinedButton(onPressed: _loadCard, child: const Text('Xem thẻ thông tin khẩn cấp'))
          else
            _EmergencyCard(card: _card!),
        ],
        const SizedBox(height: 12),
        if (commands.isEmpty) Text('Bạn không có lệnh nào với sự cố này.', style: text.bodyMedium),
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

/// Thẻ thông tin khẩn cấp (EmergencyCard): nhận dạng, nguyện vọng cuối đời, dị ứng, thuốc, bệnh mạn tính, liên hệ chính.
class _EmergencyCard extends StatelessWidget {
  const _EmergencyCard({required this.card});

  final Map<String, dynamic> card;

  List<String> _names(String key) =>
      ((card[key] as List?) ?? const []).map((e) => '${(e as Map)['name'] ?? e}').toList(growable: false);

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final id = (card['identification'] as Map?) ?? const {};
    final wish = card['endOfLifeWish'] as Map?;
    final contact = card['primaryContact'] as Map?;
    final unavailable = ((card['unavailable'] as List?) ?? const []).cast<String>();
    Widget section(String title, List<String> lines) => lines.isEmpty
        ? const SizedBox.shrink()
        : Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [Text(title, style: text.titleSmall), for (final l in lines) Text('• $l')],
            ),
          );
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Thẻ thông tin khẩn cấp', style: text.titleMedium),
            const SizedBox(height: 4),
            Text('${id['fullName'] ?? ''}${id['roomBed'] != null ? ' · ${id['roomBed']}' : ''}'),
            if (id['dateOfBirth'] != null) Text('Ngày sinh: ${id['dateOfBirth']}'),
            if (card['criticalFlag'] == true) Text('Đang có dấu nguy kịch', style: text.titleSmall),
            if (wish != null)
              Text(wish['notRecorded'] == true ? 'Chưa ghi nguyện vọng cuối đời' : 'Đã ghi nguyện vọng cuối đời'),
            section('Dị ứng', _names('allergies')),
            section('Thuốc đang dùng', _names('currentMedications')),
            section('Bệnh mạn tính', _names('chronicConditions')),
            if (card['careNotes'] != null && '${card['careNotes']}'.isNotEmpty)
              Padding(padding: const EdgeInsets.only(top: 8), child: Text('Ghi chú: ${card['careNotes']}')),
            if (contact != null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text('Liên hệ chính: ${contact['fullName']} ${contact['phoneNumber'] ?? ''}'),
              ),
            if (unavailable.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Text('Không xem được: ${unavailable.join(', ')}'),
              ),
          ],
        ),
      ),
    );
  }
}
