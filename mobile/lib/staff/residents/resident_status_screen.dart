import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/notifications/notification_api.dart';
import '../../common/theme/app_theme.dart';
import '../../common/widgets/header_app_bar.dart';
import '../../common/widgets/status_pill.dart';
import 'document_picker.dart';
import 'resident_labels.dart';

/// Chữ thường của tên lệnh, dùng trong nhãn nút xác nhận: "Xác nhận cho tạm vắng".
String _lower(String label) => label.substring(0, 1).toLowerCase() + label.substring(1);

/// Trạng thái và lệnh trạng thái trên app nhân viên (001 T052, T082; US4; FR-044 → FR-047b), cùng nội dung, thứ tự và chữ
/// với `StatusTab` của web (DESIGN.md mục 13 điều 7): trạng thái hiện tại kèm nút lệnh theo `allowedCommands`, lịch sử
/// trạng thái mới nhất trước. App chỉ có bốn lệnh Cho tạm vắng, Ghi nhận trở về, Chuyển viện, Ghi nhận qua đời (task);
/// Hoàn tất tiếp nhận, Hủy tiếp nhận, Kết thúc lưu trú làm trên web. Chỉ trực tuyến (research R-13): lệnh không vào hàng đợi
/// ngoại tuyến, mất mạng thì báo. Giao diện không tự suy ra quyền hay điều kiện; mọi kiểm tra thật do backend. Chưa đính
/// kèm giấy tờ (chụp, tải tệp: Ghi nhận trở về, Chuyển viện, bằng chứng Ghi nhận qua đời) vì `ApiClient` chưa tải tệp lên.
class ResidentStatusScreen extends StatefulWidget {
  ResidentStatusScreen({super.key, required this.api, required this.residentId, DocumentPicker? picker})
    : picker = picker ?? ImageDocumentPicker();

  final ApiClient api;
  final int residentId;

  /// Nguồn giấy tờ đính kèm lệnh (001 T082); test truyền bản giả.
  final DocumentPicker picker;

  @override
  State<ResidentStatusScreen> createState() => _ResidentStatusScreenState();
}

