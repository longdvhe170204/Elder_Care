import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_theme.dart';

/// Đầu màn xanh đêm đậm liền với thanh trạng thái (DESIGN.md 15.4; 6.0.0). Màn nào áp bố cục mới thì dùng thanh này; màn
/// cũ giữ thanh của theme tới khi đổi (15.7).
AppBar headerAppBar(BuildContext context, {required String title, Widget? leading, List<Widget>? actions}) {
  final tones = Theme.of(context).extension<Tones>()!;
  return AppBar(
    backgroundColor: tones.header,
    foregroundColor: tones.headerInkStrong,
    surfaceTintColor: Colors.transparent,
    scrolledUnderElevation: 0,
    systemOverlayStyle: SystemUiOverlayStyle.light,
    leading: leading,
    title: Text(title),
    actions: actions,
  );
}
