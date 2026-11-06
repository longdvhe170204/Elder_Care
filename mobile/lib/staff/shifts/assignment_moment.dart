import 'package:flutter/material.dart';

/// Nhập ngày dd/MM/yyyy và giờ HH:mm; không phụ thuộc bộ chọn mặc định theo locale của máy.
Future<String?> assignmentMoment(BuildContext context, DateTime initial, {required bool withTime}) async {
  String two(int value) => value.toString().padLeft(2, '0');
  var day = '${two(initial.day)}/${two(initial.month)}/${initial.year}';
  var time = '${two(initial.hour)}:${two(initial.minute)}';
  final form = GlobalKey<FormState>();
  DateTime? parseDay(String? value) {
    final match = RegExp(r'^(\d{2})/(\d{2})/(\d{4})$').firstMatch(value?.trim() ?? '');
    if (match == null) return null;
    final d = int.parse(match[1]!), m = int.parse(match[2]!), y = int.parse(match[3]!);
    final date = DateTime.utc(y, m, d);
    return date.day == d && date.month == m && date.year == y ? date : null;
  }

  String? validTime(String? value) {
    final match = RegExp(r'^(\d{2}):(\d{2})$').firstMatch(value?.trim() ?? '');
    return match != null && int.parse(match[1]!) < 24 && int.parse(match[2]!) < 60 ? null : 'Giờ không hợp lệ.';
  }

  return showDialog<String>(
    context: context,
    builder: (context) => AlertDialog(
      title: Text(withTime ? 'Chọn ngày giờ' : 'Chọn ngày'),
      content: SingleChildScrollView(
        child: Form(
          key: form,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextFormField(
                initialValue: day,
                onChanged: (value) => day = value,
                keyboardType: TextInputType.datetime,
                decoration: const InputDecoration(labelText: 'Ngày (dd/MM/yyyy)'),
                validator: (v) => parseDay(v) == null ? 'Ngày không hợp lệ.' : null,
              ),
              if (withTime) ...[
                const SizedBox(height: 16),
                TextFormField(
                  initialValue: time,
                  onChanged: (value) => time = value,
                  keyboardType: TextInputType.datetime,
                  decoration: const InputDecoration(labelText: 'Giờ (HH:mm)', helperText: 'Giờ Việt Nam'),
                  validator: validTime,
                ),
              ],
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Quay lại')),
        FilledButton(
          onPressed: () {
            if (!form.currentState!.validate()) return;
            final d = parseDay(day)!;
            final iso = '${d.year.toString().padLeft(4, '0')}-${two(d.month)}-${two(d.day)}';
            Navigator.pop(context, withTime ? '${iso}T${time.trim()}:00+07:00' : iso);
          },
          child: const Text('Chọn'),
        ),
      ],
    ),
  );
}
