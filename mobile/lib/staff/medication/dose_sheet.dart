import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/theme/app_theme.dart';
import 'medication_labels.dart';
import 'medication_round_gateway.dart';
import 'medication_round_repository.dart';

/// Khung xác nhận liều (DESIGN.md `dose-sheet`): họ tên, dị ứng; mỗi liều một khối Đã dùng / Từ chối / Không thực
/// hiện; Đã dùng có thời điểm dùng (điền sẵn giờ hiện tại) và phản ứng; Từ chối, Không thực hiện bắt buộc lý do
/// (FR-021). Một nút lưu, mỗi liều lưu riêng. Trả số liều đã lưu và số liều đưa vào hàng đợi.
class DoseSheet extends StatefulWidget {
  const DoseSheet({
    super.key,
    required this.repository,
    required this.accountId,
    required this.items,
    required this.newKey,
    required this.clock,
  });

  final MedicationRoundRepository repository;
  final int accountId;

  /// Các liều ghi được của một người cao tuổi (đã ghép, xem `MedicationRoundRepository`).
  final List<Map<String, dynamic>> items;
  final String Function() newKey;
  final DateTime Function() clock;

  @override
  State<DoseSheet> createState() => _DoseSheetState();
}

class DoseSheetResult {
  const DoseSheetResult(this.online, this.queued);
  final int online;
  final int queued;
}

class _Draft {
  _Draft(DateTime now) : time = TextEditingController(text: formatDateTime(now));
  DoseOutcome? outcome;
  final TextEditingController time;
  final TextEditingController reaction = TextEditingController();
  final TextEditingController reason = TextEditingController();
  final TextEditingController note = TextEditingController();
  bool afterDisconnect = false;
  bool saved = false;
  String? error;
  String? timeError;
  String? reasonError;
  String? signature;
  String? key;

  void dispose() {
    for (final c in [time, reaction, reason, note]) {
      c.dispose();
    }
  }
}

class _DoseSheetState extends State<DoseSheet> {
  late final List<_Draft> drafts = [for (final _ in widget.items) _Draft(widget.clock())];
  bool busy = false;
  int online = 0;
  int queued = 0;

  @override
  void dispose() {
    for (final d in drafts) {
      d.dispose();
    }
    super.dispose();
  }

  Map<String, dynamic>? _body(_Draft d) {
    d.timeError = null;
    d.reasonError = null;
    final note = d.note.text.trim();
    if (d.outcome == DoseOutcome.administered) {
      final at = parseVnDateTime(d.time.text);
      if (at == null) {
        d.timeError = 'Nhập theo dạng dd/MM/yyyy HH:mm.';
        return null;
      }
      final reaction = d.reaction.text.trim();
      return {
        'administeredAt': vnIso(at),
        'basis': 'REGULAR',
        if (reaction.isNotEmpty) 'adverseReaction': reaction,
        if (note.isNotEmpty) 'note': note,
        if (d.afterDisconnect) 'recordedAfterDisconnect': true,
      };
    }
    final reason = d.reason.text.trim();
    if (reason.isEmpty) {
      d.reasonError = 'Nhập lý do ${d.outcome!.label.toLowerCase()}.';
      return null;
    }
    return {'reason': reason, if (note.isNotEmpty) 'note': note};
  }

