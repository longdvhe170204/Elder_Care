import 'package:flutter/material.dart';

import '../../common/auth/password_login_form.dart';
import '../../common/auth/session_controller.dart';

/// Nhân viên đăng nhập trên máy dùng chung tại tầng (002 T046; FR-014a: một máy một phiên).
class StaffLoginScreen extends StatelessWidget {
  const StaffLoginScreen({super.key, required this.session});

  final SessionController session;

  @override
  Widget build(BuildContext context) => PasswordLoginForm(
    title: 'Nhân viên đăng nhập',
    hint: 'Máy dùng chung: đăng nhập sẽ kết thúc phiên của người trước trên máy này.',
    onSubmit: session.staffLogin,
  );
}
