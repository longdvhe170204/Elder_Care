import 'package:flutter/material.dart';

import '../../common/theme/app_theme.dart';

/// Ảnh đại diện chữ cái của người cao tuổi (DESIGN.md 3.6, 7.2 `resident-avatar`): màu theo `id` trong bốn cặp của
/// theme, chữ cái là chữ đầu của tên gọi (từ cuối của họ tên). Trang trí nên loại khỏi cây trợ năng; tên đã có ở dòng bên
/// cạnh. Ảnh thật cần `ApiClient` tải tệp nhị phân (chờ A), chưa dùng.
class ResidentAvatar extends StatelessWidget {
  const ResidentAvatar({super.key, required this.name, required this.id, this.size = 40});

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
