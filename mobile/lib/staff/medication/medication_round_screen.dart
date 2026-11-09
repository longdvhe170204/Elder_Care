import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/page_body.dart';
import '../../common/widgets/status_pill.dart';
import 'dose_review_screen.dart';
import 'dose_sheet.dart';
import 'medication_labels.dart';
import 'medication_round_repository.dart';

String medicationCommandKey() {
  final random = Random.secure();
  final bytes = List<int>.generate(16, (_) => random.nextInt(256));
  bytes[6] = (bytes[6] & 15) | 64;
  bytes[8] = (bytes[8] & 63) | 128;
  final h = bytes.map((v) => v.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}

/// Vòng phát thuốc của Điều dưỡng trên app (006 T028; FR-020, FR-021, FR-026, FR-029; DESIGN.md 15.4, 15.5).
/// Đầu màn xanh đêm; mất mạng thì dải ngoại tuyến và "Chưa cập nhật"; nhóm "Cần làm ngay" (Trễ, Đến giờ), "Sau đó",
/// "Đã xong"; mỗi người cao tuổi một dòng, bấm mở khung xác nhận. Nhận accountId từ shell của A.
class MedicationRoundScreen extends StatefulWidget {
  const MedicationRoundScreen({
    super.key,
    required this.repository,
    required this.accountId,
    this.newKey = medicationCommandKey,
    this.clock = DateTime.now,
  });

  final MedicationRoundRepository repository;
  final int accountId;
  final String Function() newKey;
  final DateTime Function() clock;

  @override
  State<MedicationRoundScreen> createState() => _MedicationRoundScreenState();
}

class _Row {
  _Row(this.residentId, this.resident);
  final int residentId;
  final Map<dynamic, dynamic> resident;
  final List<Map<String, dynamic>> items = [];
}

class _MedicationRoundScreenState extends State<MedicationRoundScreen> {
  MedicationRoundLoad? loaded;
  String? error;
  String? message;
  bool busy = false;

  @override
  void initState() {
    super.initState();
    load();
  }

  Future<void> load() async {
    setState(() {
      busy = true;
      error = null;
    });
    try {
      final next = await widget.repository.load(widget.accountId);
      if (mounted) setState(() => loaded = next);
    } catch (e) {
      if (mounted) {
        setState(() {
          loaded = null;
          error = e is ApiProblem ? e.detail : 'Không tải được vòng phát thuốc. Kiểm tra kết nối rồi thử lại.';
        });
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> open(_Row row) async {
    final queued = loaded!.queued;
    final items = row.items
        .where((i) => doseRecordable((i['dose'] as Map)['status'] as String?))
        .where((i) => !queued.containsKey((i['dose'] as Map)['doseKey']))
        .toList();
    if (items.isEmpty) return;
    final result = await showModalBottomSheet<DoseSheetResult>(
      context: context,
      isScrollControlled: true,
      builder: (_) => DoseSheet(
        repository: widget.repository,
        accountId: widget.accountId,
        items: items,
        newKey: widget.newKey,
        clock: widget.clock,
      ),
    );
    if (result == null || !mounted) return;
    setState(() {
      message = [
        if (result.online > 0) 'Đã lưu ${result.online} liều.',
        if (result.queued > 0) 'Đã ghi trên máy ${result.queued} liều, sẽ đồng bộ khi có mạng.',
      ].join(' ');
    });
    await load();
  }

  /// Nhóm theo người cao tuổi: Cần làm ngay (có liều Trễ, Đến giờ chưa ghi), Sau đó (Chưa đến giờ), Đã xong.
  (List<_Row>, List<_Row>, List<_Row>) _groups() {
    final queued = loaded!.queued;
    final now = <int, _Row>{}, later = <int, _Row>{}, done = <int, _Row>{};
    final items = [...loaded!.round.items]
      ..sort(
        (a, b) =>
            ((a['dose'] as Map)['scheduledTime'] as String).compareTo((b['dose'] as Map)['scheduledTime'] as String),
      );
    for (final item in items) {
      final dose = item['dose'] as Map;
      final status = dose['status'] as String?;
      final group = doseRecordable(status) && !queued.containsKey(dose['doseKey'])
          ? now
          : status == 'UPCOMING'
          ? later
          : done;
      group
          .putIfAbsent(dose['residentId'] as int, () => _Row(dose['residentId'] as int, item['resident'] as Map))
          .items
          .add(item);
    }
    bool hasLate(_Row r) => r.items.any((i) => (i['dose'] as Map)['status'] == 'LATE');
    final urgent = now.values.toList()..sort((a, b) => (hasLate(b) ? 1 : 0).compareTo(hasLate(a) ? 1 : 0));
    return (urgent, later.values.toList(), done.values.toList());
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final load = loaded;
    final groups = load == null ? null : _groups();
    return Scaffold(
      appBar: headerAppBar(context, title: 'Phát thuốc'),
      body: PageBody(
        child: RefreshIndicator(
          onRefresh: this.load,
          child: ListView(
            children: [
              if (busy) const LinearProgressIndicator(),
              if (load?.offline == true) const _OfflineBand(),
              if (load != null)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 8),
                  child: Text(
                    load.offline
                        ? 'Chưa cập nhật, tải lúc ${formatDateTime(load.round.loadedAt)}'
                        : 'Cập nhật lúc ${formatDateTime(load.round.loadedAt)}',
                    style: theme.textTheme.bodySmall,
                  ),
                ),
              if (message != null && message!.isNotEmpty)
                Semantics(liveRegion: true, child: Text(message!, style: theme.textTheme.bodyMedium)),
              if (error != null) ...[
                Semantics(liveRegion: true, child: Text(error!)),
                TextButton(onPressed: busy ? null : this.load, child: const Text('Thử lại')),
              ],
              if (groups != null) ...[
                if (groups.$1.isEmpty && groups.$2.isEmpty && groups.$3.isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 16),
                    child: Text('Không có liều trong vòng phát thuốc của bạn lúc này.'),
                  ),
                if (groups.$1.isNotEmpty) ...[
                  _GroupLabel('Cần làm ngay (${groups.$1.length})', urgent: true),
                  for (final row in groups.$1) _rowTile(context, row, tappable: true),
                ],
                if (groups.$2.isNotEmpty) ...[
                  const _GroupLabel('Sau đó'),
                  for (final row in groups.$2) _rowTile(context, row),
                ],
                if (groups.$3.isNotEmpty) ...[
                  const _GroupLabel('Đã xong'),
                  for (final row in groups.$3) _rowTile(context, row),
                ],
              ],
              const SizedBox(height: 16),
              OutlinedButton(
                onPressed: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => DoseReviewScreen(gateway: widget.repository.gateway, newKey: widget.newKey),
                  ),
                ),
                child: const Text('Chờ xem lại liều'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _rowTile(BuildContext context, _Row row, {bool tappable = false}) {
    final theme = Theme.of(context);
    final tones = theme.extension<Tones>()!;
    final queued = loaded!.queued;
    final allergies = row.resident['allergies'] as List?;
    final statuses = row.items.map((i) => (i['dose'] as Map)['status'] as String?).toList();
    final headline = doseStatusLabel(
      statuses.contains('LATE')
          ? 'LATE'
          : statuses.contains('DUE')
          ? 'DUE'
          : statuses.first,
    );
    return Material(
      color: theme.colorScheme.surface,
      child: InkWell(
        onTap: tappable && !busy ? () => open(row) : null,
        child: Container(
          constraints: const BoxConstraints(minHeight: 64),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            border: Border(bottom: BorderSide(color: theme.colorScheme.outlineVariant)),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(row.resident['fullName'] as String? ?? 'Người cao tuổi', style: theme.textTheme.titleSmall),
                    if (allergies != null && allergies.isNotEmpty)
                      Text(
                        'dị ứng ${allergies.join(', ')}',
                        style: theme.textTheme.bodySmall?.copyWith(color: tones.attentionInk),
                      ),
                    for (final item in row.items) ...[
                      Text(
                        '${formatTime(DateTime.parse((item['dose'] as Map)['scheduledTime'] as String))} '
                        '${doseLine(item)}',
                        style: theme.textTheme.bodySmall,
                      ),
                      if ((item['dose'] as Map)['requiresOnline'] == true)
                        Text('Kiểm soát đặc biệt, phải trực tuyến', style: theme.textTheme.bodySmall),
                      if (queued[(item['dose'] as Map)['doseKey']] case final type?)
                        StatusPill(label: 'Chưa đồng bộ: ${resultTypeLabel(type)}', tone: StatusTone.neutral),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 8),
              StatusPill(label: headline.$1, tone: headline.$2),
            ],
          ),
        ),
      ),
    );
  }
}

class _GroupLabel extends StatelessWidget {
  const _GroupLabel(this.text, {this.urgent = false});
  final String text;
  final bool urgent;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.only(top: 16, bottom: 8),
      child: Semantics(
        header: true,
        child: Text(
          text,
          style: theme.textTheme.labelLarge?.copyWith(
            color: urgent ? theme.colorScheme.error : theme.colorScheme.onSurfaceVariant,
          ),
        ),
      ),
    );
  }
}

/// Dải ngoại tuyến của DESIGN.md 15.5 (tông neutral, không đỏ).
class _OfflineBand extends StatelessWidget {
  const _OfflineBand();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Semantics(
      liveRegion: true,
      child: Container(
        margin: const EdgeInsets.only(top: 8),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          border: Border.all(color: theme.colorScheme.outlineVariant),
          borderRadius: BorderRadius.circular(AppRadius.md),
        ),
        child: Text(
          'Đang ngoại tuyến. Bản ghi sẽ đồng bộ khi có mạng.',
          style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ),
    );
  }
}
