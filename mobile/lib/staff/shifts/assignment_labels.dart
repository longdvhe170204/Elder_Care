const assignmentStates = {
  'DRAFT': 'Nháp',
  'SCHEDULED': 'Tương lai',
  'ACTIVE': 'Hiệu lực',
  'ENDED': 'Đã kết thúc',
  'CANCELLED': 'Đã hủy',
};
const assignmentRoles = {
  'CAREGIVER': 'Nhân viên chăm sóc',
  'NURSING_STAFF': 'Điều dưỡng',
  'FLOOR_MANAGER': 'Trưởng tầng',
  'DOCTOR': 'Bác sĩ',
  'OFFICE_STAFF': 'Hành chính',
  'KITCHEN_STAFF': 'Nhân viên bếp',
};
const assignmentTargets = {'FLOOR': 'Tầng', 'AREA': 'Khu vực', 'ROOM': 'Phòng', 'RESIDENT': 'Người cao tuổi'};
String assignmentName(List<Map<String, dynamic>> names, Object? id, {String fallback = 'Nhân viên'}) {
  for (final row in names) {
    if (row['id'] == id || row['staffId'] == id) {
      return (row['fullName'] ?? row['name'] ?? row['code'] ?? '$fallback: chưa có tên được phép xem').toString();
    }
  }
  return '$fallback: chưa có tên được phép xem';
}

String assignmentIso(String value) =>
    DateTime.parse(value).toUtc().add(const Duration(hours: 7)).toIso8601String().replaceFirst(RegExp(r'Z$'), '+07:00');
