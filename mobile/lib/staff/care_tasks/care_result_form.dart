import 'dart:convert';
import 'package:flutter/material.dart';
import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import 'care_checklist_repository.dart';

class CareResultForm extends StatefulWidget {
  const CareResultForm({super.key, required this.task, required this.repository, required this.newKey});
  final Map<String, dynamic> task;
  final CareChecklistRepository repository;
  final String Function() newKey;
  @override
  State<CareResultForm> createState() => _CareResultFormState();
}

class _CareResultFormState extends State<CareResultForm> {
  final numeric = TextEditingController(),
      note = TextEditingController(),
      reason = TextEditingController(),
      performed = TextEditingController();
  String? code, error, signature, retryKey;
  bool busy = false, needsConfirmation = false, confirmed = false;
  static const labels = {'ML': 'Lượng nước (ml)', 'ITEM_QUANTITY': 'Số lượng', 'MEASURED_VALUE': 'Giá trị đo'};
  static const choices = {
    'EATING': {
      'ATE_ALL': 'Ăn hết',
      'MOST': 'Phần lớn',
      'SOME': 'Một phần',
      'NOT_EATEN': 'Không ăn',
      'SKIPPED': 'Bỏ bữa',
    },
    'MOVEMENT': {'INDEPENDENT': 'Tự thực hiện', 'ASSISTED': 'Cần hỗ trợ', 'UNABLE': 'Không thực hiện được'},
  };
  @override
  void dispose() {
    numeric.dispose();
    note.dispose();
    reason.dispose();
    performed.dispose();
    super.dispose();
  }

  Future<void> save() async {
    final type = widget.task['resultType'];
    final value = num.tryParse(numeric.text);
    String? performedAt;
    if (performed.text.trim().isNotEmpty) {
      final text = performed.text.trim();
      final match = RegExp(r'^(\d{2})/(\d{2})/(\d{4}) (\d{2}):(\d{2})$').firstMatch(text);
      final parsed = match == null
          ? null
          : DateTime.tryParse('${match[3]}-${match[2]}-${match[1]}T${match[4]}:${match[5]}:00+07:00');
      if (parsed == null || formatDateTime(parsed) != text) {
        setState(() => error = 'Nhập ngày giờ hợp lệ dạng dd/MM/yyyy HH:mm.');
        return;
      }
      performedAt = parsed.toUtc().toIso8601String();
    }
    if ((labels.containsKey(type) && (value == null || !value.isFinite)) ||
        (choices.containsKey(type) && code == null)) {
      setState(() => error = 'Nhập đầy đủ kết quả trước khi lưu.');
      return;
    }
    final body = <String, dynamic>{
      if (labels.containsKey(type)) 'numericValue': value,
      if (choices.containsKey(type)) 'codeValue': code,
      if (note.text.isNotEmpty) 'note': note.text,
      if (reason.text.isNotEmpty) 'reason': reason.text,
      if (confirmed) 'upperLimitConfirmed': true,
      'performedAt': ?performedAt,
    };
    final nextSignature = jsonEncode(body);
    if (signature != nextSignature) {
      signature = nextSignature;
      retryKey = widget.newKey();
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await widget.repository.gateway.record(widget.task['id'] as int, body, retryKey!);
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) {
        setState(() {
          error = e is ApiProblem ? e.detail : 'Chưa gửi được. Giữ nguyên nội dung để thử lại.';
          needsConfirmation = e is ApiProblem && e.errorCode == 'CONFIRMATION_REQUIRED';
        });
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final type = widget.task['resultType'];
    final supported = labels.containsKey(type) || choices.containsKey(type) || type == 'PERFORMED';
    return PopScope(
      canPop: !busy,
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 16, 16, MediaQuery.viewInsetsOf(context).bottom + 16),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Ghi nhận kết quả', style: Theme.of(context).textTheme.titleLarge),
              if (!supported) const Text('Chưa có danh mục kết quả để ghi nhận loại công việc này.'),
              if (labels.containsKey(type))
                TextField(
                  controller: numeric,
                  enabled: !busy,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  onChanged: (_) => setState(() => confirmed = false),
                  decoration: InputDecoration(labelText: labels[type]),
                ),
              if (choices.containsKey(type))
                RadioGroup<String>(
                  groupValue: code,
                  onChanged: (v) {
                    if (!busy) setState(() => code = v);
                  },
                  child: Column(
                    children: [
                      for (final entry in choices[type]!.entries)
                        RadioListTile<String>(value: entry.key, title: Text(entry.value), enabled: !busy),
                    ],
                  ),
                ),
              if (supported) ...[
                TextField(
                  controller: performed,
                  enabled: !busy,
                  keyboardType: TextInputType.datetime,
                  decoration: const InputDecoration(labelText: 'Thời điểm thực hiện (dd/MM/yyyy HH:mm)'),
                ),
                const Text('Giờ Việt Nam. Để trống để dùng thời điểm gửi ghi nhận.'),
                TextField(
                  controller: note,
                  enabled: !busy,
                  maxLength: 2000,
                  decoration: const InputDecoration(labelText: 'Ghi chú kết quả'),
                ),
                if (['IMPORTANT', 'MANDATORY'].contains(widget.task['importance']))
                  TextField(
                    controller: reason,
                    enabled: !busy,
                    maxLength: 2000,
                    decoration: const InputDecoration(labelText: 'Lý do hoàn thành trễ (nếu có)'),
                  ),
                if (needsConfirmation)
                  CheckboxListTile(
                    value: confirmed,
                    onChanged: busy ? null : (v) => setState(() => confirmed = v ?? false),
                    title: const Text('Xác nhận giá trị đã nhập vượt giới hạn hợp lý'),
                  ),
                if (error != null) Text(error!),
                FilledButton(onPressed: busy ? null : save, child: Text(busy ? 'Đang lưu…' : 'Lưu kết quả')),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
