import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

/// Nền màn dải trời (DESIGN.md 3.1, 15.4): nội dung đặt trên thẻ trắng, lề 16.
class SkyBackground extends StatelessWidget {
  const SkyBackground({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final tones = Theme.of(context).extension<Tones>()!;
    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: tones.canvasGradient,
          stops: const [0, 0.58, 1],
        ),
      ),
      child: SafeArea(child: Padding(padding: const EdgeInsets.all(16), child: child)),
    );
  }
}
