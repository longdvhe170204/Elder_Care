import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';

const _careLevels = {'BASIC': 'Cơ bản', 'REGULAR': 'Thường xuyên', 'SPECIAL': 'Đặc biệt'};
const _residenceTypes = {
  'LONG_TERM_RESIDENTIAL': 'Nội trú dài hạn',
  'SHORT_TERM_RESIDENTIAL': 'Nội trú ngắn ngày',
  'DAY_CARE': 'Bán trú',
};

/// Người đại diện gửi yêu cầu thay đổi lưu trú (004 T047; US5, FR-037, FR-039): đổi mức chăm sóc hoặc loại lưu trú. Yêu cầu
/// được tạo ở Nháp, Hành chính được báo để hoàn thiện và gửi duyệt; người đại diện không trực tiếp thay đổi. Các loại khác
/// (dịch vụ, giường, lịch đến, thời hạn) làm tại quầy. Chỉ trực tuyến.
class ChangeRequestScreen extends StatefulWidget {
  const ChangeRequestScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<ChangeRequestScreen> createState() => _ChangeRequestScreenState();
}

class _ChangeRequestScreenState extends State<ChangeRequestScreen> {
  final _date = TextEditingController();
  final _reason = TextEditingController();
  final _key = _uuid();
  List<Map<String, dynamic>>? _contracts;
  int? _contractId;
  String? _type;
  String? _value;
  bool _submitted = false;
  bool _busy = false;
  String? _error;
  String? _done;
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _date.dispose();
    _reason.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final items = (await widget.api.getList('/contracts?status=ACTIVE')).cast<Map<String, dynamic>>();
      if (!mounted) return;
      setState(() {
        _contracts = items.where((c) => c['status'] == 'ACTIVE').toList();
        _loadError = null;
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  // Ngày hiệu lực nhập dd/MM/yyyy, gửi yyyy-MM-dd; sai định dạng thì null
  String? _isoDate() {
    final parts = _date.text.trim().split('/');
    if (parts.length != 3) return null;
    final day = int.tryParse(parts[0]);
    final month = int.tryParse(parts[1]);
    final year = int.tryParse(parts[2]);
    if (day == null || month == null || year == null) return null;
    String two(int n) => n.toString().padLeft(2, '0');
    return '$year-${two(month)}-${two(day)}';
  }

  Future<void> _send() async {
    setState(() => _submitted = true);
    final iso = _isoDate();
    if (_contractId == null || _type == null || _value == null || iso == null || _reason.text.trim().isEmpty) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.post(
        '/residency-change-requests',
        {
          'contractId': _contractId,
          'changeTypes': [_type],
          'newValue': {_type == 'CARE_LEVEL' ? 'careLevel' : 'residenceType': _value},
          'requestedEffectiveDate': iso,
          'reason': _reason.text.trim(),
        },
        headers: {'Idempotency-Key': _key},
      );
      if (mounted) setState(() => _done = 'Đã gửi yêu cầu, Hành chính sẽ hoàn thiện và gửi duyệt.');
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    if (_loadError != null) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(backendMessage(_loadError!), style: text.bodyMedium),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: _load, child: const Text('Tải lại')),
          ],
        ),
      );
    }
    if (_contracts == null) return const Center(child: CircularProgressIndicator());
    if (_done != null) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Semantics(liveRegion: true, child: Text(_done!, style: text.bodyMedium)),
      );
    }
    final values = _type == 'CARE_LEVEL' ? _careLevels : _residenceTypes;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Yêu cầu thay đổi lưu trú', style: text.titleMedium),
        const SizedBox(height: 4),
        Text('Yêu cầu được lập ở Nháp, Hành chính hoàn thiện và gửi Quản lý viện duyệt.', style: text.bodySmall),
        const SizedBox(height: 16),
        DropdownButtonFormField<int>(
          initialValue: _contractId,
          decoration: InputDecoration(
            labelText: 'Hợp đồng',
            errorText: _submitted && _contractId == null ? 'Chọn hợp đồng' : null,
          ),
          items: [
            for (final c in _contracts!)
              DropdownMenuItem(value: c['id'] as int, child: Text('Hợp đồng ${c['contractNumber']}')),
          ],
          onChanged: (v) => setState(() => _contractId = v),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          initialValue: _type,
          decoration: InputDecoration(
            labelText: 'Nội dung thay đổi',
            errorText: _submitted && _type == null ? 'Chọn nội dung thay đổi' : null,
          ),
          items: const [
            DropdownMenuItem(value: 'CARE_LEVEL', child: Text('Mức chăm sóc')),
            DropdownMenuItem(value: 'RESIDENCE_TYPE', child: Text('Loại lưu trú')),
          ],
          onChanged: (v) => setState(() {
            _type = v;
            _value = null;
          }),
        ),
        if (_type != null) ...[
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _value,
            decoration: InputDecoration(
              labelText: 'Giá trị mới',
              errorText: _submitted && _value == null ? 'Chọn giá trị mới' : null,
            ),
            items: [for (final e in values.entries) DropdownMenuItem(value: e.key, child: Text(e.value))],
            onChanged: (v) => setState(() => _value = v),
          ),
        ],
        const SizedBox(height: 12),
        TextField(
          controller: _date,
          keyboardType: TextInputType.datetime,
          decoration: InputDecoration(
            labelText: 'Ngày hiệu lực mong muốn (dd/MM/yyyy)',
            errorText: _submitted && _isoDate() == null ? 'Nhập ngày hiệu lực mong muốn' : null,
          ),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _reason,
          maxLength: 2000,
          minLines: 2,
          maxLines: 4,
          decoration: InputDecoration(
            labelText: 'Lý do',
            errorText: _submitted && _reason.text.trim().isEmpty ? 'Nhập lý do' : null,
          ),
        ),
        if (_error != null) ...[
          const SizedBox(height: 8),
          Semantics(liveRegion: true, child: Text(_error!, style: text.bodySmall)),
        ],
        const SizedBox(height: 12),
        FilledButton(onPressed: _busy ? null : _send, child: const Text('Gửi yêu cầu')),
      ],
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
