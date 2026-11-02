import 'api_client.dart';

/// Câu báo lỗi cho phần nhân viên, người thân: nội dung backend trả (`detail`, tiếng Việt) như web (`problemOf`);
/// mất mạng thì nói rõ. Phần người cao tuổi dùng câu ngắn riêng (DESIGN.md 15.6).
String backendMessage(Object error) {
  if (error is ApiUnreachable) return 'Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.';
  if (error is ApiProblem && error.detail != null) return error.detail!;
  return 'Chưa thực hiện được. Thử lại sau.';
}
