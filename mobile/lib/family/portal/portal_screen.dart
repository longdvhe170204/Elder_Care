import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/widgets/status_pill.dart';
import '../balance/family_balance_screen.dart';
import '../belongings/family_belongings_screen.dart';
import '../charges/family_costs_screen.dart';
import '../charges/purchase_consent_screen.dart';
import '../visits/visit_registration_screen.dart';

const _statusLabels = {
  'ADMISSION_IN_PROGRESS': ('Đang tiếp nhận', StatusTone.attention),
  'IN_RESIDENCE': ('Đang lưu trú', StatusTone.settled),
  'TEMPORARILY_ABSENT': ('Tạm vắng', StatusTone.attention),
  'OFF_SITE_ACTIVITY': ('Hoạt động bên ngoài', StatusTone.neutral),
  'HOSPITALIZED': ('Điều trị tại bệnh viện', StatusTone.attention),
  'RESIDENCY_ENDED': ('Kết thúc lưu trú', StatusTone.neutral),
  'DECEASED': ('Qua đời', StatusTone.neutral),
  'ADMISSION_CANCELLED': ('Hủy tiếp nhận', StatusTone.neutral),
};

const _sections = [
  ('allergies-conditions', 'Dị ứng, bệnh nền'),
  ('vital-signs', 'Chỉ số'),
  ('weight', 'Cân nặng'),
  ('medications', 'Thuốc đang dùng'),
  ('incidents', 'Sự cố, phần sức khỏe'),
  ('care-results', 'Kết quả chăm sóc'),
  ('eating', 'Ăn uống'),
  ('diet', 'Chế độ ăn riêng'),
  ('medical-appointments', 'Lịch khám bên ngoài'),
];

const _healthLock = 'Thông tin sức khỏe chỉ hiện với người thân được người cao tuổi đồng ý chia sẻ.';

/// Phần cổng trên app người thân (012 T028; US5, FR-046 → FR-048; Q-305 cùng chức năng với cổng web): người cao tuổi có
/// quan hệ Hiệu lực kèm quyền có tác dụng; thông tin chung; Sức khỏe chỉ khi quyền có tác dụng (mỗi lần mở được viện ghi
/// lại); đăng ký thăm khi có quyền. Chỉ giá trị hiện hành; phần nguồn chưa có ghi rõ chưa có dữ liệu.
class PortalScreen extends StatefulWidget {
  const PortalScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<PortalScreen> createState() => _PortalScreenState();
}

class _PortalScreenState extends State<PortalScreen> {
  List<Map<String, dynamic>>? _residents;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final rows = (await widget.api.getList('/family-portal/residents')).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _residents = rows);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final residents = _residents;
    if (_error != null) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Text(_error!, style: text.bodyMedium),
      );
    }
    if (residents == null) return const Center(child: CircularProgressIndicator());
    if (residents.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Text('Chưa có người cao tuổi nào có quan hệ Hiệu lực với tài khoản này.', style: text.bodyMedium),
      );
    }
    return ListView(
      padding: const EdgeInsets.symmetric(vertical: 8),
      children: [
        for (final r in residents)
          ListTile(
            minTileHeight: 64,
            title: Text(r['fullName'] as String),
            subtitle: Text(r['representative'] == true ? 'Bạn là người đại diện' : 'Người thân'),
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (_) => ResidentPortalScreen(api: widget.api, resident: r),
              ),
            ),
          ),
      ],
    );
  }
}

class ResidentPortalScreen extends StatefulWidget {
  const ResidentPortalScreen({super.key, required this.api, required this.resident});

  final ApiClient api;
  final Map<String, dynamic> resident;

  @override
  State<ResidentPortalScreen> createState() => _ResidentPortalScreenState();
}

class _ResidentPortalScreenState extends State<ResidentPortalScreen> {
  Map<String, dynamic>? _general;
  String? _error;

  int get _residentId => widget.resident['residentId'] as int;
  bool _right(String right) => (widget.resident['rights'] as Map)[right] == true;

