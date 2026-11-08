import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/residents/resident_avatar.dart';
import '../../common/residents/resident_labels.dart';
import '../../common/widgets/status_pill.dart';
import 'family_profile_detail_screen.dart';

/// Hồ sơ người cao tuổi trên app người thân (001 T023; US1, US2; FR-007, FR-022; Q-305: cùng chức năng với cổng người
/// thân trên web): danh sách hồ sơ người thân được xem (GET /residents, backend lọc theo quan hệ), mỗi dòng ảnh đại
/// diện, họ tên, mức chăm sóc và chữ trạng thái (DESIGN.md 15.4); bấm dòng mở màn hồ sơ. Chỉ đọc, chỉ trực tuyến. Bố cục
/// khi một người thân có quan hệ với nhiều người cao tuổi chưa được thiết kế (DESIGN.md mục 14): giữ danh sách.
class FamilyProfileScreen extends StatefulWidget {
  const FamilyProfileScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<FamilyProfileScreen> createState() => _FamilyProfileScreenState();
}

class _FamilyProfileScreenState extends State<FamilyProfileScreen> {
  List<Map<String, dynamic>>? _items;
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _items = null;
      _loadError = null;
    });
    try {
      final page = await widget.api.get('/residents');
      final items = (page?['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _items = items);
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _items = const [];
          _loadError = e;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final items = _items;
    if (items == null) return const Center(child: CircularProgressIndicator());
    final text = Theme.of(context).textTheme;
    if (_loadError != null || items.isEmpty) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (_loadError != null)
                    Text(backendMessage(_loadError!), style: text.bodyMedium)
                  else ...[
                    Text('Chưa có hồ sơ nào để xem', style: text.titleSmall),
                    const SizedBox(height: 4),
                    Text('Hồ sơ hiện khi viện liên kết tài khoản của bạn với người thân.', style: text.bodyMedium),
                  ],
                  if (_loadError != null) ...[
                    const SizedBox(height: 12),
                    OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
                  ],
                ],
              ),
            ),
          ),
        ],
      );
    }
    return ListView(
      children: [
        for (final profile in items) ...[
          _ProfileRow(
            profile: profile,
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute<void>(
                builder: (_) => FamilyProfileDetailScreen(api: widget.api, id: profile['id'] as int),
              ),
            ),
          ),
          const Divider(height: 1),
        ],
      ],
    );
  }
}

/// Dòng danh sách (DESIGN.md 15.4): ảnh đại diện, họ tên, mức chăm sóc, bên phải chữ trạng thái.
class _ProfileRow extends StatelessWidget {
  const _ProfileRow({required this.profile, required this.onTap});

  final Map<String, dynamic> profile;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final secondary = text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant);
    final name = (profile['personalInfo'] as Map<String, dynamic>)['fullName'] as String;
    final status = profile['status'] as String;
    final careLevel = careLevelLabels[profile['currentCareLevel']];
    return Material(
      color: Theme.of(context).colorScheme.surface,
      child: InkWell(
        onTap: onTap,
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 64),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                ResidentAvatar(name: name, id: profile['id'] as int),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(name, style: text.titleSmall),
                      if (careLevel != null) Text(careLevel, style: secondary),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                StatusPill(label: statusLabel(status), tone: statusTone(status)),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
