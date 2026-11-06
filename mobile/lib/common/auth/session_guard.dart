import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/problem_message.dart';
import '../widgets/page_body.dart';
import 'session_controller.dart';

/// Hết phiên trên app phần nhân viên (002 T047c; FR-013a, FR-041a; DESIGN.md 9.1, 9.3, 15.5; bố cục duyệt 2026-11-06), cùng
/// chữ và cách làm với `SessionGuard` của web. Đếm giờ không thao tác như backend: mỗi phản hồi của yêu cầu không kèm
/// `X-Background-Refresh` đếm lại; thời hạn lấy từ `sessionIdleTimeoutSeconds` của `GET /me`, `null` thì không báo. Còn 2
/// phút thì báo, "Tiếp tục làm việc" gửi `GET /me` để đếm lại. Hết giờ hoặc gặp 401 `SESSION_EXPIRED` thì đẩy màn đăng
/// nhập lại lên trên cùng: các màn bên dưới (kể cả màn, bảng đang mở chồng) không bị tháo nên phần đang nhập còn nguyên.
/// Hàng đợi ngoại tuyến không bị đụng tới (FR-041a). Bọc màn gốc (route đầu) để đẩy được route lên Navigator của app.
class SessionGuard extends StatefulWidget {
  const SessionGuard({super.key, required this.session, required this.child, this.now = DateTime.now});

  final SessionController session;
  final Widget child;

  /// Đồng hồ; test truyền đồng hồ giả.
  final DateTime Function() now;

  static const warnBefore = Duration(minutes: 2);

  @override
  State<SessionGuard> createState() => _SessionGuardState();
}

const _ended = {'SESSION_EXPIRED', 'UNAUTHORIZED'};

String _countdown(Duration left) {
  final seconds = left.isNegative ? 0 : (left.inMilliseconds / 1000).ceil();
  return '${seconds ~/ 60}:${(seconds % 60).toString().padLeft(2, '0')}';
}

class _SessionGuardState extends State<SessionGuard> {
  final _remaining = ValueNotifier<Duration>(Duration.zero);
  late DateTime _lastActive = widget.now();
  Duration? _timeout;
  Timer? _timer;
  Route<void>? _warning;
  Route<void>? _expired;

  ApiClient get _api => widget.session.api;

  @override
  void initState() {
    super.initState();
    _api.addResponseListener(_onResponse);
    _loadTimeout();
  }

  @override
  void dispose() {
    _api.removeResponseListener(_onResponse);
    _timer?.cancel();
    _remaining.dispose();
    super.dispose();
  }

  Future<void> _loadTimeout() async {
    try {
      final seconds = UserInfo.fromJson((await _api.get('/me'))!).sessionIdleTimeoutSeconds;
      if (!mounted || seconds == null) return;
      _timeout = Duration(seconds: seconds);
      _timer = Timer.periodic(const Duration(seconds: 1), (_) => _tick());
    } on Exception {
      // Mất mạng: không đếm trên máy; backend vẫn trả 401 SESSION_EXPIRED khi phiên đã hết
    }
  }

  void _onResponse(int status, String? errorCode, bool background) {
    if (_expired != null) return;
    if (status == 401 && _ended.contains(errorCode)) {
      _expire();
    } else if (!background && status != 401) {
      _lastActive = widget.now();
      _tick();
    }
  }

  void _tick() {
    final timeout = _timeout;
    if (timeout == null || _expired != null || !mounted) return;
    final left = _lastActive.add(timeout).difference(widget.now());
    _remaining.value = left;
    if (left <= Duration.zero) {
      _expire();
    } else if (left <= SessionGuard.warnBefore) {
      _showWarning();
    } else {
      _closeWarning();
    }
  }