  Future<void> save() async {
    final chosen = [
      for (var i = 0; i < drafts.length; i++)
        if (!drafts[i].saved && drafts[i].outcome != null) i,
    ];
    if (chosen.isEmpty) return;
    final bodies = <int, Map<String, dynamic>>{};
    setState(() {
      for (final i in chosen) {
        final body = _body(drafts[i]);
        if (body != null) bodies[i] = body;
      }
    });
    if (bodies.isEmpty) return;
    setState(() => busy = true);
    for (final MapEntry(key: i, value: body) in bodies.entries) {
      final d = drafts[i];
      final signature = '${d.outcome!.name}:$body';
      if (signature != d.signature) {
        d.signature = signature;
        d.key = widget.newKey();
      }
      try {
        final saved = await widget.repository.record(
          accountId: widget.accountId,
          item: widget.items[i],
          outcome: d.outcome!,
          body: body,
          key: d.key!,
        );
        d.saved = true;
        d.error = null;
        saved == RecordSaved.online ? online++ : queued++;
      } on RequiresOnline {
        d.error =
            'Thuốc kiểm soát đặc biệt chỉ ghi được khi có mạng. Cho dùng, ghi tạm ngoài hệ thống, '
            'rồi ghi lại khi có mạng và chọn "Ghi sau mất kết nối".';
      } on ApiProblem catch (problem) {
        final closedBy = problem.properties['closedBy'];
        d.error = problem.errorCode == 'DOSE_ALREADY_CLOSED' && closedBy != null
            ? 'Liều đã được $closedBy ghi trước. Nội dung của bạn vẫn giữ để chuyển thành đính chính.'
            : problem.detail;
      } on ApiUnreachable {
        d.error = 'Chưa gửi được. Giữ nguyên nội dung để thử lại.';
      }
      if (mounted) setState(() {});
    }
    if (!mounted) return;
    setState(() => busy = false);
    if (drafts.indexed.every((e) => e.$2.saved || e.$2.outcome == null)) {
      Navigator.pop(context, DoseSheetResult(online, queued));
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final resident = widget.items.first['resident'] as Map;
    final allergies = resident['allergies'] as List?;
    return PopScope(
      canPop: !busy,
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 16, 16, MediaQuery.viewInsetsOf(context).bottom + 16),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(resident['fullName'] as String? ?? 'Người cao tuổi', style: theme.textTheme.titleLarge),
              if (allergies != null) ...[const SizedBox(height: 8), _AllergyNote(allergies.cast<String>())],
              for (final (i, item) in widget.items.indexed) _block(context, item, drafts[i]),
              const SizedBox(height: 16),
              FilledButton(
                onPressed: busy || drafts.every((d) => d.saved || d.outcome == null) ? null : save,
                child: Text(busy ? 'Đang lưu…' : 'Lưu kết quả'),
              ),
              if (online + queued > 0 && !busy)
                TextButton(
                  onPressed: () => Navigator.pop(context, DoseSheetResult(online, queued)),
                  child: const Text('Đóng'),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _block(BuildContext context, Map<String, dynamic> item, _Draft d) {
    final theme = Theme.of(context);
    final dose = item['dose'] as Map;
    final windowTo = DateTime.parse(dose['windowTo'] as String);
    final at = parseVnDateTime(d.time.text);
    final late = d.outcome == DoseOutcome.administered && at != null && at.isAfter(windowTo);
    final instruction = (item['prescription'] as Map)['instructionNote'] as String?;
    final enabled = !busy && !d.saved;
    return Padding(
      padding: const EdgeInsets.only(top: 16),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: theme.colorScheme.surface,
          border: Border.all(color: theme.colorScheme.outlineVariant),
          borderRadius: BorderRadius.circular(AppRadius.md),
        ),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(doseLine(item), style: theme.textTheme.titleMedium),
              Text(
                'Giờ ${formatTime(DateTime.parse(dose['scheduledTime'] as String))}, cửa sổ '
                '${formatTime(DateTime.parse(dose['windowFrom'] as String))}–${formatTime(windowTo)}',
                style: theme.textTheme.bodySmall,
              ),
              if (instruction != null && instruction.isNotEmpty) Text(instruction, style: theme.textTheme.bodySmall),
              ..._tags(dose, theme),
              if (d.saved)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text('Đã lưu: ${d.outcome!.label}', style: theme.textTheme.titleSmall),
                )
              else ...[
                const SizedBox(height: 8),
                SegmentedButton<DoseOutcome>(
                  emptySelectionAllowed: true,
                  showSelectedIcon: false,
                  segments: [for (final o in DoseOutcome.values) ButtonSegment(value: o, label: Text(o.label))],
                  selected: {?d.outcome},
                  onSelectionChanged: enabled
                      ? (s) => setState(() {
                          d.outcome = s.isEmpty ? null : s.first;
                          d.error = null;
                        })
                      : null,
                ),
                if (d.outcome == DoseOutcome.administered) ...[
                  const SizedBox(height: 8),
                  TextField(
                    controller: d.time,
                    enabled: enabled,
                    onChanged: (_) => setState(() {}),
                    decoration: InputDecoration(
                      labelText: 'Thời điểm dùng',
                      errorText: d.timeError,
                      helperText: late ? 'Sau cửa sổ: ghi là dùng trễ' : null,
                    ),
                  ),
                  TextField(
                    controller: d.reaction,
                    enabled: enabled,
                    decoration: const InputDecoration(labelText: 'Phản ứng (nếu có)'),
                  ),
                  if (dose['requiresOnline'] == true)
                    CheckboxListTile(
                      value: d.afterDisconnect,
                      onChanged: enabled ? (v) => setState(() => d.afterDisconnect = v ?? false) : null,
                      title: const Text('Ghi sau mất kết nối'),
                      subtitle: const Text('Đã cho dùng lúc mất mạng; bản ghi sẽ chờ Điều dưỡng khác xem lại.'),
                      controlAffinity: ListTileControlAffinity.leading,
                      contentPadding: EdgeInsets.zero,
                    ),
                ],
                if (d.outcome == DoseOutcome.refused || d.outcome == DoseOutcome.notAdministered)
                  TextField(
                    controller: d.reason,
                    enabled: enabled,
                    maxLength: 2000,
                    decoration: InputDecoration(labelText: 'Lý do (bắt buộc)', errorText: d.reasonError),
                  ),
                if (d.outcome != null)
                  TextField(
                    controller: d.note,
                    enabled: enabled,
                    decoration: InputDecoration(
                      labelText: 'Ghi chú',
                      helperText: d.outcome == DoseOutcome.refused ? 'Có thể ghi điều người cao tuổi đã nói.' : null,
                    ),
                  ),
              ],
              if (d.error != null)
                Semantics(
                  liveRegion: true,
                  child: Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(d.error!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Nhãn nguồn và điều kiện của liều (DESIGN.md `dose-card`).
List<Widget> _tags(Map<dynamic, dynamic> dose, ThemeData theme) => [
  Text(dose['medicationSource'] == 'FAMILY_SENT' ? 'Gia đình gửi' : 'Thuốc viện', style: theme.textTheme.bodySmall),
  if (dose['requiresOnline'] == true) Text('Kiểm soát đặc biệt, phải trực tuyến', style: theme.textTheme.bodySmall),
  if (dose['floorShared'] == true) Text('Liều chung của tầng', style: theme.textTheme.bodySmall),
];

/// Khối dị ứng (DESIGN.md `care-notes`: khối hổ phách duy nhất có nền); không có mục thì "Chưa ghi nhận".
class _AllergyNote extends StatelessWidget {
  const _AllergyNote(this.allergies);
  final List<String> allergies;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tones = theme.extension<Tones>()!;
    return DecoratedBox(
      decoration: BoxDecoration(
        color: tones.attentionWash,
        border: Border.all(color: tones.attentionInk),
        borderRadius: BorderRadius.circular(AppRadius.md),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            ExcludeSemantics(child: Icon(LucideIcons.triangleAlert, color: tones.attentionInk, size: 20)),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                'Dị ứng: ${allergies.isEmpty ? 'Chưa ghi nhận' : allergies.join(', ')}',
                style: theme.textTheme.bodySmall,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