class _ResidentStatusScreenState extends State<ResidentStatusScreen> {
  Map<String, dynamic>? _profile;
  List<Map<String, dynamic>> _history = const [];
  bool _forbidden = false;
  Object? _loadError;
  String? _done;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loadError = null);
    try {
      final profile = await widget.api.get('/residents/${widget.residentId}');
      final history = (await widget.api.getList(
        '/residents/${widget.residentId}/status-history',
      )).cast<Map<String, dynamic>>();
      if (mounted) {
        setState(() {
          _profile = profile;
          _history = history;
          _forbidden = false;
        });
      }
    } on ApiProblem catch (e) {
      if (!mounted) return;
      setState(() {
        if (e.status == 403) {
          _forbidden = true;
          _profile ??= const {};
        } else {
          _loadError = e;
        }
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = e);
    }
  }

  Future<void> _open(String command) async {
    final profile = _profile!;
    final result = await showModalBottomSheet<Map<String, dynamic>>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      builder: (_) => _CommandSheet(
        api: widget.api,
        residentId: widget.residentId,
        status: profile['status'] as String,
        command: command,
        picker: widget.picker,
      ),
    );
    if (result == null || !mounted) return;
    final warnings = (result['warnings'] as List<dynamic>? ?? const [])
        .map((w) => (w as Map<String, dynamic>)['description'] as String)
        .toList();
    final status = result['status'] as String;
    setState(() {
      _done = 'Đã chuyển sang ${statusLabel(status)}.${warnings.isEmpty ? '' : ' Lưu ý: ${warnings.join('; ')}'}';
    });
    await _load();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final secondary = text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant);
    final profile = _profile;
    Widget body;
    if (_loadError != null) {
      body = ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(backendMessage(_loadError!), style: text.bodyMedium),
                  const SizedBox(height: 12),
                  OutlinedButton(onPressed: _load, child: const Text('Thử lại')),
                ],
              ),
            ),
          ),
        ],
      );
    } else if (profile == null) {
      body = const Center(child: CircularProgressIndicator());
    } else if (_forbidden) {
      body = ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Không có quyền xem lịch sử trạng thái', style: text.titleSmall),
                  const SizedBox(height: 4),
                  Text('Vai trò của bạn không được xem lịch sử trạng thái của người này.', style: text.bodyMedium),
                ],
              ),
            ),
          ),
        ],
      );
    } else {
      final status = profile['status'] as String;
      final allowed = (profile['allowedCommands'] as List<dynamic>? ?? const []).cast<String>();
      final commands = [
        for (final c in commandLabels.keys)
          if (allowed.contains(c)) c,
      ];
      final since = profile['statusSince'] as String? ?? (_history.isEmpty ? null : _history.last['at'] as String);
      body = ListView(
        children: [
          _Section(
            title: 'Trạng thái hiện tại',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Wrap(
                  spacing: 12,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    StatusPill(label: statusLabel(status), tone: statusTone(status)),
                    if (since != null)
                      Text('từ ${formatDateTime(DateTime.parse(since)).substring(0, 10)}', style: secondary),
                  ],
                ),
                const SizedBox(height: 12),
                if (finalStatuses.contains(status))
                  Text('Hồ sơ ở trạng thái cuối, chỉ xem. Không còn lệnh nào thực hiện được.', style: text.bodyMedium)
                else if (commands.isEmpty)
                  Text('Bạn không có lệnh nào dùng được ở trạng thái này.', style: text.bodyMedium)
                else
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: [
                      for (final (i, c) in commands.indexed)
                        i == 0
                            ? FilledButton(onPressed: () => _open(c), child: Text(commandLabels[c]!))
                            : OutlinedButton(onPressed: () => _open(c), child: Text(commandLabels[c]!)),
                    ],
                  ),
              ],
            ),
          ),
          if (_done != null)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Semantics(
                liveRegion: true,
                child: ColoredBox(
                  color: Theme.of(context).colorScheme.surface,
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(_done!, style: text.bodyMedium),
                  ),
                ),
              ),
            ),
          _Section(
            title: 'Lịch sử trạng thái',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                for (final item in _history.reversed) ...[_HistoryItem(item: item), const Divider(height: 1)],
              ],
            ),
          ),
          const SizedBox(height: 16),
        ],
      );
    }
    return Scaffold(
      appBar: headerAppBar(context, title: 'Trạng thái'),
      body: body,
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

/// Một dòng lịch sử: trạng thái cũ → mới, giờ, người thực hiện và lệnh, lý do; lệnh do hệ thống ghi nói rõ.
class _HistoryItem extends StatelessWidget {
  const _HistoryItem({required this.item});

  final Map<String, dynamic> item;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final secondary = text.bodySmall?.copyWith(color: Theme.of(context).colorScheme.onSurfaceVariant);
    final from = item['from'] as String?;
    final to = item['to'] as String;
    final command = item['command'] as String;
    final performer = item['performedBy'];
    final actor = item['bySystem'] == true
        ? 'Hệ thống (điểm danh), người điểm danh $performer'
        : '$performer, ${historyCommandLabels[command] ?? command}';
    final waiver = item['conditionWaiver'] as Map<String, dynamic>?;
    final reason = item['reason'] as String?;
    final hidden = (item['hiddenFields'] as List<dynamic>? ?? const []).cast<String>();
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 8,
            runSpacing: 4,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              if (from != null) ...[
                StatusPill(label: statusLabel(from), tone: statusTone(from)),
                const ExcludeSemantics(child: Text('→')),
              ],
              StatusPill(label: statusLabel(to), tone: statusTone(to)),
              if (waiver != null) const StatusPill(label: 'Miễn điều kiện', tone: StatusTone.neutralOutline),
              Text(formatDateTime(DateTime.parse(item['at'] as String)), style: secondary),
            ],
          ),
          Text(actor, style: text.bodyMedium),
          if (reason != null && reason.isNotEmpty) Text(reason, style: text.bodyMedium),
          if (hidden.contains('reason'))
            Text(
              'Lý do và căn cứ chỉ hiện với người được xem thông tin sức khỏe của người cao tuổi này.',
              style: secondary,
            ),
          if (waiver != null)
            Text('Theo yêu cầu ngoại lệ đã duyệt số ${waiver['approvalRequestId']}', style: secondary),
        ],
      ),
    );
  }
}

/// Khung lệnh (bottom sheet, DESIGN.md 15.3): xem trước điều kiện (GET …/conditions/{lệnh}, chưa thỏa thì liệt kê đủ và chặn
/// nút xác nhận), lý do bắt buộc, căn cứ của Chuyển viện. Lỗi 422 báo tại ô, 409 hiện câu của backend rồi xem trước lại điều
/// kiện (DESIGN.md 7.4). Đóng với kết quả của backend khi thành công.
class _CommandSheet extends StatefulWidget {
  const _CommandSheet({
    required this.api,
    required this.residentId,
    required this.status,
    required this.command,
    required this.picker,
  });

  final ApiClient api;
  final int residentId;
  final String status;
  final String command;
  final DocumentPicker picker;

  @override
  State<_CommandSheet> createState() => _CommandSheetState();
}

