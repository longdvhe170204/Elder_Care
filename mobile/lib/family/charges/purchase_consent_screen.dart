import 'dart:convert';
import 'dart:math';
import 'package:flutter/material.dart';
import '../../../common/api/api_client.dart';
import '../../../common/api/problem_message.dart';

/// FR-023a: chỉ các đề nghị backend cho người đại diện hiện hành được trả về.
class PurchaseConsentScreen extends StatefulWidget {
  const PurchaseConsentScreen({super.key, required this.api, required this.residentId});
  final ApiClient api;
  final int residentId;
  @override
  State<PurchaseConsentScreen> createState() => _PurchaseConsentScreenState();
}

class _PurchaseConsentScreenState extends State<PurchaseConsentScreen> {
  List<Map<String, dynamic>>? _rows;
  String? _error;
  bool _denied = false;
  bool _busy = false;
  String? _message;
  final Map<String, String> _keys = {};

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final rows = await widget.api.getList(
        '/purchase-assistance-requests?residentId=${widget.residentId}&status=AWAITING_CONSENT',
      );
      if (mounted) {
        setState(() {
          _rows = rows.cast<Map<String, dynamic>>();
          _error = null;
        });
      }
    } catch (error) {
      if (mounted) {
        setState(() {
          _error = backendMessage(error);
          if (error is ApiProblem && error.status == 403) {
            _denied = true;
            _rows = null;
          }
        });
      }
    }
  }

  Future<void> _decide(Map<String, dynamic> row, bool agree) async {
    if (_busy) return;
    var reason = '';
    final approved = await showDialog<bool>(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, refresh) => AlertDialog(
          title: Text(agree ? 'Đồng ý đề nghị #${row['id']}' : 'Từ chối đề nghị #${row['id']}'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(row['description'] as String? ?? ''),
                Text('Số tiền dự kiến: ${row['estimatedAmount']} đồng'),
                if (row['additionalConsentPending'] == true)
                  Text('Số tiền xin đồng ý mới: ${_additionalAmount(row)} đồng'),
                if (row['allowedAmount'] != null) Text('Số đã đồng ý: ${row['allowedAmount']} đồng'),
                if (!agree)
                  TextField(
                    maxLength: 2000,
                    decoration: const InputDecoration(labelText: 'Lý do (bắt buộc)'),
                    onChanged: (value) => refresh(() {
                      reason = value;
                    }),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Quay lại')),
            FilledButton(
              onPressed: !agree && reason.trim().isEmpty ? null : () => Navigator.pop(context, true),
              child: Text(agree ? 'Xác nhận đồng ý' : 'Xác nhận từ chối'),
            ),
          ],
        ),
      ),
    );
    final text = reason.trim();
    if (approved != true || !mounted) return;
    setState(() {
      _busy = true;
      _error = null;
      _message = null;
    });
    try {
      final key = _keys.putIfAbsent('${jsonEncode(row)}:$agree:$text', _uuid);
      await widget.api.post(
        '/purchase-assistance-requests/${row['id']}/${agree ? 'agree' : 'reject'}',
        agree ? const {} : {'reason': text},
        headers: {'Idempotency-Key': key},
      );
      if (mounted) {
        setState(() {
          _message = 'Đã ghi nhận quyết định.';
        });
      }
      await _load();
    } catch (error) {
      if (mounted) {
        setState(() {
          _error = backendMessage(error);
          if (error is ApiProblem && error.status == 403) {
            _denied = true;
            _rows = null;
          }
        });
      }
      if (error is ApiProblem && error.status == 409) {
        await _load();
        if (mounted) {
          setState(() {
            _message = 'Đề nghị đã thay đổi. Hãy xem lại trước khi thao tác tiếp.';
          });
        }
      }
    } finally {
      if (mounted) {
        setState(() {
          _busy = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Đề nghị mua hộ')),
    body: SafeArea(
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (_denied)
            const Text('Bạn không còn quyền xem đề nghị mua hộ.')
          else ...[
            OutlinedButton(onPressed: _busy ? null : _load, child: const Text('Tải lại')),
            if (_error != null) Semantics(liveRegion: true, child: Text(_error!)),
            if (_message != null) Semantics(liveRegion: true, child: Text(_message!)),
            if (_rows == null)
              const Center(child: CircularProgressIndicator())
            else if (_rows!.isEmpty)
              const Text('Không có đề nghị đang chờ bạn đồng ý.')
            else
              for (final row in _rows!)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Đề nghị #${row['id']}', style: Theme.of(context).textTheme.titleMedium),
                      Text(row['description'] as String? ?? ''),
                      Text('Số lượng: ${row['quantity']}'),
                      Text('Dự kiến: ${row['estimatedAmount']} đồng'),
                      if (row['allowedAmount'] != null) Text('Được phép: ${row['allowedAmount']} đồng'),
                      if (row['insufficientBalance'] == true)
                        const Text('Số dư không đủ. Dấu này không chặn quyết định.'),
                      if (row['additionalConsentPending'] == true)
                        Text('Số tiền xin đồng ý mới: ${_additionalAmount(row)} đồng'),
                      Wrap(
                        spacing: 8,
                        children: [
                          FilledButton(onPressed: _busy ? null : () => _decide(row, true), child: const Text('Đồng ý')),
                          OutlinedButton(
                            onPressed: _busy ? null : () => _decide(row, false),
                            child: const Text('Từ chối'),
                          ),
                        ],
                      ),
                      const Divider(),
                    ],
                  ),
                ),
          ],
        ],
      ),
    ),
  );
}

Object? _additionalAmount(Map<String, dynamic> row) {
  final decisions = (row['decisions'] as List? ?? const []).whereType<Map<String, dynamic>>().where(
    (d) => d['type'] == 'REQUEST_ADDITIONAL',
  );
  return decisions.isEmpty ? null : decisions.last['newAmount'];
}

String _uuid() {
  final random = Random.secure();
  final bytes = List<int>.generate(16, (_) => random.nextInt(256));
  bytes[6] = (bytes[6] & 15) | 64;
  bytes[8] = (bytes[8] & 63) | 128;
  final hex = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
}
