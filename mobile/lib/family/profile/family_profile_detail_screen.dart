import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/status_pill.dart';
import 'family_profile_labels.dart';
import 'family_resident_avatar.dart';

/// Màn hồ sơ người cao tuổi của người thân (001 T023; FR-022): đầu màn xanh đêm có ảnh đại diện, họ tên; trạng thái, thông
/// tin cơ bản (ngày sinh, giới tính, mức chăm sóc hiện hành), và phần "Sức khỏe" CHỈ khi backend trả `activeHealthItems`
/// (có bản đồng ý Hiệu lực cho người thân này, chú thích ¹); thiếu thì bỏ cả phần, không báo gì thêm. Backend chỉ trả mục
/// đang Hiệu lực; màn vẫn bỏ mục Đã loại trừ phòng khi lẫn vào. Không có lưu ý chăm sóc, địa chỉ, giấy tờ: `FamilyProfile`
/// của api-001 không có. Chỉ đọc, chỉ trực tuyến.
class FamilyProfileDetailScreen extends StatefulWidget {
  const FamilyProfileDetailScreen({super.key, required this.api, required this.id});

  final ApiClient api;
  final int id;

  @override
  State<FamilyProfileDetailScreen> createState() => _FamilyProfileDetailScreenState();
}

class _FamilyProfileDetailScreenState extends State<FamilyProfileDetailScreen> {
  Map<String, dynamic>? _profile;
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loadError = null);
    try {
      final profile = await widget.api.get('/residents/${widget.id}');
      if (mounted) setState(() => _profile = profile);
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final profile = _profile;
    return Scaffold(
      appBar: headerAppBar(context, title: 'Hồ sơ người cao tuổi'),
      body: _loadError != null
          ? ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(backendMessage(_loadError!), style: Theme.of(context).textTheme.bodyMedium),
                        const SizedBox(height: 12),
                        OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
                      ],
                    ),
                  ),
                ),
              ],
            )
          : profile == null
          ? const Center(child: CircularProgressIndicator())
          : _Body(profile: profile),
    );
  }
}

class _Body extends StatelessWidget {
  const _Body({required this.profile});

  final Map<String, dynamic> profile;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final tones = Theme.of(context).extension<Tones>()!;
    final info = profile['personalInfo'] as Map<String, dynamic>;
    final name = info['fullName'] as String;
    final status = profile['status'] as String;
    final health = (profile['activeHealthItems'] as List<dynamic>?)
        ?.cast<Map<String, dynamic>>()
        .where((item) => item['status'] != 'EXCLUDED')
        .toList();
    return ListView(
      children: [
        ColoredBox(
          color: tones.header,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            child: Row(
              children: [
                FamilyResidentAvatar(name: name, id: profile['id'] as int, size: 56),
                const SizedBox(width: 16),
                Expanded(
                  child: Text(name, style: text.titleLarge?.copyWith(color: tones.headerInkStrong)),
                ),
              ],
            ),
          ),
        ),
        _Section(
          title: 'Thông tin cơ bản',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: StatusPill(label: statusLabel(status), tone: statusTone(status)),
                ),
              ),
              _Field(label: 'Ngày sinh', value: formatDate(info['dateOfBirth'] as String)),
              _Field(label: 'Giới tính', value: genderLabels[info['gender']]),
              _Field(label: 'Mức chăm sóc hiện hành', value: careLevelLabels[profile['currentCareLevel']]),
            ],
          ),
        ),
        if (health != null)
          _Section(
            title: 'Sức khỏe',
            child: _Health(items: health),
          ),
        const SizedBox(height: 16),
      ],
    );
  }
}

/// Dị ứng đứng đầu trong khối nhấn (tông `attention`, như web `AllergyNote`), rồi bệnh nền, tiền sử bệnh.
class _Health extends StatelessWidget {
  const _Health({required this.items});

  final List<Map<String, dynamic>> items;

  String _name(Map<String, dynamic> item) =>
      (item['allergen'] as Map<String, dynamic>?)?['name'] as String? ?? item['freeText'] as String? ?? '';

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final tones = Theme.of(context).extension<Tones>()!;
    if (items.isEmpty) return Text('Chưa ghi nhận', style: text.bodyMedium);
    final allergies = [
      for (final item in items)
        if (item['type'] == 'ALLERGY') '${_name(item)}${item['severity'] == null ? '' : ' (${item['severity']})'}',
    ];
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (allergies.isNotEmpty)
          DecoratedBox(
            decoration: BoxDecoration(color: tones.attentionWash, borderRadius: BorderRadius.circular(AppRadius.sm)),
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ExcludeSemantics(child: Icon(LucideIcons.triangleAlert, size: 18, color: tones.attentionInk)),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Dị ứng: ${allergies.join(', ')}',
                      style: text.bodyMedium?.copyWith(color: tones.attentionInk),
                    ),
                  ),
                ],
              ),
            ),
          ),
        for (final type in const ['CHRONIC_CONDITION', 'MEDICAL_HISTORY'])
          if (items.any((item) => item['type'] == type))
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    healthTypeLabels[type]!,
                    style: text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
                  ),
                  for (final item in items.where((item) => item['type'] == type))
                    Text(_name(item), style: text.bodyMedium),
                ],
              ),
            ),
      ],
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: ColoredBox(
        color: Theme.of(context).colorScheme.surface,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Semantics(
                header: true,
                child: Text(
                  title,
                  style: text.labelMedium?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant),
                ),
              ),
              const SizedBox(height: 8),
              child,
            ],
          ),
        ),
      ),
    );
  }
}

class _Field extends StatelessWidget {
  const _Field({required this.label, required this.value});

  final String label;
  final String? value;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant)),
          Text(value == null || value!.isEmpty ? 'Chưa có' : value!, style: text.bodyMedium),
        ],
      ),
    );
  }
}
