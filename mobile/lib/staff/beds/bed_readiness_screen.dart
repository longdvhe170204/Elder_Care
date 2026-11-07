import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/page_body.dart';
import 'bed_command.dart';

/// T060: xác nhận trực tuyến; hỏi lại điều kiện ở backend khi ghi, không giả thành công lúc mất mạng.
class BedReadinessScreen extends StatefulWidget {
  const BedReadinessScreen({super.key, required this.api, required this.bedId});
  final ApiClient api;
  final int bedId;
  @override
  State<BedReadinessScreen> createState() => _BedReadinessScreenState();
}

class _BedReadinessScreenState extends State<BedReadinessScreen> {
  Map<String, dynamic>? _conditions;
  bool _canWrite = false, _busy = false, _disinfected = false, _ppe = false, _done = false;
  final _note = TextEditingController();
  String? _error, _message, _key;
  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _note.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final permissions = await widget.api.getList('/me/permissions');
      final conditions = await widget.api.get('/beds/${widget.bedId}/readiness-conditions');
      if (mounted) {
        setState(() {
          _conditions = conditions;
          _canWrite = permissions.whereType<Map<String, dynamic>>().any(
            (p) => p['function'] == 'BED_READINESS' && p['operation'] == 'CREATE',
          );
          _error = null;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = bedProblem(e));
    }
  }

  Future<void> _confirm() async {
    if (_busy || _done) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.post(
        '/beds/${widget.bedId}/confirm-readiness',
        {'disinfected': _disinfected, 'ppeUsed': _ppe, if (_note.text.trim().isNotEmpty) 'note': _note.text.trim()},
        headers: {'Idempotency-Key': _key ??= bedCommandKey()},
      );
      if (mounted) {
        setState(() {
          _done = true;
          _message = 'Đã xác nhận giường sẵn sàng.';
        });
      }
    } catch (e) {
      if (e is ApiProblem && e.status < 500) _key = null;
      if (mounted) setState(() => _error = bedProblem(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final required = _conditions?['disinfectionRequired'] == true;
    final allowed = _conditions?['allowed'] == true && (!required || _disinfected && _ppe);
    return Scaffold(
      appBar: headerAppBar(context, title: 'Giường ${widget.bedId}: sẵn sàng'),
      body: PageBody(
        child: ListView(
          children: [
            if (_conditions == null && _error == null) const LinearProgressIndicator(),
            if (_error != null) Semantics(liveRegion: true, child: Text(_error!)),
            if (_conditions == null && _error != null) TextButton(onPressed: _load, child: const Text('Thử lại')),
            if (_message != null) Semantics(liveRegion: true, child: Text(_message!)),
            if (_conditions != null) ...[
              Text(
                required
                    ? 'Bắt buộc khử khuẩn và dùng bảo hộ trước khi nhận người mới.'
                    : 'Kiểm tra giường trước khi xác nhận.',
              ),
              CheckboxListTile(
                title: const Text('Đã khử khuẩn'),
                value: _disinfected,
                onChanged: _busy || _done
                    ? null
                    : (v) => setState(() {
                        _disinfected = v!;
                        _key = null;
                      }),
              ),
              CheckboxListTile(
                title: const Text('Đã dùng bảo hộ'),
                value: _ppe,
                onChanged: _busy || _done
                    ? null
                    : (v) => setState(() {
                        _ppe = v!;
                        _key = null;
                      }),
              ),
              TextField(
                controller: _note,
                decoration: const InputDecoration(labelText: 'Ghi chú'),
                enabled: !_busy && !_done,
                maxLength: 2000,
                minLines: 2,
                maxLines: 4,
                onChanged: (_) => _key = null,
              ),
              if (_conditions?['allowed'] != true) const Text('Giường chưa đủ điều kiện xác nhận sẵn sàng.'),
            ],
          ],
        ),
      ),
      bottomNavigationBar: _canWrite
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: FilledButton(
                  onPressed: allowed && !_busy && !_done ? _confirm : null,
                  child: Text(_busy ? 'Đang xác nhận…' : 'Xác nhận sẵn sàng'),
                ),
              ),
            )
          : null,
    );
  }
}
