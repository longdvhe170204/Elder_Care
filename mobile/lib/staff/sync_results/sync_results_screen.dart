import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/auth/session_controller.dart';
import '../../common/format/date_time.dart';
import '../../common/offline/offline_database.dart';
import '../../common/offline/offline_queue.dart';
import '../../common/offline/record_types.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/status_pill.dart';

/// Nhãn và tông của kết quả đồng bộ (BR-M15-20; bố cục duyệt 2026-10-27).
(String, StatusTone) syncResultLabel(String code) => switch (code) {
  'ACCEPTED' => ('Đã nhận', StatusTone.settled),
  'ACCEPTED_WITH_FLAG' => ('Đã nhận, kèm nhãn mất quyền', StatusTone.attention),
  'PENDING_REVIEW' => ('Chờ xem lại', StatusTone.attention),
  // Không dùng đỏ: bị từ chối không phải việc gấp (DESIGN.md mục 1 điều 4); cần ghi lại nên tông attention
  'REJECTED' => ('Bị từ chối', StatusTone.attention),
  _ => (code, StatusTone.neutral),
};

/// Kết quả đồng bộ (002 T060; BR-M15-20; DESIGN.md 15.5): số bản ghi chưa đồng bộ, nút "Đồng bộ ngay", kết quả từng
/// bản ghi đã gửi kèm lý do khi bị từ chối. Mất mạng thì dải báo ngoại tuyến; tài khoản không còn Hoạt động thì phiên
/// kết thúc (FR-013).
class SyncResultsScreen extends StatefulWidget {
  const SyncResultsScreen({super.key, required this.queue, required this.session});

  final OfflineQueue queue;
  final SessionController session;

  @override
  State<SyncResultsScreen> createState() => _SyncResultsScreenState();
}

class _SyncResultsScreenState extends State<SyncResultsScreen> {
  List<QueuedRecord> _waiting = const [];
  List<QueuedRecord> _done = const [];
  bool _offline = false;
  bool _busy = false;
  String? _message;

  int get _accountId => widget.session.user!.accountId;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final waiting = await widget.queue.unsynced(_accountId);
    final done = await widget.queue.synced(_accountId);
    if (mounted) {
      setState(() {
        _waiting = waiting;
        _done = done.reversed.toList();
      });
    }
  }

  Future<void> _sync() async {
    setState(() {
      _busy = true;
      _message = null;
    });
    try {
      final report = await widget.queue.sync(_accountId);
      if (report.sessionEnded) {
        await widget.session.signOut(
          notice: 'Tài khoản không còn hoạt động nên phiên đã kết thúc. Liên hệ Quản lý viện.',
          callBackend: false,
        );
        return;
      }
      _offline = false;
      _message = 'Đã đồng bộ ${report.results.length} bản ghi.';
      await _load();
    } on ApiUnreachable {
      _offline = true;
    } on ApiProblem catch (p) {
      _message = p.detail ?? 'Chưa đồng bộ được. Thử lại sau.';
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (_offline) ...[const OfflineBand(), const SizedBox(height: 12)],
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text('Chưa đồng bộ: ${_waiting.length} bản ghi', style: text.titleMedium),
                if (_message != null) ...[
                  const SizedBox(height: 8),
                  Semantics(liveRegion: true, child: Text(_message!, style: text.bodyMedium)),
                ],
                const SizedBox(height: 12),
                FilledButton(onPressed: _busy || _waiting.isEmpty ? null : _sync, child: const Text('Đồng bộ ngay')),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        if (_done.isEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text('Chưa có bản ghi nào đã đồng bộ trên máy này.', style: text.bodyMedium),
            ),
          ),
        for (final r in _done) ...[_ResultCard(record: r), const SizedBox(height: 12)],
      ],
    );
  }
}

/// Dải báo mất mạng (DESIGN.md 15.5): tông `neutral`, có chữ, không đỏ.
class OfflineBand extends StatelessWidget {
  const OfflineBand({super.key});

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(AppRadius.lg),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Text('Đang ngoại tuyến. Bản ghi sẽ đồng bộ khi có mạng.', style: Theme.of(context).textTheme.bodyMedium),
      ),
    );
  }
}

class _ResultCard extends StatelessWidget {
  const _ResultCard({required this.record});

  final QueuedRecord record;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final (label, tone) = syncResultLabel(record.result!);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(child: Text(recordTypeLabel(record.recordType), style: text.titleSmall)),
                StatusPill(label: label, tone: tone),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              'Ghi trên máy lúc ${formatDateTime(record.deviceTime)}'
              '${record.syncedAt == null ? '' : ', đồng bộ lúc ${formatDateTime(record.syncedAt!)}'}',
              style: text.bodySmall,
            ),
            if (record.reason != null) ...[
              const SizedBox(height: 4),
              Text('Lý do: ${record.reason}', style: text.bodyMedium),
            ],
          ],
        ),
      ),
    );
  }
}
