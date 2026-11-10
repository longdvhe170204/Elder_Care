import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/page_body.dart';
import 'bed_command.dart';
import 'bed_readiness_screen.dart';
import 'bed_transfer_screen.dart';
import 'room_isolation_screen.dart';

const _states = {
  'EMPTY': 'Trống',
  'OCCUPIED': 'Đang sử dụng',
  'HELD': 'Đang giữ chỗ',
  'AWAITING_READY': 'Chờ sẵn sàng',
  'UNDER_MAINTENANCE': 'Đang bảo trì',
  'SUSPENDED': 'Tạm ngừng sử dụng',
  'RETIRED': 'Không sử dụng',
};

/// T060: bảng giường trong phạm vi backend cho xem. Không có quyền xem cấu hình thì đọc giường Chờ sẵn sàng của tầng
/// đang là Trưởng tầng hoặc Người phụ trách ca (`GET /bed-readiness/beds`, ô ⁴⁰ chỉ có ở lớp nhiệm vụ nên không có trong
/// `/me/permissions`); 403 là không có nhiệm vụ, hiện danh sách rỗng.
class BedsScreen extends StatefulWidget {
  const BedsScreen({super.key, required this.api, this.floorId});
  final ApiClient api;
  final int? floorId;
  @override
  State<BedsScreen> createState() => _BedsScreenState();
}

class _BedsScreenState extends State<BedsScreen> {
  List<Map<String, dynamic>>? _beds;
  bool _readinessOnly = false;
  List<Map<String, dynamic>> _permissions = [];
  List<Map<String, dynamic>> _isolationRooms = [], _pendingStays = [];
  String? _error;
  @override
  void initState() {
    super.initState();
    _load();
  }

