import '../../common/api/api_client.dart';

/// Câu báo lỗi đăng nhập cho người cao tuổi (DESIGN.md 15.6: câu ngắn, không thuật ngữ); không lộ tài khoản có hay
/// không (002 FR-014).
String loginError(Object error) {
  if (error is ApiUnreachable) return 'Máy chưa kết nối mạng.';
  if (error is ApiProblem) {
    return switch (error.errorCode) {
      'INVALID_QR_CODE' => 'Mã này không dùng được nữa. Nhờ nhân viên tạo mã mới.',
      'LOGIN_FAILED' => 'Tên đăng nhập hoặc mật khẩu chưa đúng.',
      'ACCOUNT_TEMPORARILY_LOCKED' => 'Tài khoản đang tạm khóa. Nhờ nhân viên giúp.',
      'DEVICE_ALREADY_BOUND' => 'Máy này đang dùng cho tài khoản khác. Nhờ nhân viên giúp.',
      'PASSWORD_POLICY_NOT_MET' => 'Mật khẩu mới chưa đủ mạnh. Nhờ nhân viên giúp đặt.',
      _ => 'Chưa vào được. Nhờ nhân viên giúp.',
    };
  }
  return 'Chưa vào được. Nhờ nhân viên giúp.';
}
