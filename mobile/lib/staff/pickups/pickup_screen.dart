import 'dart:convert';
import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/theme/app_theme.dart';

/// Nhãn của api-012 tag pickups, cùng chữ với màn web `features/relatives/pickups/pickupLabels.js`.
const pickupTypeLabels = {
  'TEMPORARY_ABSENCE': 'Tạm vắng',
  'FAMILY_OUTING': 'Đi chơi với gia đình',
  'DAY_CARE_DEPARTURE': 'Bán trú về',
  'SELF_RETURN': 'Tự về',
  'INSTITUTE_RETURN': 'Viện đưa về',
  'RESIDENCY_END': 'Kết thúc lưu trú',
};

const _blockReasonLabels = {
  'NOT_ON_LIST': 'Người đón không thuộc danh sách được phép đón và không có ngoại lệ Hiệu lực.',
  'ID_DOCUMENT_MISMATCH': 'Giấy tờ xuất trình không khớp số đã đăng ký.',
  'EXCEPTION_EXPIRED': 'Yêu cầu ngoại lệ đã hết hạn.',
  'SELF_RETURN_OFF': 'Người cao tuổi không có dấu được tự về đang bật.',
};

const _relationshipLabels = {
  'SPOUSE': 'Vợ/chồng',
  'CHILD': 'Con',
  'GRANDCHILD': 'Cháu',
  'SIBLING': 'Anh/chị/em',
  'OTHER': 'Khác',
};

/// Nhãn ô nhân viên theo loại đón (FR-026: nhân viên tiễn với Tự về, người đưa về với Viện đưa về).
String handoverLabel(String? type) => switch (type) {
  'SELF_RETURN' => 'Nhân viên tiễn',
  'INSTITUTE_RETURN' => 'Người đưa về',
  _ => 'Nhân viên bàn giao',
};

const _exception = -1;

/// Ghi đón trên app nhân viên (012 T021; US2, FR-021 bước 1 → 5, FR-023, FR-026). Chỉ trực tuyến (FR-028): không vào hàng
/// đợi ngoại tuyến; mất mạng thì báo và giữ phần đang nhập, gửi lại cùng nội dung dùng cùng khóa idempotency. Chọn người
/// cao tuổi, loại đón, người đón trong danh sách được phép đón tại thời điểm (hoặc theo mã ngoại lệ), giấy tờ xuất
/// trình, nhân viên bàn giao. Backend kiểm danh sách, giấy tờ, quyền lệnh nguồn; không đạt thì đã ghi lần chặn và trả
/// 409 kèm `blockReason`. Không chọn sẵn giá trị nào.
class PickupScreen extends StatefulWidget {
  const PickupScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<PickupScreen> createState() => _PickupScreenState();
}

class _PickupScreenState extends State<PickupScreen> {
  final _search = TextEditingController();
  List<Map<String, dynamic>>? _residents;
  Map<String, dynamic>? _resident;
  String? _loadError;

