import 'package:flutter/material.dart';

import '../common/api/api_client.dart';
import '../common/auth/secure_store.dart';
import '../common/notifications/push_coordinator.dart';
import 'today/today_screen.dart';

/// Gốc phần người cao tuổi sau khi đăng nhập (002 T097; DESIGN.md 15.6): không có thanh điều hướng dưới, màn đầu
/// "Hôm nay" là nơi về. Đăng nhập, đổi mật khẩu, mở khóa sinh trắc học do `ElderCareApp` chuyển theo trạng thái phiên.
class ResidentRoot extends StatelessWidget {
  const ResidentRoot({super.key, required this.api, this.push, this.store});

  final ApiClient api;

  /// Kho trên máy cho nút SOS (token thiết bị, số của viện đã lưu; 012 FR-085).
  final SecureStore? store;
  final PushCoordinator? push;

  @override
  Widget build(BuildContext context) => TodayScreen(api: api, push: push, store: store);
}
