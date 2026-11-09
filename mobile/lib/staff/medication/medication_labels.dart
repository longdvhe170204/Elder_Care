import '../../common/widgets/status_pill.dart';

/// Trạng thái liều (api-006 `DoseStatus`) → chữ và tông theo DESIGN.md 3.5, cùng chữ với web.
(String, StatusTone) doseStatusLabel(String? status) => switch (status) {
  'UPCOMING' => ('Chưa đến giờ', StatusTone.neutral),
  'DUE' => ('Đến giờ', StatusTone.attention),
  'LATE' => ('Trễ', StatusTone.urgent),
  'MISSED' => ('Bỏ lỡ', StatusTone.urgent),
  'ADMINISTERED' => ('Đã dùng', StatusTone.settled),
  'REFUSED' => ('Từ chối', StatusTone.neutral),
  'NOT_ADMINISTERED' => ('Không thực hiện', StatusTone.neutral),
  'SUSPENDED' => ('Tạm dừng', StatusTone.neutral),
  'LEAVE_MEDICATION' => ('Mang theo', StatusTone.neutral),
  'CANCELLED' => ('Đã hủy', StatusTone.neutral),
  _ => (status ?? '', StatusTone.neutral),
};

/// Kết quả ngoại tuyến (`resultType`) → chữ.
String resultTypeLabel(String type) => switch (type) {
  'ADMINISTERED' => 'Đã dùng',
  'REFUSED' => 'Từ chối',
  'NOT_ADMINISTERED' => 'Không thực hiện',
  _ => type,
};

/// Liều ghi được trên app: Đến giờ, Trễ (DESIGN.md `dose-card`: Tạm dừng, Mang theo chỉ có chữ, không nút).
bool doseRecordable(String? status) => status == 'DUE' || status == 'LATE';

/// Gửi ISO-8601 có `+07:00` (mobile/README.md luật 4).
String vnIso(DateTime value) {
  final vn = value.toUtc().add(const Duration(hours: 7));
  String two(int n) => n.toString().padLeft(2, '0');
  return '${vn.year}-${two(vn.month)}-${two(vn.day)}T${two(vn.hour)}:${two(vn.minute)}:00+07:00';
}

/// Đọc "dd/MM/yyyy HH:mm" giờ Việt Nam; sai dạng thì null.
DateTime? parseVnDateTime(String text) {
  final m = RegExp(r'^(\d{2})/(\d{2})/(\d{4}) (\d{2}):(\d{2})$').firstMatch(text.trim());
  return m == null ? null : DateTime.tryParse('${m[3]}-${m[2]}-${m[1]}T${m[4]}:${m[5]}:00+07:00');
}

/// "Paracetamol 500 mg, 1 viên, đường uống".
String doseLine(Map<String, dynamic> item) {
  final medication = item['medication'] as Map;
  final prescription = item['prescription'] as Map;
  final name = [medication['name'], medication['concentration']].whereType<String>().join(' ');
  final dosage = prescription['dosage'];
  final amount = dosage == null ? null : '${_number(dosage as num)} ${medication['usageUnit'] ?? ''}'.trim();
  return [name, amount, prescription['route']].whereType<String>().where((s) => s.isNotEmpty).join(', ');
}

String _number(num value) => value == value.roundToDouble() ? value.toInt().toString() : value.toString();
