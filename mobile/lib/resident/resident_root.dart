import 'package:flutter/material.dart';

import '../common/auth/session_controller.dart';
import '../common/theme/app_theme.dart';
import 'biometric_unlock/biometric_screens.dart';
import 'qr_login/change_password_screen.dart';
import 'qr_login/qr_login_screen.dart';
import 'today/today_screen.dart';

/// Gốc phần người cao tuổi của app (002 T097; DESIGN.md 15.6): theme chữ lớn, không có thanh điều hướng dưới; màn
/// theo trạng thái phiên. Tài khoản nhân viên, người thân chưa có phần app (chủ dự án mở mobile cho người cao tuổi
/// trước, 2026-10-24).
class ResidentRoot extends StatelessWidget {
  const ResidentRoot({super.key, required this.session, this.scannerBuilder = cameraScanner});

  final SessionController session;
  final ScannerBuilder scannerBuilder;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Elder Care',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.resident(Brightness.light),
      darkTheme: AppTheme.resident(Brightness.dark),
      themeMode: ThemeMode.light,
      home: ListenableBuilder(listenable: session, builder: (context, _) => _screen()),
    );
  }

  Widget _screen() => switch (session.status) {
    SessionStatus.starting => const Scaffold(body: Center(child: CircularProgressIndicator())),
    SessionStatus.signedOut => QrLoginScreen(
      key: ValueKey(session.notice),
      session: session,
      scannerBuilder: scannerBuilder,
      notice: session.notice,
    ),
    SessionStatus.mustChangePassword => ChangePasswordScreen(session: session),
    SessionStatus.offerBiometric => BiometricOfferScreen(session: session),
    SessionStatus.locked => LockedScreen(session: session),
    SessionStatus.signedIn =>
      session.user?.type == AccountType.resident ? const TodayScreen() : const _NotOpenYet(),
  };
}

class _NotOpenYet extends StatelessWidget {
  const _NotOpenYet();

  @override
  Widget build(BuildContext context) => Scaffold(
    body: Center(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Text(
          'Ứng dụng mới có phần cho người cao tuổi. Nhân viên, người thân dùng trang web.',
          style: Theme.of(context).textTheme.bodyLarge,
          textAlign: TextAlign.center,
        ),
      ),
    ),
  );
}
