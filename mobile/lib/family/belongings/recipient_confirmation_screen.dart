import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/widgets/page_body.dart';

String _idempotencyKey() {
  final r = Random.secure();
  String hex(int n) => List.generate(n, (_) => r.nextInt(16).toRadixString(16)).join();
  return '${hex(8)}-${hex(4)}-4${hex(3)}-a${hex(3)}-${hex(12)}';
}

/// Người đại diện quyết định yêu cầu xác nhận người nhận khác (013 T019; FR-017, FR-030; api-013
/// `POST /family-portal/recipient-confirmations/{id}/{confirm|reject}`), mở từ thông báo
/// `RECIPIENT_CONFIRMATION_REQUESTED` có mã yêu cầu là nguồn. api-013 chưa có cách để người đại diện đọc tên người nhận,
/// đồ trong yêu cầu (team-plan mục 8), nên màn chỉ nêu mã. Từ chối bắt buộc lý do (DESIGN.md 2.3). Chỉ trực tuyến.
class RecipientConfirmationScreen extends StatefulWidget {
  const RecipientConfirmationScreen({super.key, required this.api, required this.confirmationId});

  final ApiClient api;
  final int confirmationId;

  @override
  State<RecipientConfirmationScreen> createState() => _RecipientConfirmationScreenState();
}

class _RecipientConfirmationScreenState extends State<RecipientConfirmationScreen> {
  final _reason = TextEditingController();
  final _keys = {'confirm': _idempotencyKey(), 'reject': _idempotencyKey()};
  bool _rejecting = false;
  bool _busy = false;
  String? _reasonError;
  String? _message;
  bool _decided = false;

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _send(String command) async {
    if (command == 'reject' && _reason.text.trim().isEmpty) {
      setState(() => _reasonError = 'Nhập lý do từ chối.');
      return;
    }
    setState(() {
      _busy = true;
      _reasonError = null;
      _message = null;
    });
    try {
      await widget.api.requestJson(
        'POST',
        '/family-portal/recipient-confirmations/${widget.confirmationId}/$command',
        headers: {'Idempotency-Key': _keys[command]!},
        body: command == 'reject' ? {'reason': _reason.text.trim()} : const <String, dynamic>{},
      );
      setState(() {
        _decided = true;
        _message = command == 'confirm'
            ? 'Đã xác nhận. Viện được trả đồ cho người này.'
            : 'Đã từ chối. Viện không trả đồ cho người này.';
      });
    } on ApiUnreachable {
      setState(() => _message = 'Cần có mạng để gửi quyết định. Kiểm tra kết nối rồi thử lại.');
    } on Exception catch (e) {
      setState(() => _message = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Xác nhận người nhận đồ gửi')),
      body: PageBody(
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Yêu cầu mã ${widget.confirmationId}', style: text.titleMedium),
              const SizedBox(height: 8),
              const Text(
                'Viện cần trả đồ gửi cho một người không thuộc danh sách được phép đón. '
                'Bạn là người đại diện nên quyết định có đồng ý hay không.',
              ),
              if (_message != null)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: Semantics(liveRegion: true, child: Text(_message!)),
                ),
              if (!_decided) ...[
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: _busy ? null : () => _send('confirm'),
                  child: const Text('Xác nhận người nhận'),
                ),
                const SizedBox(height: 12),
                if (!_rejecting)
                  OutlinedButton(
                    onPressed: _busy ? null : () => setState(() => _rejecting = true),
                    child: const Text('Từ chối'),
                  )
                else ...[
                  TextField(
                    key: const Key('reject-reason'),
                    controller: _reason,
                    maxLength: 2000,
                    maxLines: 3,
                    decoration: InputDecoration(labelText: 'Lý do từ chối', errorText: _reasonError),
                  ),
                  OutlinedButton(onPressed: _busy ? null : () => _send('reject'), child: const Text('Gửi từ chối')),
                ],
              ],
            ],
          ),
        ),
      ),
    );
  }
}