  // Như mục Đồ gửi của cổng web: người đại diện, người được phép đón (012 FR-046b; backend vẫn kiểm mỗi lần gọi)
  bool get _belongings =>
      widget.resident['representative'] == true ||
      widget.resident['canViewBelongings'] == true ||
      _right('PICKUP_ALLOWED');

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final general = await widget.api.get('/family-portal/residents/$_residentId/general');
      if (mounted) setState(() => _general = general);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final g = _general;
    final name = widget.resident['fullName'] as String;
    return Scaffold(
      appBar: AppBar(title: Text(name)),
      body: _error != null
          ? Padding(padding: const EdgeInsets.all(16), child: Text(_error!))
          : g == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Text(g['fullName'] as String, style: theme.textTheme.titleLarge),
                if (g['location'] != null) Text(g['location'] as String, style: theme.textTheme.bodyMedium),
                const SizedBox(height: 4),
                Align(
                  alignment: Alignment.centerLeft,
                  child: Builder(
                    builder: (_) {
                      final (label, tone) = _statusLabels[g['status']] ?? ('${g['status']}', StatusTone.neutral);
                      return StatusPill(label: label, tone: tone);
                    },
                  ),
                ),
                const SizedBox(height: 16),
                Text('Lượt thăm của bạn', style: theme.textTheme.titleMedium),
                if ((g['visits'] as List).isEmpty)
                  Text('Chưa có lượt thăm sắp tới.', style: theme.textTheme.bodyMedium)
                else
                  for (final v in (g['visits'] as List).cast<Map<String, dynamic>>())
                    Text(
                      '${(v['date'] as String).split('-').reversed.join('/')}, ${v['startTime']}–${v['endTime']}',
                      style: theme.textTheme.bodyMedium,
                    ),
                const SizedBox(height: 16),
                Text('Sự cố, phần chung', style: theme.textTheme.titleMedium),
                Text(
                  (g['unavailable'] as List).contains('incidents')
                      ? 'Chưa có dữ liệu, tính năng sự cố chưa hoạt động.'
                      : '${(g['incidents'] as List).length} sự cố trong 30 ngày qua',
                  style: theme.textTheme.bodyMedium,
                ),
                const SizedBox(height: 16),
                if (_right('REGISTER_VISITS'))
                  FilledButton(
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => Scaffold(
                          appBar: AppBar(title: const Text('Đăng ký thăm')),
                          body: VisitRegistrationScreen(api: widget.api, residentId: _residentId, residentName: name),
                        ),
                      ),
                    ),
                    child: const Text('Đăng ký thăm'),
                  ),
                const SizedBox(height: 8),
                if (_right('VIEW_HEALTH'))
                  OutlinedButton(
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => HealthScreen(api: widget.api, residentId: _residentId, residentName: name),
                      ),
                    ),
                    child: const Text('Xem sức khỏe'),
                  )
                else
                  Text(_healthLock, style: theme.textTheme.bodySmall),
                if (_right('VIEW_COSTS')) ...[
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => Scaffold(
                          appBar: AppBar(title: const Text('Số dư')),
                          body: FamilyBalanceScreen(api: widget.api, residentId: _residentId, residentName: name),
                        ),
                      ),
                    ),
                    child: const Text('Xem số dư'),
                  ),
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => FamilyCostsScreen(api: widget.api, residentId: _residentId),
                      ),
                    ),
                    child: const Text('Xem bảng chi phí'),
                  ),
                ],
                // BR-M10-10, 010 FR-023a: người đại diện đồng ý hoặc từ chối đề nghị mua hộ.
                if (widget.resident['representative'] == true) ...[
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => Scaffold(
                          appBar: AppBar(title: const Text('Đề nghị mua hộ')),
                          body: PurchaseConsentScreen(api: widget.api, residentId: _residentId),
                        ),
                      ),
                    ),
                    child: const Text('Đề nghị mua hộ'),
                  ),
                ],
                if (_belongings) ...[
                  const SizedBox(height: 8),
                  OutlinedButton(
                    onPressed: () => Navigator.of(context).push(
                      MaterialPageRoute<void>(
                        builder: (_) => Scaffold(
                          appBar: AppBar(title: const Text('Đồ gửi')),
                          body: FamilyBelongingsScreen(api: widget.api, residentId: _residentId),
                        ),
                      ),
                    ),
                    child: const Text('Xem đồ gửi'),
                  ),
                ],
              ],
            ),
    );
  }
}

class HealthScreen extends StatefulWidget {
  const HealthScreen({super.key, required this.api, required this.residentId, required this.residentName});

  final ApiClient api;
  final int residentId;
  final String residentName;

  @override
  State<HealthScreen> createState() => _HealthScreenState();
}

class _HealthScreenState extends State<HealthScreen> {
  final _parts = <String, Object>{};

  @override
  void initState() {
    super.initState();
    for (final (section, _) in _sections) {
      _load(section);
    }
  }

  Future<void> _load(String section) async {
    Object result;
    try {
      result = (await widget.api.get('/family-portal/residents/${widget.residentId}/health/$section'))!;
    } on ApiProblem catch (e) {
      result = e.status == 403 ? _healthLock : backendMessage(e);
    } on Exception catch (e) {
      result = backendMessage(e);
    }
    if (mounted) setState(() => _parts[section] = result);
  }

  Widget _content(Object? part, TextTheme text) {
    if (part == null) return const LinearProgressIndicator();
    if (part is String) return Text(part, style: text.bodyMedium);
    final data = part as Map<String, dynamic>;
    if (data['available'] != true) return Text('${data['note']}', style: text.bodyMedium);
    final items = (data['items'] as List).cast<Map<String, dynamic>>();
    if (items.isEmpty) return Text('Chưa ghi nhận.', style: text.bodyMedium);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (final i in items)
          Text('${i['text'] ?? i['medicationName'] ?? i['type'] ?? i['resultType'] ?? ''}', style: text.bodyMedium),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Sức khỏe')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Giá trị hiện hành. Mỗi lần xem mục này được viện ghi lại.', style: text.bodySmall),
          for (final (section, title) in _sections) ...[
            const SizedBox(height: 16),
            Text(title, style: text.titleSmall),
            const SizedBox(height: 4),
            _content(_parts[section], text),
          ],
        ],
      ),
    );
  }
}
