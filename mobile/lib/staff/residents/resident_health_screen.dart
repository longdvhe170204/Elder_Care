import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/notifications/notification_api.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/status_pill.dart';
import 'resident_labels.dart';

const _statusFilters = [('ACTIVE', 'Hiệu lực'), ('EXCLUDED', 'Đã loại trừ'), ('ALL', 'Tất cả')];

const _kinds = [
  ('ALLERGY_CATALOG', 'Dị ứng (danh mục)'),
  ('ALLERGY_OTHER', 'Dị ứng khác, nhập tay'),
  ('CHRONIC_CONDITION', 'Bệnh nền'),
  ('MEDICAL_HISTORY', 'Tiền sử bệnh'),
];

String _nameOf(Map<String, dynamic> item) =>
    (item['allergen'] as Map<String, dynamic>?)?['name'] as String? ?? item['freeText'] as String? ?? '';

String _dateOf(String iso) => formatDateTime(DateTime.parse(iso)).substring(0, 10);

/// Dị ứng, bệnh nền, tiền sử bệnh trên app nhân viên (001 T033; US2; FR-018 → FR-022; DBR-04), cùng nội dung, thứ tự và
/// chữ với `HealthItems` của web (DESIGN.md mục 13 điều 7): bộ lọc Hiệu lực | Đã loại trừ | Tất cả, khối dị ứng đứng trước,
/// mục chia theo loại kèm nguồn, người ghi, ngày. Nút "Ghi nhận mục", "Loại trừ" mở bottom sheet (15.3) và hiện theo
/// `allowedCommands` của hồ sơ (`RECORD_HEALTH_ITEM`, `EXCLUDE_HEALTH_ITEM`); mục không có nút Sửa, Xóa. Ghi mục sức khỏe
/// chỉ trực tuyến (CLAUDE.md giai đoạn mobile; không vào hàng đợi ngoại tuyến): mất mạng thì báo, giữ form. Hồ sơ sức khỏe
/// ban đầu (FR-017) chưa có trên app.
class ResidentHealthScreen extends StatefulWidget {
  const ResidentHealthScreen({super.key, required this.api, required this.residentId, required this.allowedCommands});

  final ApiClient api;
  final int residentId;
  final List<String> allowedCommands;

  @override
  State<ResidentHealthScreen> createState() => _ResidentHealthScreenState();
}

class _ResidentHealthScreenState extends State<ResidentHealthScreen> {
  String _filter = 'ACTIVE';
  List<Map<String, dynamic>>? _items;
  Object? _loadError;
  List<String> _warnings = const [];

