import 'dart:convert';
import 'package:flutter/material.dart';
import '../../common/api/api_client.dart';
import '../../common/format/date_time.dart';
import 'care_checklist_repository.dart';

class CareResultForm extends StatefulWidget {
  const CareResultForm({
    super.key,
    required this.task,
    this.repository,
    required this.newKey,
    this.resultValues = const [],
    this.onRecord,
    this.commandContext = '',
  });
  final Map<String, dynamic> task;
  final CareChecklistRepository? repository;
  final Future<void> Function(Map<String, dynamic> body, String key)? onRecord;
  final String commandContext;
  final String Function() newKey;
  final List<Map<String, dynamic>> resultValues;
  @override
  State<CareResultForm> createState() => _CareResultFormState();
}

class _CareResultFormState extends State<CareResultForm> {
  final numeric = TextEditingController(),
      note = TextEditingController(),
      reason = TextEditingController(),
      behavior = TextEditingController(),
      performed = TextEditingController();
  String? code, occurred, error, signature, retryKey;
  bool busy = false, needsConfirmation = false, confirmed = false;
  static const labels = {
    'ML': 'Lượng nước (ml)',
    'ITEM_QUANTITY': 'Số lượng',
    'MEASURED_VALUE': 'Giá trị đo',
    'SLEEP': 'Thời gian ngủ (phút)',
  };
  Map<String, String> codes(String type) =>
      choices[type] ??
      {
        for (final row in widget.resultValues.where(
          (r) =>
              r['status'] == 'ACTIVE' &&
              r['type'] == const {'SLEEP': 'SLEEP_QUALITY', 'MOOD': 'MOOD', 'BEHAVIOR': 'BEHAVIOR'}[type],
        ))
          row['code'] as String: row['name'] as String,
      };
  bool needsCode(String type) =>
      choices.containsKey(type) || ['SLEEP', 'MOOD'].contains(type) || (type == 'BEHAVIOR' && occurred == 'yes');
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
    behavior.dispose();
    super.dispose();
  }

  Future<void> save() async {
    final type = widget.task['resultType'] as String;
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
        (needsCode(type) && code == null) ||
        (type == 'BEHAVIOR' && (occurred == null || (occurred == 'yes' && behavior.text.trim().isEmpty)))) {
      setState(() => error = 'Nhập đầy đủ kết quả trước khi lưu.');
      return;
    }
    final body = <String, dynamic>{
      if (labels.containsKey(type)) 'numericValue': value,
      if (needsCode(type) && type != 'BEHAVIOR') 'codeValue': code,
      if (type == 'BEHAVIOR')
        'jsonValue': {
          'occurred': occurred == 'yes',
          if (occurred == 'yes') ...{'behaviorCode': code, 'description': behavior.text},
        },
      if (note.text.isNotEmpty) 'note': note.text,
      if (reason.text.isNotEmpty) 'reason': reason.text,
      if (confirmed) 'upperLimitConfirmed': true,
      'performedAt': ?performedAt,
    };
    final nextSignature = jsonEncode({'body': body, 'context': widget.commandContext});
    if (signature != nextSignature) {
      signature = nextSignature;
      retryKey = widget.newKey();
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      if (widget.onRecord != null) {
        await widget.onRecord!(body, retryKey!);
      } else {
        await widget.repository!.record(widget.task['id'] as int, body, retryKey!);
        if (mounted) Navigator.pop(context, true);
      }
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
    final type = widget.task['resultType'] as String;
    final supported =
        labels.containsKey(type) || choices.containsKey(type) || ['PERFORMED', 'MOOD', 'BEHAVIOR'].contains(type);
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
              if (type == 'BEHAVIOR')
                RadioGroup<String>(
                  groupValue: occurred,
                  onChanged: busy
                      ? (_) {}
                      : (v) => setState(() {
                          occurred = v;
                          code = null;
                        }),
                  child: const Column(
                    children: [
                      RadioListTile<String>(value: 'no', title: Text('Không có hành vi bất thường')),
                      RadioListTile<String>(value: 'yes', title: Text('Có hành vi bất thường')),
                    ],
                  ),
                ),
              if (needsCode(type))
                RadioGroup<String>(
                  groupValue: code,
                  onChanged: (v) {
                    if (!busy) setState(() => code = v);
                  },
                  child: Column(
                    children: [
                      for (final entry in codes(type).entries)
                        RadioListTile<String>(value: entry.key, title: Text(entry.value), enabled: !busy),
                    ],
                  ),
                ),
              if (type == 'BEHAVIOR' && occurred == 'yes')
                TextField(
                  controller: behavior,
                  enabled: !busy,
                  maxLength: 2000,
                  decoration: const InputDecoration(labelText: 'Mô tả hành vi (bắt buộc)'),
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