class _CommandSheetState extends State<_CommandSheet> {
  List<Map<String, dynamic>>? _conditions;
  final _reason = TextEditingController();
  final _basisText = TextEditingController();
  String? _basisKind;
  final List<PickedDocument> _documents = [];
  final Map<PickedDocument, int> _uploaded = {};
  Map<String, String> _errors = {};
  Object? _failure;
  bool _busy = false;

  /// Giấy tờ đính kèm: Ghi nhận trở về từ Điều trị tại bệnh viện, Chuyển viện (tối đa 10, `attachmentFileIds`), bằng chứng Ghi
  /// nhận qua đời (một tệp, `details.evidenceFileId`) (001 T082, FR-047b).
  bool get _takesDocuments =>
      widget.command == 'TRANSFER_OUT' ||
      widget.command == 'RECORD_DEATH' ||
      (widget.command == 'RECORD_RETURN' && widget.status == 'HOSPITALIZED');

  int get _maxDocuments => widget.command == 'RECORD_DEATH' ? 1 : 10;

  Future<void> _addDocument(Future<PickedDocument?> Function() source) async {
    final document = await source();
    if (document != null && mounted) setState(() => _documents.add(document));
  }

  /// Tải lên các tệp chưa tải (tệp đã tải ở lần gửi trước không tải lại); trả mã tệp theo thứ tự chọn.
  Future<List<int>> _uploadDocuments() async {
    for (final document in _documents) {
      if (_uploaded.containsKey(document)) continue;
      final result = await widget.api.uploadFile('/files', 'file', document.bytes, document.name, document.contentType);
      _uploaded[document] = result!['id'] as int;
    }
    return [for (final document in _documents) _uploaded[document]!];
  }

  String get _path => switch (widget.command) {
    'GRANT_TEMPORARY_ABSENCE' => 'grant-temporary-absence',
    'RECORD_RETURN' => 'record-return',
    'TRANSFER_OUT' => 'transfer-out',
    _ => 'record-death',
  };

  @override
  void initState() {
    super.initState();
    _loadConditions();
  }

  @override
  void dispose() {
    _reason.dispose();
    _basisText.dispose();
    super.dispose();
  }

  Future<void> _loadConditions() async {
    try {
      final conditions = (await widget.api.getList(
        '/residents/${widget.residentId}/conditions/${widget.command}',
      )).cast<Map<String, dynamic>>();
      if (mounted) setState(() => _conditions = conditions);
    } on Exception catch (e) {
      if (mounted) {
        setState(() {
          _conditions = const [];
          _failure = e;
        });
      }
    }
  }