  bool get _canRecord => widget.allowedCommands.contains('RECORD_HEALTH_ITEM');
  bool get _canExclude => widget.allowedCommands.contains('EXCLUDE_HEALTH_ITEM');
  bool get _canVerify => widget.allowedCommands.contains('VERIFY_HEALTH_ITEM');

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final items = (await widget.api.getList(
        '/residents/${widget.residentId}/health-items?status=$_filter',
      )).cast<Map<String, dynamic>>();
      if (mounted) {
        setState(() {
          _items = items;
          _loadError = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _items = const [];
          _loadError = e;
        });
      }
    }
  }

  void _setFilter(String value) {
    setState(() {
      _filter = value;
      _items = null;
      _loadError = null;
    });
    _load();
  }

  Future<void> _record() async {
    final warnings = await showModalBottomSheet<List<String>>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => _RecordSheet(api: widget.api, residentId: widget.residentId),
    );
    if (warnings == null || !mounted) return;
    setState(() => _warnings = warnings);
    await _load();
  }

  /// Bác sĩ, Điều dưỡng kiểm chứng mục tự khai của gia đình (Q-400, BR-M01-17); một chiều, không cần thân yêu cầu.
  Future<void> _verify(Map<String, dynamic> item) async {
    try {
      await widget.api.post(
        '/health-items/${item['id']}/verify',
        {},
        headers: {'Idempotency-Key': newIdempotencyKey()},
      );
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
      return;
    }
    if (mounted) await _load();
  }

  Future<void> _exclude(Map<String, dynamic> item) async {
    final done = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => _ExcludeSheet(api: widget.api, item: item),
    );
    if (done == true && mounted) await _load();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;
    final tones = Theme.of(context).extension<Tones>()!;
    final items = _items;
    final allergies = [
      for (final i in items ?? const <Map<String, dynamic>>[])
        if (i['type'] == 'ALLERGY' && i['status'] == 'ACTIVE')
          i['severity'] == null ? _nameOf(i) : '${_nameOf(i)} (${i['severity']})',
    ];
    return Scaffold(
      appBar: headerAppBar(context, title: 'Sức khỏe'),
      body: items == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                Row(
                  children: [
                    Expanded(child: Text('Dị ứng, bệnh nền, tiền sử bệnh', style: text.titleMedium)),
                    if (_canRecord) FilledButton(onPressed: _record, child: const Text('Ghi nhận mục')),
                  ],
                ),
                if (_warnings.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Semantics(
                    liveRegion: true,
                    child: _Callout(title: 'Đã ghi nhận, cần chú ý', lines: _warnings),
                  ),
                ],
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  child: Row(
                    children: [
                      for (final (value, label) in _statusFilters)
                        Padding(
                          padding: const EdgeInsets.only(right: 8),
                          child: ChoiceChip(
                            label: Text(label),
                            selected: _filter == value,
                            onSelected: (_) => _setFilter(value),
                          ),
                        ),
                    ],
                  ),
                ),
                if (_loadError != null)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text(backendMessage(_loadError!), style: text.bodyMedium),
                          const SizedBox(height: 12),
                          OutlinedButton(onPressed: () => _setFilter(_filter), child: const Text('Thử lại')),
                        ],
                      ),
                    ),
                  )
                else if (items.isEmpty)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Chưa ghi nhận dị ứng, bệnh nền hay tiền sử bệnh', style: text.titleSmall),
                          const SizedBox(height: 4),
                          Text(
                            _filter == 'EXCLUDED' ? 'Chưa có mục nào bị loại trừ.' : 'Mục ghi nhận xong sẽ hiện ở đây.',
                            style: text.bodyMedium,
                          ),
                        ],
                      ),
                    ),
                  ),
                if (allergies.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: DecoratedBox(
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
                  ),
                for (final type in healthTypeLabels.keys)
                  if (items.any((i) => i['type'] == type)) ...[
                    Padding(
                      padding: const EdgeInsets.only(top: 4, bottom: 8),
                      child: Semantics(
                        header: true,
                        child: Text(
                          healthTypeLabels[type]!,
                          style: text.labelMedium?.copyWith(color: scheme.onSurfaceVariant),
                        ),
                      ),
                    ),
                    for (final item in items.where((i) => i['type'] == type)) ...[
                      _ItemRow(
                        item: item,
                        canExclude: _canExclude,
                        canVerify: _canVerify,
                        onExclude: () => _exclude(item),
                        onVerify: () => _verify(item),
                      ),
                      const Divider(height: 1),
                    ],
                  ],
              ],
            ),
    );
  }
}

/// Dòng mục sức khỏe: tên (kèm mức độ với dị ứng), nhãn, nguồn và người ghi; mục đã loại trừ ghi người, ngày, lý do.
class _ItemRow extends StatelessWidget {
  const _ItemRow({
    required this.item,
    required this.canExclude,
    required this.canVerify,
    required this.onExclude,
    required this.onVerify,
  });

  final Map<String, dynamic> item;
  final bool canExclude;
  final bool canVerify;
  final VoidCallback onExclude;
  final VoidCallback onVerify;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final secondary = text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant);
    final excluded = item['status'] == 'EXCLUDED';
    final exclusion = item['exclusion'] as Map<String, dynamic>?;
    return Material(
      color: Theme.of(context).colorScheme.surface,
      child: ConstrainedBox(
        constraints: const BoxConstraints(minHeight: 64),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Wrap(
                spacing: 12,
                runSpacing: 4,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Text(_nameOf(item), style: text.titleSmall),
                  if (item['type'] == 'ALLERGY' && item['severity'] != null)
                    Text('Mức độ: ${item['severity']}', style: secondary),
                  if (excluded) const StatusPill(label: 'Đã loại trừ', tone: StatusTone.neutral),
                  if (item['unverified'] == true && !excluded)
                    const StatusPill(label: 'Tự khai, chưa được Bác sĩ kiểm chứng', tone: StatusTone.attention),
                  if (item['notAutoChecked'] == true)
                    const StatusPill(label: 'Không kiểm tra tự động', tone: StatusTone.attention),
                  if (item['pendingCheck'] == true && !excluded)
                    const StatusPill(label: 'Chờ kiểm tra thuốc và chế độ ăn', tone: StatusTone.neutral),
                ],
              ),
              Text(
                'Nguồn: ${item['source']}. Ghi bởi ${item['recordedBy']}, ${_dateOf(item['at'] as String)}',
                style: secondary,
              ),
              if (excluded && exclusion != null)
                Text(
                  'Loại trừ bởi ${exclusion['by']}, ${_dateOf(exclusion['at'] as String)}. Lý do: ${exclusion['reason']}',
                  style: secondary,
                ),
              if (!excluded && item['unverified'] == true && canVerify)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: OutlinedButton(onPressed: onVerify, child: const Text('Kiểm chứng')),
                ),
              if (!excluded && canExclude)
                Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: OutlinedButton(onPressed: onExclude, child: const Text('Loại trừ')),
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Khối nhấn tông `attention` có tiêu đề và các dòng (cảnh báo sau khi ghi, mục trùng).
class _Callout extends StatelessWidget {
  const _Callout({required this.title, required this.lines, this.action});

