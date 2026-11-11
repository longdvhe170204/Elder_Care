import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';

/// Tiếp nhận thuốc cung ứng (006 T065; FR-054, FR-055, BR-M07-24; Q-410): dòng thuốc đã mua chờ tiếp nhận của người cao
/// tuổi trong phạm vi (`GET /procurement-medication-lines`, không có giá); ghi Đạt hoặc Không đạt (Không đạt bắt buộc sai
/// lệch và lý do). Lệnh trực tuyến, gửi `Idempotency-Key`. Cùng chữ với màn web.
class SuppliedIntakeScreen extends StatefulWidget {
  const SuppliedIntakeScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<SuppliedIntakeScreen> createState() => _SuppliedIntakeScreenState();
}

class _SuppliedIntakeScreenState extends State<SuppliedIntakeScreen> {
  List<Map<String, dynamic>>? _lines;
  Map<int, String> _names = const {};
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final lines = (await widget.api.getList('/procurement-medication-lines')).cast<Map<String, dynamic>>();
      if (mounted) {
        setState(() {
          _lines = lines;
          _loadError = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
    try {
      final page = await widget.api.get('/residents');
      final names = <int, String>{
        for (final r in (page?['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>())
          r['id'] as int: (r['personalInfo'] as Map<String, dynamic>?)?['fullName'] as String? ?? '',
      };
      if (mounted) setState(() => _names = names);
    } on Exception {
      // Không đọc được tên thì hiện mã người cao tuổi.
    }
  }

  String _resident(Map<String, dynamic> line) {
    final name = _names[line['residentId']];
    return name == null || name.isEmpty ? 'Người cao tuổi #${line['residentId']}' : name;
  }

  Future<void> _open(Map<String, dynamic> line) async {
    final message = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => _IntakeSheet(api: widget.api, line: line, resident: _resident(line)),
    );
    if (message == null || !mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    if (_loadError != null) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    _loadError is ApiProblem && (_loadError! as ApiProblem).status == 403
                        ? 'Bạn không có quyền tiếp nhận thuốc cung ứng.'
                        : backendMessage(_loadError!),
                    style: text.bodyMedium,
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
                ],
              ),
            ),
          ),
        ],
      );
    }
    final lines = _lines;
    if (lines == null) return const Center(child: CircularProgressIndicator());
    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Tiếp nhận thuốc cung ứng', style: text.titleLarge),
          Text(
            'Thuốc viện mua theo yêu cầu mua sắm cho người cao tuổi bạn phụ trách. Đạt không tạo tồn kho, không tính phí, '
            'không đổi đơn.',
            style: text.bodySmall,
          ),
          const SizedBox(height: 12),
          if (lines.isEmpty)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text('Không có thuốc mua về đang chờ tiếp nhận.', style: text.bodyMedium),
              ),
            ),
          for (final line in lines) ...[
            Card(
              clipBehavior: Clip.antiAlias,
              child: InkWell(
                onTap: () => _open(line),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${line['medicationName']} ${line['strength']}, ${line['dosageForm']}',
                        style: text.titleSmall,
                      ),
                      Text(
                        '${_resident(line)} · Số lượng mua ${line['quantity']} ${line['unit']} · '
                        'Yêu cầu #${line['procurementRequestId']}',
                        style: text.bodySmall,
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12),
          ],
        ],
      ),
    );
  }
}

class _IntakeSheet extends StatefulWidget {
  const _IntakeSheet({required this.api, required this.line, required this.resident});

  final ApiClient api;
  final Map<String, dynamic> line;
  final String resident;

  @override
  State<_IntakeSheet> createState() => _IntakeSheetState();
}

