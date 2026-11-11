import 'dart:async';

import 'package:flutter/material.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';

import '../common/auth/session_controller.dart';
import '../common/notifications/notification_api.dart';
import '../common/notifications/push_coordinator.dart';
import '../common/offline/offline_queue.dart';
import '../common/theme/app_theme.dart';
import '../common/widgets/header_app_bar.dart';
import 'activities/trip_list_screen.dart';
import 'alerts/alerts_screen.dart';
import 'alerts/emergency_assist_screen.dart';
import 'approval_requests/approval_inbox_screen.dart';
import 'beds/beds_screen.dart';
import 'care_tasks/care_tasks_home_screen.dart';
import 'early_unlock/early_unlock_screen.dart';
import 'handover/handover_cache.dart';
import 'handover/handover_screen.dart';
import 'incidents/incidents_screen.dart';
import 'medication/medication_round_cache.dart';
import 'medication/medication_round_gateway.dart';
import 'medication/medication_round_repository.dart';
import 'medication/medication_round_screen.dart';
import 'notifications/staff_inbox_screen.dart';
import 'nutrition/family_food_screen.dart';
import 'nutrition/meal_slip_screen.dart';
import 'nutrition/serving_confirmation_screen.dart';
import 'pending_review/pending_review_screen.dart';
import 'resident_login_qr/resident_login_qr_screen.dart';
import 'residents/resident_lookup_screen.dart';
import 'shift_swaps/shift_swaps_screen.dart';
import 'shifts/assignments_screen.dart';
import 'shifts/my_shifts_screen.dart';
import 'sync_results/sync_results_screen.dart';

