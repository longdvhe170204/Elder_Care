import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';

/// Nguy kịch trên app nhân viên (007 T046; US11; FR-050a → FR-050d): ghi dấu khi chưa có cảnh báo; có cảnh báo (mã dấu
/// từ `Alert.criticalFlagId`) thì xác nhận hoặc gỡ dấu, ghi kết quả xác nhận lại nguyện vọng, thực hiện lựa chọn.
/// Cần mạng (lệnh nguy kịch không vào hàng đợi ngoại tuyến). Kết quả "nguyện vọng mới" ghi ở hồ sơ trên web.
class CriticalCareScreen extends StatelessWidget {
  const CriticalCareScreen({super.key, required this.api, required this.residentId, this.alertId, this.criticalFlagId});

  final ApiClient api;
  final int residentId;
  final int? alertId;
  final int? criticalFlagId;

  @override
  Widget build(BuildContext context) {
    final flagId = criticalFlagId;
    final alert = alertId;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (flagId == null || alert == null)
          _Section(
            title: 'Ghi dấu nguy kịch',
            child: _RecordFlag(api: api, residentId: residentId),
          )
        else ...[
          _Section(
            title: 'Dấu nguy kịch',
            child: _FlagActions(api: api, flagId: flagId),
          ),
          _Section(
            title: 'Xác nhận lại nguyện vọng',
            child: _Reconfirm(api: api, alertId: alert),
          ),
          _Section(
            title: 'Thực hiện lựa chọn',
            child: _ExecuteChoice(api: api, alertId: alert),
          ),
        ],
      ],
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) => Card(
    margin: const EdgeInsets.only(bottom: 12),
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleSmall),
          const SizedBox(height: 12),
          child,
        ],
      ),
    ),
  );
}

/// Trạng thái gửi lệnh dùng chung: bận, lỗi chung, thông báo xong; 422 đưa về ô nhập qua [onInvalid].
mixin _Command<T extends StatefulWidget> on State<T> {
  bool busy = false;
  String? error;
  String? done;

  Future<void> send(Future<void> Function() call, String doneText) async {
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await call();
      if (mounted) setState(() => done = doneText);
    } on Exception catch (e) {
      if (mounted) setState(() => error = backendMessage(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  List<Widget> feedback(BuildContext context) => [
    if (error != null)
      Text(error!, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error)),
    if (done != null) Text(done!, style: Theme.of(context).textTheme.bodyMedium),
  ];
}

class _Choices extends StatelessWidget {
  const _Choices({required this.label, required this.items, required this.value, required this.onChanged, this.error});

  final String label;
  final Map<String, String> items;
  final String? value;
  final ValueChanged<String> onChanged;
  final String? error;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      Text(label, style: Theme.of(context).textTheme.labelLarge),
      const SizedBox(height: 8),
      Wrap(
        spacing: 8,
        runSpacing: 8,
        children: [
          for (final e in items.entries)
            ChoiceChip(label: Text(e.value), selected: value == e.key, onSelected: (_) => onChanged(e.key)),
        ],
      ),
      if (error != null)
        Text(
          error!,
          style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Theme.of(context).colorScheme.error),
        ),
    ],
  );
}

class _RecordFlag extends StatefulWidget {
  const _RecordFlag({required this.api, required this.residentId});

  final ApiClient api;
  final int residentId;

  @override
  State<_RecordFlag> createState() => _RecordFlagState();
}

class _RecordFlagState extends State<_RecordFlag> with _Command {
  final _assessment = TextEditingController();
  String? _type;
  bool _submitted = false;

  @override
  void dispose() {
    _assessment.dispose();
    super.dispose();
  }

