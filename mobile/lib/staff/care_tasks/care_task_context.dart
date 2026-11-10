import 'package:flutter/material.dart';
import '../../common/format/date_time.dart';

/// FR-030: ngữ cảnh đã được API trả, giữ nguyên khi xem bản đã tải.
class CareTaskContext extends StatelessWidget {
  const CareTaskContext({super.key, required this.task});
  final Map<String, dynamic> task;

  static const sources = {
    'CARE_PLAN_ITEM': 'Kế hoạch chăm sóc',
    'MEASUREMENT': 'Lịch đo',
    'MEAL': 'Bữa ăn',
    'ACTIVITY_SESSION': 'Buổi hoạt động',
    'POST_FALL_MONITORING': 'Theo dõi sau ngã',
    'CONTACT_MONITORING': 'Theo dõi tiếp xúc',
    'WATER_INTAKE_ALERT': 'Cảnh báo uống nước',
    'SHIFT_START_CHECK': 'Kiểm tra đầu ca',
    'SUPERVISION': 'Giám sát',
    'AD_HOC': 'Phát sinh',
    'AD_HOC_RECORDING': 'Ghi nhận phát sinh',
    'REWORK': 'Làm lại',
  };

  @override
  Widget build(BuildContext context) {
    final card = task['residentCard'] as Map?;
    final basis = task['basis'] as Map?;
    final source = sources[basis?['type']];
    final importance = const {
      'NORMAL': 'Thường',
      'IMPORTANT': 'Quan trọng',
      'MANDATORY': 'Bắt buộc',
    }[task['importance']];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(card?['fullName'] as String? ?? task['location'] as String? ?? 'Công việc tầng/khu'),
        if (card?['roomBed'] case final String roomBed) Text(roomBed),
        if (task['content'] case final String content) Text(content),
        if (task['plannedAt'] case final String at) Text('Dự kiến: ${formatDateTime(DateTime.parse(at))}'),
        if (task['windowFrom'] case final String from)
          if (task['windowTo'] case final String to)
            Text('Khung giờ: ${formatTime(DateTime.parse(from))}–${formatTime(DateTime.parse(to))}'),
        if (importance != null) Text(importance),
        if (source != null) Text('Nguồn: $source'),
        Text(task['performer'] == null ? 'Chưa ai nhận' : 'Phụ trách: ${task['performer']}'),
        if (task['carryover'] == 'FROM_PREVIOUS_SHIFT') const Text('Tồn từ ca trước'),
        if (task['carryover'] == 'AWAITING_CONFIRMATION') const Text('Tồn, chờ xác nhận bàn giao'),
        if (card?['careNotes'] case final String notes) Text(notes),
      ],
    );
  }
}
