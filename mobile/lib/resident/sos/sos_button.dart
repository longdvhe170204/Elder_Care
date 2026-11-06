import 'dart:async';
import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:geolocator/geolocator.dart';
import 'package:lucide_icons_flutter/lucide_icons.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../common/api/api_client.dart';
import '../../common/auth/secure_store.dart';
import '../../common/auth/session_controller.dart';

// Nút "Gọi người giúp" của người cao tuổi (012 T071, T074; FR-085, Q-372 → Q-374; 007 FR-047d, FR-047f, FR-047h,
// FR-047j; DESIGN.md 15.6). Lệnh /sos thuộc 007, dùng token thiết bị (002 research A-15), không cần phiên. Không xếp hàng
// gửi sau: không gửi được thì hiện số của viện đã lưu và 115.

/// Số liên hệ của viện nhận được gần nhất (FR-085: hiện khi mất kết nối, bị từ chối).
const sosFacilityPhoneKey = 'sos.facilityPhone';

/// Đã xem màn giới thiệu "nút không thay chuông gọi" (FR-085).
const sosIntroSeenKey = 'sos.introSeen';

/// Vị trí GPS gửi cho lần bấm (007 `LocationReport`).
class SosPosition {
  const SosPosition({
    required this.latitude,
    required this.longitude,
    required this.accuracyMeters,
    required this.measuredAt,
  });

  final double latitude;
  final double longitude;
  final double accuracyMeters;
  final DateTime measuredAt;

  Map<String, dynamic> toJson() => {
    'latitude': latitude,
    'longitude': longitude,
    'accuracyMeters': accuracyMeters,
    'measuredAt': measuredAt.toUtc().toIso8601String(),
  };
}

/// Nguồn vị trí; chỉ xin quyền khi đang dùng ứng dụng, không theo dõi nền (007 FR-047h, Q-373).
abstract class SosLocationSource {
  Future<bool> requestPermission();

  Future<SosPosition?> current();
}

class GeolocatorLocationSource implements SosLocationSource {
  const GeolocatorLocationSource();

  @override
  Future<bool> requestPermission() async {
    if (!await Geolocator.isLocationServiceEnabled()) return false;
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) permission = await Geolocator.requestPermission();
    return permission == LocationPermission.whileInUse || permission == LocationPermission.always;
  }

  @override
  Future<SosPosition?> current() async {
    final p = await Geolocator.getCurrentPosition();
    return SosPosition(
      latitude: p.latitude,
      longitude: p.longitude,
      accuracyMeters: p.accuracy,
      measuredAt: p.timestamp,
    );
  }
}

/// Quyền vị trí trong phiên chạy của ứng dụng: từ chối thì không hỏi lại (research F-18).
bool? _locationGranted;

@visibleForTesting
void resetSosLocationAsked() => _locationGranted = null;

String _uuid() {
  final r = Random.secure();
  final b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  final h = b.map((x) => x.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}

Future<void> _call(String number) => launchUrl(Uri(scheme: 'tel', path: number));

/// Nút ở đáy màn đầu: rộng hết màn, cao 72, nền đỏ chữ trắng (DESIGN.md 15.6, chỗ duy nhất đỏ là màu nền).
class SosButton extends StatelessWidget {
  const SosButton({super.key, required this.api, required this.store, this.location});

  final ApiClient api;
  final SecureStore store;
  final SosLocationSource? location;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return SafeArea(
      minimum: const EdgeInsets.all(16),
      child: Semantics(
        button: true,
        label: 'Gọi người giúp. Chạm để gửi, có ba giây để hủy.',
        excludeSemantics: true,
        child: SizedBox(
          height: 72,
          child: FilledButton.icon(
            style: FilledButton.styleFrom(backgroundColor: colors.error, foregroundColor: colors.onError),
            onPressed: () => Navigator.of(context).push<void>(
              MaterialPageRoute(
                builder: (_) =>
                    _CountdownScreen(api: api, store: store, location: location ?? const GeolocatorLocationSource()),
              ),
            ),
            icon: const Icon(LucideIcons.siren),
            label: const Text('Gọi người giúp'),
          ),
        ),
      ),
    );
  }
}

/// Đếm ngược 3 giây, nút Hủy lớn; chạm "Gọi ngay" thì gửi ngay (Q-372).
class _CountdownScreen extends StatefulWidget {
  const _CountdownScreen({required this.api, required this.store, required this.location});

  final ApiClient api;
  final SecureStore store;
  final SosLocationSource location;

  @override
  State<_CountdownScreen> createState() => _CountdownScreenState();
}

