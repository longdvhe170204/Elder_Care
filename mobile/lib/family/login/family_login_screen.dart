import 'package:flutter/material.dart';

import '../../common/auth/password_login_form.dart';
import '../../common/auth/session_controller.dart';

/// Người thân đăng nhập (002 T047); lần đầu buộc đổi mật khẩu ở màn đổi mật khẩu chung.
class FamilyLoginScreen extends StatelessWidget {
  const FamilyLoginScreen({super.key, required this.session});

  final SessionController session;

  @override
  Widget build(BuildContext context) => PasswordLoginForm(
    title: 'Người thân đăng nhập',
    hint: 'Dùng tên đăng nhập và mật khẩu viện cấp.',
    onSubmit: session.familyLogin,
  );
}