  void _submit() {
    setState(() => _submitted = true);
    final text = _assessment.text.trim();
    if (_type == null || text.isEmpty) return;
    send(
      () => widget.api.post('/residents/${widget.residentId}/critical-flags', {'type': _type, 'assessment': text}),
      'Đã ghi dấu nguy kịch và tạo cảnh báo Khẩn cấp.',
    );
  }

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      _Choices(
        label: 'Loại dấu',
        items: const {'OFFICIAL': 'Dấu chính thức', 'PROVISIONAL': 'Dấu tạm (không có Bác sĩ trực)'},
        value: _type,
        onChanged: (v) => setState(() => _type = v),
        error: _submitted && _type == null ? 'Chọn loại dấu' : null,
      ),
      const SizedBox(height: 12),
      TextField(
        controller: _assessment,
        maxLines: 3,
        decoration: InputDecoration(
          labelText: 'Nhận định',
          errorText: _submitted && _assessment.text.trim().isEmpty ? 'Nhập nhận định' : null,
        ),
      ),
      const SizedBox(height: 12),
      ...feedback(context),
      FilledButton(onPressed: busy || done != null ? null : _submit, child: const Text('Ghi dấu nguy kịch')),
    ],
  );
}

class _FlagActions extends StatefulWidget {
  const _FlagActions({required this.api, required this.flagId});

  final ApiClient api;
  final int flagId;

  @override
  State<_FlagActions> createState() => _FlagActionsState();
}

class _FlagActionsState extends State<_FlagActions> with _Command {
  final _reason = TextEditingController();
  bool _removing = false;
  bool _submitted = false;

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  void _remove() {
    setState(() => _submitted = true);
    final reason = _reason.text.trim();
    if (reason.isEmpty) return;
    send(
      () => widget.api.post('/critical-flags/${widget.flagId}/remove', {'reason': reason}),
      'Đã gỡ dấu. Cảnh báo nguy kịch vẫn phải đóng.',
    );
  }

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      Wrap(
        spacing: 8,
        children: [
          FilledButton.tonal(
            onPressed: busy
                ? null
                : () => send(() => widget.api.post('/critical-flags/${widget.flagId}/confirm', {}), 'Đã xác nhận dấu.'),
            child: const Text('Xác nhận dấu tạm'),
          ),
          if (!_removing)
            OutlinedButton(onPressed: () => setState(() => _removing = true), child: const Text('Gỡ dấu')),
        ],
      ),
      if (_removing) ...[
        const SizedBox(height: 12),
        TextField(
          controller: _reason,
          maxLines: 3,
          decoration: InputDecoration(
            labelText: 'Lý do gỡ dấu',
            errorText: _submitted && _reason.text.trim().isEmpty ? 'Nhập lý do gỡ dấu' : null,
          ),
        ),
        const SizedBox(height: 12),
        FilledButton(onPressed: busy ? null : _remove, child: const Text('Xác nhận gỡ dấu')),
      ],
      const SizedBox(height: 8),
      ...feedback(context),
    ],
  );
}

class _Reconfirm extends StatefulWidget {
  const _Reconfirm({required this.api, required this.alertId});

  final ApiClient api;
  final int alertId;

  @override
  State<_Reconfirm> createState() => _ReconfirmState();
}

class _ReconfirmState extends State<_Reconfirm> with _Command {
  final _name = TextEditingController();
  String? _channel;
  String? _result;
  bool _submitted = false;

  @override
  void dispose() {
    _name.dispose();
    super.dispose();
  }

