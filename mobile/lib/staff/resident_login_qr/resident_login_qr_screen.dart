import 'dart:async';

import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:qr_flutter/qr_flutter.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/notifications/notification_api.dart' show newIdempotencyKey;
import '../../common/theme/app_theme.dart';

/// Nhãn xác nhận của chính người cao tuổi (api-002 `ResidentConsent.method`), cùng chữ với web `accountLabels.js`.
const consentMethodLabels = {'REQUESTED': 'Tự yêu cầu', 'AGREED': 'Đồng ý'};

String _countdown(Duration left) {
  final seconds = left.isNegative ? 0 : (left.inMilliseconds / 1000).ceil();
  return '${seconds ~/ 60}:${(seconds % 60).toString().padLeft(2, '0')}';
}

/// Điều dưỡng tạo mã QR đăng nhập cho người cao tuổi (002 T096, T104; US8, FR-069; bố cục duyệt 2026-11-06, cùng chữ với
/// khối web duyệt 2026-11-02). Chọn người cao tuổi trong phạm vi (`GET /residents`, backend lọc theo phạm vi), xem máy đang
/// gắn (`GET /resident-accounts/{id}`), ghi xác nhận của chính người đó rồi tạo mã dùng một lần kèm đếm ngược. Mã chỉ hiện
/// trên màn này, không lưu. Hết hạn thì tạo mã mới với xác nhận vừa nhập điền sẵn. Chỉ trực tuyến.
class ResidentLoginQrScreen extends StatefulWidget {
  const ResidentLoginQrScreen({super.key, required this.api, this.now = DateTime.now});

  final ApiClient api;

  /// Đồng hồ cho đếm ngược; test truyền đồng hồ giả.
  final DateTime Function() now;

  @override
  State<ResidentLoginQrScreen> createState() => _ResidentLoginQrScreenState();
}

class _ResidentLoginQrScreenState extends State<ResidentLoginQrScreen> {
  final _search = TextEditingController();
  List<Map<String, dynamic>>? _residents;
  Map<String, dynamic>? _resident;
  String? _loadError;

  @override
  void initState() {
    super.initState();
    _find();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _find() async {
    final q = _search.text.trim();
    try {
      final page = await widget.api.get(q.isEmpty ? '/residents' : '/residents?q=${Uri.encodeQueryComponent(q)}');
      final items = ((page?['items'] as List?) ?? const []).cast<Map<String, dynamic>>();
      if (mounted) {
        setState(() {
          _residents = items;
          _loadError = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = backendMessage(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    final resident = _resident;
    if (resident != null) {
      return _ResidentQr(
        key: ValueKey(resident['id']),
        api: widget.api,
        now: widget.now,
        residentId: resident['id'] as int,
        residentName: (resident['personalInfo'] as Map)['fullName'] as String,
        onBack: () => setState(() => _resident = null),
      );
    }
    final text = Theme.of(context).textTheme;
    final residents = _residents;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Chọn người cao tuổi để tạo mã đăng nhập', style: text.titleMedium),
        const SizedBox(height: 12),
        TextField(
          controller: _search,
          textInputAction: TextInputAction.search,
          onSubmitted: (_) => _find(),
          decoration: const InputDecoration(
            labelText: 'Tìm theo họ tên hoặc mã hồ sơ',
            prefixIcon: Icon(LucideIcons.search),
          ),
        ),
        const SizedBox(height: 8),
        if (_loadError != null) ...[
          Text(_loadError!, style: text.bodyMedium),
          const SizedBox(height: 8),
          OutlinedButton(onPressed: _find, child: const Text('Tải lại')),
        ] else if (residents == null)
          const Center(child: CircularProgressIndicator())
        else if (residents.isEmpty)
          Text('Không có người cao tuổi khớp.', style: text.bodyMedium)
        else
          for (final r in residents)
            ListTile(
              minTileHeight: 64,
              leading: CircleAvatar(child: Text(_initial((r['personalInfo'] as Map)['fullName'] as String))),
              title: Text((r['personalInfo'] as Map)['fullName'] as String),
              subtitle: Text('${r['profileCode']}'),
              trailing: const Icon(LucideIcons.chevronRight),
              onTap: () => setState(() => _resident = r),
            ),
      ],
    );
  }
}

/// Chữ cái đầu của tên gọi (chữ cuối của họ tên) cho ảnh chữ cái.
String _initial(String fullName) {
  final words = fullName.trim().split(RegExp(r'\s+'));
  return words.isEmpty || words.last.isEmpty ? '?' : words.last.characters.first.toUpperCase();
}

class _ResidentQr extends StatefulWidget {
  const _ResidentQr({
    super.key,
    required this.api,
    required this.now,
    required this.residentId,
    required this.residentName,
    required this.onBack,
  });

  final ApiClient api;
  final DateTime Function() now;
  final int residentId;
  final String residentName;
  final VoidCallback onBack;

  @override
  State<_ResidentQr> createState() => _ResidentQrState();
}

class _ResidentQrState extends State<_ResidentQr> {
  final _staffId = TextEditingController();
  String? _method;
  Map<String, dynamic>? _state;
  bool _noAccount = false;
  String? _loadError;

  /// Khóa của lần mở form: gửi lại sau khi mất mạng dùng cùng khóa; "Tạo mã mới" mở form mới với khóa mới.
  String _key = newIdempotencyKey();
  bool _submitted = false;
  bool _busy = false;
  String? _error;
  ({String code, DateTime expiresAt})? _qr;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _staffId.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loadError = null;
      _noAccount = false;
    });
    try {
      final state = await widget.api.get('/resident-accounts/${widget.residentId}');
      if (mounted) setState(() => _state = state ?? const {});
    } on ApiProblem catch (p) {
      if (!mounted) return;
      setState(() => p.status == 404 ? _noAccount = true : _loadError = backendMessage(p));
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = backendMessage(e));
    }
  }