class _CountdownScreenState extends State<_CountdownScreen> {
  int _left = 3;
  bool _cancelled = false;
  bool _sending = false;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (_left <= 1) {
        HapticFeedback.vibrate();
        _send();
      } else {
        setState(() => _left--);
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _cancel() {
    _timer?.cancel();
    setState(() => _cancelled = true);
    _timer = Timer(const Duration(seconds: 3), () {
      if (mounted) Navigator.of(context).pop();
    });
  }

  Future<void> _send() async {
    if (_sending || _cancelled) return;
    _timer?.cancel();
    setState(() => _sending = true);
    final saved = await widget.store.read(sosFacilityPhoneKey);
    final token = await widget.store.read(SessionController.deviceTokenKey);
    if (!mounted) return;
    if (token == null) {
      _fail('Máy này chưa được gắn với tài khoản.', saved);
      return;
    }
    try {
      final body = await widget.api.post(
        '/sos',
        const {},
        headers: {'X-Device-Token': token, 'Idempotency-Key': _uuid()},
      );
      if (!mounted || body == null) return;
      final phone = (body['contacts'] as Map<String, dynamic>?)?['facilityPhone'] as String?;
      if (phone != null) await widget.store.write(sosFacilityPhoneKey, phone);
      if (!mounted) return;
      await Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(
          builder: (_) => _StatusScreen(api: widget.api, token: token, location: widget.location, first: body),
        ),
      );
    } on ApiUnreachable {
      _fail('Máy chưa kết nối mạng.', saved);
    } on ApiProblem catch (e) {
      final phone = (e.properties['contacts'] as Map<String, dynamic>?)?['facilityPhone'] as String?;
      if (phone != null) await widget.store.write(sosFacilityPhoneKey, phone);
      _fail(_reason(e), phone ?? saved);
    }
  }

  static String _reason(ApiProblem e) => switch (e.errorCode) {
    'NOT_AT_FACILITY' => 'Đang ghi là tạm vắng hoặc ở bệnh viện.',
    'NOT_PRESENT' => 'Đang ghi là chưa có mặt ở viện.',
    'INVALID_DEVICE' => 'Máy này chưa được gắn với tài khoản.',
    _ => 'Viện chưa nhận được.',
  };

  void _fail(String reason, String? phone) {
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute<void>(
        builder: (_) => _FailedScreen(reason: reason, facilityPhone: phone),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              if (_cancelled)
                Semantics(
                  liveRegion: true,
                  child: Text('Đã hủy, chưa gửi.', style: text.headlineMedium, textAlign: TextAlign.center),
                )
              else ...[
                Semantics(
                  liveRegion: true,
                  child: Text(
                    'Đang gọi người giúp sau $_left…',
                    style: text.headlineMedium,
                    textAlign: TextAlign.center,
                  ),
                ),
                const SizedBox(height: 16),
                Text('$_left', style: text.displayLarge, textAlign: TextAlign.center),
              ],
              const Spacer(),
              if (!_cancelled) ...[
                SizedBox(
                  height: 72,
                  child: FilledButton(
                    style: FilledButton.styleFrom(backgroundColor: colors.error, foregroundColor: colors.onError),
                    onPressed: _sending ? null : _send,
                    child: const Text('Gọi ngay'),
                  ),
                ),
                const SizedBox(height: 12),
                SizedBox(
                  height: 72,
                  child: OutlinedButton(onPressed: _sending ? null : _cancel, child: const Text('Hủy')),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

const _places = [
  ('ROOM', 'Ở phòng'),
  ('RESTROOM', 'Nhà vệ sinh'),
  ('CORRIDOR', 'Hành lang'),
  ('COMMON_ROOM', 'Phòng sinh hoạt chung'),
  ('YARD', 'Ngoài sân'),
  ('OTHER', 'Chỗ khác'),
];

/// Sau khi gửi: tự cập nhật mỗi 5 giây; nơi đang ở chọn một lần; gửi vị trí khi backend yêu cầu. Không có nút hủy.
class _StatusScreen extends StatefulWidget {
  const _StatusScreen({required this.api, required this.token, required this.location, required this.first});

  final ApiClient api;
  final String token;
  final SosLocationSource location;
  final Map<String, dynamic> first;

  @override
  State<_StatusScreen> createState() => _StatusScreenState();
}

class _StatusScreenState extends State<_StatusScreen> {
  late Map<String, dynamic> _status = widget.first;
  late final int _pressId = widget.first['pressId'] as int;
  Timer? _poll;
  Timer? _locationTimer;
  bool _placeChosen = false;
  bool _stopLocation = false;

  Map<String, String> get _headers => {'X-Device-Token': widget.token};

  @override
  void initState() {
    super.initState();
    _poll = Timer.periodic(const Duration(seconds: 5), (_) => _refresh());
    _startLocation();
  }

  @override
  void dispose() {
    _poll?.cancel();
    _locationTimer?.cancel();
    super.dispose();
  }

  Future<void> _refresh() async {
    try {
      final body = await widget.api.requestJson('GET', '/sos/current', headers: _headers);
      if (!mounted || body is! Map<String, dynamic>) return;
      setState(() => _status = body);
      if (body['sendLocation'] != true) _locationTimer?.cancel();
    } on Exception {
      // Mất kết nối giữa chừng: giữ trạng thái cuối, lần sau hỏi lại
    }
  }

  Future<void> _startLocation() async {
    if (_status['sendLocation'] != true) return;
    _locationGranted ??= await widget.location.requestPermission();
    if (_locationGranted != true || !mounted) return;
    await _sendLocation();
    final seconds = _status['locationIntervalSeconds'] as int? ?? 30;
    if (!_stopLocation && mounted) {
      _locationTimer = Timer.periodic(Duration(seconds: seconds), (_) => _sendLocation());
    }
  }

  Future<void> _sendLocation() async {
    if (_stopLocation) return;
    try {
      final position = await widget.location.current();
      if (position == null) return;
      await widget.api.requestJson('POST', '/sos/$_pressId/location', body: position.toJson(), headers: _headers);
    } on ApiProblem {
      // 409 STOP_SENDING_LOCATION hoặc cảnh báo đã đóng: dừng gửi
      _stopLocation = true;
      _locationTimer?.cancel();
    } on Exception {
      // Mất kết nối: lần sau gửi lại
    }
  }

  Future<void> _choosePlace(String place) async {
    setState(() => _placeChosen = true);
    try {
      await widget.api.requestJson('POST', '/sos/$_pressId/place', body: {'place': place}, headers: _headers);
    } on Exception {
      // Không bắt buộc (007 FR-047f): không gửi được thì thôi
    }
  }

  String get _message {
    final name = _status['receiverName'] as String?;
    return switch (_status['status']) {
      'RECEIVED' => '${name ?? 'Nhân viên'} đang đến.',
      'ARRIVED' => '${name ?? 'Nhân viên'} đã tới.',
      _ => 'Đã gửi. Nhân viên đang được báo.',
    };
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const SizedBox(height: 32),
            Semantics(
              liveRegion: true,
              child: Text(_message, style: text.headlineMedium, textAlign: TextAlign.center),
            ),
            const SizedBox(height: 32),
            if (!_placeChosen) ...[
              Text('Đang ở đâu?', style: text.titleLarge),
              const SizedBox(height: 12),
              GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                mainAxisSpacing: 12,
                crossAxisSpacing: 12,
                childAspectRatio: 2.2,
                children: [
                  for (final p in _places)
                    OutlinedButton(
                      onPressed: () => _choosePlace(p.$1),
                      child: Text(p.$2, textAlign: TextAlign.center),
                    ),
                ],
              ),
              const SizedBox(height: 24),
            ],
            FilledButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Về màn Hôm nay')),
          ],
        ),
      ),
    );
  }
}

