import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/status_pill.dart';

const _contractStatus = {
  'DRAFT': ('Nháp', StatusTone.neutral),
  'PENDING_SIGNATURE': ('Chờ ký', StatusTone.attention),
  'ACTIVE': ('Hiệu lực', StatusTone.settled),
  'ENDED': ('Kết thúc', StatusTone.neutral),
  'TERMINATED': ('Chấm dứt', StatusTone.neutral),
  'CANCELLED': ('Đã hủy', StatusTone.neutral),
};

const _residenceTypes = {
  'LONG_TERM_RESIDENTIAL': 'Nội trú dài hạn',
  'SHORT_TERM_RESIDENTIAL': 'Nội trú ngắn ngày',
  'DAY_CARE': 'Bán trú',
};

const _depositStatus = {
  'NOT_SATISFIED': 'Chưa đặt cọc',
  'SATISFIED': 'Đã đặt cọc',
  'NOT_REQUIRED': 'Không yêu cầu đặt cọc',
};

const _addendumStatus = {
  'PENDING_EFFECTIVE': 'Chờ hiệu lực',
  'APPLIED': 'Đã áp dụng',
  'NOT_APPLICABLE': 'Áp dụng không thành',
};

String _money(Object? value) {
  final digits = '${value ?? 0}';
  final buffer = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) buffer.write('.');
    buffer.write(digits[i]);
  }
  return '$buffer đ';
}

/// Hợp đồng của người cao tuổi mà người đại diện đại diện (004 T038; US3, FR-028 → FR-031): hợp đồng gốc, phụ lục và
/// trạng thái đặt cọc, chỉ xem (Phụ lục 27: Người thân X). Chỉ trực tuyến. Trường tài chính do backend bỏ theo quyền.
class ContractScreen extends StatefulWidget {
  const ContractScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<ContractScreen> createState() => _ContractScreenState();
}

class _ContractScreenState extends State<ContractScreen> {
  List<Map<String, dynamic>>? _contracts;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = (await widget.api.getList('/contracts')).cast<Map<String, dynamic>>();
      if (!mounted) return;
      setState(() {
        _contracts = items;
        _error = null;
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    if (_error != null) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(backendMessage(_error!), style: text.bodyMedium),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: _load, child: const Text('Tải lại')),
          ],
        ),
      );
    }
    final contracts = _contracts;
    if (contracts == null) return const Center(child: CircularProgressIndicator());
    if (contracts.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Text('Chưa có hợp đồng nào.', style: text.bodyMedium),
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: contracts.length,
      separatorBuilder: (_, _) => const SizedBox(height: 8),
      itemBuilder: (_, i) => _ContractCard(key: ValueKey(contracts[i]['id']), api: widget.api, contract: contracts[i]),
    );
  }
}

class _ContractCard extends StatefulWidget {
  const _ContractCard({super.key, required this.api, required this.contract});

  final ApiClient api;
  final Map<String, dynamic> contract;

  @override
  State<_ContractCard> createState() => _ContractCardState();
}

class _ContractCardState extends State<_ContractCard> {
  bool _open = false;
  Map<String, dynamic>? _deposit;
  List<Map<String, dynamic>>? _addenda;
  String? _error;

  Future<void> _toggle() async {
    setState(() => _open = !_open);
    if (!_open || _addenda != null) return;
    final id = widget.contract['id'];
    try {
      final deposit = await widget.api.get('/contracts/$id/deposit');
      final addenda = (await widget.api.getList('/contracts/$id/addenda')).cast<Map<String, dynamic>>();
      if (mounted) {
        setState(() {
          _deposit = deposit;
          _addenda = addenda;
          _error = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final contract = widget.contract;
    final (statusLabel, tone) = _contractStatus[contract['status']] ?? ('${contract['status']}', StatusTone.neutral);
    final end = contract['endDate'] as String?;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text('Hợp đồng ${contract['contractNumber'] ?? ''}', style: theme.textTheme.titleSmall),
                ),
                StatusPill(label: statusLabel, tone: tone),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              '${_residenceTypes[contract['residenceType']] ?? contract['residenceType']}, '
              'từ ${formatDate(contract['startDate'] as String)}${end == null ? '' : ' đến ${formatDate(end)}'}',
              style: theme.textTheme.bodyMedium,
            ),
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerLeft,
              child: OutlinedButton(onPressed: _toggle, child: Text(_open ? 'Ẩn chi tiết' : 'Xem phụ lục, đặt cọc')),
            ),
            if (_open && _error != null) Text(_error!, style: theme.textTheme.bodySmall),
            if (_open && _addenda != null) ...[
              const SizedBox(height: 12),
              Text('Đặt cọc', style: theme.textTheme.titleSmall),
              Text(_depositStatus[_deposit?['status']] ?? '${_deposit?['status']}', style: theme.textTheme.bodyMedium),
              if (_deposit?['requiredAmount'] != null)
                Text('Khoản cần đặt cọc: ${_money(_deposit!['requiredAmount'])}', style: theme.textTheme.bodyMedium),
              const SizedBox(height: 12),
              Text('Phụ lục', style: theme.textTheme.titleSmall),
              if (_addenda!.isEmpty) Text('Chưa có phụ lục.', style: theme.textTheme.bodyMedium),
              for (final a in _addenda!)
                Text(
                  'Hiệu lực ${formatDate(a['effectiveDate'] as String)}, ${_addendumStatus[a['status']] ?? a['status']}',
                  style: theme.textTheme.bodyMedium,
                ),
            ],
          ],
        ),
      ),
    );
  }
}
