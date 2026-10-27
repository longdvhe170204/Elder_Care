import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../common/auth/session_controller.dart';
import '../common/offline/offline_queue.dart';
import '../common/theme/app_theme.dart';
import 'early_unlock/early_unlock_screen.dart';
import 'pending_review/pending_review_screen.dart';
import 'sync_results/sync_results_screen.dart';

/// Gốc phần nhân viên (000 T007; 002 T046, T060; DESIGN.md 15.4): thanh tiêu đề theo màn kèm nút "Đăng xuất" (máy dùng
/// chung tại tầng, FR-014a), thanh điều hướng dưới. Màn của từng feature thêm vào [_tabs] (tối đa 5 mục, mục thứ 6 trở
/// đi gom vào "Thêm"); hiện có Đồng bộ, Chờ xem lại, Mở khóa sớm. Biểu tượng lucide cùng tên với web.
class StaffRoot extends StatefulWidget {
  const StaffRoot({super.key, required this.session, required this.queue});

  final SessionController session;
  final OfflineQueue queue;

  @override
  State<StaffRoot> createState() => _StaffRootState();
}

class _StaffRootState extends State<StaffRoot> {
  int _index = 0;

  List<(String, IconData, Widget Function())> get _tabs => [
    ('Đồng bộ', LucideIcons.refreshCw, () => SyncResultsScreen(queue: widget.queue, session: widget.session)),
    ('Chờ xem lại', LucideIcons.cloudOff, () => PendingReviewScreen(api: widget.session.api)),
    ('Mở khóa sớm', LucideIcons.lockOpen, () => EarlyUnlockScreen(api: widget.session.api)),
  ];

  @override
  Widget build(BuildContext context) {
    final tabs = _tabs;
    final tones = Theme.of(context).extension<Tones>()!;
    return Scaffold(
      appBar: AppBar(
        title: Text(tabs[_index].$1),
        actions: [
          TextButton.icon(
            onPressed: () => widget.session.signOut(),
            icon: const Icon(LucideIcons.logOut),
            label: const Text('Đăng xuất'),
          ),
        ],
      ),
      body: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: tones.canvasGradient,
            stops: const [0, 0.58, 1],
          ),
        ),
        child: SizedBox.expand(
          child: KeyedSubtree(key: ValueKey(_index), child: tabs[_index].$3()),
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (i) => setState(() => _index = i),
        destinations: [for (final (label, icon, _) in tabs) NavigationDestination(icon: Icon(icon), label: label)],
      ),
    );
  }
}
