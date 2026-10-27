import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/offline/record_types.dart';
import '../../common/widgets/status_pill.dart';

/// Lý do chờ xem lại (FR-041c), cùng chữ với web.
String pendingReasonLabel(String? code) => switch (code) {
  'LATER_THAN_SYNC' => 'Muộn hơn lúc đồng bộ',
  'EARLIER_THAN_LAST_CONNECTION' => 'Sớm hơn lần kết nối cuối',
  'EXCEEDS_CFG_M15_08' => 'Quá CFG-M15-08 kể từ lần kết nối cuối',
  _ => code ?? '',
};

/// Bản ghi chờ xem lại trên app nhân viên (002 T060; FR-041c; bố cục duyệt 2026-10-27, cùng nội dung màn web): thẻ
/// từng bản ghi người dùng được quyết định (backend lọc), bấm thẻ mở khung dưới để chấp nhận hoặc từ chối có lý do.
class PendingReviewScreen extends StatefulWidget {
  const PendingReviewScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<PendingReviewScreen> createState() => _PendingReviewScreenState();
}

class _PendingReviewScreenState extends State<PendingReviewScreen> {
  List<Map<String, dynamic>>? _items;
  String? _nextCursor;
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load([String? cursor]) async {
    try {
      final page = await widget.api.get('/pending-review-records${cursor == null ? '' : '?cursor=$cursor'}');
      final items = (page?['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
      if (!mounted) return;
      setState(() {
        _items = cursor == null ? items : [...?_items, ...items];
        _nextCursor = page?['nextCursor'] as String?;
        _loadError = null;
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  Future<void> _open(Map<String, dynamic> record) async {
    final message = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => _ReviewSheet(api: widget.api, record: record),
    );
    if (message == null || !mounted) return;
    setState(() => _items = _items!.where((r) => r['id'] != record['id']).toList());
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    if (_loadError != null) {
      return _LoadError(message: backendMessage(_loadError!), onRetry: _load);
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
              child: Text('Không có bản ghi nào chờ bạn xem lại.', style: text.bodyMedium),
            ),
          ),
        for (final r in items) ...[
          Card(
            clipBehavior: Clip.antiAlias,
            child: InkWell(
              onTap: () => _open(r),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(child: Text(recordTypeLabel(r['recordType'] as String), style: text.titleSmall)),
                        const StatusPill(label: 'Chờ xem lại', tone: StatusTone.attention),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text('${_recorder(r)}, ${_floor(r)}', style: text.bodyMedium),
                    Text(
                      'Giờ trên thiết bị ${formatDateTime(DateTime.parse(r['deviceTime'] as String))}',
                      style: text.bodySmall,
                    ),
                    Text(pendingReasonLabel(r['pendingReason'] as String?), style: text.bodySmall),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),
        ],
        if (_nextCursor != null) OutlinedButton(onPressed: () => _load(_nextCursor), child: const Text('Xem thêm')),
      ],
    );
  }
}

String _recorder(Map<String, dynamic> r) => (r['recorderName'] as String?) ?? 'Tài khoản #${r['recorderId']}';

String _floor(Map<String, dynamic> r) =>
    r['floorAreaId'] == null ? 'Không gắn tầng' : (r['floorAreaName'] as String?) ?? 'Tầng #${r['floorAreaId']}';

class _ReviewSheet extends StatefulWidget {
  const _ReviewSheet({required this.api, required this.record});

  final ApiClient api;
  final Map<String, dynamic> record;

  @override
  State<_ReviewSheet> createState() => _ReviewSheetState();
}

class _ReviewSheetState extends State<_ReviewSheet> {
  final _reason = TextEditingController();
  bool _rejecting = false;
  bool _busy = false;
  String? _reasonError;
  String? _error;

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _decide(String command, Map<String, dynamic> body, String done) async {
    setState(() {
      _busy = true;
      _error = null;
      _reasonError = null;
    });
    final r = widget.record;
    try {
      await widget.api.post(
        '/pending-review-records/${r['id']}/$command',
        body,
        headers: {'If-Match': '"${r['version']}"'},
      );
      if (mounted) Navigator.of(context).pop(done);
    } on ApiProblem catch (p) {
      if (p.status == 409 && p.errorCode == 'ALREADY_PROCESSED') {
        if (mounted) Navigator.of(context).pop('Bản ghi đã được người khác quyết định.');
        return;
      }
      if (mounted) setState(() => p.status == 422 ? _reasonError = backendMessage(p) : _error = backendMessage(p));
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _reject() {
    if (_reason.text.trim().isEmpty) {
      setState(() => _reasonError = 'Nhập lý do từ chối');
      return;
    }
    _decide('reject', {'reason': _reason.text.trim()}, 'Đã từ chối bản ghi. Bản ghi được lưu nhưng không có hiệu lực.');
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final r = widget.record;
    final content = (r['content'] as Map<String, dynamic>?) ?? const {};
    DateTime at(String key) => DateTime.parse(r[key] as String);
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 0, 16, 16 + MediaQuery.viewInsetsOf(context).bottom),
      child: ListView(
        shrinkWrap: true,
        children: [
          Text(recordTypeLabel(r['recordType'] as String), style: text.titleLarge),
          Text('${_recorder(r)}, ${_floor(r)}', style: text.bodyMedium),
          const SizedBox(height: 8),
          const Align(
            alignment: Alignment.centerLeft,
            child: StatusPill(label: 'Chờ xem lại', tone: StatusTone.attention),
          ),
          const SizedBox(height: 12),
          Text('Giờ trên thiết bị: ${formatDateTime(at('deviceTime'))}', style: text.bodyMedium),
          Text('Kết nối cuối: ${formatDateTime(at('lastConnectedAt'))}', style: text.bodyMedium),
          Text('Đồng bộ lúc: ${formatDateTime(at('syncedAt'))}', style: text.bodyMedium),
          Text('Lý do chờ: ${pendingReasonLabel(r['pendingReason'] as String?)}', style: text.bodyMedium),
          if (content.isNotEmpty) ...[
            const SizedBox(height: 12),
            Text('Nội dung gốc từ thiết bị', style: text.labelMedium),
            for (final e in content.entries) Text('${e.key}: ${e.value}', style: text.bodyMedium),
          ],
          if (_error != null) ...[
            const SizedBox(height: 12),
            Text(_error!, style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error)),
          ],
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _busy
                ? null
                : () => _decide('accept', const {}, 'Đã chấp nhận bản ghi. Bản ghi có hiệu lực từ lúc chấp nhận.'),
            child: const Text('Chấp nhận bản ghi'),
          ),
          const SizedBox(height: 12),
          if (!_rejecting)
            OutlinedButton(
              onPressed: _busy ? null : () => setState(() => _rejecting = true),
              child: const Text('Từ chối'),
            ),
          if (_rejecting) ...[
            TextField(
              controller: _reason,
              maxLength: 2000,
              maxLines: 3,
              decoration: InputDecoration(labelText: 'Lý do từ chối', errorText: _reasonError),
            ),
            OutlinedButton(onPressed: _busy ? null : _reject, child: const Text('Xác nhận từ chối')),
          ],
        ],
      ),
    );
  }
}

class _LoadError extends StatelessWidget {
  const _LoadError({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => ListView(
    padding: const EdgeInsets.all(16),
    children: [
      Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(message, style: Theme.of(context).textTheme.bodyMedium),
              const SizedBox(height: 12),
              OutlinedButton(onPressed: onRetry, child: const Text('Thử lại')),
            ],
          ),
        ),
      ),
    ],
  );
}
