import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';

// Nhãn cùng chữ với web `features/family-portal/live/LiveCostsPage.jsx`; mobile/README luật 1 cấm import từ `staff/`.
const _types = {
  'CASH_PAYMENT': 'Nộp tiền mặt',
  'BANK_TRANSFER_PAYMENT': 'Nộp chuyển khoản',
  'STATEMENT_PAYMENT': 'Thanh toán bảng chi phí',
  'REFUND': 'Hoàn tiền',
  'DEPOSIT_OFFSET': 'Cấn trừ tiền cọc',
  'ADJUSTMENT': 'Điều chỉnh',
  'DEPOSIT_RECEIPT': 'Thu cọc',
  'DEPOSIT_REFUND': 'Hoàn cọc',
};

/// Tiền VND, dấu chấm ngăn nghìn (NFR-11), ví dụ `-9.300.000 đồng`.
String money(num amount) {
  final digits = amount.abs().round().toString();
  final grouped = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) grouped.write('.');
    grouped.write(digits[i]);
  }
  return '${amount < 0 ? '-' : ''}$grouped đồng';
}

/// Số dư trên app người thân (017 T011; FR-003; api-017 `GET /family-portal/residents/{residentId}/balance`): số dư,
/// cọc, tài khoản thu của viện, mã nộp tiền và mã QR VietQR không số tiền, giao dịch Đã xác nhận (thanh toán bảng chỉ có
/// tổng). Backend kiểm quyền xem chi phí mỗi lần; không có thanh toán trực tuyến (Q-285).
class FamilyBalanceScreen extends StatefulWidget {
  const FamilyBalanceScreen({super.key, required this.api, required this.residentId, required this.residentName});

  final ApiClient api;
  final int residentId;
  final String residentName;

  @override
  State<FamilyBalanceScreen> createState() => _FamilyBalanceScreenState();
}

class _FamilyBalanceScreenState extends State<FamilyBalanceScreen> {
  Map<String, dynamic>? _balance;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final data = await widget.api.get('/family-portal/residents/${widget.residentId}/balance');
      if (mounted) setState(() => _balance = data);
    } on ApiProblem catch (e) {
      if (!mounted) return;
      setState(
        () => _error = e.status == 403
            ? 'Bạn chưa có quyền xem chi phí của ${widget.residentName}. Người đại diện có thể đề nghị viện cấp quyền.'
            : backendMessage(e),
      );
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Widget _row(TextTheme text, String label, String value) => Padding(
    padding: const EdgeInsets.only(top: 8),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: text.bodySmall),
        Text(value, style: text.bodyLarge),
      ],
    ),
  );

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final b = _balance;
    if (_error != null) return PageBody(child: Text(_error!));
    if (b == null) return const PageBody(child: Center(child: CircularProgressIndicator()));
    final balance = b['balance'] as num;
    final instructions = b['paymentInstructions'] as Map<String, dynamic>?;
    final transactions = (b['transactions'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
    return PageBody(
      child: ListView(
        children: [
          Text('Số dư hiện tại', style: text.titleMedium),
          Text(balance < 0 ? 'Còn nợ ${money(-balance)}' : money(balance), style: text.headlineSmall),
          Text('Tiền cọc ${money(b['depositBalance'] as num)}', style: text.bodyMedium),
          const SizedBox(height: 24),
          Text('Nộp tiền', style: text.titleMedium),
          Text(
            'Nộp tiền mặt tại viện, hoặc chuyển khoản với nội dung là mã nộp tiền; số tiền do gia đình tự nhập.',
            style: text.bodyMedium,
          ),
          if (instructions == null)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text('Liên hệ Kế toán để biết tài khoản nộp tiền.', style: text.bodyMedium),
            )
          else ...[
            const SizedBox(height: 12),
            Semantics(
              label: 'Mã QR chuyển khoản cho viện, không kèm số tiền',
              child: Align(
                alignment: Alignment.centerLeft,
                child: QrImageView(
                  data: instructions['vietQr'] as String,
                  size: 168,
                  // Nền trắng cố định để app ngân hàng quét được cả khi máy ở chế độ tối
                  backgroundColor: Colors.white,
                ),
              ),
            ),
            _row(text, 'Ngân hàng', instructions['bank'] as String),
            _row(text, 'Số tài khoản', instructions['accountNumber'] as String),
            _row(text, 'Chủ tài khoản', instructions['accountHolder'] as String),
            _row(text, 'Nội dung chuyển khoản', instructions['paymentCode'] as String),
          ],
          const SizedBox(height: 24),
          Text('Các khoản đã ghi nhận', style: text.titleMedium),
          if (transactions.isEmpty)
            Text('Chưa có khoản nộp, trừ nào.', style: text.bodyMedium)
          else
            for (final t in transactions)
              Container(
                constraints: const BoxConstraints(minHeight: 56),
                padding: const EdgeInsets.symmetric(vertical: 8),
                decoration: BoxDecoration(
                  border: Border(bottom: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(_types[t['type']] ?? '${t['type']}', style: text.bodyLarge),
                          Text(
                            '${formatDateTime(DateTime.parse(t['at'] as String))}'
                            '${t['ledger'] == 'DEPOSIT' ? ', sổ tiền cọc' : ''}',
                            style: text.bodySmall,
                          ),
                        ],
                      ),
                    ),
                    Text(money(t['amount'] as num), style: text.titleSmall),
                  ],
                ),
              ),
        ],
      ),
    );
  }
}