  final String title;
  final List<String> lines;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final tones = Theme.of(context).extension<Tones>()!;
    return DecoratedBox(
      decoration: BoxDecoration(color: tones.attentionWash, borderRadius: BorderRadius.circular(AppRadius.sm)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: text.titleSmall?.copyWith(color: tones.attentionInk)),
            for (final line in lines) Text(line, style: text.bodyMedium?.copyWith(color: tones.attentionInk)),
            ?action,
          ],
        ),
      ),
    );
  }
}

/// Khung ghi nhận mục (bottom sheet): loại mục, dị nguyên hoặc nội dung, mức độ (dị ứng), nguồn. Danh mục lấy từ
/// GET /catalogs/{type}?status=ACTIVE. Mục trùng Hiệu lực: 409 `DUPLICATE_HEALTH_ITEM`, bấm "Vẫn ghi nhận mục này" gửi lại
/// với `confirmDuplicate` (FR-021). Đóng với danh sách cảnh báo của backend khi ghi xong.
class _RecordSheet extends StatefulWidget {
  const _RecordSheet({required this.api, required this.residentId});

  final ApiClient api;
  final int residentId;

  @override
  State<_RecordSheet> createState() => _RecordSheetState();
}

class _RecordSheetState extends State<_RecordSheet> {
  Map<String, List<Map<String, dynamic>>>? _catalogs;
  String _kind = 'ALLERGY_CATALOG';
  int? _allergenId;
  int? _severityId;
  int? _sourceId;
  final _freeText = TextEditingController();
  Map<String, String> _errors = {};
  Object? _failure;
  bool _duplicate = false;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _loadCatalogs();
  }

  @override
  void dispose() {
    _freeText.dispose();
    super.dispose();
  }

  Future<List<Map<String, dynamic>>> _catalog(String type) async =>
      (await widget.api.getList('/catalogs/$type?status=ACTIVE')).cast<Map<String, dynamic>>();

  Future<void> _loadCatalogs() async {
    setState(() => _failure = null);
    try {
      final allergens = await _catalog('allergens');
      final severities = await _catalog('allergy-severities');
      final sources = await _catalog('information-sources');
      if (mounted) setState(() => _catalogs = {'allergens': allergens, 'severities': severities, 'sources': sources});
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = e);
    }
  }

  bool get _isAllergy => _kind.startsWith('ALLERGY');

  Map<String, String> _validate() => {
    if (_kind == 'ALLERGY_CATALOG' && _allergenId == null) 'allergen': 'Chọn dị nguyên',
    if (_kind != 'ALLERGY_CATALOG' && _freeText.text.trim().isEmpty) 'freeText': 'Nhập nội dung',
    if (_isAllergy && _severityId == null) 'severity': 'Chọn mức độ',
    if (_sourceId == null) 'source': 'Chọn nguồn thông tin',
  };

  Future<void> _submit({bool confirmDuplicate = false}) async {
    final errors = _validate();
    setState(() {
      _errors = errors;
      _failure = null;
      if (!confirmDuplicate) _duplicate = false;
    });
    if (errors.isNotEmpty) return;
    setState(() => _busy = true);
    try {
      final result = await widget.api.post(
        '/residents/${widget.residentId}/health-items',
        {
          'type': _isAllergy ? 'ALLERGY' : _kind,
          if (_kind == 'ALLERGY_CATALOG') 'allergenId': _allergenId else 'freeText': _freeText.text.trim(),
          if (_isAllergy) 'severityId': _severityId,
          'sourceId': _sourceId,
          if (confirmDuplicate) 'confirmDuplicate': true,
        },
        headers: {'Idempotency-Key': newIdempotencyKey()},
      );
      final warnings = (result?['warnings'] as List<dynamic>? ?? const [])
          .map((w) => (w as Map<String, dynamic>)['description'] as String)
          .toList();
      if (mounted) Navigator.of(context).pop(warnings);
    } on ApiProblem catch (e) {
      if (mounted) {
        setState(() {
          if (e.errorCode == 'DUPLICATE_HEALTH_ITEM') {
            _duplicate = true;
          } else {
            _failure = e;
          }
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Widget _dropdown(
    String label,
    List<Map<String, dynamic>> options,
    int? value,
    String errorKey,
    ValueChanged<int?> set,
  ) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: DropdownButtonFormField<int>(
        initialValue: value,
        isExpanded: true,
        icon: const Icon(LucideIcons.chevronDown),
        decoration: InputDecoration(labelText: label, errorText: _errors[errorKey]),
        items: [
          for (final o in options)
            DropdownMenuItem(
              value: o['id'] as int,
              child: Text(o['name'] as String, overflow: TextOverflow.ellipsis),
            ),
        ],
        onChanged: (v) => setState(() => set(v)),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final catalogs = _catalogs;
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Ghi nhận mục sức khỏe', style: text.titleMedium),
            const SizedBox(height: 12),
            if (catalogs == null)
              _failure != null
                  ? Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Text(backendMessage(_failure!), style: text.bodyMedium),
                        const SizedBox(height: 12),
                        OutlinedButton(onPressed: _loadCatalogs, child: const Text('Thử lại')),
                      ],
                    )
                  : const Center(child: CircularProgressIndicator())
            else ...[
              Text('Loại mục', style: text.labelMedium),
              Wrap(
                spacing: 8,
                children: [
                  for (final (value, label) in _kinds)
                    ChoiceChip(
                      label: Text(label),
                      selected: _kind == value,
                      onSelected: (_) => setState(() {
                        _kind = value;
                        _errors = {};
                        _duplicate = false;
                      }),
                    ),
                ],
              ),
              const SizedBox(height: 12),
              if (_kind == 'ALLERGY_CATALOG')
                _dropdown('Dị nguyên', catalogs['allergens']!, _allergenId, 'allergen', (v) => _allergenId = v)
              else
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: TextField(
                    controller: _freeText,
                    maxLength: 1000,
                    minLines: 2,
                    maxLines: 4,
                    decoration: InputDecoration(labelText: 'Nội dung', errorText: _errors['freeText']),
                  ),
                ),
              if (_isAllergy)
                _dropdown('Mức độ', catalogs['severities']!, _severityId, 'severity', (v) => _severityId = v),
              _dropdown('Nguồn thông tin', catalogs['sources']!, _sourceId, 'source', (v) => _sourceId = v),
              if (_kind == 'ALLERGY_OTHER')
                const Padding(
                  padding: EdgeInsets.only(bottom: 12),
                  child: _Callout(
                    title: 'Dị ứng nhập tay không kiểm tra tự động',
                    lines: [
                      'Xung đột với thuốc và chế độ ăn sẽ không được phát hiện tự động. '
                          'Nên chọn từ danh mục khi dị nguyên đã có.',
                    ],
                  ),
                ),
              if (_failure != null)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: Text(
                    backendMessage(_failure!),
                    style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error),
                  ),
                ),
              if (_duplicate)
                Padding(
                  padding: const EdgeInsets.only(bottom: 12),
                  child: _Callout(
                    title: 'Đã có mục đang hiệu lực trùng nội dung',
                    lines: const ['Kiểm tra danh sách rồi quyết định có ghi thêm không.'],
                    action: Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: OutlinedButton(
                        onPressed: _busy ? null : () => _submit(confirmDuplicate: true),
                        child: const Text('Vẫn ghi nhận mục này'),
                      ),
                    ),
                  ),
                ),
              FilledButton(onPressed: _busy ? null : _submit, child: const Text('Ghi nhận')),
              const SizedBox(height: 8),
              OutlinedButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Hủy')),
            ],
          ],
        ),
      ),
    );
  }
}

