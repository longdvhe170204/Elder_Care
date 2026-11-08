import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/notifications/notification_api.dart' show newIdempotencyKey;
import '../../common/widgets/status_pill.dart';
import 'swap_labels.dart';

/// Đổi ca và nghỉ đột xuất trên app nhân viên (015 US3, US4; SC-006 ≤ 2 phút). Mọi lệnh chỉ trực tuyến (FR-043, Q-01):
/// không vào hàng đợi ngoại tuyến, mất mạng thì báo. Lý do của người khác không hiển thị (FR-045; backend lọc).
class ShiftSwapsScreen extends StatelessWidget {
  const ShiftSwapsScreen({super.key, required this.api});

  final ApiClient api;

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Đổi ca, nghỉ đột xuất'),
          bottom: const TabBar(
            tabs: [
              Tab(text: 'Đổi ca'),
              Tab(text: 'Nghỉ đột xuất'),
            ],
          ),
        ),
        body: TabBarView(
          children: [
            _RequestList(api: api, kind: _Kind.swap),
            _RequestList(api: api, kind: _Kind.leave),
          ],
        ),
      ),
    );
  }
}

enum _Kind { swap, leave }

class _RequestList extends StatefulWidget {
  const _RequestList({required this.api, required this.kind});

  final ApiClient api;
  final _Kind kind;

  @override
  State<_RequestList> createState() => _RequestListState();
}

class _RequestListState extends State<_RequestList> {
  List<dynamic>? _rows;
  Object? _error;

  String get _path => widget.kind == _Kind.swap ? '/shift-swaps' : '/leave-requests';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final rows = await widget.api.getList(_path);
      if (mounted) {
        setState(() {
          _rows = rows;
          _error = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = e);
    }
  }

  Future<void> _create() async {
    final done = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => widget.kind == _Kind.swap ? SwapCreateSheet(api: widget.api) : LeaveCreateSheet(api: widget.api),
    );
    if (done == true) await _load();
  }

  Future<void> _command(Map<String, dynamic> row, String command) async {
    final table = widget.kind == _Kind.swap ? swapCommands : leaveCommands;
    final meta = table[command] ?? (command, false);
    final done = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => _CommandSheet(
        api: widget.api,
        path: '$_path/${row['id']}/$command',
        title: '${meta.$1} yêu cầu #${row['id']}',
        confirm: meta.$1,
        reasonRequired: meta.$2,
        withWarning: widget.kind == _Kind.swap && command == 'approve',
      ),
    );
    if (done == true) await _load();
  }

  @override
  Widget build(BuildContext context) {
    final rows = _rows;
    final theme = Theme.of(context);
    return Scaffold(
      floatingActionButton: FloatingActionButton.extended(
        key: const ValueKey('create-request'),
        onPressed: _create,
        label: Text(widget.kind == _Kind.swap ? 'Lập yêu cầu đổi ca' : 'Lập yêu cầu nghỉ'),
        icon: const Icon(Icons.add),
      ),
      body: _error != null
          ? Center(child: Text(backendMessage(_error!)))
          : rows == null
          ? const Center(child: CircularProgressIndicator())
          : rows.isEmpty
          ? const Center(child: Text('Chưa có yêu cầu.'))
          : ListView.separated(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
              itemCount: rows.length,
              separatorBuilder: (_, _) => const SizedBox(height: 12),
              itemBuilder: (context, i) {
                final row = rows[i] as Map<String, dynamic>;
                final status = row['status'] as String;
                final commands = (row['allowedCommands'] as List<dynamic>? ?? const []).cast<String>();
                final kinds = widget.kind == _Kind.swap ? swapKindLabels : leaveKindLabels;
                return Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(child: Text('#${row['id']} · ${kinds[row['kind']] ?? row['kind']}')),
                            StatusPill(label: swapStatusLabels[status] ?? status, tone: swapTone(status)),
                          ],
                        ),
                        if (row['reason'] != null) Text('Lý do: ${row['reason']}', style: theme.textTheme.bodySmall),
                        if (row['recipientDeclineReason'] != null)
                          Text(
                            'Lý do người nhận từ chối: ${row['recipientDeclineReason']}',
                            style: theme.textTheme.bodySmall,
                          ),
                        if (commands.isNotEmpty)
                          Wrap(
                            spacing: 8,
                            children: [
                              for (final command in commands)
                                OutlinedButton(
                                  key: ValueKey('command-${row['id']}-$command'),
                                  onPressed: () => _command(row, command),
                                  child: Text(
                                    (widget.kind == _Kind.swap ? swapCommands : leaveCommands)[command]?.$1 ?? command,
                                  ),
                                ),
                            ],
                          ),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }
}

/// Lệnh trên một yêu cầu: lý do khi cần, xác nhận cảnh báo giờ liên tục khi duyệt đổi ca (FR-021 (b)).
class _CommandSheet extends StatefulWidget {
  const _CommandSheet({
    required this.api,
    required this.path,
    required this.title,
    required this.confirm,
    required this.reasonRequired,
    required this.withWarning,
  });

