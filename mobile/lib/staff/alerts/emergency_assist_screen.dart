import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';

/// Mở trình quay số của máy; `false` khi máy không mở được.
Future<bool> dialPhone(String phone) async {
  try {
    return await launchUrl(Uri(scheme: 'tel', path: phone));
  } on PlatformException {
    return false;
  }
}

/// Nút hỗ trợ khẩn cấp trên app nhân viên (007 T102; US13; FR-047i, FR-047j, FR-047k; BR-M05-29, 31). Chạm nút thì đếm
/// ngược 3 giây, có nút Hủy; chạm lần nữa khi đang đếm thì gửi ngay. Gửi xong hiện trạng thái (Đã gửi, "[họ tên] đang
/// tới", "[họ tên] đã tới", hỏi lại mỗi [pollInterval]), chọn loại sự việc, nơi (mỗi trường một lần), nút "Gọi 115" ghi
/// lượt gọi rồi mở trình quay số. Chỉ trực tuyến (lệnh không vào hàng đợi ngoại tuyến); mất mạng hoặc bị từ chối thì hiện
/// số của viện và 115. Gửi vị trí GPS làm ở T096.
class EmergencyAssistScreen extends StatefulWidget {
  const EmergencyAssistScreen({
    super.key,
    required this.api,
    this.dial = dialPhone,
    this.pollInterval = const Duration(seconds: 5),
  });

  final ApiClient api;
  final Future<bool> Function(String phone) dial;
  final Duration pollInterval;

  @override
  State<EmergencyAssistScreen> createState() => _EmergencyAssistScreenState();
}

const _events = [
  ('CARDIAC_ARREST_OR_FAINTING', 'Ngừng tim, ngất'),
  ('FALL', 'Ngã'),
  ('AGITATION_OR_VIOLENCE', 'Kích động, bạo lực'),
  ('OTHER', 'Khác'),
];
const _places = [
  ('ROOM', 'Phòng'),
  ('RESTROOM', 'Nhà vệ sinh'),
  ('CORRIDOR', 'Hành lang'),
  ('COMMON_ROOM', 'Phòng sinh hoạt chung'),
  ('YARD', 'Sân'),
  ('OTHER', 'Nơi khác'),
];

String _newKey() {
  final random = Random.secure();
  return List.generate(16, (_) => random.nextInt(256).toRadixString(16).padLeft(2, '0')).join();
}

class _EmergencyAssistScreenState extends State<EmergencyAssistScreen> {
  Timer? _countdown;
  Timer? _poll;
  int? _secondsLeft;
  bool _busy = false;
  String? _error;
  Map<String, dynamic>? _contacts;
  Map<String, dynamic>? _status;
  String? _eventType;
  String? _place;
  Map<String, dynamic>? _call;
  String? _callResult;
  final String _pressKey = _newKey();
  final String _callKey = _newKey();

  @override
  void dispose() {
    _countdown?.cancel();
    _poll?.cancel();
    super.dispose();
  }

  void _tapButton() {
    if (_secondsLeft != null) {
      _send();
      return;
    }
    setState(() => _secondsLeft = 3);
    _countdown = Timer.periodic(const Duration(seconds: 1), (timer) {
      final left = (_secondsLeft ?? 1) - 1;
      if (left <= 0) {
        _send();
      } else {
        setState(() => _secondsLeft = left);
      }
    });
  }

  void _cancel() {
    _countdown?.cancel();
    setState(() => _secondsLeft = null);
  }

