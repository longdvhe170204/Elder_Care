import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/status_pill.dart';
import '../assessments/assessment_screen.dart';
import 'resident_avatar.dart';
import 'resident_health_screen.dart';
import 'resident_labels.dart';
import 'resident_status_screen.dart';

/// Thẻ người cao tuổi trên app nhân viên (001 T023; US1; FR-008, FR-009; DESIGN.md 15.3: hồ sơ là màn riêng): đầu màn xanh
/// đêm có ảnh đại diện, họ tên, mã hồ sơ; rồi trạng thái, "An toàn" (dị ứng, lưu ý chăm sóc), thông tin cá nhân, nhu cầu
/// chăm sóc, hồ sơ trước của cùng người. Cùng thứ tự nội dung và chữ với khung hồ sơ web `ResidentDetail`. Chỉ trường có
/// trong `Profile` của api-001; trường backend bỏ theo quyền nằm ở `hiddenFields`. Chỉ đọc, chỉ trực tuyến; các phần
/// Sức khỏe, Đánh giá, Trạng thái và lệnh cập nhật thuộc task khác của 001.
class ResidentCardScreen extends StatefulWidget {
  const ResidentCardScreen({super.key, required this.api, required this.id});

  final ApiClient api;
  final int id;

  @override
  State<ResidentCardScreen> createState() => _ResidentCardScreenState();
}

class _ResidentCardScreenState extends State<ResidentCardScreen> {
  Map<String, dynamic>? _profile;
  List<Map<String, dynamic>> _linked = const [];
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
      if (!mounted) return;
      setState(() => _profile = profile);
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
      return;
    }
    try {
      final linked = (await widget.api.getList('/residents/${widget.id}/linked-profiles')).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _linked = linked);
    } on Exception {
      // Hồ sơ trước chỉ bổ sung; không tải được thì bỏ khối này (như web).
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
          : _CardBody(api: widget.api, profile: profile, linked: _linked, onChanged: _load),
    );
  }
}

class _CardBody extends StatelessWidget {
  const _CardBody({required this.api, required this.profile, required this.linked, required this.onChanged});

  final ApiClient api;
  final Map<String, dynamic> profile;
  final List<Map<String, dynamic>> linked;

