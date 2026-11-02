import 'package:flutter/material.dart';

import '../api/problem_message.dart';
import '../widgets/page_body.dart';

/// Màn đầu khi chưa đăng nhập (000 T006; 002 T046, T047; sửa 2026-10-28, chủ dự án duyệt): một form tên đăng nhập, mật
/// khẩu cho nhân viên và người thân (kênh `MOBILE_APP`, backend gán kênh phiên theo loại tài khoản nên thời gian tự đăng
/// xuất theo kênh giữ nguyên, FR-013a), kèm câu máy dùng chung (FR-014a); nút phụ mở màn quét mã của người cao tuổi
/// (FR-069). Máy đã gắn với tài khoản người cao tuổi không qua màn này. Màn quét mã do `ElderCareApp` truyền vào để
/// `common/` không phụ thuộc `resident/` (mobile/README.md luật 1). Lỗi theo câu backend trả (không lộ tài khoản có hay
/// không, FR-014).
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key, required this.onSubmit, required this.residentLogin, this.notice});

  final Future<void> Function(String username, String password) onSubmit;
  final WidgetBuilder residentLogin;

  /// Câu báo, ví dụ sau khi phiên bị kết thúc.
  final String? notice;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
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
      body: PageBody(
        child: Align(
          alignment: Alignment.topCenter,
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: ListView(
                shrinkWrap: true,
                children: [
                  Text('Elder Care', style: text.headlineMedium),
                  const SizedBox(height: 8),
                  Text(
                    'Nhân viên, người thân đăng nhập bằng tài khoản viện cấp. Máy dùng chung: đăng nhập sẽ kết thúc phiên '
                    'của người trước trên máy này.',
                    style: text.bodyMedium,
                  ),
                  if (widget.notice != null) ...[
                    const SizedBox(height: 12),
                    Text(widget.notice!, style: text.bodyMedium),
                  ],
                  if (_error != null) ...[
                    const SizedBox(height: 12),
                    Text(_error!, style: text.bodyMedium?.copyWith(color: Theme.of(context).colorScheme.error)),
                  ],
                  const SizedBox(height: 16),
                  // Cho trình quản lý mật khẩu điền (DESIGN.md 15.5; WCAG 3.3.8)
                  AutofillGroup(
                    child: Column(
                      children: [
                        TextField(
                          controller: _username,
                          decoration: const InputDecoration(labelText: 'Tên đăng nhập'),
                          autocorrect: false,
                          autofillHints: const [AutofillHints.username],
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: _password,
                          decoration: const InputDecoration(labelText: 'Mật khẩu'),
                          obscureText: true,
                          autofillHints: const [AutofillHints.password],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  FilledButton(onPressed: _busy ? null : _submit, child: const Text('Đăng nhập')),
                  const SizedBox(height: 12),
                  OutlinedButton(
                    onPressed: () => Navigator.of(context).push(MaterialPageRoute<void>(builder: widget.residentLogin)),
                    child: const Text('Người cao tuổi: quét mã QR'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
