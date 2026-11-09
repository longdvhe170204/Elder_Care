import 'package:flutter/material.dart';
import '../../../common/api/api_client.dart';
import '../../../common/api/problem_message.dart';
import '../../../common/format/date_time.dart';
import '../../../common/widgets/page_body.dart';

/// FR-037/038: A nối màn vào cổng; mọi dữ liệu và quyền do backend cung cấp.
class FamilyCostsScreen extends StatefulWidget {
  const FamilyCostsScreen({super.key, required this.api, required this.residentId});
  final ApiClient api;
  final int residentId;
  @override
  State<FamilyCostsScreen> createState() => _FamilyCostsScreenState();
}

class _FamilyCostsScreenState extends State<FamilyCostsScreen> {
  List<Map<String, dynamic>>? _statements;
  Map<String, dynamic>? _summary;
  DateTimeRange? _range;
  String? _error;
  bool _denied = false;
  bool _busy = false;
  int _generation = 0;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void didUpdateWidget(covariant FamilyCostsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.residentId != widget.residentId) {
      _generation++;
      _statements = null;
      _summary = null;
      _range = null;
      _error = null;
      _denied = false;
      _busy = false;
      _load();
    }
  }

  void _failed(Object error) {
    _error = backendMessage(error);
    if (error is ApiProblem && error.status == 403) {
      _denied = true;
      _statements = null;
      _summary = null;
    }
  }

  Future<void> _load() async {
    final generation = _generation;
    try {
      final rows = await widget.api.getList('/residents/${widget.residentId}/costs/statements');
      if (mounted && generation == _generation) {
        setState(() {
          _statements = rows.cast<Map<String, dynamic>>();
          _error = null;
        });
      }
    } catch (error) {
      if (mounted && generation == _generation) setState(() => _failed(error));
    }
  }

  String _iso(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';

  Future<void> _chooseRange() async {
    final selected = await showDateRangePicker(
      context: context,
      firstDate: DateTime(1900),
      lastDate: DateTime(2200),
      initialDateRange: _range,
      helpText: 'Khoảng ngày trong kỳ đang mở',
      saveText: 'Chọn',
    );
    if (mounted && selected != null) {
      setState(() {
        _range = selected;
        _summary = null;
      });
    }
  }

  Future<void> _provisional() async {
    if (_busy) return;
    final generation = _generation;
    setState(() {
      _busy = true;
      _error = null;
      _summary = null;
    });
    final query = _range == null ? '' : '?from=${_iso(_range!.start)}&to=${_iso(_range!.end)}';
    try {
      final data = await widget.api.get('/residents/${widget.residentId}/costs/provisional$query');
      if (mounted && generation == _generation) setState(() => _summary = data);
    } catch (error) {
      if (mounted && generation == _generation) setState(() => _failed(error));
    } finally {
      if (mounted && generation == _generation) setState(() => _busy = false);
    }
  }

  static const _types = {
    'RESIDENCY_FEE': 'Phí lưu trú',
    'DAY_CARE_SESSION_FEE': 'Phí buổi bán trú',
    'SERVICE': 'Dịch vụ',
    'CONSUMABLE': 'Vật phẩm tiêu hao',
    'ACTIVITY': 'Hoạt động',
    'MEDICATION': 'Thuốc',
    'FAMILY_OVERNIGHT_STAY': 'Người thân ở lại',
    'FAMILY_MEAL': 'Suất ăn người thân',
    'PURCHASE_ASSISTANCE': 'Mua hộ',
    'OTHER': 'Khoản khác',
  };
  String _money(Object? value) => value == null ? 'Chưa có số tiền' : '$value đồng';
  List<Widget> _totals(Object? raw) => (raw as List<dynamic>? ?? []).map((value) {
    final row = value as Map<String, dynamic>;
    return ListTile(title: Text(_types[row['type']] ?? 'Chi phí'), trailing: Text(_money(row['total'])));
  }).toList();

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Chi phí')),
    body: PageBody(
      child: _denied
          ? const Text('Bạn không có quyền xem chi phí của người cao tuổi này.')
          : ListView(
              children: [
                if (_error != null) ...[
                  Text(_error!, semanticsLabel: _error),
                  TextButton(onPressed: _load, child: const Text('Tải lại')),
                ],
                Text('Tạm tính, chưa chốt', style: Theme.of(context).textTheme.titleMedium),
                const Text('Để trống khoảng ngày để xem từ đầu tháng tới hôm nay.'),
                TextButton(
                  onPressed: _busy ? null : _chooseRange,
                  child: Text(
                    _range == null
                        ? 'Chọn khoảng ngày'
                        : '${formatDate(_iso(_range!.start))} – ${formatDate(_iso(_range!.end))}',
                  ),
                ),
                FilledButton(onPressed: _busy ? null : _provisional, child: Text(_busy ? 'Đang tải…' : 'Xem tạm tính')),
                if (_summary != null) ...[
                  ..._totals(_summary!['totalsByType']),
                  if (_summary!['hasUnpricedItems'] == true) const Text('Còn khoản chưa có đơn giá'),
                ],
                const Divider(),
                Text('Bảng đã chốt', style: Theme.of(context).textTheme.titleMedium),
                if (_statements == null && _error == null) const Center(child: CircularProgressIndicator()),
                if (_statements?.isEmpty == true) const Text('Chưa có bảng chi phí đã chốt.'),
                for (final row in _statements ?? <Map<String, dynamic>>[])
                  ExpansionTile(
                    title: Text(
                      'Kỳ ${(row['period'] as String).substring(5)}/${(row['period'] as String).substring(0, 4)} · ${_money(row['total'])}',
                    ),
                    subtitle: Text(
                      row['statementType'] == 'SUPPLEMENTARY'
                          ? 'Bổ sung'
                          : row['finalPeriod'] == true
                          ? 'Kỳ cuối'
                          : 'Bảng thường',
                    ),
                    children: [
                      ..._totals(row['totalsByType']),
                      for (final raw in row['items'] as List<dynamic>? ?? []) _item(raw as Map<String, dynamic>),
                    ],
                  ),
              ],
            ),
    ),
  );
  Widget _item(Map<String, dynamic> item) => ListTile(
    title: Text(item['medicationName'] as String? ?? item['description'] as String? ?? 'Khoản chi phí'),
    subtitle: Text(
      '${formatDate(item['incurredOn'] as String)} · ${item['quantity']} ${item['unit']} · Đơn giá ${_money(item['unitPrice'])}'
      '${item['originalPeriod'] == null ? '' : '\nĐiều chỉnh kỳ ${item['originalPeriod']}'}',
    ),
    trailing: Text(_money(item['amount'])),
  );
}
