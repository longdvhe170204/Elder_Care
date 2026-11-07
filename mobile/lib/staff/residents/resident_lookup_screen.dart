import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/residents/resident_avatar.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/status_pill.dart';
import 'resident_card_screen.dart';
import 'resident_labels.dart';

/// Tra cứu người cao tuổi trên app nhân viên (001 T023; US1; FR-007; UC-02): ô tìm (`q`: họ tên, mã hồ sơ hoặc số giấy
/// tờ), chip lọc trạng thái (`status`), danh sách dòng theo DESIGN.md 15.4, bấm dòng mở thẻ người cao tuổi. Cùng ô tìm,
/// bộ lọc, thứ tự nội dung và chữ với màn web `ResidentListPage` (DESIGN.md mục 13 điều 7); chưa có "Cần đánh giá lại"
/// (US6). Chỉ đọc và chỉ trực tuyến: hợp đồng không có lệnh ghi ở màn này, mất mạng thì báo. Kết quả đã lọc theo phạm vi ở
/// backend; vai trò không được xem (Nhân viên bếp) nhận 403 và màn hiện đúng câu backend trả.
class ResidentLookupScreen extends StatefulWidget {
  const ResidentLookupScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<ResidentLookupScreen> createState() => _ResidentLookupScreenState();
}

class _ResidentLookupScreenState extends State<ResidentLookupScreen> {
  final _search = TextEditingController();
  String _query = '';
  String? _status;
  List<Map<String, dynamic>>? _items;
  String? _nextCursor;
  Object? _loadError;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _load([String? cursor]) async {
    final query = Uri(queryParameters: {'q': ?(_query.isEmpty ? null : _query), 'status': ?_status, 'cursor': ?cursor});
    try {
      final page = await widget.api.get('/residents${query.hasQuery ? '?${query.query}' : ''}');
      final items = (page?['items'] as List<dynamic>? ?? const []).cast<Map<String, dynamic>>();
      if (!mounted) return;
      setState(() {
        _items = cursor == null ? items : [...?_items, ...items];
        _nextCursor = page?['nextCursor'] as String?;
        _loadError = null;
      });
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _items = [...?(cursor == null ? null : _items)];
          _loadError = e;
        });
      }
    }
  }

  void _reload(VoidCallback change) {
    setState(() {
      change();
      _items = null;
      _nextCursor = null;
      _loadError = null;
    });
    _load();
  }

  void _open(Map<String, dynamic> profile) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => ResidentCardScreen(api: widget.api, id: profile['id'] as int),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final items = _items;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _SearchHeader(controller: _search, onSubmitted: (value) => _reload(() => _query = value.trim())),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Row(
            children: [
              Padding(
                padding: const EdgeInsets.only(right: 8),
                child: ChoiceChip(
                  label: const Text('Tất cả'),
                  selected: _status == null,
                  onSelected: (_) => _reload(() => _status = null),
                ),
              ),
              for (final status in statusFilters)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: Text(statusLabel(status)),
                    selected: _status == status,
                    onSelected: (_) => _reload(() => _status = status),
                  ),
                ),
            ],
          ),
        ),
        Expanded(
          child: items == null
              ? const Center(child: CircularProgressIndicator())
              : ListView(
                  children: [
                    if (_loadError != null)
                      _Notice(
                        title: backendMessage(_loadError!),
                        actionLabel: 'Thử lại',
                        onAction: () => _nextCursor == null ? _reload(() {}) : _load(_nextCursor),
                      )
                    else if (items.isEmpty)
                      const _Notice(
                        title: 'Không có hồ sơ khớp điều kiện',
                        description: 'Đổi từ khóa tìm hoặc chọn "Tất cả".',
                      ),
                    for (final profile in items) ...[
                      _ResidentRow(profile: profile, onTap: () => _open(profile)),
                      const Divider(height: 1),
                    ],
                    if (_nextCursor != null && _loadError == null)
                      Padding(
                        padding: const EdgeInsets.all(16),
                        child: OutlinedButton(onPressed: () => _load(_nextCursor), child: const Text('Xem thêm')),
                      ),
                  ],
                ),
        ),
      ],
    );
  }
}

/// Khối xanh đêm chứa ô tìm nền trắng (DESIGN.md 15.4, màn danh sách); liền với thanh tiêu đề của `StaffRoot`.
class _SearchHeader extends StatelessWidget {
  const _SearchHeader({required this.controller, required this.onSubmitted});

  final TextEditingController controller;
  final ValueChanged<String> onSubmitted;

  @override
  Widget build(BuildContext context) {
    final tones = Theme.of(context).extension<Tones>()!;
    return ColoredBox(
      color: tones.header,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
        child: Semantics(
          label: 'Tìm hồ sơ',
          child: TextField(
            controller: controller,
            textInputAction: TextInputAction.search,
            onSubmitted: onSubmitted,
            decoration: InputDecoration(
              filled: true,
              fillColor: Theme.of(context).colorScheme.surface,
              hintText: 'Họ tên, mã hồ sơ hoặc số giấy tờ',
              prefixIcon: const ExcludeSemantics(child: Icon(LucideIcons.search)),
              suffixIcon: IconButton(
                tooltip: 'Tìm',
                icon: const Icon(LucideIcons.arrowRight),
                onPressed: () => onSubmitted(controller.text),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Dòng danh sách (DESIGN.md 15.4): ảnh đại diện, họ tên, mã hồ sơ và mức chăm sóc, bên phải chữ trạng thái.
class _ResidentRow extends StatelessWidget {
  const _ResidentRow({required this.profile, required this.onTap});

  final Map<String, dynamic> profile;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final secondary = text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant);
    final info = profile['personalInfo'] as Map<String, dynamic>;
    final name = info['fullName'] as String;
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
                      Text([profile['profileCode'], ?careLevel].join(' · '), style: secondary),
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

/// Khối báo trên danh sách: lỗi tải (có nút) hoặc không có kết quả (mục 7.3).
class _Notice extends StatelessWidget {
  const _Notice({required this.title, this.description, this.actionLabel, this.onAction});

  final String title;
  final String? description;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(title, style: description == null ? text.bodyMedium : text.titleSmall),
              if (description != null) ...[const SizedBox(height: 4), Text(description!, style: text.bodyMedium)],
              if (actionLabel != null) ...[
                const SizedBox(height: 12),
                OutlinedButton(onPressed: onAction, child: Text(actionLabel!)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
