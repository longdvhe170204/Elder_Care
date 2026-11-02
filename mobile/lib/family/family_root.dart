import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../common/auth/session_controller.dart';
import '../common/notifications/push_coordinator.dart';
import '../common/theme/app_theme.dart';
import '../common/widgets/header_app_bar.dart';
import 'notifications/family_inbox_screen.dart';

/// Gốc phần người thân (000 T007; 002 T047; Q-346, Q-305: cùng chức năng với cổng người thân trên web). Màn của từng
/// feature đặt ở `family/<feature>/` (mobile/README.md mục 2). Hiện có một màn Thông báo (009 T031); thanh điều hướng
/// dưới (DESIGN.md 15.4) thêm khi có màn thứ hai.
class FamilyRoot extends StatelessWidget {
  const FamilyRoot({super.key, required this.session, this.push});

  final SessionController session;
  final PushCoordinator? push;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: headerAppBar(
      context,
      title: 'Thông báo',
      actions: [
        TextButton.icon(
          style: TextButton.styleFrom(foregroundColor: Theme.of(context).extension<Tones>()!.headerInkStrong),
          onPressed: () => session.signOut(),
          icon: const Icon(LucideIcons.logOut),
          label: const Text('Đăng xuất'),
        ),
      ],
    ),
    body: FamilyInboxScreen(api: session.api, push: push),
  );
}
