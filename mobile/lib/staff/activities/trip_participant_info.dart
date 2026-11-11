import 'package:flutter/material.dart';

import '../../common/format/date_time.dart';

// Sáu phần thông tin của một người tham gia chuyến (014 FR-036, FR-038, FR-039; api-014 TripParticipantInfo, chủ dự án
// duyệt 2026-11-10). Cùng chữ, cùng thứ tự với web (DESIGN.md 13.7): mỗi phần nói rõ có dữ liệu, không có dữ liệu, chưa lấy
// được từ nguồn, hay người xem không có quyền.

const _unavailable = {'NOT_PERMITTED': 'Không có quyền xem', 'SOURCE_UNAVAILABLE': 'Chưa lấy được dữ liệu từ nguồn'};

String _time(String iso) {
  final t = DateTime.parse(iso).toUtc().add(const Duration(hours: 7));
  return '${t.hour.toString().padLeft(2, '0')}:${t.minute.toString().padLeft(2, '0')}';
}

class TripParticipantInfo extends StatelessWidget {
  const TripParticipantInfo({super.key, required this.info});

  final Map<String, dynamic> info;

  Map<String, dynamic>? _section(String key) => info[key] as Map<String, dynamic>?;

  @override
  Widget build(BuildContext context) {
    final card = _section('emergencyCard');
    final allergies = (card?['allergies'] as List<dynamic>?)?.cast<String>() ?? const [];
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        spacing: 8,
        children: [
          if (card?['availability'] == 'AVAILABLE' && card?['identificationOnly'] != true && allergies.isNotEmpty)
            Text('Dị ứng: ${allergies.join(', ')}'),
          _part(context, 'Liều trong khoảng đi', _section('medicationDoses'), 'Không có liều trong khoảng đi', (s) {
            return (s['items'] as List<dynamic>)
                .cast<Map<String, dynamic>>()
                .map(
                  (d) =>
                      '${d['medicationName']}${d['strength'] != null ? ' ${d['strength']}' : ''}, '
                      '${_time(d['scheduledTime'] as String)}',
                )
                .join('; ');
          }),
          _part(context, 'Thuốc mang theo', _section('carriedMedication'), 'Chưa giao thuốc mang theo', (s) {
            if (s['notCarriedReason'] != null) return 'Không mang thuốc: ${s['notCarriedReason']}';
            final items = (s['items'] as List<dynamic>? ?? [])
                .cast<Map<String, dynamic>>()
                .map((i) => '${i['medicationName']} ${i['quantity']}${i['unit'] != null ? ' ${i['unit']}' : ''}')
                .join(', ');
            return 'Đã giao ${formatDateTime(DateTime.parse(s['handedAt'] as String))} cho ${s['recipientName']}: $items';
          }),
          _part(context, 'Sức khỏe đang mở', _section('health'), 'Không có', (s) {
            final parts = [
              if (s['suspectedInfection'] == true) 'Đang nghi nhiễm',
              if ((s['openAlerts'] as List<dynamic>? ?? []).isNotEmpty)
                '${(s['openAlerts'] as List<dynamic>).length} cảnh báo chưa đóng',
              if ((s['openIncidents'] as List<dynamic>? ?? []).isNotEmpty)
                '${(s['openIncidents'] as List<dynamic>).length} sự cố chưa đóng',
            ];
            return parts.isEmpty ? 'Không có cảnh báo, sự cố đang mở' : parts.join(', ');
          }),
          _part(context, 'Lưu ý chăm sóc', _section('careNotes'), 'Chưa ghi nhận', (s) => s['text'] as String),
          _part(
            context,
            'Liên hệ chính',
            _section('primaryContact'),
            'Chưa có người liên hệ chính',
            (s) => [s['fullName'], s['phoneNumber']].whereType<String>().join(', '),
          ),
          _part(context, 'Thẻ khẩn cấp', card, 'Chưa có', (s) {
            return [
              s['fullName'],
              if (s['roomBed'] != null) s['roomBed'],
              if (s['identificationOnly'] == true) 'chỉ phần nhận dạng',
              if (s['criticalFlag'] == true) 'đang có dấu nguy kịch',
            ].whereType<String>().join(', ');
          }),
        ],
      ),
    );
  }

  Widget _part(
    BuildContext context,
    String title,
    Map<String, dynamic>? section,
    String empty,
    String Function(Map<String, dynamic>) render,
  ) {
    final status = section?['availability'] as String? ?? 'SOURCE_UNAVAILABLE';
    final muted = Theme.of(context).colorScheme.onSurfaceVariant;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(title, style: Theme.of(context).textTheme.labelMedium?.copyWith(color: muted)),
        status == 'AVAILABLE'
            ? Text(render(section!))
            : Text(status == 'NO_DATA' ? empty : _unavailable[status]!, style: TextStyle(color: muted)),
      ],
    );
  }
}
