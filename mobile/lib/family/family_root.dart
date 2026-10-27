import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../common/auth/session_controller.dart';
import '../common/widgets/not_designed_state.dart';
import '../common/widgets/sky_background.dart';

/// Gốc phần người thân (000 T007; 002 T047; Q-346, Q-305: cùng chức năng với cổng người thân trên web): màn của từng
/// feature đặt ở `family/<feature>/` (mobile/README.md mục 2). Thanh điều hướng dưới (DESIGN.md 2.3, 15.4) thêm khi có
/// màn đầu tiên; tới đó hiện trạng thái "chưa có màn" (7.3) và nút "Đăng xuất".
class FamilyRoot extends StatelessWidget {
  const FamilyRoot({super.key, required this.session});

  final SessionController session;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Elder Care'),
      actions: [
        TextButton.icon(
          onPressed: () => session.signOut(),
          icon: const Icon(LucideIcons.logOut),
          label: const Text('Đăng xuất'),
        ),
      ],
    ),
    body: const SkyBackground(
      child: Align(
        alignment: Alignment.topCenter,
        child: NotDesignedState(
          title: 'Phần người thân: chưa có màn',
          description: 'Các màn cho người thân được thêm theo từng chức năng. Hiện dùng cổng người thân trên web.',
        ),
      ),
    ),
  );
}
