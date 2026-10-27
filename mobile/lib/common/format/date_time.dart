/// Ngày giờ hiển thị dd/MM/yyyy HH:mm theo giờ Việt Nam (mobile/README.md luật 4; DESIGN.md 8).
String formatDateTime(DateTime value) {
  final vn = value.toUtc().add(const Duration(hours: 7));
  String two(int n) => n.toString().padLeft(2, '0');
  return '${two(vn.day)}/${two(vn.month)}/${vn.year} ${two(vn.hour)}:${two(vn.minute)}';
}

/// Giờ HH:mm theo giờ Việt Nam.
String formatTime(DateTime value) {
  final vn = value.toUtc().add(const Duration(hours: 7));
  String two(int n) => n.toString().padLeft(2, '0');
  return '${two(vn.hour)}:${two(vn.minute)}';
}
