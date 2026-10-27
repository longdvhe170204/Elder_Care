import 'package:flutter/material.dart';

import '../api/problem_message.dart';
import '../widgets/sky_background.dart';

/// Form đăng nhập bằng tên đăng nhập, mật khẩu dùng chung cho phần nhân viên và người thân (002 T046, T047): thẻ trắng
/// trên nền trời, nút chính "Đăng nhập", lỗi theo câu backend trả (không lộ tài khoản có hay không, FR-014).
class PasswordLoginForm extends StatefulWidget {
  const PasswordLoginForm({super.key, required this.title, required this.hint, required this.onSubmit});

  final String title;

  /// Dòng phụ dưới tiêu đề, ví dụ cảnh báo máy dùng chung.
  final String hint;
  final Future<void> Function(String username, String password) onSubmit;

  @override
  State<PasswordLoginForm> createState() => _PasswordLoginFormState();
}

class _PasswordLoginFormState extends State<PasswordLoginForm> {
  final _username = TextEditingController();
  final _password = TextEditingController();
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
      // Đăng nhập xong `ElderCareApp` dựng lại màn theo trạng thái phiên
      await widget.onSubmit(_username.text.trim(), _password.text);
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
      appBar: AppBar(title: Text(widget.title)),
      body: SkyBackground(
        child: Card(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: ListView(
              shrinkWrap: true,
              children: [
                Text(widget.hint, style: text.bodyMedium),
                if (_error != null) ...[
                  const SizedBox(height: 12),
                  Text(_error!, style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error)),
                ],
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
                FilledButton(onPressed: _busy ? null : _submit, child: const Text('Đăng nhập')),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
