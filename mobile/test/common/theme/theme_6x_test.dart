import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/auth/change_password_screen.dart';
import 'package:mobile/common/auth/login_screen.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/common/widgets/status_pill.dart';

import '../../support/fake_backend.dart';

// Theme 6.x của app (DESIGN.md 3.1, 3.5, 4, 15.2, 15.5): nền trang trơn, nhãn trạng thái chữ màu, font 400/500/600,
// form đăng nhập, đổi mật khẩu cho trình quản lý mật khẩu điền (WCAG 3.3.8).

Widget _host(Widget child) => MaterialApp(
  theme: AppTheme.light(),
  home: Scaffold(body: Center(child: child)),
);

void main() {
  test('nenTrangTron_vaTokenMuc31', () {
    final light = AppTheme.light();
    expect(light.scaffoldBackgroundColor, const Color(0xFFEEF2F6));
    expect(light.colorScheme.error, const Color(0xFFB42318));
    expect(light.colorScheme.outline, const Color(0xFF7E8591));
    expect(light.colorScheme.outlineVariant, const Color(0xFFDDE3EA));
    expect(AppTheme.dark().scaffoldBackgroundColor, const Color(0xFF0F1A2A));
  });

  test('chuChiDung400_500_600', () {
    final t = AppTheme.light().textTheme;
    final weights = {
      for (final s in [
        t.displaySmall,
        t.headlineMedium,
        t.headlineSmall,
        t.titleLarge,
        t.titleMedium,
        t.titleSmall,
        t.bodyLarge,
        t.bodyMedium,
        t.bodySmall,
        t.labelLarge,
        t.labelMedium,
        t.labelSmall,
      ])
        s?.fontWeight,
    };
    expect(weights, {FontWeight.w400, FontWeight.w500, FontWeight.w600});
  });

  testWidgets('nhanTrangThai_chuMauKhongNen', (tester) async {
    await tester.pumpWidget(_host(const StatusPill(label: 'Quá hạn', tone: StatusTone.urgent)));
    final label = tester.widget<Text>(find.text('Quá hạn'));
    expect(label.style?.color, AppTheme.light().colorScheme.error);
    expect(find.descendant(of: find.byType(StatusPill), matching: find.byType(DecoratedBox)), findsNothing);
  });

  testWidgets('nhanGiuCho_coVienCungMauChu', (tester) async {
    await tester.pumpWidget(_host(const StatusPill(label: 'Đang giữ chỗ', tone: StatusTone.held)));
    final box = tester.widget<DecoratedBox>(
      find.descendant(of: find.byType(StatusPill), matching: find.byType(DecoratedBox)),
    );
    final shape = (box.decoration as ShapeDecoration).shape as StadiumBorder;
    expect(shape.side.color, AppTheme.light().colorScheme.secondary);
    expect(tester.widget<Text>(find.text('Đang giữ chỗ')).style?.color, AppTheme.light().colorScheme.secondary);
  });

  List<String>? hints(WidgetTester tester, String label) =>
      tester.widget<TextField>(find.widgetWithText(TextField, label)).autofillHints?.toList();

  testWidgets('dangNhap_trongAutofillGroup_coGoiY', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: LoginScreen(onSubmit: (_, _) async {}, residentLogin: (_) => const SizedBox()),
      ),
    );
    expect(find.byType(AutofillGroup), findsOneWidget);
    expect(hints(tester, 'Tên đăng nhập'), [AutofillHints.username]);
    expect(hints(tester, 'Mật khẩu'), [AutofillHints.password]);
  });

  testWidgets('doiMatKhau_trongAutofillGroup_matKhauMoi', (tester) async {
    final s = session(FakeBackend(), MemoryStore(), FakeBiometric(available: false));
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: ChangePasswordScreen(session: s),
      ),
    );
    expect(find.byType(AutofillGroup), findsOneWidget);
    expect(hints(tester, 'Mật khẩu nhân viên đưa'), [AutofillHints.password]);
    expect(hints(tester, 'Mật khẩu mới'), [AutofillHints.newPassword]);
  });
}