  final ApiClient api;
  final String path;
  final String title;
  final String confirm;
  final bool reasonRequired;
  final bool withWarning;

  @override
  State<_CommandSheet> createState() => _CommandSheetState();
}

class _CommandSheetState extends State<_CommandSheet> {
  final _reason = TextEditingController();
  final _key = newIdempotencyKey();
  bool _busy = false;
  bool _confirmWarning = false;
  String? _message;
  String? _errorCode;

  Future<void> _submit() async {
    if (_busy) return;
    if (widget.reasonRequired && _reason.text.trim().isEmpty) {
      setState(() => _message = 'Nhập lý do trước khi xác nhận.');
      return;
    }
    setState(() {
      _busy = true;
      _message = null;
    });
    final body = <String, dynamic>{
      if (_reason.text.trim().isNotEmpty) 'reason': _reason.text.trim(),
      if (widget.withWarning) 'confirmWarning': _confirmWarning,
    };
    try {
      await widget.api.post(widget.path, body, headers: {'Idempotency-Key': _key});
      if (mounted) Navigator.of(context).pop(true);
    } on Object catch (error) {
      if (!mounted) return;
      setState(() {
        _busy = false;
        _message = backendMessage(error);
        _errorCode = error is ApiProblem ? error.errorCode : null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(widget.title, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            if (_message != null) Text(_message!, key: const ValueKey('command-error')),
            if (widget.withWarning && _errorCode == 'WARNING_CONFIRMATION_REQUIRED')
              CheckboxListTile(
                key: const ValueKey('confirm-warning'),
                value: _confirmWarning,
                onChanged: (v) => setState(() => _confirmWarning = v ?? false),
                title: const Text('Tôi đã xem cảnh báo giờ làm liên tục'),
              ),
            TextField(
              key: const ValueKey('command-reason'),
              controller: _reason,
              minLines: 2,
              maxLines: 4,
              decoration: InputDecoration(labelText: widget.reasonRequired ? 'Lý do' : 'Lý do (nếu có)'),
            ),
            const SizedBox(height: 16),
            FilledButton(onPressed: _busy ? null : _submit, child: Text(widget.confirm)),
          ],
        ),
      ),
    );
  }
}

/// Lập yêu cầu đổi ca: chọn ca của mình, người nhận, dạng, lý do (FR-019); chọn ca Y khi đổi hai chiều (FR-020).
class SwapCreateSheet extends StatefulWidget {
  const SwapCreateSheet({super.key, required this.api});

  final ApiClient api;

  @override
  State<SwapCreateSheet> createState() => _SwapCreateSheetState();
}

class _SwapCreateSheetState extends State<SwapCreateSheet> {
  final _recipient = TextEditingController();
  final _reason = TextEditingController();
  final _key = newIdempotencyKey();
  List<Map<String, dynamic>> _mine = const [];
  List<Map<String, dynamic>> _options = const [];
  String _kind = 'TAKE_OVER';
  int? _shiftX;
  int? _shiftY;
  bool _busy = false;
  String? _message;

  @override
  void initState() {
    super.initState();
    _loadMine();
  }

  Future<void> _loadMine() async {
    final now = DateTime.now();
    String day(DateTime d) =>
        '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
    try {
      final rows = await widget.api.getList('/me/shifts?from=${day(now)}&to=${day(now.add(const Duration(days: 60)))}');
      if (mounted) {
        setState(() => _mine = rows.cast<Map<String, dynamic>>().where((s) => s['status'] == 'PUBLISHED').toList());
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _message = backendMessage(e));
    }
  }

  Future<void> _loadOptions() async {
    final recipient = int.tryParse(_recipient.text.trim());
    if (_kind != 'MUTUAL' || _shiftX == null || recipient == null) return;
    try {
      final rows = await widget.api.getList('/shift-swaps/recipient-shifts?shiftXId=$_shiftX&recipientId=$recipient');
      if (mounted) setState(() => _options = rows.cast<Map<String, dynamic>>());
    } on Exception catch (e) {
      if (mounted) setState(() => _message = backendMessage(e));
    }
  }

