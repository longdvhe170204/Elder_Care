import 'dart:async';

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
import 'handover/handover_screen.dart';
import 'notifications/staff_inbox_screen.dart';
import 'pending_review/pending_review_screen.dart';
import 'resident_login_qr/resident_login_qr_screen.dart';
import 'residents/resident_lookup_screen.dart';
import 'shifts/assignments_screen.dart';
import 'shifts/my_shifts_screen.dart';
import 'sync_results/sync_results_screen.dart';

/// Gốc phần nhân viên (000 T007; 002 T046, T060; DESIGN.md 15.4): thanh tiêu đề theo màn kèm nút "Đăng xuất" (máy dùng
/// chung tại tầng, FR-014a), thanh điều hướng dưới. Màn của từng feature thêm vào [_tabs] (tối đa 5 mục, mục thứ 6 trở
/// đi gom vào "Thêm"); hiện có Thông báo, Đồng bộ, Chờ xem lại, Mở khóa sớm, Thêm (Người cao tuổi (001 T023), Phê duyệt, Mã đăng nhập cho người cao
/// tuổi; 002 T104). Mục trong "Thêm" mở ngay trong tab, thanh tiêu đề có nút quay lại danh sách. Biểu tượng lucide cùng
/// tên với web.
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
  static const _headerTabs = {'Thông báo', 'Thêm', 'Người cao tuổi', 'Phê duyệt', 'Mã đăng nhập cho người cao tuổi', 'Ca của tôi', 'Phân công', 'Bàn giao ca'};

  int _index = _inboxIndex;

  /// Mục đang mở trong "Thêm"; `null` là danh sách.
  int? _more;
  int _urgent = 0;
  bool _urgentBand = false;
  bool _canViewShifts = false;
  bool _canViewHandovers = false;

  @override
  void initState() {
    super.initState();
    widget.push?.addListener(_onPush);
    unawaited(_loadShiftPermission());
  }

  Future<void> _loadShiftPermission() async {
    var allowed = false;
    var handovers = false;
    try {
      final grants = await widget.session.api.getList('/me/permissions');
      allowed = grants.whereType<Map<String, dynamic>>().any(
        (grant) => grant['function'] == 'SHIFT_SCHEDULE' && grant['operation'] == 'VIEW',
      );
      handovers = grants.whereType<Map<String, dynamic>>().any(
        (grant) => grant['function'] == 'SHIFT_HANDOVER' && grant['operation'] == 'VIEW',
      );
    } on Object {
      // Chưa xác nhận được quyền thì không mở mục lịch ca.
    }
    if (mounted) {
      setState(() {
        _canViewShifts = allowed;
        _canViewHandovers = handovers;
      });
    }
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
    _more = null;
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
    ('Thêm', LucideIcons.ellipsis, () => _MoreList(items: _moreItems, onOpen: (i) => setState(() => _more = i))),
  ];

  List<(String, IconData, Widget Function())> get _moreItems => [
    ('Người cao tuổi', LucideIcons.users, () => ResidentLookupScreen(api: widget.session.api)),
    ('Phê duyệt', LucideIcons.clipboardCheck, () => ApprovalInboxScreen(api: widget.session.api)),
    ('Mã đăng nhập cho người cao tuổi', LucideIcons.qrCode, () => ResidentLoginQrScreen(api: widget.session.api)),
    if (_canViewShifts) ('Ca của tôi', LucideIcons.calendarDays, () => MyShiftsScreen(api: widget.session.api)),
    if (_canViewShifts) ('Phân công', LucideIcons.users, () => AssignmentsScreen(api: widget.session.api)),
    if (_canViewHandovers) ('Bàn giao ca', LucideIcons.arrowLeftRight, () => HandoverScreen(api: widget.session.api)),
  ];

  @override
  Widget build(BuildContext context) {
    final tabs = _tabs;
    final more = _more;
    final (title, _, body) = more == null ? tabs[_index] : _moreItems[more];
    final back = more == null
        ? null
        : IconButton(
            tooltip: 'Quay lại',
            icon: const Icon(LucideIcons.arrowLeft),
            onPressed: () => setState(() => _more = null),
          );
    return Scaffold(
      appBar: _headerTabs.contains(title)
          ? headerAppBar(
              context,
              title: title,
              leading: back,
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
              title: Text(title),
              leading: back,
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
            child: KeyedSubtree(key: ValueKey((_index, more)), child: body()),
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

/// Danh sách dòng của mục "Thêm" (DESIGN.md 15.4).
class _MoreList extends StatelessWidget {
  const _MoreList({required this.items, required this.onOpen});

  final List<(String, IconData, Widget Function())> items;
  final ValueChanged<int> onOpen;

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: [
        for (final (i, (label, icon, _)) in items.indexed)
          ListTile(
            minTileHeight: 56,
            leading: Icon(icon),
            title: Text(label),
            trailing: const Icon(LucideIcons.chevronRight),
            onTap: () => onOpen(i),
          ),
      ],
    );
  }
}
