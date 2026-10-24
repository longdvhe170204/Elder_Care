import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../../common/auth/session_controller.dart';
import '../../common/widgets/sky_background.dart';
import 'login_messages.dart';

/// Khung quét mã; test thay bằng bản giả gọi thẳng [onCode].
typedef ScannerBuilder = Widget Function(void Function(String code) onCode);

Widget cameraScanner(void Function(String code) onCode) => MobileScanner(
  onDetect: (capture) {
    final value = capture.barcodes.isEmpty ? null : capture.barcodes.first.rawValue;
    if (value != null) onCode(value);
  },
);

/// Đăng nhập của người cao tuổi (002 T097, FR-069; DESIGN.md 15.6): quét mã QR nhân viên đưa là vào; nút phụ "Dùng
/// mật khẩu" ở cuối.
class QrLoginScreen extends StatefulWidget {
  const QrLoginScreen({super.key, required this.session, this.scannerBuilder = cameraScanner, this.notice});

  final SessionController session;
  final ScannerBuilder scannerBuilder;

  /// Câu báo khi vừa quay về màn này, ví dụ sau khi đổi mật khẩu.
  final String? notice;

  @override
  State<QrLoginScreen> createState() => _QrLoginScreenState();
}

class _QrLoginScreenState extends State<QrLoginScreen> {
  bool _usePassword = false;
  bool _busy = false;
  String? _error;
  final _username = TextEditingController();
  final _password = TextEditingController();

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _run(Future<void> Function() login) async {
    if (_busy) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await login();
    } on Exception catch (e) {
      if (mounted) setState(() => _error = loginError(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: SkyBackground(
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                if (widget.notice != null && _error == null) ...[
                  Text(widget.notice!, style: text.bodyLarge),
                  const SizedBox(height: 12),
                ],
                if (_error != null) ...[
                  Text(_error!, style: text.bodyLarge?.copyWith(color: Theme.of(context).colorScheme.error)),
                  const SizedBox(height: 12),
                ],
                Expanded(child: _usePassword ? _passwordForm(text) : _scanner(text)),
                const SizedBox(height: 12),
                if (_usePassword)
                  OutlinedButton(
                    onPressed: () => setState(() {
                      _usePassword = false;
                      _error = null;
                    }),
                    child: const Text('Quét mã QR'),
                  )
                else
                  OutlinedButton(
                    onPressed: () => setState(() {
                      _usePassword = true;
                      _error = null;
                    }),
                    child: const Text('Dùng mật khẩu'),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _scanner(TextTheme text) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      Text('Đặt mã QR nhân viên đưa vào trong khung.', style: text.headlineMedium),
      const SizedBox(height: 16),
      Expanded(
        child: ClipRRect(
          borderRadius: BorderRadius.circular(16),
          child: widget.scannerBuilder((code) => _run(() => widget.session.qrLogin(code))),
        ),
      ),
    ],
  );

  Widget _passwordForm(TextTheme text) => ListView(
    children: [
      Text('Đăng nhập bằng mật khẩu', style: text.headlineMedium),
      const SizedBox(height: 16),
      TextField(
        controller: _username,
        decoration: const InputDecoration(labelText: 'Tên đăng nhập'),
        autocorrect: false,
      ),
      const SizedBox(height: 12),
      TextField(
        controller: _password,
        decoration: const InputDecoration(labelText: 'Mật khẩu'),
        obscureText: true,
      ),
      const SizedBox(height: 16),
      FilledButton(
        onPressed: _busy
            ? null
            : () => _run(() => widget.session.passwordLogin(_username.text.trim(), _password.text)),
        child: const Text('Đăng nhập'),
      ),
    ],
  );
}
