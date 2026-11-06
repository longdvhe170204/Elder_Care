import 'dart:math';

import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/widgets/page_body.dart';
import 'belonging_labels.dart';
import 'photo_picker.dart';

/// Câu báo khi mất mạng: lệnh đồ gửi chỉ trực tuyến, không vào hàng đợi ngoại tuyến (FR-031; tasks 013 Notes).
const offlineMessage = 'Lệnh đồ gửi chỉ thực hiện được khi có mạng. Kiểm tra kết nối rồi lưu lại.';

String newIdempotencyKey() {
  final r = Random.secure();
  String hex(int n) => List.generate(n, (_) => r.nextInt(16).toRadixString(16)).join();
  return '${hex(8)}-${hex(4)}-4${hex(3)}-a${hex(3)}-${hex(12)}';
}

/// Giờ hiện tại theo giờ Việt Nam, ISO-8601 có +07:00 (mobile/README luật 4).
String nowVietnamIso() {
  final t = DateTime.now().toUtc().add(const Duration(hours: 7));
  String two(int v) => v.toString().padLeft(2, '0');
  return '${t.year}-${two(t.month)}-${two(t.day)}T${two(t.hour)}:${two(t.minute)}:${two(t.second)}+07:00';
}

/// Tiếp nhận đồ gửi trên app nhân viên (013 T011; US1; FR-003 → FR-008; cùng trường, thứ tự và chữ với form web
/// `IntakeForm`, DESIGN.md 13 điều 7). Ảnh chụp bằng máy ảnh rồi tải lên `POST /files`; đồ có giá trị bắt buộc ảnh;
/// giao ngay đồ thuộc loại cần đồng ý phải chụp bản ký của người đại diện. Thời điểm tiếp nhận là lúc lưu.
class IntakeScreen extends StatefulWidget {
  const IntakeScreen({
    super.key,
    required this.api,
    required this.residentId,
    required this.residentName,
    required this.types,
    required this.levels,
    required this.locations,
    required this.pickPhoto,
  });

  final ApiClient api;
  final int residentId;
  final String residentName;
  final List<Map<String, dynamic>> types;
  final List<Map<String, dynamic>> levels;
  final List<Map<String, dynamic>> locations;
  final PhotoPicker pickPhoto;

  @override
  State<IntakeScreen> createState() => _IntakeScreenState();
}

class _IntakeScreenState extends State<IntakeScreen> {
  final _description = TextEditingController();
  final _amount = TextEditingController();
  final _giverName = TextEditingController();
  final _giverDocument = TextEditingController();
  final _key = newIdempotencyKey();
  int? _typeId;
  int? _levelId;
  int? _locationId;
  String? _giverType;
  bool _giveNow = false;
  final List<(List<int>, String)> _photos = [];
  (List<int>, String)? _signedForm;
  Map<String, String> _errors = {};
  String? _failure;
  bool _busy = false;

  Map<String, dynamic>? get _type => widget.types.where((t) => t['id'] == _typeId).firstOrNull;

  @override
  void dispose() {
    _description.dispose();
    _amount.dispose();
    _giverName.dispose();
    _giverDocument.dispose();
    super.dispose();
  }

  Map<String, String> _validate() {
    final type = _type;
    final amount = num.tryParse(_amount.text.replaceAll(',', '.'));
    return {
      if (_typeId == null) 'type': 'Chọn loại đồ.',
      if (_description.text.trim().isEmpty) 'description': 'Nhập mô tả đồ.',
      if (amount == null || amount <= 0)
        'amount': type?['hasCashAmount'] == true ? 'Nhập số tiền lớn hơn 0.' : 'Nhập số lượng lớn hơn 0.',
      if (_levelId == null) 'level': 'Chọn mức tình trạng.',
      if (_giverType == null) 'giver': 'Chọn người giao.',
      if (_giverType == 'OTHER_PERSON' && _giverName.text.trim().isEmpty) 'giverName': 'Nhập họ tên người giao.',
      if (_giverType == 'OTHER_PERSON' && _giverDocument.text.trim().isEmpty)
        'giverDocument': 'Nhập giấy tờ của người giao.',
      if (!_giveNow && _locationId == null) 'location': 'Chọn vị trí lưu giữ.',
      if (type?['valuable'] == true && _photos.isEmpty) 'photos': 'Đồ có giá trị bắt buộc có ít nhất một ảnh.',
      if (_giveNow && type?['requiresConsentForUse'] == true && _signedForm == null)
        'signedForm': 'Loại đồ này cần bản ký đồng ý của người đại diện khi giao ngay.',
    };
  }