  void _submit() {
    setState(() => _submitted = true);
    final name = _name.text.trim();
    if (name.isEmpty || _channel == null || _result == null) return;
    send(
      () => widget.api.post('/alerts/${widget.alertId}/wish-reconfirmation', {
        'contactedPerson': {'fullName': name},
        'channel': _channel,
        'contactedAt': DateTime.now().toUtc().toIso8601String(),
        'result': _result,
      }),
      'Đã ghi kết quả xác nhận lại.',
    );
  }

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      TextField(
        controller: _name,
        decoration: InputDecoration(
          labelText: 'Người đã liên hệ',
          errorText: _submitted && _name.text.trim().isEmpty ? 'Nhập họ tên người đã liên hệ' : null,
        ),
      ),
      const SizedBox(height: 12),
      _Choices(
        label: 'Cách liên hệ',
        items: const {'PHONE_CALL': 'Gọi điện', 'IN_PERSON': 'Trực tiếp'},
        value: _channel,
        onChanged: (v) => setState(() => _channel = v),
        error: _submitted && _channel == null ? 'Chọn cách liên hệ' : null,
      ),
      const SizedBox(height: 12),
      _Choices(
        label: 'Kết quả',
        items: const {'KEPT': 'Giữ nguyện vọng', 'UNREACHABLE': 'Không liên lạc được'},
        value: _result,
        onChanged: (v) => setState(() => _result = v),
        error: _submitted && _result == null ? 'Chọn kết quả' : null,
      ),
      const SizedBox(height: 12),
      ...feedback(context),
      FilledButton(onPressed: busy || done != null ? null : _submit, child: const Text('Ghi kết quả xác nhận lại')),
    ],
  );
}

class _ExecuteChoice extends StatefulWidget {
  const _ExecuteChoice({required this.api, required this.alertId});

  final ApiClient api;
  final int alertId;

  @override
  State<_ExecuteChoice> createState() => _ExecuteChoiceState();
}

class _ExecuteChoiceState extends State<_ExecuteChoice> with _Command {
  final _reason = TextEditingController();
  final _unit = TextEditingController();
  final _companion = TextEditingController();
  String? _choice;
  bool _submitted = false;

  bool get _transfer => _choice == 'TRANSFER_TO_HOSPITAL';

  @override
  void dispose() {
    _reason.dispose();
    _unit.dispose();
    _companion.dispose();
    super.dispose();
  }

  void _submit() {
    setState(() => _submitted = true);
    if (_choice == null) return;
    if (_transfer && (_unit.text.trim().isEmpty || _companion.text.trim().isEmpty)) return;
    final reason = _reason.text.trim();
    send(
      () => widget.api.post('/alerts/${widget.alertId}/execute-choice', {
        'choice': _choice,
        if (reason.isNotEmpty) 'reason': reason,
        if (_transfer)
          'transferOut': {
            'transferUnit': _unit.text.trim(),
            'transferAt': DateTime.now().toUtc().toIso8601String(),
            'companion': _companion.text.trim(),
          },
      }),
      'Đã ghi lựa chọn thực hiện.',
    );
  }

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      _Choices(
        label: 'Lựa chọn thực hiện',
        items: const {
          'RETURN_HOME': 'Về nhà',
          'STAY_PALLIATIVE': 'Ở lại, chăm sóc giảm nhẹ',
          'TRANSFER_TO_HOSPITAL': 'Chuyển bệnh viện',
        },
        value: _choice,
        onChanged: (v) => setState(() => _choice = v),
        error: _submitted && _choice == null ? 'Chọn lựa chọn thực hiện' : null,
      ),
      if (_transfer) ...[
        const SizedBox(height: 12),
        TextField(
          controller: _unit,
          decoration: InputDecoration(
            labelText: 'Bệnh viện chuyển đến',
            errorText: _submitted && _unit.text.trim().isEmpty ? 'Nhập nơi chuyển đến' : null,
          ),
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _companion,
          decoration: InputDecoration(
            labelText: 'Người đi cùng (họ tên, quan hệ hoặc chức danh)',
            errorText: _submitted && _companion.text.trim().isEmpty ? 'Nhập người đi cùng' : null,
          ),
        ),
      ],
      const SizedBox(height: 12),
      TextField(
        controller: _reason,
        maxLines: 2,
        decoration: const InputDecoration(labelText: 'Lý do (bắt buộc khi không liên lạc được và chưa có nguyện vọng)'),
      ),
      const SizedBox(height: 12),
      ...feedback(context),
      FilledButton(onPressed: busy || done != null ? null : _submit, child: const Text('Thực hiện lựa chọn')),
    ],
  );
}
