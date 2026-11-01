import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/status_pill.dart';
import 'approval_labels.dart';
import 'approval_request_screen.dart';

const _inboxes = [('AWAITING_MY_APPROVAL', 'Chờ tôi duyệt'), ('MINE', 'Của tôi'), ('ALL_IN_SCOPE', 'Trong phạm vi')];

/// Hộp duyệt trên app nhân viên (000 T098; US5; FR-031 → FR-040), cùng ba hộp, thứ tự nội dung và chữ với màn web (T097;
/// DESIGN.md mục 13 điều 7). Chỉ trực tuyến (R-13): lệnh không vào hàng đợi ngoại tuyến, mất mạng thì báo. Danh sách
/// chỉ gồm yêu cầu trong phạm vi người xem (backend lọc).
class ApprovalInboxScreen extends StatefulWidget {
  const ApprovalInboxScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<ApprovalInboxScreen> createState() => _ApprovalInboxScreenState();
}

class _ApprovalInboxScreenState extends State<ApprovalInboxScreen> {
  String _inbox = _inboxes.first.$1;
  List<Map<String, dynamic>>? _items;
  String? _nextCursor;
  Object? _loadError;
  String? _status;
  String? _type;
  String? _awaitingCount;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load([String? cursor]) async {
    final query = Uri(queryParameters: {'inbox': _inbox, 'status': ?_status, 'type': ?_type, 'cursor': ?cursor}).query;
    try {
      final page = await widget.api.get('/approval-requests?$query');
      final items = (page?['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
      if (!mounted) return;
      setState(() {
        _items = cursor == null ? items : [...?_items, ...items];
        _nextCursor = page?['nextCursor'] as String?;
        _loadError = null;
        if (cursor == null && _inbox == _inboxes.first.$1 && _status == null && _type == null) {
          _awaitingCount = '${items.length}${_nextCursor != null ? '+' : ''}';
        }
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  void _reload(VoidCallback change) {
    setState(() {
      change();
      _items = null;
      _loadError = null;
    });
    _load();
  }

  void _open(Map<String, dynamic> request) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => ApprovalRequestScreen(
          api: widget.api,
          id: request['id'] as int,
          displayCode: request['displayCode'] as String,
          onChanged: _replaceRow,
        ),
      ),
    );
  }

  void _replaceRow(Map<String, dynamic> changed) {
    if (!mounted) return;
    setState(() => _items = _items?.map((r) => r['id'] == changed['id'] ? changed : r).toList());
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final items = _items;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 12),
          child: Row(
            children: [
              for (final (value, label) in _inboxes)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(
                      value == _inboxes.first.$1 && _awaitingCount != null ? '$label ($_awaitingCount)' : label,
                    ),
                    selected: _inbox == value,
                    onSelected: (_) => _reload(() => _inbox = value),
                  ),
                ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: Row(
            children: [
              Expanded(
                child: _Filter(
                  key: const ValueKey('approval-status'),
                  label: 'Trạng thái',
                  options: statusLabels,
                  value: _status,
                  onChanged: (value) => _reload(() => _status = value),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _Filter(
                  key: const ValueKey('approval-type'),
                  label: 'Loại yêu cầu',
                  options: typeLabels,
                  value: _type,
                  onChanged: (value) => _reload(() => _type = value),
                ),
              ),
            ],
          ),
        ),
        Expanded(
          child: _loadError != null
              ? _LoadError(message: backendMessage(_loadError!), onRetry: _load)
              : items == null
              ? const Center(child: CircularProgressIndicator())
              : ListView(
                  children: [
                    if (items.isEmpty)
                      Padding(
                        padding: const EdgeInsets.all(16),
                        child: Card(
                          child: Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('Không có yêu cầu nào', style: text.titleSmall),
                                const SizedBox(height: 4),
                                Text('Đổi hộp hoặc bộ lọc để xem yêu cầu khác.', style: text.bodyMedium),
                              ],
                            ),
                          ),
                        ),
                      ),
                    for (final r in items) ...[
                      _RequestRow(request: r, onTap: () => _open(r)),
                      const Divider(height: 1),
                    ],
                    if (_nextCursor != null)
                      Padding(
                        padding: const EdgeInsets.all(16),
                        child: OutlinedButton(onPressed: () => _load(_nextCursor), child: const Text('Xem thêm')),
                      ),
                  ],
                ),
        ),
      ],
    );
  }
}

/// Ô chọn một bộ lọc, mục đầu "Tất cả" là không lọc.
class _Filter extends StatelessWidget {
  const _Filter({super.key, required this.label, required this.options, required this.value, required this.onChanged});

  final String label;
  final Map<String, String> options;
  final String? value;
  final ValueChanged<String?> onChanged;

  @override
  Widget build(BuildContext context) => DropdownButtonFormField<String?>(
    initialValue: value,
    isExpanded: true,
    icon: const Icon(LucideIcons.chevronDown),
    decoration: InputDecoration(labelText: label),
    items: [
      const DropdownMenuItem(child: Text('Tất cả')),
      for (final e in options.entries)
        DropdownMenuItem(
          value: e.key,
          child: Text(e.value, overflow: TextOverflow.ellipsis),
        ),
    ],
    onChanged: onChanged,
  );
}

/// Dòng danh sách (DESIGN.md 15.4): loại yêu cầu và mã, người yêu cầu và ngày hiệu lực, bên phải chữ trạng thái.
class _RequestRow extends StatelessWidget {
  const _RequestRow({required this.request, required this.onTap});

  final Map<String, dynamic> request;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final secondary = text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant);
    final status = request['status'] as String;
    return Material(
      color: Theme.of(context).colorScheme.surface,
      child: InkWell(
        onTap: onTap,
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 64),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${typeLabel(request['type'] as String)} · ${request['displayCode']}',
                        style: text.titleSmall,
                      ),
                      Text(
                        '${actorLabel(request['requester'] as Map<String, dynamic>?)}, '
                        'hiệu lực ${formatDate(request['requestedEffectiveDate'] as String)}',
                        style: secondary,
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                StatusPill(label: statusLabel(status), tone: statusTone(status)),
              ],
            ),
          ),
        ),
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
