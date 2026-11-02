import 'package:flutter/material.dart';

/// Thân màn trên nền trang trơn của `Scaffold` (DESIGN.md 3.1, 15.4; 6.0.0 bỏ nền dải trời): lề 16, tránh vùng hệ thống.
class PageBody extends StatelessWidget {
  const PageBody({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) => SafeArea(
    child: Padding(padding: const EdgeInsets.all(16), child: child),
  );
}