/// Khung loại trừ mục (bottom sheet): lý do bắt buộc; mục không bị sửa hay xóa (DBR-04).
class _ExcludeSheet extends StatefulWidget {
  const _ExcludeSheet({required this.api, required this.item});

  final ApiClient api;
  final Map<String, dynamic> item;

  @override
  State<_ExcludeSheet> createState() => _ExcludeSheetState();
}

class _ExcludeSheetState extends State<_ExcludeSheet> {
  final _reason = TextEditingController();
  String? _error;
  Object? _failure;
  bool _busy = false;

  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_reason.text.trim().isEmpty) {
      setState(() => _error = 'Nhập lý do loại trừ');
      return;
    }
    setState(() {
      _error = null;
      _failure = null;
      _busy = true;
    });
    try {
      await widget.api.post(
        '/health-items/${widget.item['id']}/exclude',
        {'reason': _reason.text.trim()},
        headers: {'Idempotency-Key': newIdempotencyKey()},
      );
      if (mounted) Navigator.of(context).pop(true);
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Loại trừ: ${_nameOf(widget.item)}', style: text.titleMedium),
            const SizedBox(height: 12),
            TextField(
              controller: _reason,
              maxLength: 2000,
              minLines: 2,
              maxLines: 4,
              decoration: InputDecoration(labelText: 'Lý do loại trừ', errorText: _error),
            ),
            if (_failure != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Text(
                  backendMessage(_failure!),
                  style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error),
                ),
              ),
            FilledButton(onPressed: _busy ? null : _submit, child: const Text('Xác nhận loại trừ')),
            const SizedBox(height: 8),
            OutlinedButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Hủy')),
          ],
        ),
      ),
    );
  }
}