/// Không gửi được: lý do bằng câu thường, gọi viện và 115 (FR-085, DESIGN.md 15.6).
class _FailedScreen extends StatelessWidget {
  const _FailedScreen({required this.reason, required this.facilityPhone});

  final String reason;
  final String? facilityPhone;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            const SizedBox(height: 32),
            Semantics(
              liveRegion: true,
              child: Text('Chưa gửi được.', style: text.headlineMedium?.copyWith(color: colors.error)),
            ),
            const SizedBox(height: 8),
            Text(reason, style: text.bodyLarge),
            const SizedBox(height: 24),
            if (facilityPhone != null) ...[
              FilledButton(onPressed: () => _call(facilityPhone!), child: Text('Gọi viện: $facilityPhone')),
              const SizedBox(height: 12),
            ],
            FilledButton(onPressed: () => _call('115'), child: const Text('Gọi cấp cứu 115')),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Về màn Hôm nay')),
          ],
        ),
      ),
    );
  }
}

/// Lần dùng đầu: giới thiệu nút không thay chuông gọi (FR-085); xem một lần.
class SosIntroGate extends StatefulWidget {
  const SosIntroGate({super.key, required this.store, required this.child});

  final SecureStore store;
  final Widget child;

  @override
  State<SosIntroGate> createState() => _SosIntroGateState();
}

class _SosIntroGateState extends State<SosIntroGate> {
  bool? _seen;

  @override
  void initState() {
    super.initState();
    widget.store.read(sosIntroSeenKey).then((v) {
      if (mounted) setState(() => _seen = v != null);
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_seen != false) return widget.child;
    final text = Theme.of(context).textTheme;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Spacer(),
              Text(
                'Nút Gọi người giúp báo nhân viên đang trực. Nút không thay chuông gọi ở đầu giường.',
                style: text.headlineSmall,
              ),
              const Spacer(),
              FilledButton(
                onPressed: () async {
                  await widget.store.write(sosIntroSeenKey, DateTime.now().toUtc().toIso8601String());
                  if (mounted) setState(() => _seen = true);
                },
                child: const Text('Đã hiểu'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