  void _showWarning() {
    if (_warning != null) return;
    _warning = DialogRoute<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => PopScope(
        canPop: false,
        child: _WarningDialog(
          remaining: _remaining,
          onContinue: () => _api.get('/me').catchError((_) => null),
          onSignOut: () => widget.session.signOut(),
        ),
      ),
    );
    Navigator.of(context).push(_warning!);
  }

  void _closeWarning() {
    final route = _warning;
    if (route == null) return;
    _warning = null;
    Navigator.of(context).removeRoute(route);
  }

  void _expire() {
    if (_expired != null || !mounted) return;
    _closeWarning();
    _expired = MaterialPageRoute<void>(
      fullscreenDialog: true,
      builder: (_) => PopScope(
        canPop: false,
        child: _ExpiredScreen(session: widget.session, onResumed: _resumed),
      ),
    );
    Navigator.of(context).push(_expired!);
  }

  void _resumed() {
    final route = _expired;
    if (route == null || !mounted) return;
    _expired = null;
    _lastActive = widget.now();
    Navigator.of(context).removeRoute(route);
    _tick();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}

class _WarningDialog extends StatelessWidget {
  const _WarningDialog({required this.remaining, required this.onContinue, required this.onSignOut});

  final ValueListenable<Duration> remaining;
  final VoidCallback onContinue;
  final VoidCallback onSignOut;

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Phiên đăng nhập sắp hết'),
      content: ValueListenableBuilder(
        valueListenable: remaining,
        builder: (context, left, _) => Text(
          'Bạn chưa thao tác một lúc. Phiên sẽ kết thúc sau ${_countdown(left)}; phần đang nhập vẫn còn nếu bạn đăng '
          'nhập lại.',
        ),
      ),
      actions: [
        TextButton(onPressed: onSignOut, child: const Text('Đăng xuất')),
        // Focus đầu tiên vào nút giữ phiên, không vào Đăng xuất
        FilledButton(autofocus: true, onPressed: onContinue, child: const Text('Tiếp tục làm việc')),
      ],
    );
  }
}

/// Màn che kín để người khác dùng máy không đọc được dữ liệu; nút quay lại không đóng được. Tên đăng nhập điền sẵn và
/// khóa khi đã biết, vì chỉ đúng tài khoản này được làm tiếp (DESIGN.md 9.1).
class _ExpiredScreen extends StatefulWidget {
  const _ExpiredScreen({required this.session, required this.onResumed});

  final SessionController session;
  final VoidCallback onResumed;

  @override
  State<_ExpiredScreen> createState() => _ExpiredScreenState();
}

class _ExpiredScreenState extends State<_ExpiredScreen> {
  late final _username = TextEditingController(text: widget.session.username);
  final _password = TextEditingController();
  late final bool _known = widget.session.username != null;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.session.resume(_username.text.trim(), _password.text);
      widget.onResumed();
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: PageBody(
        child: Align(
          alignment: Alignment.topCenter,
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: ListView(
                shrinkWrap: true,
                children: [
                  Semantics(header: true, child: Text('Phiên đăng nhập đã hết', style: text.titleLarge)),
                  const SizedBox(height: 8),
                  Text(
                    'Phiên kết thúc vì không có thao tác quá thời gian cho phép. Đăng nhập lại đúng tài khoản này để làm '
                    'tiếp; phần đang nhập vẫn còn nguyên.',
                    style: text.bodyMedium,
                  ),
                  if (_error != null) ...[
                    const SizedBox(height: 12),
                    Semantics(
                      liveRegion: true,
                      child: Text(
                        _error!,
                        style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error),
                      ),
                    ),
                  ],
                  const SizedBox(height: 16),
                  AutofillGroup(
                    child: Column(
                      children: [
                        TextField(
                          controller: _username,
                          readOnly: _known,
                          decoration: const InputDecoration(labelText: 'Tên đăng nhập'),
                          autocorrect: false,
                          autofillHints: const [AutofillHints.username],
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: _password,
                          autofocus: true,
                          decoration: const InputDecoration(labelText: 'Mật khẩu'),
                          obscureText: true,
                          autofillHints: const [AutofillHints.password],
                          onSubmitted: (_) => _busy ? null : _submit(),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  FilledButton(onPressed: _busy ? null : _submit, child: const Text('Đăng nhập lại')),
                  const SizedBox(height: 8),
                  TextButton(onPressed: () => widget.session.signOut(), child: const Text('Đăng xuất')),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
