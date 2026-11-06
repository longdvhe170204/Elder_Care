import 'dart:async';

import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';
import '../../common/widgets/status_pill.dart';
import 'my_shifts_controller.dart';
import 'shift_absence_sheet.dart';
import 'shift_labels.dart';

/// Ca của tôi (008 T016b, FR-033, FR-054); bố cục và hai thay đổi nền được A duyệt qua xác nhận của E.
class MyShiftsScreen extends StatefulWidget {
  const MyShiftsScreen({super.key, required this.api, this.now});

  final ApiClient api;
  final DateTime? now;

  @override
  State<MyShiftsScreen> createState() => _MyShiftsScreenState();
}

class _MyShiftsScreenState extends State<MyShiftsScreen> {
  late final MyShiftsController _controller;

  @override
  void initState() {
    super.initState();
    _controller = MyShiftsController(api: widget.api, now: widget.now);
    unawaited(_controller.load());
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _showDetails(Map<String, dynamic> shift) async {
    final refresh = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      showDragHandle: true,
      builder: (context) => _ShiftDetails(api: widget.api, shift: shift, names: _controller.staffNames),
    );
    if (refresh == true && mounted) await _controller.load();
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: _controller,
    builder: (context, _) {
      final text = Theme.of(context).textTheme;
      final month = _controller.month;
      return PageBody(
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            Text('Tháng ${month.month}/${month.year}', style: text.titleLarge),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                TextButton.icon(
                  onPressed: () => _controller.changeMonth(-1),
                  icon: const Icon(LucideIcons.chevronLeft),
                  label: const Text('Tháng trước'),
                ),
                TextButton.icon(
                  onPressed: () => _controller.changeMonth(1),
                  icon: const Icon(LucideIcons.chevronRight),
                  label: const Text('Tháng sau'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text('Chỉ ca của bạn trong tháng', style: text.bodyMedium),
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: _controller.loading ? null : _controller.load,
              icon: const Icon(LucideIcons.refreshCw),
              label: const Text('Làm mới'),
            ),
            const SizedBox(height: 12),
            if (_controller.loading)
              const _MessageCard(message: 'Đang tải lịch ca…')
            else if (_controller.error != null)
              _MessageCard(message: backendMessage(_controller.error!), retry: _controller.load)
            else if (_controller.shifts.isEmpty)
              const _MessageCard(message: 'Bạn không có ca nào trong tháng này.')
            else
              ColoredBox(
                color: Theme.of(context).colorScheme.surface,
                child: Column(
                  children: [
                    for (final (index, shift) in _controller.shifts.indexed) ...[
                      if (index > 0) const Divider(height: 1),
                      _ShiftRow(shift: shift, onDetails: () => unawaited(_showDetails(shift))),
                    ],
                  ],
                ),
              ),
          ],
        ),
      );
    },
  );
}

class _MessageCard extends StatelessWidget {
  const _MessageCard({required this.message, this.retry});

  final String message;
  final VoidCallback? retry;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Semantics(liveRegion: true, child: Text(message, style: Theme.of(context).textTheme.bodyMedium)),
          if (retry != null) ...[
            const SizedBox(height: 12),
            OutlinedButton(onPressed: retry, child: const Text('Thử lại')),
          ],
        ],
      ),
    ),
  );
}

class _ShiftRow extends StatelessWidget {
  const _ShiftRow({required this.shift, required this.onDetails});

  final Map<String, dynamic> shift;
  final VoidCallback onDetails;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final start = formatDateTime(DateTime.parse(shift['startsAt'] as String));
    final end = formatDateTime(DateTime.parse(shift['endsAt'] as String));
    final (label, tone) = shiftStatusLabel(shift['status'] as String);
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Wrap(
            spacing: 12,
            runSpacing: 8,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              Text(start.split(' ').first, style: text.titleMedium),
              StatusPill(label: label, tone: tone),
            ],
          ),
          const SizedBox(height: 12),
          Text('Từ $start', style: text.bodyMedium),
          Text('Đến $end', style: text.bodyMedium),
          const SizedBox(height: 8),
          Text('Nhân viên trong ca: ${(shift['staff'] as List<dynamic>).length}', style: text.bodyMedium),
          const SizedBox(height: 12),
          OutlinedButton.icon(
            onPressed: onDetails,
            icon: const Icon(LucideIcons.calendarDays),
            label: const Text('Chi tiết ca'),
          ),
        ],
      ),
    );
  }
}

class _ShiftDetails extends StatelessWidget {
  const _ShiftDetails({required this.api, required this.shift, required this.names});

  final ApiClient api;
  final Map<String, dynamic> shift;
  final Map<int, String> names;

  String _name(int id) => names[id] ?? 'Chưa có thông tin được phép xem';

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final lead = shift['leadStaffId'] as int?;
    final (label, tone) = shiftStatusLabel(shift['status'] as String);
    return FractionallySizedBox(
      heightFactor: 0.85,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(child: Text('Chi tiết ca', style: text.titleLarge)),
                IconButton(
                  tooltip: 'Đóng chi tiết ca',
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(LucideIcons.x),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text('Từ ${formatDateTime(DateTime.parse(shift['startsAt'] as String))}', style: text.bodyMedium),
            Text('Đến ${formatDateTime(DateTime.parse(shift['endsAt'] as String))}', style: text.bodyMedium),
            const SizedBox(height: 8),
            StatusPill(label: label, tone: tone),
            const SizedBox(height: 20),
            Text('Người phụ trách ca', style: text.titleSmall),
            const SizedBox(height: 8),
            Text(lead == null ? 'Chưa có người phụ trách' : _name(lead), style: text.bodyMedium),
            const SizedBox(height: 20),
            Text('Nhân viên trong ca', style: text.titleSmall),
            for (final member in (shift['staff'] as List<dynamic>).cast<Map<String, dynamic>>()) ...[
              const SizedBox(height: 12),
              Text(_name(member['staffId'] as int), style: text.bodyMedium),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (member['onDutyDoctor'] == true) const StatusPill(label: 'Bác sĩ trực', tone: StatusTone.neutral),
                  if (member['absent'] == true) const StatusPill(label: 'Vắng ca', tone: StatusTone.attention),
                  if (member['approvedLeave'] == true)
                    const StatusPill(label: 'Nghỉ được duyệt', tone: StatusTone.neutral),
                ],
              ),
            ],
            if ((shift['allowedCommands'] as List<dynamic>? ?? []).contains('recordAbsence')) ...[
              const SizedBox(height: 20),
              OutlinedButton.icon(
                icon: const Icon(LucideIcons.userMinus),
                label: const Text('Ghi nhận vắng ca'),
                onPressed: () async {
                  final refresh = await showModalBottomSheet<bool>(
                    context: context,
                    isScrollControlled: true,
                    useSafeArea: true,
                    showDragHandle: true,
                    builder: (_) => ShiftAbsenceSheet(api: api, shift: shift, names: names),
                  );
                  if (refresh == true && context.mounted) Navigator.of(context).pop(true);
                },
              ),
            ],
          ],
        ),
      ),
    );
  }
}
