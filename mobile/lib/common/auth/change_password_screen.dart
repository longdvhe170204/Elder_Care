import 'package:flutter/material.dart';

import '../api/api_client.dart';
import '../api/problem_message.dart';
import '../widgets/page_body.dart';
import 'session_controller.dart';

/// Đổi mật khẩu bắt buộc lần đầu (002 FR-006) cho cả ba loại tài khoản; [message] đổi lỗi thành câu báo theo phần app
/// (người cao tuổi dùng câu ngắn của DESIGN.md 15.6).
class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key, required this.session, this.message = backendMessage});

  final SessionController session;
  final String Function(Object error) message;

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final _current = TextEditingController();
  final _next = TextEditingController();
  bool _busy = false;
  String? _error;
  String? _newPasswordError;

  @override
  void dispose() {
    _current.dispose();
    _next.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    setState(() {
      _busy = true;
      _error = null;
      _newPasswordError = null;
    });
    try {
      await widget.session.changePassword(_current.text, _next.text);
    } on ApiProblem catch (p) {
      // 422 báo tại ô (DESIGN.md 15.3)
      if (mounted) {
        setState(() => p.status == 422 ? _newPasswordError = widget.message(p) : _error = widget.message(p));
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = widget.message(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: PageBody(
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: ListView(
              children: [
                Text('Đặt mật khẩu mới', style: text.headlineMedium),
                const SizedBox(height: 8),
                Text('Lần đầu vào ứng dụng cần đặt mật khẩu của riêng mình.', style: text.bodyLarge),
                if (_error != null) ...[
                  const SizedBox(height: 12),
                  Text(_error!, style: text.bodyLarge?.copyWith(color: Theme.of(context).colorScheme.error)),
                ],
                const SizedBox(height: 16),
                // Cho trình quản lý mật khẩu điền, lưu mật khẩu mới (DESIGN.md 15.5; WCAG 3.3.8)
                AutofillGroup(
                  child: Column(
                    children: [
                      TextField(
                        controller: _current,
                        decoration: const InputDecoration(labelText: 'Mật khẩu nhân viên đưa'),
                        obscureText: true,
                        autofillHints: const [AutofillHints.password],
                      ),
                      const SizedBox(height: 12),
                      TextField(
                        controller: _next,
                        decoration: InputDecoration(labelText: 'Mật khẩu mới', errorText: _newPasswordError),
                        obscureText: true,
                        autofillHints: const [AutofillHints.newPassword],
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 16),
                FilledButton(onPressed: _busy ? null : _save, child: const Text('Lưu mật khẩu mới')),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