  Future<void> _send() async {
    _countdown?.cancel();
    setState(() {
      _secondsLeft = null;
      _busy = true;
      _error = null;
      _contacts = null;
    });
    try {
      final status = await widget.api.post('/emergency-assist', {}, headers: {'Idempotency-Key': _pressKey});
      if (!mounted) return;
      setState(() => _status = status);
      _poll = Timer.periodic(widget.pollInterval, (_) => _refresh());
    } on ApiProblem catch (problem) {
      if (!mounted) return;
      setState(() {
        _error = problem.detail ?? backendMessage(problem);
        _contacts = problem.properties['contacts'] as Map<String, dynamic>?;
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _error = backendMessage(error));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _refresh() async {
    try {
      final status = await widget.api.get('/emergency-assist/current');
      if (!mounted) return;
      setState(() => _status = status ?? _status);
      if (status == null) _poll?.cancel();
    } catch (_) {
      // Giữ trạng thái cũ; lần hỏi sau thử lại.
    }
  }

  Future<void> _details(String field, String value) async {
    final alertId = _status?['alertId'];
    if (alertId == null) return;
    setState(() => _error = null);
    try {
      await widget.api.post('/emergency-assist/$alertId/details', {field: value});
      if (!mounted) return;
      setState(() => field == 'eventType' ? _eventType = value : _place = value);
    } catch (error) {
      if (mounted) setState(() => _error = backendMessage(error));
    }
  }

  Future<void> _call115() async {
    final alertId = _status?['alertId'];
    if (alertId != null && _call == null) {
      try {
        final call = await widget.api.post('/alerts/$alertId/call-115', {}, headers: {'Idempotency-Key': _callKey});
        if (mounted) setState(() => _call = call);
      } catch (_) {
        // Không ghi được lượt gọi vẫn mở trình quay số: gọi cấp cứu không chờ máy chủ.
      }
    }
    await widget.dial('115');
  }

  Future<void> _callResultTap(String result) async {
    final id = _call?['id'];
    if (id == null) return;
    try {
      await widget.api.post('/call-115-attempts/$id/result', {'result': result});
      if (mounted) setState(() => _callResult = result);
    } catch (error) {
      if (mounted) setState(() => _error = backendMessage(error));
    }
  }

  String _statusText(Map<String, dynamic> status) {
    final name = status['receiverName'] as String?;
    return switch (status['status']) {
      'RECEIVED' => '${name ?? 'Người tiếp nhận'} đang tới',
      'ARRIVED' => '${name ?? 'Người tiếp nhận'} đã tới',
      _ => 'Đã gửi, đang chờ người tiếp nhận',
    };
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final status = _status;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (status == null) ...[
          SizedBox(
            height: 96,
            child: FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: theme.colorScheme.error,
                foregroundColor: theme.colorScheme.onError,
              ),
              onPressed: _busy ? null : _tapButton,
              child: Text(
                _secondsLeft == null ? 'Gọi hỗ trợ khẩn cấp' : 'Gửi sau $_secondsLeft giây, chạm để gửi ngay',
                textAlign: TextAlign.center,
                style: theme.textTheme.titleMedium?.copyWith(color: theme.colorScheme.onError),
              ),
            ),
          ),
          if (_secondsLeft != null) ...[
            const SizedBox(height: 12),
            OutlinedButton(onPressed: _cancel, child: const Text('Hủy')),
          ],
        ] else ...[
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Text(_statusText(status), style: theme.textTheme.titleMedium, semanticsLabel: _statusText(status)),
            ),
          ),
          const SizedBox(height: 16),
          Text('Loại sự việc', style: theme.textTheme.titleSmall),
          const SizedBox(height: 8),
          _Choices(items: _events, selected: _eventType, onSelected: (v) => _details('eventType', v)),
          const SizedBox(height: 16),
          Text('Nơi', style: theme.textTheme.titleSmall),
          const SizedBox(height: 8),
          _Choices(items: _places, selected: _place, onSelected: (v) => _details('place', v)),
        ],
        if (_error != null) ...[
          const SizedBox(height: 16),
          Text(_error!, style: TextStyle(color: theme.colorScheme.error)),
        ],
        if (_contacts?['facilityPhone'] != null) ...[
          const SizedBox(height: 12),
          OutlinedButton(
            onPressed: () => widget.dial(_contacts!['facilityPhone'] as String),
            child: Text('Gọi số của viện ${_contacts!['facilityPhone']}'),
          ),
        ],
        const SizedBox(height: 16),
        OutlinedButton(onPressed: _call115, child: const Text('Gọi 115')),
        if (_call != null && _callResult == null) ...[
          const SizedBox(height: 12),
          const Text('Sau cuộc gọi, chọn kết quả:'),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: [
              FilledButton(onPressed: () => _callResultTap('CONNECTED'), child: const Text('Đã gọi được')),
              OutlinedButton(onPressed: () => _callResultTap('NOT_CONNECTED'), child: const Text('Không gọi được')),
            ],
          ),
        ],
        if (_callResult != null) ...[
          const SizedBox(height: 12),
          Text(_callResult == 'CONNECTED' ? 'Đã ghi: đã gọi được cấp cứu 115.' : 'Đã ghi: không gọi được 115.'),
        ],
      ],
    );
  }
}

class _Choices extends StatelessWidget {
  const _Choices({required this.items, required this.selected, required this.onSelected});

  final List<(String, String)> items;
  final String? selected;
  final ValueChanged<String> onSelected;

  @override
  Widget build(BuildContext context) => Wrap(
    spacing: 8,
    runSpacing: 8,
    children: [
      for (final (value, label) in items)
        ChoiceChip(
          label: Text(label),
          selected: selected == value,
          // Mỗi trường đặt một lần: đã chọn thì khóa (FR-047i)
          onSelected: selected == null ? (_) => onSelected(value) : null,
        ),
    ],
  );
}
