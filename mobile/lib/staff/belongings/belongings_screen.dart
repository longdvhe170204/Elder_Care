import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/widgets/page_body.dart';
import '../../common/widgets/status_pill.dart';
import 'belonging_labels.dart';
import 'intake_screen.dart';
import 'photo_picker.dart';

export 'photo_picker.dart' show PhotoPicker;

/// Đồ gửi trên app nhân viên (013 T011; FR-011; cùng chữ với màn web Đồ gửi, DESIGN.md 13 điều 7, 15.4): tìm người cao
/// tuổi (GET /residents của 001), danh sách dòng đồ gửi (mô tả, loại và số lượng, chữ trạng thái), nút chính "Tiếp nhận
/// đồ". Mọi lệnh đồ gửi chỉ trực tuyến (FR-031). Gắn vào mục "Thêm" của phần nhân viên khi mục đó có (G4, 001-followup).
class BelongingsScreen extends StatefulWidget {
  const BelongingsScreen({super.key, required this.api, this.pickPhoto = cameraPhoto});

  final ApiClient api;
  final PhotoPicker pickPhoto;

  @override
  State<BelongingsScreen> createState() => _BelongingsScreenState();
}

class _BelongingsScreenState extends State<BelongingsScreen> {
  final _query = TextEditingController();
  List<Map<String, dynamic>> _types = const [];
  List<Map<String, dynamic>> _levels = const [];
  List<Map<String, dynamic>> _locations = const [];
  List<Map<String, dynamic>>? _residents;
  Map<String, dynamic>? _resident;
  List<Map<String, dynamic>>? _belongings;
  String? _error;
  String? _notice;

  @override
  void initState() {
    super.initState();
    _loadCatalogs();
  }

  @override
  void dispose() {
    _query.dispose();
    super.dispose();
  }

  List<Map<String, dynamic>> _maps(List<dynamic> list) => list.cast<Map<String, dynamic>>();

  Future<void> _loadCatalogs() async {
    try {
      final types = await widget.api.getList('/belongings/types');
      final levels = await widget.api.getList('/belongings/condition-levels');
      final locations = await widget.api.getList('/belongings/storage-locations');
      if (!mounted) return;
      setState(() {
        _types = _maps(types);
        _levels = _maps(levels)..sort((a, b) => (a['order'] as int).compareTo(b['order'] as int));
        _locations = _maps(locations);
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Future<void> _search() async {
    final q = _query.text.trim();
    try {
      final page = await widget.api.get('/residents${q.isEmpty ? '' : '?q=${Uri.encodeQueryComponent(q)}'}');
      if (mounted) setState(() => _residents = _maps(page?['items'] as List<dynamic>? ?? const []));
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Future<void> _open(Map<String, dynamic> resident) async {
    setState(() {
      _resident = resident;
      _residents = null;
      _belongings = null;
      _notice = null;
    });
    await _loadBelongings();
  }

  Future<void> _loadBelongings() async {
    try {
      final list = await widget.api.getList('/residents/${_resident!['id']}/belongings');
      if (mounted) setState(() => _belongings = _maps(list));
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  String _name(Map<String, dynamic> r) => (r['personalInfo'] as Map<String, dynamic>)['fullName'] as String;

  String _typeName(Object? id) => _types.where((t) => t['id'] == id).firstOrNull?['name'] as String? ?? 'Loại #$id';

  Future<void> _intake() async {
    final r = _resident!;
    final saved = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => IntakeScreen(
          api: widget.api,
          residentId: r['id'] as int,
          residentName: _name(r),
          types: _types,
          levels: _levels,
          locations: _locations,
          pickPhoto: widget.pickPhoto,
        ),
      ),
    );
    if (saved == true && mounted) {
      setState(() => _notice = 'Đã tiếp nhận đồ gửi.');
      await _loadBelongings();
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final resident = _resident;
    return PageBody(
      child: ListView(
        children: [
          Row(
            children: [
              Expanded(
                child: TextField(
                  key: const Key('belongings-search'),
                  controller: _query,
                  decoration: const InputDecoration(labelText: 'Tìm người cao tuổi'),
                  onSubmitted: (_) => _search(),
                ),
              ),
              const SizedBox(width: 8),
              OutlinedButton(onPressed: _search, child: const Text('Tìm')),
            ],
          ),
          if (_error != null) Padding(padding: const EdgeInsets.only(top: 12), child: Text(_error!)),
          for (final r in _residents ?? const <Map<String, dynamic>>[])
            ListTile(title: Text(_name(r)), subtitle: Text(r['profileCode'] as String? ?? ''), onTap: () => _open(r)),
          if (_residents != null && _residents!.isEmpty)
            const Padding(
              padding: EdgeInsets.only(top: 12),
              child: Text('Không có người cao tuổi khớp với từ tìm trong phạm vi của bạn.'),
            ),
          if (resident != null) ...[
            const SizedBox(height: 16),
            Text('Đồ gửi của ${_name(resident)}', style: text.titleLarge),
            if (_notice != null) Semantics(liveRegion: true, child: Text(_notice!)),
            const SizedBox(height: 8),
            FilledButton(onPressed: _types.isEmpty ? null : _intake, child: const Text('Tiếp nhận đồ')),
            const SizedBox(height: 8),
            if (_belongings == null)
              const Center(child: CircularProgressIndicator())
            else if (_belongings!.isEmpty)
              const Text('Chưa có đồ gửi. Đồ gia đình gửi viện giữ hộ sẽ hiện ở đây sau khi được tiếp nhận.')
            else
              for (final b in _belongings!)
                Container(
                  constraints: const BoxConstraints(minHeight: 64),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surface,
                    border: Border(bottom: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
                  ),
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(b['description'] as String, style: text.titleSmall),
                            Text('${_typeName(b['typeId'])}, ${amountText(b)}', style: text.bodySmall),
                          ],
                        ),
                      ),
                      StatusPill(
                        label: belongingStatusLabel(b['status'] as String?),
                        tone: belongingTone(b['status'] as String?),
                      ),
                    ],
                  ),
                ),
          ],
        ],
      ),
    );
  }
}
