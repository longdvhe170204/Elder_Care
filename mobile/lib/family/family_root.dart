import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../common/auth/session_controller.dart';
import '../common/notifications/notification_inbox.dart';
import '../common/notifications/push_coordinator.dart';
import '../common/theme/app_theme.dart';
import '../common/widgets/header_app_bar.dart';
import 'profile/family_profile_screen.dart';

/// Gốc phần người thân (000 T007; 002 T047; Q-346, Q-305: cùng chức năng với cổng người thân trên web). Màn của từng
/// feature đặt ở `family/<feature>/` (mobile/README.md mục 2) và thêm vào [_tabs] (tối đa 5 mục, DESIGN.md 15.4); hiện có
/// Thông báo (009 T031) và Hồ sơ (001 T023).
class FamilyRoot extends StatefulWidget {
  const FamilyRoot({super.key, required this.session, this.push});

  final SessionController session;
  final PushCoordinator? push;

  @override
  State<FamilyRoot> createState() => _FamilyRootState();
}

class _FamilyRootState extends State<FamilyRoot> {
  int _index = 0;

  List<(String, IconData, Widget Function())> get _tabs => [
    ('Thông báo', LucideIcons.bell, () => NotificationInbox(api: widget.session.api, push: widget.push)),
    ('Hồ sơ', LucideIcons.userRound, () => FamilyProfileScreen(api: widget.session.api)),
  ];

  @override
  Widget build(BuildContext context) {
    final tabs = _tabs;
    return Scaffold(
      appBar: headerAppBar(
        context,
        title: tabs[_index].$1,
        actions: [
          TextButton.icon(
            style: TextButton.styleFrom(foregroundColor: Theme.of(context).extension<Tones>()!.headerInkStrong),
            onPressed: () => widget.session.signOut(),
            icon: const Icon(LucideIcons.logOut),
            label: const Text('Đăng xuất'),
          ),
        ],
      ),
      body: KeyedSubtree(key: ValueKey(_index), child: tabs[_index].$3()),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (index) => setState(() => _index = index),
        destinations: [for (final (label, icon, _) in tabs) NavigationDestination(icon: Icon(icon), label: label)],
      ),
    );
  }
}
