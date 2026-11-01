import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/problem_message.dart';
import 'notification_api.dart';

/// Khẩn cấp chưa xác nhận: chỉ mở không phải xác nhận (FR-026).
bool isOpenUrgent(Map<String, dynamic> n) => n['level'] == 'URGENT' && n['acknowledgedAt'] == null;

/// Nút xác nhận thông báo Khẩn cấp (FR-026, FR-026a): "Xác nhận và tiếp nhận" chỉ hiện khi backend cho phép. Xác nhận bắt
/// buộc trực tuyến (Q-01), mất mạng thì báo rõ, không đưa vào hàng đợi. Giữ một khóa cho mỗi nút để bấm lại khi mạng chập
/// chờn không ghi hai lần.
class AcknowledgeActions extends StatefulWidget {
  const AcknowledgeActions({super.key, required this.notification, required this.notifications, required this.onDone});

  final Map<String, dynamic> notification;
  final NotificationApi notifications;
  final Future<void> Function() onDone;

  @override
  State<AcknowledgeActions> createState() => _AcknowledgeActionsState();
}

class _AcknowledgeActionsState extends State<AcknowledgeActions> {
  final _acknowledgeKey = newIdempotencyKey();
  final _takeChargeKey = newIdempotencyKey();
  bool _busy = false;
  String? _failure;

  int get _id => widget.notification['id'] as int;

  Future<void> _run(Future<String> Function() command) async {
    setState(() {
      _busy = true;
      _failure = null;
    });
    try {
      final done = await command();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(done)));
      await widget.onDone();
    } on ApiUnreachable {
      if (mounted) setState(() => _failure = 'Xác nhận cần kết nối mạng. Kiểm tra mạng rồi thử lại.');
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<String> _acknowledge() async {
    await widget.notifications.acknowledge(_id, _acknowledgeKey);
    return 'Đã xác nhận thông báo khẩn cấp';
  }

  Future<String> _acknowledgeAndTakeCharge() async {
    final taken = await widget.notifications.acknowledgeAndTakeCharge(_id, _takeChargeKey);
    return taken ? 'Đã xác nhận và tiếp nhận' : 'Đã xác nhận. Chưa tiếp nhận được, có thể đã có người khác tiếp nhận.';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final takeCharge = widget.notification['canAcknowledgeAndTakeCharge'] == true;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (_failure != null) ...[
          Text(_failure!, style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.error)),
          const SizedBox(height: 8),
        ],
        if (takeCharge) ...[
          FilledButton(
            onPressed: _busy ? null : () => _run(_acknowledgeAndTakeCharge),
            child: const Text('Xác nhận và tiếp nhận'),
          ),
          const SizedBox(height: 8),
          OutlinedButton(onPressed: _busy ? null : () => _run(_acknowledge), child: const Text('Xác nhận đã nhận')),
        ] else
          FilledButton(onPressed: _busy ? null : () => _run(_acknowledge), child: const Text('Xác nhận đã nhận')),
      ],
    );
  }
}
