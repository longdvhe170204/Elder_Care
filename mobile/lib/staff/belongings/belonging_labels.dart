import '../../common/widgets/status_pill.dart';

/// Nhãn tiếng Việt của api-013 (013; cùng chữ với web `features/belongings/belongingLabels.js`, DESIGN.md 3.5, 13 điều 7).
const belongingStatusLabels = {
  'HELD': 'Đang giữ',
  'IN_USE': 'Đang được người cao tuổi sử dụng',
  'RETURNED': 'Đã trả',
  'LOST': 'Thất lạc',
  'DAMAGED': 'Hư hỏng',
  'DISPOSED': 'Đã xử lý',
};

const giverLabels = {
  'FAMILY_MEMBER': 'Người thân',
  'OTHER_PERSON': 'Người khác',
  'RESIDENT': 'Người cao tuổi',
  'STAFF': 'Nhân viên',
};

String belongingStatusLabel(String? code) => belongingStatusLabels[code] ?? code ?? '';

/// Thất lạc, Hư hỏng tông `attention`; trạng thái khác `neutral` (DESIGN.md 3.5).
StatusTone belongingTone(String? code) =>
    code == 'LOST' || code == 'DAMAGED' ? StatusTone.attention : StatusTone.neutral;

String _number(num value) {
  final text = value == value.roundToDouble() ? value.toInt().toString() : value.toString().replaceAll('.', ',');
  return text.replaceAllMapped(RegExp(r'\B(?=(\d{3})+(?!\d))'), (_) => '.');
}

/// Đồ tiền mặt mang số tiền, đồ khác mang số lượng (FR-005).
String amountText(Map<String, dynamic> item) {
  final cash = item['cashAmount'] as num?;
  if (cash != null) return '${_number(cash)} đồng';
  final quantity = item['quantity'] as num?;
  return quantity == null ? '' : _number(quantity);
}
