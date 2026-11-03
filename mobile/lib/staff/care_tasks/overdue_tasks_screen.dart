import 'package:flutter/material.dart';
import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';
import '../../common/widgets/status_pill.dart';

typedef OverdueTasksLoader = Future<List<Map<String, dynamic>>> Function(int accountId, int? floorAreaId);

/// T046: transport mảng/shell do A cung cấp; không gắn nguồn fixture vào production.
class OverdueTasksScreen extends StatefulWidget {
  const OverdueTasksScreen({
    super.key,
    required this.accountId,
    required this.canView,
    required this.load,
    this.floorAreaId,
  });
  final int accountId;
  final bool canView;
  final int? floorAreaId;
  final OverdueTasksLoader load;
  @override
  State<OverdueTasksScreen> createState() => _OverdueTasksScreenState();
}

class _OverdueTasksScreenState extends State<OverdueTasksScreen> {
  List<Map<String, dynamic>> _rows = [];
  bool _loading = false;
  String? _error;
  int _generation = 0;
  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void didUpdateWidget(OverdueTasksScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.accountId != widget.accountId ||
        oldWidget.floorAreaId != widget.floorAreaId ||
        oldWidget.canView != widget.canView ||
        oldWidget.load != widget.load) {
      _rows = [];
      _load();
    }
  }

  Future<void> _load() async {
    final token = ++_generation;
    setState(() {
      _rows = [];
      _error = null;
      _loading = widget.canView;
    });
    if (!widget.canView) return;
    try {
      final rows = await widget.load(widget.accountId, widget.floorAreaId);
      if (mounted && token == _generation) setState(() => _rows = rows);
    } catch (error) {
      if (mounted && token == _generation) {
        setState(
          () => _error = error is ApiProblem
              ? (error.status == 403
                    ? 'Bạn không có quyền xem công việc quá hạn.'
                    : error.detail ?? 'Không tải được công việc quá hạn.')
              : 'Không tải được công việc quá hạn. Kiểm tra kết nối rồi thử lại.',
        );
      }
    } finally {
      if (mounted && token == _generation) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return PageBody(
      child: ListView(
        children: [
          Text('Công việc quá hạn', style: text.headlineSmall),
          if (!widget.canView)
            const Text('Bạn không có quyền xem công việc quá hạn.')
          else ...[
            OutlinedButton(onPressed: _loading ? null : _load, child: const Text('Tải lại')),
            if (_loading)
              const Center(child: CircularProgressIndicator())
            else if (_error != null)
              Semantics(liveRegion: true, child: Text(_error!))
            else if (_rows.isEmpty)
              const Text('Không có công việc quá hạn')
            else
              for (final task in _rows)
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Divider(),
                    Text(task['taskType'] as String? ?? 'Công việc', style: text.titleMedium),
                    const StatusPill(label: 'Quá hạn', tone: StatusTone.urgent),
                    if (task['residentCard'] case final Map<String, dynamic> card)
                      Text(card['fullName'] as String? ?? 'Người cao tuổi')
                    else
                      Text(task['location'] as String? ?? 'Việc chung của tầng'),
                    Text(
                      {'NORMAL': 'Thường', 'IMPORTANT': 'Quan trọng', 'MANDATORY': 'Bắt buộc'}[task['importance']] ??
                          '',
                    ),
                    if (task['plannedAt'] case final String at) Text('Dự kiến: ${formatDateTime(DateTime.parse(at))}'),
                    if (task['content'] case final String content) Text(content),
                    if (task['performer'] case final String performer)
                      Text('Phụ trách: $performer')
                    else
                      const Text('Chưa ai nhận'),
                    if (task['carryover'] == 'FROM_PREVIOUS_SHIFT') const Text('Tồn từ ca trước'),
                    if (task['carryover'] == 'AWAITING_HANDOVER_CONFIRMATION') const Text('Tồn, chờ xác nhận bàn giao'),
                    Text('Đã nhắc và báo: ${task['reminderCount'] ?? 0}'),
                  ],
                ),
          ],
        ],
      ),
    );
  }
}