  /// Tải lại thẻ sau khi quay về từ một phần của hồ sơ (trạng thái, mục sức khỏe có thể đã đổi).
  final VoidCallback onChanged;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final secondary = text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant);
    final tones = Theme.of(context).extension<Tones>()!;
    final info = profile['personalInfo'] as Map<String, dynamic>;
    final name = info['fullName'] as String;
    final status = profile['status'] as String;
    final hidden = (profile['hiddenFields'] as List<dynamic>? ?? const []).cast<String>();
    final healthHidden = hidden.contains('activeHealthItems');
    final allergies = [
      for (final item in (profile['activeHealthItems'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>())
        if (item['type'] == 'ALLERGY' && item['status'] != 'EXCLUDED')
          (item['allergen'] as Map<String, dynamic>?)?['name'] as String? ?? item['freeText'] as String? ?? '',
    ];
    final careNote = profile['careNote'] as String?;
    final careNeeds = (profile['careNeeds'] as List<dynamic>?)?.cast<String>();
    final contact = info['contact'] as Map<String, dynamic>?;
    final idType = info['idDocumentType'] as String?;
    final since = profile['statusSince'] as String?;
    final allowedCommands = (profile['allowedCommands'] as List<dynamic>? ?? const []).cast<String>();
    return ListView(
      children: [
        ColoredBox(
          color: tones.header,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            child: Row(
              children: [
                ResidentAvatar(name: name, id: profile['id'] as int, size: 56),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(name, style: text.titleLarge?.copyWith(color: tones.headerInkStrong)),
                      Text(profile['profileCode'] as String, style: text.bodySmall?.copyWith(color: tones.headerInk)),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
        _Section(
          title: 'Trạng thái',
          child: Row(
            children: [
              StatusPill(label: statusLabel(status), tone: statusTone(status)),
              if (since != null) ...[
                const SizedBox(width: 8),
                Text('từ ${formatDateTime(DateTime.parse(since)).substring(0, 10)}', style: secondary),
              ],
            ],
          ),
        ),
        if (!healthHidden)
          _Section(
            title: 'An toàn',
            child: allergies.isEmpty && (careNote == null || careNote.isEmpty)
                ? Text('Chưa ghi nhận', style: text.bodyMedium)
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      if (allergies.isNotEmpty)
                        DecoratedBox(
                          decoration: BoxDecoration(
                            color: tones.attentionWash,
                            borderRadius: BorderRadius.circular(AppRadius.sm),
                          ),
                          child: Padding(
                            padding: const EdgeInsets.all(12),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                ExcludeSemantics(
                                  child: Icon(LucideIcons.triangleAlert, size: 18, color: tones.attentionInk),
                                ),
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
                      if (careNote != null && careNote.isNotEmpty) ...[
                        if (allergies.isNotEmpty) const SizedBox(height: 12),
                        _Field(label: 'Lưu ý chăm sóc', value: careNote),
                      ],
                    ],
                  ),
          ),
        _Section(
          title: 'Thông tin cá nhân',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _Field(label: 'Ngày sinh', value: formatDate(info['dateOfBirth'] as String)),
              _Field(label: 'Giới tính', value: genderLabels[info['gender']]),
              _Field(
                label: 'Giấy tờ',
                value: idType == null
                    ? null
                    : '${idDocumentLabels[idType] ?? idType} ${info['idDocumentNumber'] ?? ''}'.trim(),
              ),
              _Field(label: 'Điện thoại', value: contact?['phoneNumber'] as String?),
              _Field(label: 'Thư điện tử', value: contact?['email'] as String?),
              _Field(label: 'Mức chăm sóc hiện hành', value: careLevelLabels[profile['currentCareLevel']]),
              _Field(label: 'Loại hình lưu trú', value: residenceTypeLabels[profile['currentResidenceType']]),
              _Field(label: 'Địa chỉ thường trú', value: info['permanentAddress'] as String?),
              _Field(label: 'Địa chỉ liên hệ', value: info['contactAddress'] as String?),
              _Field(label: 'Thông tin đặc biệt', value: info['specialNotes'] as String?),
              if (hidden.isNotEmpty) Text('Một số thông tin không hiển thị theo quyền của bạn.', style: secondary),
            ],
          ),
        ),
        if (careNeeds != null)
          _Section(
            title: 'Nhu cầu chăm sóc',
            child: careNeeds.isEmpty
                ? Text('Chưa ghi nhận', style: text.bodyMedium)
                : Wrap(spacing: 8, runSpacing: 8, children: [for (final need in careNeeds) Chip(label: Text(need))]),
          ),
        if (linked.isNotEmpty)
          _Section(
            title: 'Hồ sơ trước của cùng người',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                for (final l in linked)
                  Text(
                    l['viewable'] == true && l['status'] != null
                        ? '${l['profileCode']} · ${statusLabel(l['status'] as String)}'
                        : '${l['profileCode']} · không có quyền xem',
                    style: text.bodyMedium,
                  ),
              ],
            ),
          ),
        _Section(
          title: 'Các phần của hồ sơ',
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (!healthHidden)
                _PartButton(
                  label: 'Sức khỏe',
                  onPressed: () => _open(
                    context,
                    ResidentHealthScreen(api: api, residentId: profile['id'] as int, allowedCommands: allowedCommands),
                  ),
                ),
              if (allowedCommands.contains('CREATE_ASSESSMENT'))
                _PartButton(
                  label: 'Đánh giá',
                  onPressed: () => _open(
                    context,
                    AssessmentScreen(api: api, residentId: profile['id'] as int, allowedCommands: allowedCommands),
                  ),
                ),
              _PartButton(
                label: 'Trạng thái',
                onPressed: () => _open(context, ResidentStatusScreen(api: api, residentId: profile['id'] as int)),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
      ],
    );
  }

  Future<void> _open(BuildContext context, Widget screen) async {
    await Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => screen));
    onChanged();
  }
}

/// Nút mở một phần của hồ sơ (DESIGN.md 15.3: phần dài là màn riêng).
class _PartButton extends StatelessWidget {
  const _PartButton({required this.label, required this.onPressed});

  final String label;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: OutlinedButton(onPressed: onPressed, child: Text(label)),
  );
}

/// Một phần của thẻ: nhãn nhỏ phía trên, nội dung trên nền trắng, các phần cách nhau 8.
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

/// Nhãn nhỏ và giá trị; giá trị rỗng ghi "Chưa có" (như web).
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
