import 'package:flutter/material.dart';

import 'today/today_screen.dart';

/// Gốc phần người cao tuổi sau khi đăng nhập (002 T097; DESIGN.md 15.6): không có thanh điều hướng dưới, màn đầu
/// "Hôm nay" là nơi về. Đăng nhập, đổi mật khẩu, mở khóa sinh trắc học do `ElderCareApp` (`lib/app.dart`) chọn theo
/// trạng thái phiên.
class ResidentRoot extends StatelessWidget {
  const ResidentRoot({super.key});

  @override
  Widget build(BuildContext context) => const TodayScreen();
}
