import 'package:flutter/material.dart';

import '../../common/theme/app_theme.dart';

/// Ảnh đại diện chữ cái (DESIGN.md 3.6, 7.2 `resident-avatar`), cùng công thức với bản ở `staff/residents/`; không
/// import chéo `staff/` (mobile/README.md mục 4 luật 1), chờ A đưa lên `common/`.
class FamilyResidentAvatar extends StatelessWidget {
  const FamilyResidentAvatar({super.key, required this.name, required this.id, this.size = 40});

  final String name;
  final int id;
  final double size;

  @override
  Widget build(BuildContext context) {
    final tones = Theme.of(context).extension<Tones>()!;
    final (background, ink) = tones.avatars[id % tones.avatars.length];
    final words = name.trim().split(RegExp(r'\s+'));
    final initial = words.last.isEmpty ? '?' : words.last.substring(0, 1).toUpperCase();
    return ExcludeSemantics(
      child: Container(
        width: size,
        height: size,
        alignment: Alignment.center,
        decoration: BoxDecoration(color: background, shape: BoxShape.circle),
        child: Text(initial, style: Theme.of(context).textTheme.titleMedium?.copyWith(color: ink)),
      ),
    );
  }
}
