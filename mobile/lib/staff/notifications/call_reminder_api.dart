import '../../common/api/api_client.dart';
import '../../common/widgets/status_pill.dart';

/// Lượt nhắc gọi (009 FR-027 → FR-031, Q-307). Ghi kết quả bắt buộc trực tuyến (Q-01).
class CallReminderApi {
  const CallReminderApi(this._api);

  final ApiClient _api;

  /// Lượt đang Chờ kết quả mà tôi được nhắc hoặc có quyền gọi.
  Future<List<Map<String, dynamic>>> mine() async =>
      (await _api.getList('/me/call-reminders')).cast<Map<String, dynamic>>();

  /// Số điện thoại là số hiện hành lúc mở (FR-027).
  Future<Map<String, dynamic>> detail(int id) async => (await _api.get('/call-reminders/$id'))!;

  Future<void> record(int id, Map<String, dynamic> result, String key) =>
      _api.post('/call-reminders/$id/results', result, headers: {'Idempotency-Key': key});
}

/// Nhãn của hợp đồng api-009.
const reminderStatusLabels = {
  'AWAITING_RESULT': 'Chờ kết quả',
  'RESULT_RECORDED': 'Đã có kết quả',
  'STOPPED': 'Đã dừng',
};

const callResultLabels = {'REACHED': 'Đã liên lạc được', 'NOT_REACHED': 'Không liên lạc được'};

/// Ba lý do của FR-030; `OTHER` có ở hợp đồng nhưng spec không nêu nên app không cho chọn, chỉ hiện nếu dữ liệu có.
const callReasonChoices = {'NO_ANSWER': 'Không nghe máy', 'WRONG_NUMBER': 'Sai số', 'NO_NUMBER': 'Không có số'};

const callReasonLabels = {...callReasonChoices, 'OTHER': 'Khác'};

/// Lý do của lịch sử chuyển nhắc (`handoffs`); mã khác hiện chung là "Chuyển nhắc".
const handoffLabels = {
  'INITIAL': 'Nhắc lần đầu',
  'OVERDUE': 'Chuyển nhắc, quá hạn',
  'SHIFT_ENDED': 'Chuyển nhắc, hết ca',
};

StatusTone reminderTone(String status) => switch (status) {
  'AWAITING_RESULT' => StatusTone.attention,
  'RESULT_RECORDED' => StatusTone.settled,
  _ => StatusTone.neutral,
};