  @override
  void initState() {
    super.initState();
    _find();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _find() async {
    final q = _search.text.trim();
    try {
      final page = await widget.api.get(q.isEmpty ? '/residents' : '/residents?q=${Uri.encodeQueryComponent(q)}');
      final items = ((page?['items'] as List?) ?? const []).cast<Map<String, dynamic>>();
      if (mounted) {
        setState(() {
          _residents = items;
          _loadError = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = backendMessage(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    final resident = _resident;
    if (resident != null) {
      return _PickupForm(
        key: ValueKey(resident['id']),
        api: widget.api,
        residentId: resident['id'] as int,
        residentName: (resident['personalInfo'] as Map)['fullName'] as String,
        onBack: () => setState(() => _resident = null),
      );
    }
    final text = Theme.of(context).textTheme;
    final residents = _residents;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Chọn người cao tuổi để ghi đón', style: text.titleMedium),
        const SizedBox(height: 12),
        TextField(
          controller: _search,
          textInputAction: TextInputAction.search,
          onSubmitted: (_) => _find(),
          decoration: const InputDecoration(labelText: 'Tìm theo họ tên hoặc mã hồ sơ'),
        ),
        const SizedBox(height: 8),
        if (_loadError != null) ...[
          Text(_loadError!, style: text.bodyMedium),
          const SizedBox(height: 8),
          OutlinedButton(onPressed: _find, child: const Text('Tải lại')),
        ] else if (residents == null)
          const Center(child: CircularProgressIndicator())
        else if (residents.isEmpty)
          Text('Không có người cao tuổi khớp.', style: text.bodyMedium)
        else
          for (final r in residents)
            ListTile(
              minTileHeight: 64,
              title: Text((r['personalInfo'] as Map)['fullName'] as String),
              subtitle: Text('${r['profileCode']}'),
              onTap: () => setState(() => _resident = r),
            ),
      ],
    );
  }
}

class _PickupForm extends StatefulWidget {
  const _PickupForm({
    super.key,
    required this.api,
    required this.residentId,
    required this.residentName,
    required this.onBack,
  });

  final ApiClient api;
  final int residentId;
  final String residentName;
  final VoidCallback onBack;

  @override
  State<_PickupForm> createState() => _PickupFormState();
}

class _PickupFormState extends State<_PickupForm> {
  final _exceptionId = TextEditingController();
  final _docType = TextEditingController();
  final _docNumber = TextEditingController();
  final _keys = <String, String>{};
  List<Map<String, dynamic>>? _pickupList;
  List<Map<String, dynamic>> _staff = const [];
  String? _type;
  int? _picker;
  int? _staffId;
  bool _submitted = false;
  bool _busy = false;
  String? _error;
  String? _blockReason;
  String? _done;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _exceptionId.dispose();
    _docType.dispose();
    _docNumber.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final list = (await widget.api.getList(
        '/residents/${widget.residentId}/pickup-list',
      )).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _pickupList = list);
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _pickupList = const [];
          _error = backendMessage(e);
        });
      }
    }
    try {
      // Nhân viên đang làm việc mà người ghi xem được (api-008: nhân viên chỉ thấy người cùng ca)
      final staff = (await widget.api.getList('/staff?status=WORKING')).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _staff = staff);
    } on Exception {
      // danh sách trống: ô nhân viên báo chưa chọn được
    }
  }

  bool get _selfReturn => _type == 'SELF_RETURN';
  bool get _byException => _picker == _exception;

  Map<String, String?> get _errors => {
    'type': _type == null ? 'Chọn loại đón' : null,
    'picker': !_selfReturn && _picker == null ? 'Chọn người đón' : null,
    'exception': !_selfReturn && _byException && int.tryParse(_exceptionId.text.trim()) == null
        ? 'Nhập mã yêu cầu ngoại lệ'
        : null,
    'document': !_selfReturn && _docNumber.text.trim().isEmpty ? 'Nhập số giấy tờ xuất trình' : null,
    'staff': _staffId == null ? 'Chọn ${handoverLabel(_type).toLowerCase()}' : null,
  };

  String? _show(String name) => _submitted ? _errors[name] : null;

  Future<void> _submit() async {
    setState(() {
      _submitted = true;
      _done = null;
    });
    if (_errors.values.any((e) => e != null) || _busy) return;
    final body = <String, dynamic>{'type': _type};
    if (!_selfReturn) {
      if (_byException) {
        body['exceptionId'] = int.parse(_exceptionId.text.trim());
      } else {
        body['familyMemberId'] = _picker;
      }
      final docType = _docType.text.trim();
      body['presentedIdDocument'] = {if (docType.isNotEmpty) 'type': docType, 'number': _docNumber.text.trim()};
    }
    body['handoverStaffId'] = _staffId;
    final signature = jsonEncode(body);
    final key = _keys.putIfAbsent(signature, _uuid);
    setState(() {
      _busy = true;
      _error = null;
      _blockReason = null;
    });
    try {
      await widget.api.post('/residents/${widget.residentId}/pickups', body, headers: {'Idempotency-Key': key});
      _keys.remove(signature);
      if (mounted) setState(() => _done = 'Đã ghi đón');
    } on ApiProblem catch (e) {
      if (mounted) {
        setState(() {
          _error = backendMessage(e);
          _blockReason = e.properties['blockReason'] as String?;
        });
      }
    } on ApiUnreachable {
      if (mounted) {
        setState(() => _error = 'Ghi đón cần kết nối mạng, không lưu để gửi sau. Thông tin đang nhập được giữ lại.');
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _groupError(String? message, ThemeData theme) => message == null
      ? const SizedBox.shrink()
      : Padding(
          padding: const EdgeInsets.only(left: 16, top: 4),
          child: Text(message, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
        );

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tones = theme.extension<Tones>()!;
    final pickupList = _pickupList;
    // Cả form dựng một lần (không dựng lười như ListView) để focus, cuộn tới ô lỗi không mất ô đã nhập
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(onPressed: widget.onBack, child: const Text('Chọn người cao tuổi khác')),
          ),
          Text('Ghi đón cho ${widget.residentName}', style: theme.textTheme.titleLarge),
          const SizedBox(height: 4),
          Text(
            'Đối chiếu người đón với danh sách được phép đón và giấy tờ xuất trình. Cần kết nối mạng; thời điểm ghi theo giờ hệ thống.',
            style: theme.textTheme.bodySmall,
          ),
          const SizedBox(height: 16),
          Text('Loại đón', style: theme.textTheme.titleSmall),
          RadioGroup<String>(
            groupValue: _type,
            onChanged: (v) => setState(() => _type = v),
            child: Column(
              children: [
                for (final e in pickupTypeLabels.entries)
                  RadioListTile<String>(value: e.key, title: Text(e.value), enabled: !_busy),
              ],
            ),
          ),
          _groupError(_show('type'), theme),
          if (!_selfReturn) ...[
            const SizedBox(height: 12),
            Text('Người đón', style: theme.textTheme.titleSmall),
            if (pickupList == null)
              const Center(child: CircularProgressIndicator())
            else
              RadioGroup<int>(
                groupValue: _picker,
                onChanged: (v) => setState(() => _picker = v),
                child: Column(
                  children: [
                    if (pickupList.isEmpty) const ListTile(title: Text('Danh sách được phép đón đang trống.')),
                    for (final p in pickupList)
                      RadioListTile<int>(
                        value: p['familyMemberId'] as int,
                        title: Text(p['fullName'] as String),
                        subtitle: Text(_relationshipLabels[p['relationship']] ?? '${p['relationship']}'),
                        enabled: !_busy,
                      ),
                    RadioListTile<int>(
                      value: _exception,
                      title: const Text('Người đón theo ngoại lệ'),
                      enabled: !_busy,
                    ),
                  ],
                ),
              ),
            _groupError(_show('picker'), theme),
            if (_byException) ...[
              const SizedBox(height: 8),
              TextField(
                controller: _exceptionId,
                keyboardType: TextInputType.number,
                onChanged: (_) => setState(() {}),
                decoration: InputDecoration(labelText: 'Mã yêu cầu ngoại lệ', errorText: _show('exception')),
              ),
            ],
            const SizedBox(height: 12),
            TextField(
              controller: _docType,
              decoration: const InputDecoration(labelText: 'Loại giấy tờ xuất trình'),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _docNumber,
              onChanged: (_) => setState(() {}),
              decoration: InputDecoration(labelText: 'Số giấy tờ xuất trình', errorText: _show('document')),
            ),
          ],
          const SizedBox(height: 12),
          DropdownButtonFormField<int>(
            initialValue: _staffId,
            isExpanded: true,
            decoration: InputDecoration(labelText: handoverLabel(_type), errorText: _show('staff')),
            items: [
              for (final s in _staff) DropdownMenuItem(value: s['id'] as int, child: Text(s['fullName'] as String)),
            ],
            onChanged: _busy ? null : (v) => setState(() => _staffId = v),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Semantics(
              liveRegion: true,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(_error!, style: theme.textTheme.bodyMedium?.copyWith(color: tones.attentionInk)),
                  if (_blockReason != null) ...[
                    Text(
                      _blockReasonLabels[_blockReason] ?? _blockReason!,
                      style: theme.textTheme.bodyMedium?.copyWith(color: tones.attentionInk),
                    ),
                    Text(
                      'Đã ghi lần chặn đón, Trưởng tầng được báo. Người ngoài danh sách cần yêu cầu ngoại lệ.',
                      style: theme.textTheme.bodySmall,
                    ),
                  ],
                ],
              ),
            ),
          ],
          if (_done != null) ...[
            const SizedBox(height: 12),
            Semantics(liveRegion: true, child: Text(_done!, style: theme.textTheme.bodyMedium)),
          ],
          const SizedBox(height: 16),
          FilledButton(onPressed: _busy ? null : _submit, child: const Text('Ghi bản ghi đón')),
        ],
      ),
    );
  }
}

/// Khóa idempotency dạng UUID v4 cho một nội dung lệnh ghi đón.
String _uuid() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