  Future<void> _submit() async {
    final errors = <String, String>{};
    if (_reason.text.trim().isEmpty) errors['reason'] = 'Nhập lý do của lệnh';
    final kind = basisKinds.where((k) => k.value == _basisKind).firstOrNull;
    final value = _basisText.text.trim();
    if (widget.command == 'TRANSFER_OUT' &&
        (kind == null || value.isEmpty || (kind.numeric && int.tryParse(value) == null))) {
      errors['basis'] = 'Chọn và nhập căn cứ chuyển viện';
    }
    setState(() {
      _errors = errors;
      _failure = null;
    });
    if (errors.isNotEmpty) return;
    setState(() => _busy = true);
    try {
      final fileIds = _takesDocuments ? await _uploadDocuments() : const <int>[];
      final result = await widget.api.post(
        '/residents/${widget.residentId}/$_path',
        {
          'reason': _reason.text.trim(),
          'details': {
            if (widget.command == 'TRANSFER_OUT') 'basis': {kind!.value: kind.numeric ? int.parse(value) : value},
            if (widget.command == 'RECORD_DEATH' && fileIds.isNotEmpty) 'evidenceFileId': fileIds.first,
          },
          if (widget.command != 'RECORD_DEATH' && fileIds.isNotEmpty) 'attachmentFileIds': fileIds,
        },
        headers: {'Idempotency-Key': newIdempotencyKey()},
      );
      if (mounted) Navigator.of(context).pop(result ?? const {'status': ''});
    } on ApiProblem catch (e) {
      if (!mounted) return;
      setState(() {
        if (e.status == 422 && e.errorCode == 'REASON_REQUIRED') {
          _errors = {'reason': e.detail ?? 'Nhập lý do của lệnh'};
        } else if (e.status == 422 && e.errorCode == 'BASIS_REQUIRED') {
          _errors = {'basis': 'Chọn và nhập căn cứ chuyển viện'};
        } else if (e.status == 422 && e.errorCode == 'EVIDENCE_REQUIRED') {
          _errors = {
            'evidence':
                'Phải kèm giấy tờ bằng chứng khi Hành chính ghi nhận qua đời ngoài viện. Chụp hoặc chọn giấy tờ ở trên.',
          };
        } else {
          _failure = e;
          if (e.status == 409) _loadConditions();
        }
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _failure = e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final scheme = Theme.of(context).colorScheme;
    final label = commandLabels[widget.command]!;
    final conditions = _conditions;
    final unmet = conditions ?? const <Map<String, dynamic>>[];
    final blocked = conditions == null || unmet.isNotEmpty;
    return Padding(
      padding: EdgeInsets.fromLTRB(16, 16, 16, 16 + MediaQuery.of(context).viewInsets.bottom),
      child: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(label, style: text.titleMedium),
            const SizedBox(height: 4),
            Text(commandEffects[widget.command]!, style: text.bodyMedium),
            const SizedBox(height: 12),
            if (conditions == null)
              const LinearProgressIndicator()
            else if (unmet.isNotEmpty)
              Semantics(
                liveRegion: true,
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    border: Border.all(color: scheme.outlineVariant),
                    borderRadius: BorderRadius.circular(AppRadius.sm),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Chưa đủ điều kiện để thực hiện', style: text.titleSmall),
                        const SizedBox(height: 4),
                        for (final c in unmet)
                          Wrap(
                            spacing: 8,
                            crossAxisAlignment: WrapCrossAlignment.center,
                            children: [
                              Text(c['description'] as String, style: text.bodyMedium),
                              if (c['basis'] != null)
                                Text(
                                  '(${c['basis']})',
                                  style: text.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
                                ),
                              if (c['waivable'] == true)
                                const StatusPill(label: 'Miễn được', tone: StatusTone.neutralOutline),
                            ],
                          ),
                      ],
                    ),
                  ),
                ),
              )
            else
              Text('Đủ điều kiện để thực hiện.', style: text.bodyMedium?.copyWith(color: scheme.onSurfaceVariant)),
            const SizedBox(height: 12),
            TextField(
              controller: _reason,
              minLines: 3,
              maxLines: 5,
              maxLength: 2000,
              decoration: InputDecoration(labelText: 'Lý do của lệnh (bắt buộc)', errorText: _errors['reason']),
            ),
            if (widget.command == 'TRANSFER_OUT') ...[
              const SizedBox(height: 4),
              Text('Căn cứ chuyển viện (bắt buộc)', style: text.labelMedium),
              Wrap(
                spacing: 8,
                children: [
                  for (final k in basisKinds)
                    ChoiceChip(
                      label: Text(k.label),
                      selected: _basisKind == k.value,
                      onSelected: (_) => setState(() => _basisKind = k.value),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              TextField(
                controller: _basisText,
                keyboardType: _basisKind == 'incidentId' ? TextInputType.number : TextInputType.text,
                decoration: InputDecoration(
                  labelText: 'Nội dung căn cứ',
                  hintText: basisKinds.where((k) => k.value == _basisKind).firstOrNull?.hint,
                  errorText: _errors['basis'],
                ),
              ),
            ],
            if (_takesDocuments) ...[
              const SizedBox(height: 12),
              Text(
                widget.command == 'RECORD_DEATH' ? 'Giấy tờ bằng chứng' : 'Giấy tờ đính kèm (tối đa $_maxDocuments)',
                style: text.labelMedium,
              ),
              for (final document in _documents)
                Row(
                  children: [
                    Expanded(child: Text(document.name, style: text.bodyMedium)),
                    IconButton(
                      tooltip: 'Bỏ ${document.name}',
                      icon: const Icon(LucideIcons.x),
                      onPressed: _busy ? null : () => setState(() => _documents.remove(document)),
                    ),
                  ],
                ),
              if (_documents.length < _maxDocuments)
                Wrap(
                  spacing: 8,
                  children: [
                    OutlinedButton.icon(
                      onPressed: _busy ? null : () => _addDocument(widget.picker.capture),
                      icon: const Icon(LucideIcons.camera),
                      label: const Text('Chụp giấy tờ'),
                    ),
                    OutlinedButton.icon(
                      onPressed: _busy ? null : () => _addDocument(widget.picker.pickFromGallery),
                      icon: const Icon(LucideIcons.image),
                      label: const Text('Chọn ảnh'),
                    ),
                  ],
                ),
            ],
            if (_errors['evidence'] != null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Semantics(
                  liveRegion: true,
                  child: Text(_errors['evidence']!, style: text.bodyMedium?.copyWith(color: scheme.error)),
                ),
              ),
            if (_failure != null)
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Semantics(
                  liveRegion: true,
                  child: Text(backendMessage(_failure!), style: text.bodyMedium?.copyWith(color: scheme.error)),
                ),
              ),
            const SizedBox(height: 12),
            FilledButton(onPressed: _busy || blocked ? null : _submit, child: Text('Xác nhận ${_lower(label)}')),
            const SizedBox(height: 8),
            OutlinedButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Thôi')),
          ],
        ),
      ),
    );
  }
}