class _IntakeSheetState extends State<_IntakeSheet> {
  late final _concentration = TextEditingController(text: widget.line['strength'] as String? ?? '');
  late final _dosageForm = TextEditingController(text: widget.line['dosageForm'] as String? ?? '');
  late final _quantity = TextEditingController(text: '${widget.line['quantity'] ?? ''}');
  late final _unit = TextEditingController(text: widget.line['unit'] as String? ?? '');
  final _expiry = TextEditingController();
  final _condition = TextEditingController();
  final _deliverer = TextEditingController();
  final _discrepancy = TextEditingController();
  final _reason = TextEditingController();
  final _key = _uuid();
  String _result = 'PASSED';
  bool _submitted = false;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    for (final c in [
      _concentration,
      _dosageForm,
      _quantity,
      _unit,
      _expiry,
      _condition,
      _deliverer,
      _discrepancy,
      _reason,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  bool get _failed => _result == 'NOT_PASSED';

  String? _required(TextEditingController c, String message) => _submitted && c.text.trim().isEmpty ? message : null;

  String? get _quantityError {
    if (!_submitted) return null;
    final value = num.tryParse(_quantity.text.trim());
    return value == null || value <= 0 ? 'Nhập số lượng lớn hơn 0' : null;
  }

  String? get _expiryError {
    if (!_submitted) return null;
    return DateTime.tryParse(_expiry.text.trim()) == null ? 'Nhập hạn dùng theo dạng 2027-11-01' : null;
  }

  Future<void> _save() async {
    setState(() => _submitted = true);
    final missing = [
      _required(_concentration, 'x'),
      _required(_dosageForm, 'x'),
      _quantityError,
      _required(_unit, 'x'),
      _expiryError,
      _required(_condition, 'x'),
      _required(_deliverer, 'x'),
      if (_failed) _required(_discrepancy, 'x'),
      if (_failed) _required(_reason, 'x'),
    ].any((e) => e != null);
    if (missing) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.post(
        '/supplied-medication-intakes',
        {
          'requestLineId': widget.line['id'],
          'concentration': _concentration.text.trim(),
          'dosageForm': _dosageForm.text.trim(),
          'receivedQuantity': num.parse(_quantity.text.trim()),
          'unit': _unit.text.trim(),
          'expiryDate': _expiry.text.trim(),
          'condition': _condition.text.trim(),
          'result': _result,
          if (_failed) 'discrepancy': _discrepancy.text.trim(),
          if (_failed) 'reason': _reason.text.trim(),
          'delivererName': _deliverer.text.trim(),
        },
        headers: {'Idempotency-Key': _key},
      );
      if (mounted) {
        Navigator.of(context).pop(
          _failed
              ? 'Đã ghi Không đạt; viện đã báo người lập yêu cầu và Phụ trách bộ phận. Giao bù thì ghi lần tiếp nhận mới.'
              : 'Đã ghi tiếp nhận Đạt.',
        );
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _field(TextEditingController c, String label, {String? error, TextInputType? keyboard, int lines = 1}) =>
      Padding(
        padding: const EdgeInsets.only(bottom: 12),
        child: TextField(
          controller: c,
          keyboardType: keyboard,
          maxLines: lines,
          decoration: InputDecoration(labelText: label, errorText: error),
        ),
      );

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final line = widget.line;
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 0, 16, 16 + MediaQuery.viewInsetsOf(context).bottom),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Tiếp nhận ${line['medicationName']}', style: text.titleLarge),
            Text(
              '${widget.resident}; mua ${line['quantity']} ${line['unit']}, yêu cầu #${line['procurementRequestId']}',
              style: text.bodySmall,
            ),
            const SizedBox(height: 12),
            _field(_concentration, 'Hàm lượng', error: _required(_concentration, 'Nhập hàm lượng')),
            _field(_dosageForm, 'Dạng bào chế', error: _required(_dosageForm, 'Nhập dạng bào chế')),
            _field(
              _quantity,
              'Số lượng nhận',
              error: _quantityError,
              keyboard: const TextInputType.numberWithOptions(decimal: true),
            ),
            _field(_unit, 'Đơn vị', error: _required(_unit, 'Nhập đơn vị')),
            _field(_expiry, 'Hạn dùng (yyyy-mm-dd)', error: _expiryError, keyboard: TextInputType.datetime),
            _field(_condition, 'Tình trạng bao bì', error: _required(_condition, 'Nhập tình trạng bao bì')),
            _field(_deliverer, 'Người giao', error: _required(_deliverer, 'Nhập người giao')),
            Text('Kết quả', style: text.titleSmall),
            RadioGroup<String>(
              groupValue: _result,
              onChanged: (v) => setState(() => _result = v ?? 'PASSED'),
              child: const Column(
                children: [
                  RadioListTile<String>(contentPadding: EdgeInsets.zero, value: 'PASSED', title: Text('Đạt')),
                  RadioListTile<String>(contentPadding: EdgeInsets.zero, value: 'NOT_PASSED', title: Text('Không đạt')),
                ],
              ),
            ),
            if (_failed) ...[
              _field(_discrepancy, 'Sai lệch', error: _required(_discrepancy, 'Nhập sai lệch'), lines: 2),
              _field(_reason, 'Lý do', error: _required(_reason, 'Nhập lý do'), lines: 2),
            ],
            if (_error != null) ...[
              Text(_error!, style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error)),
              const SizedBox(height: 12),
            ],
            FilledButton(onPressed: _busy ? null : _save, child: const Text('Ghi tiếp nhận')),
          ],
        ),
      ),
    );
  }
}

String _uuid() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
