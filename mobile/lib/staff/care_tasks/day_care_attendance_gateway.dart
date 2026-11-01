/// T031: hợp đồng nội bộ màn trực tuyến; adapter production chờ transport JSON array của A.
abstract interface class DayCareAttendanceGateway {
  Future<DayCareAttendanceData> load(String date);
  Future<void> checkIn(int residentId, String idempotencyKey);
  Future<void> checkOut(int attendanceId, int pickupRecordId, String idempotencyKey);
  Future<void> correct(int recordId, String at, String reason, String idempotencyKey);
}

class DayCareAttendanceData {
  const DayCareAttendanceData({required this.rows, required this.residents, required this.canCreate});
  final List<Map<String, dynamic>> rows, residents;
  final bool canCreate;
}
