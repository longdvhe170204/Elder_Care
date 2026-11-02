import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../common/auth/session_controller.dart';
import '../common/notifications/notification_api.dart';
import '../common/notifications/push_coordinator.dart';
import '../common/offline/offline_queue.dart';
import '../common/theme/app_theme.dart';
import '../common/widgets/header_app_bar.dart';
import 'approval_requests/approval_inbox_screen.dart';
import 'early_unlock/early_unlock_screen.dart';
import 'notifications/staff_inbox_screen.dart';
import 'pending_review/pending_review_screen.dart';
import 'sync_results/sync_results_screen.dart';

/// Gốc phần nhân viên (000 T007; 002 T046, T060; DESIGN.md 15.4): thanh tiêu đề theo màn kèm nút "Đăng xuất" (máy dùng
/// chung tại tầng, FR-014a), thanh điều hướng dưới. Màn của từng feature thêm vào [_tabs] (tối đa 5 mục, mục thứ 6 trở
/// đi gom vào "Thêm"); hiện có Thông báo, Đồng bộ, Chờ xem lại, Mở khóa sớm, Phê duyệt. Biểu tượng lucide cùng tên với web.
/// Tab Thông báo đứng đầu, có huy hiệu số Khẩn cấp chưa xác nhận; đang ở tab khác mà có Khẩn cấp thì hiện dải báo, vì khi app
/// đang mở máy không hiện thông báo hệ thống (009 T022).
class StaffRoot extends StatefulWidget {
  const StaffRoot({super.key, required this.session, required this.queue, this.push});

  final SessionController session;
  final OfflineQueue queue;
  final PushCoordinator? push;

  @override
  State<StaffRoot> createState() => _StaffRootState();
}

class _StaffRootState extends State<StaffRoot> {
  static const _inboxIndex = 0;

  /// Tab đã áp đầu màn xanh đêm của DESIGN.md 15.4; tab khác giữ thanh của theme tới khi đổi (15.7).
  static const _headerTabs = {'Thông báo', 'Phê duyệt'};

  int _index = _inboxIndex;
  int _urgent = 0;
  bool _urgentBand = false;

  @override
  void initState() {
    super.initState();
    widget.push?.addListener(_onPush);
  }

  @override
  void dispose() {
    widget.push?.removeListener(_onPush);
    super.dispose();
  }

  /// Hộp đang mở tự tải lại và mở thông báo; tab khác thì chuyển sang hộp hoặc báo.
  void _onPush() {
    final push = widget.push!;
    final urgent = push.takeUrgent();
    if (_index == _inboxIndex) return;
    if (push.hasOpened) {
      _select(_inboxIndex);
      return;
    }
    if (urgent) setState(() => _urgentBand = true);
    _refreshUrgent();
  }

  Future<void> _refreshUrgent() async {
    try {
      _setUrgent((await NotificationApi(widget.session.api).page()).urgentCount);
    } on Exception {
      // giữ số trước đó, hộp thông báo báo lỗi tải khi mở
    }
  }

  void _setUrgent(int count) {
    if (mounted) setState(() => _urgent = count);
  }

  void _select(int index) => setState(() {
    _index = index;
    if (index == _inboxIndex) _urgentBand = false;
  });

  List<(String, IconData, Widget Function())> get _tabs => [
    (
      'Thông báo',
      LucideIcons.bell,
      () => StaffInboxScreen(api: widget.session.api, push: widget.push, onUrgentCount: _setUrgent),
    ),
    ('Đồng bộ', LucideIcons.refreshCw, () => SyncResultsScreen(queue: widget.queue, session: widget.session)),
    ('Chờ xem lại', LucideIcons.cloudOff, () => PendingReviewScreen(api: widget.session.api)),
    ('Mở khóa sớm', LucideIcons.lockOpen, () => EarlyUnlockScreen(api: widget.session.api)),
    ('Phê duyệt', LucideIcons.clipboardCheck, () => ApprovalInboxScreen(api: widget.session.api)),
  ];

  @override
  Widget build(BuildContext context) {
    final tabs = _tabs;
    return Scaffold(
      appBar: _headerTabs.contains(tabs[_index].$1)
          ? headerAppBar(
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
            )
          : AppBar(
              title: Text(tabs[_index].$1),
              actions: [
                TextButton.icon(
                  onPressed: () => widget.session.signOut(),
                  icon: const Icon(LucideIcons.logOut),
                  label: const Text('Đăng xuất'),
                ),
              ],
            ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_urgentBand) _UrgentBand(onOpen: () => _select(_inboxIndex)),
          Expanded(
            child: KeyedSubtree(key: ValueKey(_index), child: tabs[_index].$3()),
          ),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: _select,
        destinations: [
          for (final (i, (label, icon, _)) in tabs.indexed)
            NavigationDestination(
              icon: i == _inboxIndex
                  ? Badge.count(count: _urgent, isLabelVisible: _urgent > 0, child: Icon(icon))
                  : Icon(icon),
              label: label,
            ),
        ],
      ),
    );
  }
}

/// Dải báo Khẩn cấp vừa tới khi đang ở tab khác; đỏ vì đây là việc gấp (DESIGN.md mục 1 điều 4).
class _UrgentBand extends StatelessWidget {
  const _UrgentBand({required this.onOpen});

  final VoidCallback onOpen;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Semantics(
      liveRegion: true,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
        child: DecoratedBox(
          decoration: BoxDecoration(
            color: theme.colorScheme.surface,
            border: Border.all(color: theme.colorScheme.error, width: 1.5),
            borderRadius: BorderRadius.circular(AppRadius.lg),
          ),
          child: Padding(
            padding: const EdgeInsets.all(12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Có thông báo khẩn cấp mới',
                  style: theme.textTheme.titleSmall?.copyWith(color: theme.colorScheme.error),
                ),
                const SizedBox(height: 8),
                FilledButton(onPressed: onOpen, child: const Text('Mở hộp thông báo')),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
