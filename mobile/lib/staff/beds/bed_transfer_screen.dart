import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/page_body.dart';
import 'bed_command.dart';

/// T036/T056: lệnh chuyển trực tuyến, tự chọn giường và lý do/bác sĩ, không chọn sẵn.
class BedTransferScreen extends StatefulWidget {
  const BedTransferScreen({super.key, required this.api, required this.allocationId, this.infectionControl = false});
  final ApiClient api;
  final int allocationId;
  final bool infectionControl;
  @override
  State<BedTransferScreen> createState() => _BedTransferScreenState();
}

class _BedTransferScreenState extends State<BedTransferScreen> {
  final _reason = TextEditingController();
  Map<String, dynamic>? _allocation;
  List<Map<String, dynamic>> _beds = [], _reasons = [], _doctors = [];
  int? _bed, _reasonId, _doctor;
  int _optionsVersion = 0;
  bool _busy = false, _loading = true, _canWrite = false, _permitted = true, _done = false;
  String? _error, _message, _key;
  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
      _bed = _reasonId = _doctor = null;
      _key = null;
      _optionsVersion++;
    });
    try {
      final permissions = (await widget.api.getList('/me/permissions')).whereType<Map<String, dynamic>>();
      if (!permissions.any((p) => p['function'] == 'BED_ALLOCATION' && p['operation'] == 'VIEW')) {
        if (mounted) {
          setState(() {
            _permitted = false;
            _loading = false;
          });
        }
        return;
      }
      final allocation = (await widget.api.get('/bed-allocations/${widget.allocationId}'))!;
      final beds = await widget.api.getList(
        '/bed-allocations/suitable-beds?residentId=${allocation['residentId']}&fromAllocationId=${widget.allocationId}&infectionControl=${widget.infectionControl}',
      );
      final reasons = widget.infectionControl ? <dynamic>[] : await widget.api.getList('/bed-transfer-reasons');
      final doctors = widget.infectionControl
          ? await widget.api.getList('/bed-allocations/prescribing-doctors')
          : <dynamic>[];
      if (mounted) {
        setState(() {
          _allocation = allocation;
          _beds = beds.whereType<Map<String, dynamic>>().toList();
          _reasons = reasons.whereType<Map<String, dynamic>>().where((r) => r['status'] == 'ACTIVE').toList();
          _doctors = doctors.whereType<Map<String, dynamic>>().toList();
          _canWrite = permissions.any((p) => p['function'] == 'BED_ALLOCATION' && p['operation'] == 'CREATE');
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = bedProblem(e);
          _loading = false;
        });
      }
    }
  }

  Future<void> _transfer() async {
    if (_busy || _done) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final path = widget.infectionControl ? 'infection-control-transfer' : 'transfer';
      await widget.api.post(
        '/bed-allocations/${widget.allocationId}/$path',
        {
          'newBedId': _bed,
          if (widget.infectionControl) 'prescribingDoctorId': _doctor else 'transferReasonId': _reasonId,
          'reason': _reason.text.trim(),
        },
        headers: {'Idempotency-Key': _key ??= bedCommandKey()},
      );
      if (mounted) {
        setState(() {
          _done = true;
          _message = 'Đã chuyển giường.';
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
    final canSubmit =
        !_loading &&
        !_busy &&
        !_done &&
        _bed != null &&
        (widget.infectionControl ? _doctor != null : _reasonId != null) &&
        _reason.text.trim().isNotEmpty;
    return Scaffold(
      appBar: headerAppBar(context, title: widget.infectionControl ? 'Chuyển kiểm soát lây nhiễm' : 'Chuyển giường'),
      body: PageBody(
        child: ListView(
          children: [
            if (_loading) const LinearProgressIndicator(),
            if (!_permitted) const Text('Bạn không có quyền xem phân bổ giường.'),
            if (_error != null) Semantics(liveRegion: true, child: Text(_error!)),
            if (_message != null) Semantics(liveRegion: true, child: Text(_message!)),
            if (_allocation != null) ...[
              Text(
                'Hồ sơ ${_allocation!['residentId']}, ${(_allocation!['location'] as Map<String, dynamic>)['label']}',
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<int>(
                key: ValueKey('bed-$_optionsVersion'),
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Giường mới'),
                hint: const Text('Chọn giường'),
                items: [
                  for (final b in _beds)
                    DropdownMenuItem(
                      value: (b['location'] as Map<String, dynamic>)['bedId'] as int,
                      child: Text((b['location'] as Map<String, dynamic>)['label'] as String),
                    ),
                ],
                onChanged: _busy || _done || _loading
                    ? null
                    : (v) => setState(() {
                        _bed = v;
                        _key = null;
                      }),
              ),
              const SizedBox(height: 16),
              if (!widget.infectionControl)
                DropdownButtonFormField<int>(
                  key: ValueKey('reason-$_optionsVersion'),
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: 'Lý do chuyển'),
                  hint: const Text('Chọn lý do'),
                  items: [
                    for (final r in _reasons) DropdownMenuItem(value: r['id'] as int, child: Text(r['name'] as String)),
                  ],
                  onChanged: _busy || _done || _loading
                      ? null
                      : (v) => setState(() {
                          _reasonId = v;
                          _key = null;
                        }),
                ),
              if (widget.infectionControl)
                DropdownButtonFormField<int>(
                  key: ValueKey('doctor-$_optionsVersion'),
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: 'Bác sĩ chỉ định'),
                  hint: const Text('Chọn Bác sĩ Hoạt động'),
                  items: [
                    for (final d in _doctors)
                      DropdownMenuItem(
                        value: d['id'] as int,
                        child: Text(d['fullName'] as String? ?? 'Tài khoản ${d['id']}'),
                      ),
                  ],
                  onChanged: _busy || _done || _loading
                      ? null
                      : (v) => setState(() {
                          _doctor = v;
                          _key = null;
                        }),
                ),
              const SizedBox(height: 16),
              TextField(
                controller: _reason,
                enabled: !_busy && !_done,
                maxLength: 2000,
                minLines: 2,
                maxLines: 4,
                decoration: const InputDecoration(labelText: 'Lý do (bắt buộc)'),
                onChanged: (_) => setState(() => _key = null),
              ),
              if (_beds.isEmpty) const Text('Chưa có giường phù hợp. Tải lại để kiểm tra.'),
              if (!_done)
                TextButton(onPressed: _busy || _loading ? null : _load, child: const Text('Tải lại giường phù hợp')),
              if (!widget.infectionControl) const Text('Chuyển tại thời điểm hiện tại. Nhập bù chưa sẵn sàng.'),
            ],
          ],
        ),
      ),
      bottomNavigationBar: _canWrite
          ? SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: FilledButton(
                  onPressed: canSubmit ? _transfer : null,
                  child: Text(
                    _busy
                        ? 'Đang chuyển…'
                        : widget.infectionControl
                        ? 'Chuyển theo chỉ định'
                        : 'Chuyển giường',
                  ),
                ),
              ),
            )
          : null,
    );
  }
}