  Future<void> _save() async {
    final errors = _validate();
    setState(() {
      _errors = errors;
      _failure = null;
    });
    if (errors.isNotEmpty) return;
    setState(() => _busy = true);
    try {
      final photoIds = [for (final p in _photos) await widget.api.uploadFile(p.$1, p.$2)];
      final type = _type!;
      final consentNeeded = _giveNow && type['requiresConsentForUse'] == true;
      final signedFormId = consentNeeded ? await widget.api.uploadFile(_signedForm!.$1, _signedForm!.$2) : null;
      final amount = num.parse(_amount.text.replaceAll(',', '.'));
      await widget.api.requestJson(
        'POST',
        '/residents/${widget.residentId}/belongings',
        headers: {'Idempotency-Key': _key},
        body: {
          'typeId': _typeId,
          'description': _description.text.trim(),
          if (type['hasCashAmount'] == true) 'cashAmount': amount else 'quantity': amount,
          'condition': {'levelId': _levelId},
          'giver': {
            'type': _giverType,
            if (_giverName.text.trim().isNotEmpty) 'fullName': _giverName.text.trim(),
            if (_giverDocument.text.trim().isNotEmpty) 'idDocument': _giverDocument.text.trim(),
          },
          'at': nowVietnamIso(),
          'storageLocationId': ?_locationId,
          if (photoIds.isNotEmpty) 'photoFileIds': photoIds,
          if (_giveNow) 'giveForUseNow': true,
          'consentSignedFormFileId': ?signedFormId,
        },
      );
      if (mounted) Navigator.of(context).pop(true);
    } on ApiUnreachable {
      if (mounted) setState(() => _failure = offlineMessage);
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _select<T>(String key, String label, T? value, List<(T, String)> options, ValueChanged<T?> onChanged) =>
      DropdownButtonFormField<T>(
        key: Key(key),
        initialValue: value,
        decoration: InputDecoration(labelText: label, errorText: _errors[key.replaceFirst('intake-', '')]),
        items: [for (final (v, text) in options) DropdownMenuItem(value: v, child: Text(text))],
        onChanged: onChanged,
      );

  List<(int, String)> _active(List<Map<String, dynamic>> items) => [
    for (final i in items)
      if (i['status'] != 'INACTIVE') (i['id'] as int, i['name'] as String),
  ];

  @override
  Widget build(BuildContext context) {
    final type = _type;
    final cash = type?['hasCashAmount'] == true;
    return Scaffold(
      appBar: AppBar(title: Text('Tiếp nhận đồ của ${widget.residentName}')),
      body: PageBody(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _select('intake-type', 'Loại đồ', _typeId, _active(widget.types), (v) => setState(() => _typeId = v)),
              if (type?['valuable'] == true)
                const Padding(
                  padding: EdgeInsets.only(top: 4),
                  child: Text('Đồ có giá trị: bắt buộc ảnh, được kiểm kê định kỳ.'),
                ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('intake-description'),
                controller: _description,
                maxLength: 2000,
                decoration: InputDecoration(labelText: 'Mô tả', errorText: _errors['description']),
              ),
              TextField(
                key: const Key('intake-amount'),
                controller: _amount,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: InputDecoration(
                  labelText: cash ? 'Số tiền (đồng)' : 'Số lượng',
                  errorText: _errors['amount'],
                ),
              ),
              const SizedBox(height: 12),
              _select(
                'intake-level',
                'Mức tình trạng',
                _levelId,
                _active(widget.levels),
                (v) => setState(() => _levelId = v),
              ),
              const SizedBox(height: 12),
              _select('intake-giver', 'Người giao', _giverType, [
                for (final e in giverLabels.entries)
                  // người thân cần mã quan hệ (chưa có danh sách trên app); Nhân viên không là người giao (FR-003)
                  if (e.key == 'OTHER_PERSON' || e.key == 'RESIDENT') (e.key, e.value),
              ], (v) => setState(() => _giverType = v)),
              if (_giverType == 'OTHER_PERSON')
                TextField(
                  key: const Key('intake-giver-name'),
                  controller: _giverName,
                  decoration: InputDecoration(labelText: 'Họ tên người giao', errorText: _errors['giverName']),
                ),
              if (_giverType == 'OTHER_PERSON')
                TextField(
                  key: const Key('intake-giver-doc'),
                  controller: _giverDocument,
                  decoration: InputDecoration(labelText: 'Giấy tờ người giao', errorText: _errors['giverDocument']),
                ),
              const SizedBox(height: 12),
              _select(
                'intake-location',
                'Vị trí lưu giữ',
                _locationId,
                _active(widget.locations),
                (v) => setState(() => _locationId = v),
              ),
              const SizedBox(height: 12),
              OutlinedButton.icon(
                onPressed: _busy
                    ? null
                    : () async {
                        final photo = await widget.pickPhoto();
                        if (photo != null) setState(() => _photos.add(photo));
                      },
                icon: const Icon(LucideIcons.camera),
                label: const Text('Chụp ảnh'),
              ),
              Text(
                _photos.isEmpty ? 'Chưa có ảnh' : 'Đã chụp ${_photos.length} ảnh',
                style: _errors['photos'] == null ? null : TextStyle(color: Theme.of(context).colorScheme.error),
              ),
              if (_errors['photos'] != null)
                Text(_errors['photos']!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
              CheckboxListTile(
                contentPadding: EdgeInsets.zero,
                value: _giveNow,
                onChanged: (v) => setState(() => _giveNow = v ?? false),
                title: const Text('Giao ngay cho người cao tuổi sử dụng'),
              ),
              if (_giveNow && type?['requiresConsentForUse'] == true) ...[
                OutlinedButton.icon(
                  onPressed: () async {
                    final form = await widget.pickPhoto();
                    if (form != null) setState(() => _signedForm = form);
                  },
                  icon: const Icon(LucideIcons.fileSignature),
                  label: const Text('Chụp bản ký đồng ý cho tự giữ'),
                ),
                Text(_signedForm == null ? 'Chưa có bản ký' : 'Đã chụp bản ký'),
                if (_errors['signedForm'] != null)
                  Text(_errors['signedForm']!, style: TextStyle(color: Theme.of(context).colorScheme.error)),
              ],
              if (_failure != null)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: Semantics(liveRegion: true, child: Text(_failure!)),
                ),
              const SizedBox(height: 12),
              FilledButton(onPressed: _busy ? null : _save, child: const Text('Lưu tiếp nhận')),
            ],
          ),
        ),
      ),
    );
  }
}
