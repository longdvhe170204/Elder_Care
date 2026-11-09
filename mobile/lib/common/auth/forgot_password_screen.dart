import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/problem_message.dart';
import '../widgets/page_body.dart';

/// Tự đặt lại mật khẩu (002 T105; US3 #12 → #14; FR-052a, BR-M15-25; api-002 `/auth/password-reset/*`): nhập tên đăng
/// nhập, nhận mã qua tin nhắn tới số trong hồ sơ, nhập mã và mật khẩu mới. Máy chủ trả như nhau dù tài khoản có thật hay
/// không, nên màn không nói mã đã được gửi hay chưa. Đặt xong thì đóng màn, trả `true` cho màn đăng nhập.
class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _username = TextEditingController();
  final _code = TextEditingController();
  final _next = TextEditingController();
  final _again = TextEditingController();
  bool _codeStep = false;
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _username.dispose();
    _code.dispose();
    _next.dispose();
    _again.dispose();
    super.dispose();
  }

  Future<void> _run(Future<void> Function() work) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await work();
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _request() async {
    if (_username.text.trim().isEmpty) {
      setState(() => _error = 'Nhập tên đăng nhập');
      return;
    }
    await _run(() async {
      await widget.api.post('/auth/password-reset/request', {'username': _username.text.trim()});
      if (mounted) setState(() => _codeStep = true);
    });
  }

  Future<void> _confirm() async {
    if (_code.text.trim().isEmpty || _next.text.isEmpty) {
      setState(() => _error = 'Nhập mã trong tin nhắn và mật khẩu mới');
      return;
    }
    if (_next.text != _again.text) {
      setState(() => _error = 'Hai lần nhập mật khẩu mới chưa giống nhau');
      return;
    }
    await _run(() async {
      await widget.api.post('/auth/password-reset/confirm', {
        'username': _username.text.trim(),
        'code': _code.text.trim(),
        'newPassword': _next.text,
      });
      if (mounted) Navigator.of(context).pop(true);
    });
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      appBar: AppBar(title: const Text('Quên mật khẩu')),
      body: PageBody(
        child: ListView(
          children: [
            Text(
              _codeStep
                  ? 'Nếu tài khoản có số điện thoại đã đăng ký, mã đã được gửi tới số đó. Không nhận được mã thì liên hệ '
                        'viện để được cấp lại mật khẩu.'
                  : 'Nhập tên đăng nhập. Nếu hồ sơ có số điện thoại đã đăng ký, viện gửi mã đặt lại mật khẩu tới số đó.',
              style: text.bodyMedium,
            ),
            if (_error != null) ...[
              const SizedBox(height: 12),
              Text(_error!, style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error)),
            ],
            const SizedBox(height: 16),
            if (!_codeStep) ...[
              TextField(
                controller: _username,
                decoration: const InputDecoration(labelText: 'Tên đăng nhập'),
                autocorrect: false,
                autofillHints: const [AutofillHints.username],
              ),
              const SizedBox(height: 16),
              FilledButton(onPressed: _busy ? null : _request, child: const Text('Gửi mã')),
            ] else
              AutofillGroup(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    TextField(
                      controller: _code,
                      decoration: const InputDecoration(labelText: 'Mã trong tin nhắn'),
                      keyboardType: TextInputType.number,
                      autofillHints: const [AutofillHints.oneTimeCode],
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _next,
                      decoration: const InputDecoration(labelText: 'Mật khẩu mới'),
                      obscureText: true,
                      autofillHints: const [AutofillHints.newPassword],
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _again,
                      decoration: const InputDecoration(labelText: 'Nhập lại mật khẩu mới'),
                      obscureText: true,
                      autofillHints: const [AutofillHints.newPassword],
                    ),
                    const SizedBox(height: 16),
                    FilledButton(onPressed: _busy ? null : _confirm, child: const Text('Đặt mật khẩu mới')),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
