import 'package:flutter/material.dart';

import 'common/api/problem_message.dart';
import 'common/auth/change_password_screen.dart';
import 'common/auth/login_screen.dart';
import 'common/auth/session_controller.dart';
import 'common/auth/session_guard.dart';
import 'common/notifications/push_coordinator.dart';
import 'common/offline/offline_queue.dart';
import 'common/theme/app_theme.dart';
import 'family/family_root.dart';
import 'resident/biometric_unlock/biometric_screens.dart';
import 'resident/qr_login/login_messages.dart';
import 'resident/qr_login/qr_login_screen.dart';
import 'resident/resident_root.dart';
import 'staff/staff_root.dart';

/// App chung (000 T006, T007; Q-346, Q-365): một `MaterialApp`, màn theo trạng thái phiên. Chưa đăng nhập thì mở màn
/// đăng nhập chung của nhân viên, người thân, có nút sang màn quét mã người cao tuổi (002 T046, T047); máy đã gắn với
/// tài khoản người cao tuổi vào thẳng màn quét mã (FR-014a). Đăng nhập xong chuyển sang phần tương ứng theo loại tài khoản. Phần người cao tuổi dùng
/// theme chữ lớn (DESIGN.md 15.6), hai phần kia dùng theme chung (15.2).
class ElderCareApp extends StatelessWidget {
  const ElderCareApp({
    super.key,
    required this.session,
    required this.queue,
    this.push,
    this.scannerBuilder = cameraScanner,
  });

  final SessionController session;

  /// Hàng đợi ngoại tuyến của phần nhân viên (000 T100).
  final OfflineQueue queue;
  final ScannerBuilder scannerBuilder;

  /// Nhận đẩy (009 T031, T042); test không truyền thì không có đẩy.
  final PushCoordinator? push;

  bool get _resident => session.user == null ? session.boundToResident : session.user!.type == AccountType.resident;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: session,
      builder: (context, _) {
        final resident = _resident;
        return MaterialApp(
          // Đổi trạng thái phiên hoặc tài khoản thì dựng lại Navigator: màn đăng nhập mở từ màn chọn không đè lên màn mới,
          // đăng nhập lại khi hết phiên bằng tài khoản khác không thấy phần đang nhập của người trước (002 T047c)
          key: ValueKey((session.status, session.user?.type, session.user?.accountId)),
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
    SessionStatus.signedOut => LoginScreen(
      key: ValueKey(session.notice),
      notice: session.notice,
      onSubmit: session.appLogin,
      api: session.api,
      // Màn quét mã của người cao tuổi giữ theme chữ lớn dù mở từ màn đăng nhập chung
      residentLogin: (_) => Theme(data: AppTheme.resident(Brightness.light), child: _qrLogin()),
    ),
    SessionStatus.mustChangePassword => ChangePasswordScreen(
      session: session,
      message: _resident ? loginError : backendMessage,
    ),
    SessionStatus.offerBiometric => BiometricOfferScreen(session: session),
    SessionStatus.locked => LockedScreen(session: session),
    SessionStatus.signedIn => switch (session.user!.type) {
      // Báo trước khi hết phiên, đăng nhập lại giữ phần đang nhập (002 T047c)
      AccountType.staff => SessionGuard(
        session: session,
        child: StaffRoot(session: session, queue: queue, push: push),
      ),
      AccountType.familyMember => FamilyRoot(session: session, push: push),
      AccountType.resident => ResidentRoot(api: session.api, push: push, store: session.store),
    },
  };
}
