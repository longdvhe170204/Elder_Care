import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';
import 'medication_round_gateway.dart';

/// Chờ xem lại liều (006 FR-029, SC-012): bản ghi thuốc kiểm soát đặc biệt "ghi sau mất kết nối" trong phạm vi. Điều
/// dưỡng khác người ghi gắn "Chấp nhận" hoặc "Cần đính chính"; chỉ gắn kết quả, không sửa bản ghi (Q-66). Cần mạng.
class DoseReviewScreen extends StatefulWidget {
  const DoseReviewScreen({super.key, required this.gateway, required this.newKey});
  final MedicationRoundGateway gateway;
  final String Function() newKey;

  @override
  State<DoseReviewScreen> createState() => _DoseReviewScreenState();
}

class _DoseReviewScreenState extends State<DoseReviewScreen> {
  List<Map<String, dynamic>>? reviews;
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
      final next = await widget.gateway.pendingReviews();
      if (mounted) setState(() => reviews = next);
    } catch (e) {
      if (mounted) setState(() => error = _message(e, 'Không tải được danh sách chờ xem lại.'));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  static String _message(Object e, String fallback) => switch (e) {
    ApiProblem(:final detail) => detail ?? fallback,
    ApiUnreachable() => 'Xem lại liều cần kết nối mạng.',
    _ => fallback,
  };

  Future<void> decide(Map<String, dynamic> review, String result) async {
    final note = result == 'REQUIRES_CORRECTION' ? await _askNote() : null;
    if (result == 'REQUIRES_CORRECTION' && note == null) return;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await widget.gateway.reviewResult(review['id'] as int, result, note, widget.newKey());
      if (!mounted) return;
      setState(() {
        reviews!.remove(review);
        message = result == 'ACCEPTED' ? 'Đã chấp nhận bản ghi.' : 'Đã ghi cần đính chính.';
      });
    } catch (e) {
      if (mounted) setState(() => error = _message(e, 'Chưa gắn được kết quả xem lại.'));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<String?> _askNote() => showDialog<String>(context: context, builder: (_) => const _CorrectionNoteDialog());

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final list = reviews;
    return Scaffold(
      appBar: AppBar(title: const Text('Chờ xem lại liều')),
      body: PageBody(
        child: ListView(
          children: [
            if (busy) const LinearProgressIndicator(),
            if (message != null) Semantics(liveRegion: true, child: Text(message!)),
            if (error != null) Semantics(liveRegion: true, child: Text(error!)),
            if (list != null && list.isEmpty)
              const Padding(
                padding: EdgeInsets.symmetric(vertical: 16),
                child: Text('Không có bản ghi thuốc chờ xem lại trong phạm vi của bạn.'),
              ),
            for (final review in list ?? const <Map<String, dynamic>>[])
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: theme.colorScheme.surface,
                  border: Border(bottom: BorderSide(color: theme.colorScheme.outlineVariant)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(review['medicationName'] as String? ?? 'Liều thuốc', style: theme.textTheme.titleSmall),
                    if (review['administeredAt'] != null)
                      Text(
                        'Thời điểm dùng ${formatDateTime(DateTime.parse(review['administeredAt'] as String))}',
                        style: theme.textTheme.bodySmall,
                      ),
                    if (review['recordedBy'] != null)
                      Text('Người ghi: ${review['recordedBy']}', style: theme.textTheme.bodySmall),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: FilledButton.tonal(
                            onPressed: busy ? null : () => decide(review, 'ACCEPTED'),
                            child: const Text('Chấp nhận'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: OutlinedButton(
                            onPressed: busy ? null : () => decide(review, 'REQUIRES_CORRECTION'),
                            child: const Text('Cần đính chính'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// Hộp ghi điều cần đính chính; tự giữ controller để không bị hủy khi hộp đang đóng.
class _CorrectionNoteDialog extends StatefulWidget {
  const _CorrectionNoteDialog();

  @override
  State<_CorrectionNoteDialog> createState() => _CorrectionNoteDialogState();
}

class _CorrectionNoteDialogState extends State<_CorrectionNoteDialog> {
  final controller = TextEditingController();

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('Cần đính chính'),
    content: TextField(
      controller: controller,
      autofocus: true,
      decoration: const InputDecoration(labelText: 'Điều cần đính chính'),
    ),
    actions: [
      TextButton(onPressed: () => Navigator.pop(context), child: const Text('Hủy')),
      FilledButton(
        onPressed: () => Navigator.pop(context, controller.text.trim()),
        child: const Text('Ghi cần đính chính'),
      ),
    ],
  );
}
