import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/notifications/notification_api.dart' show newIdempotencyKey;

/// T041b/FR-020: lệnh trực tuyến theo hợp đồng008; backend kiểm quyền và cửa sổ ghi vắng.
class ShiftAbsenceSheet extends StatefulWidget {
  const ShiftAbsenceSheet({super.key, required this.api, required this.shift, required this.names});

  final ApiClient api;
  final Map<String, dynamic> shift;
  final Map<int, String> names;

  @override
  State<ShiftAbsenceSheet> createState() => _ShiftAbsenceSheetState();
}

class _ShiftAbsenceSheetState extends State<ShiftAbsenceSheet> {
  final _form = GlobalKey<FormState>();
  final _reason = TextEditingController();
  final _keys = <String, String>{};
  int? _staff;
  int? _replacement;
  bool _busy = false;
  bool _blocked = false;
  Object? _failure;

  List<int> get _members => (widget.shift['staff'] as List<dynamic>)
      .cast<Map<String, dynamic>>()
      .where((m) => m['absent'] != true && m['approvedLeave'] != true)
      .map((m) => m['staffId'] as int)
      .where(widget.names.containsKey)
      .toList();

  bool get _allowed => (widget.shift['allowedCommands'] as List<dynamic>? ?? []).contains('recordAbsence');

  Future<void> _submit() async {
    if (_busy || _blocked || !_allowed || !(_form.currentState?.validate() ?? false)) return;
    final body = <String, dynamic>{
      'staffId': _staff,
      'reason': _reason.text.trim(),
      if (_staff == widget.shift['leadStaffId'] && _replacement != null) 'replacementLeadStaffId': _replacement,
    };
    final key = _keys.putIfAbsent(jsonEncode(body), newIdempotencyKey);
    setState(() {
      _busy = true;
      _failure = null;
    });
    try {
      await widget.api.post('/shifts/${widget.shift['id']}/record-absence', body, headers: {'Idempotency-Key': key});
      if (!mounted) return;
      setState(() {
        _busy = false;
      });
      Navigator.of(context).pop(true);
    } on Object catch (error) {
      if (!mounted) return;
      setState(() {
        _busy = false;
        _failure = error;
        if (error is ApiProblem && (error.status == 403 || error.status == 404)) {
          _blocked = true;
          _reason.clear();
          _staff = null;
          _replacement = null;
          _keys.clear();
        }
      });
    }
  }

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final members = _members;
    return PopScope(
      canPop: !_busy,
      child: SingleChildScrollView(
        padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.viewInsetsOf(context).bottom),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Expanded(child: Text('Ghi nhận vắng ca', style: text.titleLarge)),
                IconButton(
                  tooltip: 'Đóng ghi vắng',
                  icon: const Icon(LucideIcons.x),
                  onPressed: _busy ? null : () => Navigator.of(context).pop(_blocked ? true : null),
                ),
              ],
            ),
            const SizedBox(height: 12),
            if (_failure != null) ...[
              Semantics(
                liveRegion: true,
                child: Text(
                  _failure is ApiUnreachable
                      ? 'Chưa nhận được xác nhận từ máy chủ. Giữ nguyên nội dung để thử lại.'
                      : backendMessage(_failure!),
                  style: text.bodyMedium,
                ),
              ),
              const SizedBox(height: 16),
            ],
            if (!_allowed)
              const Text('Bạn không có thao tác ghi vắng được phép trên ca này.')
            else if (!_blocked)
              Form(
                key: _form,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (members.isEmpty)
                      const Text('Chưa có thông tin nhân viên được phép chọn. Đóng và làm mới ca để thử lại.'),
                    DropdownButtonFormField<int>(
                      key: const ValueKey('absent-staff'),
                      initialValue: _staff,
                      isExpanded: true,
                      decoration: const InputDecoration(labelText: 'Nhân viên vắng'),
                      items: [
                        for (final id in members)
                          DropdownMenuItem(
                            value: id,
                            child: Text(widget.names[id]!, overflow: TextOverflow.ellipsis),
                          ),
                      ],
                      onChanged: _busy
                          ? null
                          : (value) => setState(() {
                              _staff = value;
                              _replacement = null;
                            }),
                      validator: (value) => value == null ? 'Chọn nhân viên vắng.' : null,
                    ),
                    const SizedBox(height: 16),
                    if (_staff != null && _staff == widget.shift['leadStaffId']) ...[
                      DropdownButtonFormField<int>(
                        key: const ValueKey('replacement-lead'),
                        initialValue: _replacement,
                        isExpanded: true,
                        decoration: const InputDecoration(labelText: 'Người phụ trách thay thế'),
                        items: [
                          for (final id in members.where((id) => id != _staff))
                            DropdownMenuItem(
                              value: id,
                              child: Text(widget.names[id]!, overflow: TextOverflow.ellipsis),
                            ),
                        ],
                        onChanged: _busy
                            ? null
                            : (value) => setState(() {
                                _replacement = value;
                              }),
                      ),
                      const SizedBox(height: 8),
                      const Text('Nếu còn người đủ điều kiện, phải chọn người thay trong cùng lần ghi nhận.'),
                      const SizedBox(height: 16),
                    ],
                    TextFormField(
                      key: const ValueKey('absence-reason'),
                      controller: _reason,
                      enabled: !_busy,
                      decoration: const InputDecoration(labelText: 'Lý do'),
                      minLines: 2,
                      maxLines: 4,
                      maxLength: 2000,
                      validator: (value) =>
                          value == null || value.trim().isEmpty ? 'Nhập lý do trước khi xác nhận.' : null,
                    ),
                    const SizedBox(height: 16),
                    FilledButton(
                      onPressed: _busy || members.isEmpty ? null : _submit,
                      child: const Text('Xác nhận ghi vắng'),
                    ),
                    if (_busy) const Padding(padding: EdgeInsets.only(top: 12), child: Text('Đang gửi ghi nhận…')),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
