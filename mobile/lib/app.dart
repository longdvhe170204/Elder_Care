import 'package:flutter/material.dart';

import 'common/api/problem_message.dart';
import 'common/auth/change_password_screen.dart';
import 'common/auth/login_choice_screen.dart';
import 'common/auth/session_controller.dart';
import 'common/offline/offline_queue.dart';
import 'common/theme/app_theme.dart';
import 'family/family_root.dart';
import 'family/login/family_login_screen.dart';
import 'resident/biometric_unlock/biometric_screens.dart';
import 'resident/qr_login/login_messages.dart';
import 'resident/qr_login/qr_login_screen.dart';
import 'resident/resident_root.dart';
import 'staff/login/staff_login_screen.dart';
import 'staff/staff_root.dart';

/// App chung (000 T006, T007; Q-346, Q-365): một `MaterialApp`, màn theo trạng thái phiên. Chưa đăng nhập thì chọn
/// "Tôi là" nhân viên, người thân hay người cao tuổi (002 T046, T047); máy đã gắn với tài khoản người cao tuổi vào
/// thẳng màn quét mã (FR-014a). Đăng nhập xong chuyển sang phần tương ứng theo loại tài khoản. Phần người cao tuổi dùng
/// theme chữ lớn (DESIGN.md 15.6), hai phần kia dùng theme chung (15.2).
class ElderCareApp extends StatelessWidget {
  const ElderCareApp({super.key, required this.session, required this.queue, this.scannerBuilder = cameraScanner});

  final SessionController session;

  /// Hàng đợi ngoại tuyến của phần nhân viên (000 T100).
  final OfflineQueue queue;
  final ScannerBuilder scannerBuilder;

  bool get _resident => session.user == null ? session.boundToResident : session.user!.type == AccountType.resident;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: session,
      builder: (context, _) {
        final resident = _resident;
        return MaterialApp(
          // Đổi trạng thái phiên thì dựng lại Navigator: màn đăng nhập mở từ màn chọn không đè lên màn mới
          key: ValueKey((session.status, session.user?.type)),
          title: 'Elder Care',
          debugShowCheckedModeBanner: false,
          theme: resident ? AppTheme.resident(Brightness.light) : AppTheme.light(),
          darkTheme: resident ? AppTheme.resident(Brightness.dark) : AppTheme.dark(),
          themeMode: ThemeMode.light,
          home: _screen(),
        );
      },
    );
  }

  Widget _qrLogin() => QrLoginScreen(
    key: ValueKey(session.notice),
    session: session,
    scannerBuilder: scannerBuilder,
    notice: session.notice,
  );

  Widget _screen() => switch (session.status) {
    SessionStatus.starting => const Scaffold(body: Center(child: CircularProgressIndicator())),
    SessionStatus.signedOut when session.boundToResident => _qrLogin(),
    SessionStatus.signedOut => LoginChoiceScreen(
      key: ValueKey(session.notice),
      notice: session.notice,
      staffLogin: (_) => StaffLoginScreen(session: session),
      familyLogin: (_) => FamilyLoginScreen(session: session),
      // Màn quét mã của người cao tuổi giữ theme chữ lớn dù mở từ màn chọn
      residentLogin: (_) => Theme(data: AppTheme.resident(Brightness.light), child: _qrLogin()),
    ),
    SessionStatus.mustChangePassword => ChangePasswordScreen(
      session: session,
      message: _resident ? loginError : backendMessage,
    ),
    SessionStatus.offerBiometric => BiometricOfferScreen(session: session),
    SessionStatus.locked => LockedScreen(session: session),
    SessionStatus.signedIn => switch (session.user!.type) {
      AccountType.staff => StaffRoot(session: session, queue: queue),
      AccountType.familyMember => FamilyRoot(session: session),
      AccountType.resident => const ResidentRoot(),
    },
  };
}
