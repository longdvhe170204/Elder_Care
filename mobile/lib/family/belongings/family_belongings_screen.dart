import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';
import '../../common/widgets/status_pill.dart';

// Nhãn cùng chữ với web `features/belongings/belongingLabels.js`; mobile/README luật 1 cấm import từ `staff/`.
const _status = {
  'HELD': 'Đang giữ',
  'IN_USE': 'Đang được người cao tuổi sử dụng',
  'RETURNED': 'Đã trả',
  'LOST': 'Thất lạc',
  'DAMAGED': 'Hư hỏng',
  'DISPOSED': 'Đã xử lý',
};
const _kinds = {
  'INTAKE': 'Tiếp nhận',
  'GIVE_FOR_USE': 'Giao sử dụng',
  'TAKE_BACK': 'Thu lại',
  'TRANSFER_CUSTODY': 'Chuyển giữ',
  'RETURN': 'Trả',
  'REPORT_LOST': 'Báo thất lạc',
  'RECORD_DAMAGE': 'Ghi hư hỏng',
  'FOUND': 'Tìm thấy',
  'DISPOSAL': 'Xử lý đồ',
};

/// Đồ gửi trên app người thân (013 T028; US6; FR-030, BR-M12-09; api-013 `GET /family-portal/residents/{residentId}/
/// belongings`): chỉ xem, cùng thứ tự và chữ với cổng web (DESIGN.md 2.3, 13 điều 7); không vị trí lưu giữ, không tên
/// nhân viên. Backend kiểm quyền mỗi lần (người đại diện, người được phép đón). Gắn vào thanh dưới của phần người thân
/// khi có (G4, `impl/001-followup`).
class FamilyBelongingsScreen extends StatefulWidget {
  const FamilyBelongingsScreen({super.key, required this.api, required this.residentId});

  final ApiClient api;
  final int residentId;

  @override
  State<FamilyBelongingsScreen> createState() => _FamilyBelongingsScreenState();
}

class _FamilyBelongingsScreenState extends State<FamilyBelongingsScreen> {
  List<Map<String, dynamic>>? _items;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final list = await widget.api.getList('/family-portal/residents/${widget.residentId}/belongings');
      if (mounted) setState(() => _items = list.cast<Map<String, dynamic>>());
    } on ApiProblem catch (e) {
      if (!mounted) return;
      setState(
        () => _error = e.status == 403
            ? 'Chỉ người đại diện và người được phép đón đang hiệu lực xem được đồ gửi.'
            : backendMessage(e),
      );
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  String _amount(Map<String, dynamic> b) {
    final cash = b['cashAmount'] as num?;
    if (cash != null) return '$cash đồng';
    final quantity = b['quantity'] as num?;
    return quantity == null ? '' : 'Số lượng $quantity';
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final items = _items;
    return PageBody(
      child: _error != null
          ? Text(_error!)
          : items == null
          ? const Center(child: CircularProgressIndicator())
          : items.isEmpty
          ? const Text('Chưa có đồ gửi viện giữ hộ.')
          : ListView(
              children: [
                for (final b in items)
                  Container(
                    constraints: const BoxConstraints(minHeight: 64),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surface,
                      border: Border(bottom: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(child: Text(b['description'] as String, style: text.titleSmall)),
                            StatusPill(
                              label: _status[b['status']] ?? '${b['status']}',
                              tone: b['status'] == 'LOST' || b['status'] == 'DAMAGED'
                                  ? StatusTone.attention
                                  : StatusTone.neutral,
                            ),
                          ],
                        ),
                        Text(_amount(b), style: text.bodySmall),
                        for (final h in (b['history'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>())
                          Text(
                            '${formatDateTime(DateTime.parse(h['occurredAt'] as String))} '
                            '${_kinds[h['kind']] ?? h['kind']}'
                            '${h['conditionDescription'] == null ? '' : ', ${h['conditionDescription']}'}',
                            style: text.bodySmall,
                          ),
                      ],
                    ),
                  ),
              ],
            ),
    );
  }
}