/// Gốc phần nhân viên (000 T007; 002 T046, T060; DESIGN.md 15.4): thanh tiêu đề theo màn kèm nút "Đăng xuất" (máy dùng
/// chung tại tầng, FR-014a), thanh điều hướng dưới. Màn của từng feature thêm vào [_tabs] (tối đa 5 mục, mục thứ 6 trở
/// đi gom vào "Thêm"); hiện có Thông báo, Đồng bộ, Chờ xem lại, Mở khóa sớm, Thêm (Người cao tuổi (001 T023), Phê duyệt, Mã đăng nhập cho người cao
/// tuổi, Chăm sóc hằng ngày; 002 T104; theo quyền: Phát thuốc (006 T028), Giường (003), Hoạt động ngoài viện (014 T018)).
/// Mục trong "Thêm" mở ngay trong tab, thanh tiêu đề có nút quay lại danh sách. Biểu tượng lucide cùng tên với web.
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
  static const _headerTabs = {
    'Thông báo',
    'Thêm',
    'Người cao tuổi',
    'Phê duyệt',
    'Mã đăng nhập cho người cao tuổi',
    'Ca của tôi',
    'Phân công',
    'Bàn giao ca',
    'Chăm sóc hằng ngày',
    'Phát thuốc',
    'Giường',
    'Hoạt động ngoài viện',
  };

  int _index = _inboxIndex;

  /// Mục đang mở trong "Thêm"; `null` là danh sách.
  int? _more;
  int _urgent = 0;
  bool _urgentBand = false;

  /// Quyền hiện hành dạng `CHUC_NANG:THAO_TAC` từ `/me/permissions`; chưa tải được thì rỗng, mục theo quyền không hiện.
  Set<String> _grants = const {};

  /// 003 T060: Trưởng tầng, Người phụ trách ca đọc được giường Chờ sẵn sàng của tầng (`GET /bed-readiness/beds` trả 200);
  /// ô ⁴⁰ của Điều dưỡng chỉ có ở lớp nhiệm vụ nên không có trong `/me/permissions`.
  bool _readinessDuty = false;

  /// Bộ nhớ đệm vòng phát thuốc trên máy (006 T028): mở một lần, đóng khi rời phần nhân viên.
  MedicationRoundCache? _medicationCache;

  bool _can(String function, String operation) => _grants.contains('$function:$operation');

  bool get _canViewShifts => _can('SHIFT_SCHEDULE', 'VIEW');
  bool get _canViewShiftSwaps => _can('SHIFT_SWAP_REQUEST', 'VIEW');
  bool get _canViewHandovers => _can('SHIFT_HANDOVER', 'VIEW');
  bool get _canRunMedicationRound =>
      _can('MEDICATION_ADMINISTRATION', 'VIEW') || _can('MEDICATION_ADMINISTRATION', 'PROFESSIONAL');

  @override
  void initState() {
    super.initState();
    widget.push?.addListener(_onPush);
    unawaited(_loadPermissions());
  }

  Future<void> _loadPermissions() async {
    var grants = <String>{};
    try {
      grants = {
        for (final grant in (await widget.session.api.getList('/me/permissions')).whereType<Map<String, dynamic>>())
          '${grant['function']}:${grant['operation']}',
      };
    } on Object {
      // Chưa xác nhận được quyền thì không mở mục theo quyền.
    }
    var readinessDuty = false;
    if (!grants.contains('ROOM_BED_CONFIGURATION:VIEW')) {
      try {
        await widget.session.api.getList('/bed-readiness/beds');
        readinessDuty = true;
      } on Object {
        // 403 (không có nhiệm vụ) hoặc lỗi mạng: không hiện mục Giường.
      }
    }
    if (mounted) {
      setState(() {
        _grants = grants;
        _readinessDuty = readinessDuty;
      });
    }
  }

  @override
  void dispose() {
    widget.push?.removeListener(_onPush);
    unawaited(_medicationCache?.close());
    super.dispose();
  }

  /// Ngày hôm nay theo máy, `yyyy-MM-dd` cho danh sách chuyến (014 T018).
  static String _today() => DateTime.now().toIso8601String().substring(0, 10);

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
    if (_canViewShiftSwaps) ('Đổi ca, nghỉ', LucideIcons.calendarDays, () => ShiftSwapsScreen(api: widget.session.api)),
    if (_canViewHandovers)
      (
        'Bàn giao ca',
        LucideIcons.arrowLeftRight,
        () => HandoverScreen(
          api: widget.session.api,
          cache: widget.session.user == null
              ? null
              : HandoverCache(widget.session.store, widget.session.user!.accountId),
        ),
      ),
    (
      'Chăm sóc hằng ngày',
      LucideIcons.clipboardCheck,
      () =>
          CareTasksHomeScreen(api: widget.session.api, accountId: widget.session.user!.accountId, queue: widget.queue),
    ),
    (
      'Xác nhận phục vụ suất ăn',
      LucideIcons.utensils,
      () => ServingConfirmationScreen(
        api: widget.session.api,
        queue: widget.queue,
        accountId: widget.session.user!.accountId,
      ),
    ),
    ('Phiếu bữa ăn', LucideIcons.clipboardList, () => MealSlipScreen(api: widget.session.api)),
    ('Đồ ăn gia đình', LucideIcons.apple, () => FamilyFoodScreen(api: widget.session.api)),
    if (_can('ALERT_HANDLING', 'VIEW') || _can('ALERT_HANDLING', 'UPDATE'))
      ('Cảnh báo', LucideIcons.bellRing, () => AlertsScreen(api: widget.session.api)),
    ('Hỗ trợ khẩn cấp', LucideIcons.siren, () => EmergencyAssistScreen(api: widget.session.api)),
    if (_can('INCIDENT_EMERGENCY', 'VIEW') || _can('INCIDENT_EMERGENCY', 'UPDATE'))
      ('Sự cố', LucideIcons.siren, () => IncidentsScreen(api: widget.session.api)),
    if (_canRunMedicationRound)
      (
        'Phát thuốc',
        LucideIcons.pill,
        () => MedicationRoundScreen(
          repository: MedicationRoundRepository(
            ApiMedicationRoundGateway(widget.session.api),
            _medicationCache ??= MedicationRoundCache.onDevice(),
            widget.queue,
          ),
          accountId: widget.session.user!.accountId,
        ),
      ),
    if (_can('ROOM_BED_CONFIGURATION', 'VIEW') || _readinessDuty)
      ('Giường', LucideIcons.bedDouble, () => BedsScreen(api: widget.session.api)),
    if (_can('ACTIVITY_OUTING', 'VIEW'))
      ('Hoạt động ngoài viện', LucideIcons.bus, () => TripListScreen(api: widget.session.api, date: _today())),
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
