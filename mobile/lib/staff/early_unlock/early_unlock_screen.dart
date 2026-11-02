import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';

/// Mở khóa sớm tài khoản nhân viên đang Khóa tạm (002 T046; FR-019c): Trưởng tầng, Người phụ trách ca thấy người cùng
/// tầng và ca (backend lọc, `GET /early-unlock-candidates`); phải gặp trực tiếp người đó và nhập lý do.
class EarlyUnlockScreen extends StatefulWidget {
  const EarlyUnlockScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<EarlyUnlockScreen> createState() => _EarlyUnlockScreenState();
}

class _EarlyUnlockScreenState extends State<EarlyUnlockScreen> {
  List<Map<String, dynamic>>? _items;
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final page = await widget.api.get('/early-unlock-candidates');
      final items = (page?['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
      if (mounted) {
        setState(() {
          _items = items;
          _loadError = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  Future<void> _open(Map<String, dynamic> account) async {
    final message = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => _UnlockSheet(api: widget.api, account: account),
    );
    if (message == null || !mounted) return;
    setState(() => _items = _items!.where((a) => a['accountId'] != account['accountId']).toList());
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    if (_loadError != null) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(backendMessage(_loadError!), style: text.bodyMedium),
                  const SizedBox(height: 12),
                  OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
                ],
              ),
            ),
          ),
        ],
      );
    }
    final items = _items;
    if (items == null) return const Center(child: CircularProgressIndicator());
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (items.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text('Không có tài khoản nào đang khóa tạm trong tầng và ca của bạn.', style: text.bodyMedium),
            ),
          ),
        for (final a in items) ...[
          Card(
            clipBehavior: Clip.antiAlias,
            child: InkWell(
              onTap: () => _open(a),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(_name(a), style: text.titleSmall),
                    Text(
                      'Khóa tạm tới ${formatTime(DateTime.parse(a['lockedUntil'] as String))}',
                      style: text.bodySmall,
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
        ],
      ],
    );
  }
}

String _name(Map<String, dynamic> a) => (a['fullName'] as String?) ?? 'Tài khoản #${a['accountId']}';

class _UnlockSheet extends StatefulWidget {
  const _UnlockSheet({required this.api, required this.account});

  final ApiClient api;
  final Map<String, dynamic> account;

  @override
  State<_UnlockSheet> createState() => _UnlockSheetState();
}

class _UnlockSheetState extends State<_UnlockSheet> {
  final _reason = TextEditingController();
  bool _metInPerson = false;
  bool _busy = false;
  String? _reasonError;
  String? _error;

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _unlock() async {
    final reason = _reason.text.trim();
    setState(() {
      _error = _metInPerson ? null : 'Cần gặp trực tiếp người này trước khi mở khóa.';
      _reasonError = reason.isEmpty ? 'Nhập lý do' : null;
    });
    if (!_metInPerson || reason.isEmpty) return;
    setState(() => _busy = true);
    final a = widget.account;
    try {
      await widget.api.post(
        '/accounts/${a['accountId']}/early-unlock',
        {'reason': reason, 'verifiedInPerson': true},
        headers: {'If-Match': '"${a['version']}"'},
      );
      if (mounted) Navigator.of(context).pop('Đã mở khóa cho ${_name(a)}.');
    } on ApiProblem catch (p) {
      if (mounted) setState(() => p.status == 422 ? _reasonError = backendMessage(p) : _error = backendMessage(p));
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 0, 16, 16 + MediaQuery.viewInsetsOf(context).bottom),
      child: ListView(
        shrinkWrap: true,
        children: [
          Text('Mở khóa sớm cho ${_name(widget.account)}', style: text.titleLarge),
          const SizedBox(height: 8),
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            value: _metInPerson,
            onChanged: (v) => setState(() => _metInPerson = v ?? false),
            title: const Text('Tôi đã gặp trực tiếp người này'),
            controlAffinity: ListTileControlAffinity.leading,
          ),
          TextField(
            controller: _reason,
            maxLength: 2000,
            maxLines: 3,
            decoration: InputDecoration(labelText: 'Lý do', errorText: _reasonError),
          ),
          if (_error != null) ...[
            Text(_error!, style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error)),
            const SizedBox(height: 12),
          ],
          FilledButton(onPressed: _busy ? null : _unlock, child: const Text('Mở khóa sớm')),
        ],
      ),
    );
  }
}