  bool _allowed(String function, String operation) =>
      _permissions.any((p) => p['function'] == function && p['operation'] == operation);
  Future<void> _load() async {
    try {
      final permissions = (await widget.api.getList('/me/permissions')).whereType<Map<String, dynamic>>().toList();
      bool has(String function, String operation) =>
          permissions.any((p) => p['function'] == function && p['operation'] == operation);
      final canReadBeds = has('ROOM_BED_CONFIGURATION', 'VIEW');
      final data = canReadBeds
          ? await widget.api.get('/beds${widget.floorId == null ? '' : '?floorId=${widget.floorId}'}')
          : null;
      final readiness = canReadBeds ? null : await _readinessBeds();
      final rooms = !canReadBeds && has('ROOM_ISOLATION', 'VIEW')
          ? (await widget.api.getList('/room-isolation/rooms')).whereType<Map<String, dynamic>>().toList()
          : <Map<String, dynamic>>[];
      final me = !canReadBeds && has('ROOM_ISOLATION', 'UPDATE') ? await widget.api.get('/me') : null;
      final doctor = (me?['roles'] as List<dynamic>? ?? []).contains('AC-03');
      final pending = doctor
          ? (await widget.api.getList('/room-isolation/pending-stays')).whereType<Map<String, dynamic>>().toList()
          : <Map<String, dynamic>>[];
      if (mounted) {
        setState(() {
          _permissions = permissions;
          _readinessOnly = readiness != null;
          _beds = readiness ?? (data?['beds'] as List<dynamic>? ?? []).whereType<Map<String, dynamic>>().toList();
          _isolationRooms = rooms;
          _pendingStays = pending;
          _error = null;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _error = bedProblem(e));
    }
  }

  Future<List<Map<String, dynamic>>> _readinessBeds() async {
    try {
      final query = widget.floorId == null ? '' : '?floorId=${widget.floorId}';
      return (await widget.api.getList('/bed-readiness/beds$query')).whereType<Map<String, dynamic>>().toList();
    } on ApiProblem catch (e) {
      if (e.status == 403) return [];
      rethrow;
    }
  }

  Future<void> _openReadiness(Map<String, dynamic> bed) async {
    await Navigator.of(context).push<void>(
      MaterialPageRoute(
        builder: (_) => BedReadinessScreen(api: widget.api, bedId: bed['id'] as int),
      ),
    );
    if (mounted) await _load();
  }

  Future<void> _open(Map<String, dynamic> bed) async {
    final roomId = (bed['location'] as Map<String, dynamic>?)?['roomId'] as int?;
    try {
      final canTransfer = _allowed('BED_ALLOCATION', 'CREATE');
      final allocations = canTransfer
          ? (await widget.api.getList(
              '/bed-allocations?bedId=${bed['id']}',
            )).whereType<Map<String, dynamic>>().where((a) => a['status'] == 'ACTIVE').toList()
          : <Map<String, dynamic>>[];
      final me = _allowed('ROOM_ISOLATION', 'UPDATE') ? await widget.api.get('/me') : null;
      final doctor = (me?['roles'] as List<dynamic>? ?? []).contains('AC-03');
      final pendingStays = doctor
          ? (await widget.api.getList('/room-isolation/pending-stays'))
                .whereType<Map<String, dynamic>>()
                .where((a) => (a['location'] as Map<String, dynamic>?)?['bedId'] == bed['id'])
                .toList()
          : <Map<String, dynamic>>[];
      if (!mounted) return;
      final action = await showModalBottomSheet<Widget>(
        context: context,
        showDragHandle: true,
        builder: (context) => SafeArea(
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ListTile(
                  title: Text((bed['location'] as Map<String, dynamic>?)?['label'] as String? ?? 'Giường ${bed['id']}'),
                ),
                if (bed['state'] == 'AWAITING_READY' && _allowed('BED_READINESS', 'VIEW'))
                  ListTile(
                    title: const Text('Xác nhận sẵn sàng'),
                    onTap: () => Navigator.pop(context, BedReadinessScreen(api: widget.api, bedId: bed['id'] as int)),
                  ),
                if (roomId != null && _allowed('ROOM_ISOLATION', 'VIEW'))
                  ListTile(
                    title: const Text('Cách ly phòng'),
                    onTap: () => Navigator.pop(context, RoomIsolationScreen(api: widget.api, roomId: roomId)),
                  ),
                for (final allocation in allocations) ...[
                  if (canTransfer)
                    ListTile(
                      title: Text('Chuyển giường hồ sơ ${allocation['residentId']}'),
                      onTap: () => Navigator.pop(
                        context,
                        BedTransferScreen(api: widget.api, allocationId: allocation['id'] as int),
                      ),
                    ),
                  if (canTransfer)
                    ListTile(
                      title: Text('Chuyển theo chỉ định hồ sơ ${allocation['residentId']}'),
                      onTap: () => Navigator.pop(
                        context,
                        BedTransferScreen(
                          api: widget.api,
                          allocationId: allocation['id'] as int,
                          infectionControl: true,
                        ),
                      ),
                    ),
                ],
                for (final allocation in pendingStays)
                  if (_allowed('ROOM_ISOLATION', 'UPDATE') &&
                      roomId != null &&
                      (allocation['flags'] as List<dynamic>? ?? []).contains('AWAITING_DOCTOR_CONFIRMATION'))
                    ListTile(
                      title: Text('Xác nhận ở lại hồ sơ ${allocation['residentId']}'),
                      onTap: () => Navigator.pop(
                        context,
                        RoomIsolationScreen(api: widget.api, roomId: roomId, allocationId: allocation['id'] as int),
                      ),
                    ),
              ],
            ),
          ),
        ),
      );
      if (action != null && mounted) {
        await Navigator.of(context).push<void>(MaterialPageRoute(builder: (_) => action));
        if (mounted) await _load();
      }
    } catch (e) {
      if (mounted) setState(() => _error = bedProblem(e));
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: headerAppBar(context, title: widget.floorId == null ? 'Phòng, giường' : 'Giường tầng ${widget.floorId}'),
    body: PageBody(
      child: ListView(
        children: [
          if (_error != null) ...[
            Semantics(liveRegion: true, child: Text(_error!)),
            TextButton(onPressed: _load, child: const Text('Thử lại')),
          ],
          if (_beds == null && _error == null) const LinearProgressIndicator(),
          if (_beds?.isEmpty == true) const Text('Chưa có giường trong phạm vi được xem.'),
          for (final room in _isolationRooms)
            ListTile(
              title: Text(room['name'] as String? ?? 'Phòng ${room['id']}'),
              subtitle: Text(room['isolated'] == true ? 'Đang cách ly' : 'Không cách ly'),
              onTap: () async {
                await Navigator.of(context).push<void>(
                  MaterialPageRoute(
                    builder: (_) => RoomIsolationScreen(api: widget.api, roomId: room['id'] as int),
                  ),
                );
                if (mounted) await _load();
              },
            ),
          for (final allocation in _pendingStays)
            ListTile(
              title: Text('Xác nhận ở lại hồ sơ ${allocation['residentId']}'),
              onTap: () async {
                await Navigator.of(context).push<void>(
                  MaterialPageRoute(
                    builder: (_) => RoomIsolationScreen(
                      api: widget.api,
                      roomId: (allocation['location'] as Map<String, dynamic>)['roomId'] as int,
                      allocationId: allocation['id'] as int,
                    ),
                  ),
                );
                if (mounted) await _load();
              },
            ),
          for (final bed in _beds ?? <Map<String, dynamic>>[]) ...[
            ListTile(
              minVerticalPadding: 16,
              leading: const Icon(LucideIcons.bed),
              title: Text((bed['location'] as Map<String, dynamic>?)?['label'] as String? ?? 'Giường ${bed['code']}'),
              subtitle: Text(
                '${_states[bed['state']] ?? 'Chưa có trạng thái'}${bed['readinessOverdue'] == true ? ', quá hạn' : ''}',
              ),
              onTap: () => _readinessOnly ? _openReadiness(bed) : _open(bed),
            ),
            const Divider(height: 1),
          ],
        ],
      ),
    ),
  );
}