  String? get _methodError => _method == null ? 'Chọn người cao tuổi tự yêu cầu hay đồng ý' : null;

  String? get _staffError => RegExp(r'^\d+$').hasMatch(_staffId.text.trim()) ? null : 'Nhập mã hồ sơ nhân viên';

  Future<void> _submit() async {
    setState(() {
      _submitted = true;
      _error = null;
    });
    if (_methodError != null || _staffError != null) return;
    setState(() => _busy = true);
    try {
      final body = await widget.api.post(
        '/resident-accounts/${widget.residentId}/login-qr-code',
        {
          'consent': {'method': _method, 'withStaffId': int.parse(_staffId.text.trim())},
        },
        headers: {'Idempotency-Key': _key},
      );
      if (!mounted) return;
      setState(() => _qr = (code: body!['code'] as String, expiresAt: DateTime.parse(body['expiresAt'] as String)));
      _timer?.cancel();
      _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
        if (!mounted) return;
        if (!_qr!.expiresAt.isAfter(widget.now())) timer.cancel();
        setState(() {});
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _newCode() {
    _timer?.cancel();
    setState(() {
      _qr = null;
      _key = newIdempotencyKey();
      _submitted = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final text = theme.textTheme;
    final tones = theme.extension<Tones>()!;
    final state = _state;
    final boundAt = (state?['boundDevice'] as Map?)?['boundAt'] as String?;
    final qr = _qr;
    final left = qr?.expiresAt.difference(widget.now());
    final expired = left != null && left <= Duration.zero;

    final content = <Widget>[
      Align(
        alignment: Alignment.centerLeft,
        child: TextButton(onPressed: widget.onBack, child: const Text('Chọn người cao tuổi khác')),
      ),
      Semantics(header: true, child: Text('Mã đăng nhập cho ${widget.residentName}', style: text.titleLarge)),
      const SizedBox(height: 4),
      Text('Mã dùng một lần. Tạo mã mới làm mã cũ hết hiệu lực. Cần kết nối mạng.', style: text.bodySmall),
      const SizedBox(height: 16),
    ];

    if (_noAccount) {
      content.add(
        Text(
          'Người cao tuổi này chưa có tài khoản đăng nhập trên app. Hành chính tạo tài khoản theo lời người cao tuổi.',
          style: text.bodyMedium,
        ),
      );
    } else if (_loadError != null) {
      content.addAll([
        Text(_loadError!, style: text.bodyMedium),
        const SizedBox(height: 8),
        OutlinedButton(onPressed: _load, child: const Text('Tải lại')),
      ]);
    } else if (state == null) {
      content.add(const Center(child: CircularProgressIndicator()));
    } else if (qr != null) {
      content.add(
        expired
            ? Semantics(liveRegion: true, child: Text('Mã đã hết hạn.', style: text.bodyLarge))
            : Column(
                children: [
                  Center(
                    child: Semantics(
                      image: true,
                      label: 'Mã QR đăng nhập',
                      child: ExcludeSemantics(
                        // Nền trắng, viền trắng để máy quét đọc được cả ở chế độ tối
                        child: ColoredBox(
                          color: Colors.white,
                          child: QrImageView(data: qr.code, size: 240, padding: const EdgeInsets.all(16)),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Còn ${_countdown(left!)}',
                    style: text.titleMedium?.copyWith(
                      color: tones.attentionInk,
                      fontFeatures: const [FontFeature.tabularFigures()],
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text('Đưa người cao tuổi quét mã bằng app trên máy của họ.', style: text.bodySmall),
                  if (boundAt != null) ...[
                    const SizedBox(height: 4),
                    Text('Quét mã sẽ gỡ máy đang gắn.', style: text.bodySmall?.copyWith(color: tones.attentionInk)),
                  ],
                ],
              ),
      );
    } else {
      content.addAll([
        if (boundAt != null) ...[
          _AttentionBand(text: 'Đang gắn máy từ ${formatDateTime(DateTime.parse(boundAt))}. Quét mã mới sẽ gỡ máy đó.'),
          const SizedBox(height: 16),
        ],
        Text('Người cao tuổi đã', style: text.titleSmall),
        RadioGroup<String>(
          groupValue: _method,
          onChanged: (v) => setState(() => _method = v),
          child: Column(
            children: [
              for (final e in consentMethodLabels.entries)
                RadioListTile<String>(value: e.key, title: Text(e.value), enabled: !_busy),
            ],
          ),
        ),
        if (_submitted && _methodError != null)
          Padding(
            padding: const EdgeInsets.only(left: 16, bottom: 8),
            child: Text(_methodError!, style: text.bodySmall?.copyWith(color: theme.colorScheme.error)),
          ),
        const SizedBox(height: 8),
        TextField(
          controller: _staffId,
          keyboardType: TextInputType.number,
          onChanged: (_) => setState(() {}),
          decoration: InputDecoration(
            labelText: 'Mã hồ sơ nhân viên đã nghe',
            helperText: 'Người cao tuổi nói trực tiếp với nhân viên này.',
            errorText: _submitted ? _staffError : null,
          ),
        ),
        if (_error != null) ...[
          const SizedBox(height: 12),
          Semantics(
            liveRegion: true,
            child: Text(_error!, style: text.bodyMedium?.copyWith(color: theme.colorScheme.error)),
          ),
        ],
      ]);
    }

    Widget? action;
    if (!_noAccount && _loadError == null && state != null) {
      if (qr == null) {
        action = FilledButton(onPressed: _busy ? null : _submit, child: const Text('Tạo mã QR'));
      } else if (expired) {
        action = FilledButton(onPressed: _newCode, child: const Text('Tạo mã mới'));
      }
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: content),
          ),
        ),
        // Nút chính cố định ở chân màn (DESIGN.md 15.4)
        if (action != null)
          SafeArea(
            top: false,
            child: Padding(padding: const EdgeInsets.all(16), child: action),
          ),
      ],
    );
  }
}

/// Dải `attention` báo việc cần để ý trước khi làm (DESIGN.md 3.5): quét mã mới sẽ gỡ máy đang gắn.
class _AttentionBand extends StatelessWidget {
  const _AttentionBand({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final tones = theme.extension<Tones>()!;
    return DecoratedBox(
      decoration: BoxDecoration(color: tones.attentionWash, borderRadius: BorderRadius.circular(AppRadius.lg)),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(LucideIcons.triangleAlert, size: 20, color: tones.attentionInk),
            const SizedBox(width: 8),
            Expanded(
              child: Text(text, style: theme.textTheme.bodyMedium?.copyWith(color: tones.attentionInk)),
            ),
          ],
        ),
      ),
    );
  }
}