  Future<void> _submit() async {
    final recipient = int.tryParse(_recipient.text.trim());
    if (_busy) return;
    if (_shiftX == null || recipient == null || _reason.text.trim().isEmpty || (_kind == 'MUTUAL' && _shiftY == null)) {
      setState(() => _message = 'Chọn ca, nhập mã người nhận, ca của người nhận (nếu đổi hai chiều) và lý do.');
      return;
    }
    setState(() {
      _busy = true;
      _message = null;
    });
    try {
      await widget.api.post(
        '/shift-swaps',
        {
          'kind': _kind,
          'shiftXId': _shiftX,
          'recipientId': recipient,
          if (_kind == 'MUTUAL') 'shiftYId': _shiftY,
          'reason': _reason.text.trim(),
        },
        headers: {'Idempotency-Key': _key},
      );
      if (mounted) Navigator.of(context).pop(true);
    } on Object catch (error) {
      if (mounted) {
        setState(() {
          _busy = false;
          _message = backendMessage(error);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Lập yêu cầu đổi ca', style: Theme.of(context).textTheme.titleMedium),
            if (_message != null) Text(_message!, key: const ValueKey('create-error')),
            SegmentedButton<String>(
              key: const ValueKey('swap-kind'),
              segments: const [
                ButtonSegment(value: 'TAKE_OVER', label: Text('Nhận thay')),
                ButtonSegment(value: 'MUTUAL', label: Text('Đổi hai chiều')),
              ],
              selected: {_kind},
              onSelectionChanged: (s) => setState(() {
                _kind = s.first;
                _shiftY = null;
              }),
            ),
            DropdownButtonFormField<int>(
              key: const ValueKey('swap-shift-x'),
              initialValue: _shiftX,
              decoration: const InputDecoration(labelText: 'Ca của tôi'),
              items: [
                for (final s in _mine)
                  DropdownMenuItem(
                    value: s['id'] as int,
                    child: Text(formatDateTime(DateTime.parse(s['startsAt'] as String))),
                  ),
              ],
              onChanged: (v) {
                setState(() => _shiftX = v);
                _loadOptions();
              },
            ),
            TextField(
              key: const ValueKey('swap-recipient'),
              controller: _recipient,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Mã nhân viên người nhận'),
              onChanged: (_) => _loadOptions(),
            ),
            if (_kind == 'MUTUAL')
              DropdownButtonFormField<int>(
                key: const ValueKey('swap-shift-y'),
                initialValue: _shiftY,
                decoration: const InputDecoration(labelText: 'Ca của người nhận'),
                items: [
                  for (final s in _options)
                    DropdownMenuItem(
                      value: s['shiftId'] as int,
                      child: Text(formatDateTime(DateTime.parse(s['startsAt'] as String))),
                    ),
                ],
                onChanged: (v) => setState(() => _shiftY = v),
              ),
            TextField(
              key: const ValueKey('swap-reason'),
              controller: _reason,
              minLines: 2,
              maxLines: 4,
              decoration: const InputDecoration(labelText: 'Lý do'),
            ),
            const SizedBox(height: 16),
            FilledButton(onPressed: _busy ? null : _submit, child: const Text('Gửi yêu cầu')),
          ],
        ),
      ),
    );
  }
}

/// Lập yêu cầu nghỉ theo ca (FR-026): chọn một hoặc nhiều ca Đã công bố chưa bắt đầu của mình, lý do.
class LeaveCreateSheet extends StatefulWidget {
  const LeaveCreateSheet({super.key, required this.api});

  final ApiClient api;

  @override
  State<LeaveCreateSheet> createState() => _LeaveCreateSheetState();
}

class _LeaveCreateSheetState extends State<LeaveCreateSheet> {
  final _reason = TextEditingController();
  final _key = newIdempotencyKey();
  List<Map<String, dynamic>> _mine = const [];
  final _chosen = <int>{};
  bool _busy = false;
  String? _message;

  @override
  void initState() {
    super.initState();
    _loadMine();
  }

  Future<void> _loadMine() async {
    final now = DateTime.now();
    String day(DateTime d) =>
        '${d.year.toString().padLeft(4, '0')}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
    try {
      final rows = await widget.api.getList('/me/shifts?from=${day(now)}&to=${day(now.add(const Duration(days: 60)))}');
      if (mounted) {
        setState(() => _mine = rows.cast<Map<String, dynamic>>().where((s) => s['status'] == 'PUBLISHED').toList());
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _message = backendMessage(e));
    }
  }

  Future<void> _submit() async {
    if (_busy) return;
    if (_chosen.isEmpty || _reason.text.trim().isEmpty) {
      setState(() => _message = 'Chọn ít nhất một ca và nhập lý do.');
      return;
    }
    setState(() {
      _busy = true;
      _message = null;
    });
    try {
      await widget.api.post(
        '/leave-requests',
        {'kind': 'BY_SHIFT', 'shiftIds': _chosen.toList()..sort(), 'reason': _reason.text.trim()},
        headers: {'Idempotency-Key': _key},
      );
      if (mounted) Navigator.of(context).pop(true);
    } on Object catch (error) {
      if (mounted) {
        setState(() {
          _busy = false;
          _message = backendMessage(error);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Lập yêu cầu nghỉ', style: Theme.of(context).textTheme.titleMedium),
            if (_message != null) Text(_message!, key: const ValueKey('leave-error')),
            for (final s in _mine)
              CheckboxListTile(
                key: ValueKey('leave-shift-${s['id']}'),
                value: _chosen.contains(s['id'] as int),
                onChanged: (v) =>
                    setState(() => v == true ? _chosen.add(s['id'] as int) : _chosen.remove(s['id'] as int)),
                title: Text(formatDateTime(DateTime.parse(s['startsAt'] as String))),
              ),
            TextField(
              key: const ValueKey('leave-reason'),
              controller: _reason,
              minLines: 2,
              maxLines: 4,
              decoration: const InputDecoration(labelText: 'Lý do'),
            ),
            const SizedBox(height: 16),
            FilledButton(onPressed: _busy ? null : _submit, child: const Text('Gửi yêu cầu')),
          ],
        ),
      ),
    );
  }
}
