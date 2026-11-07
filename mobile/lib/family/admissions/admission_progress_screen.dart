import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';

const _stepLabels = {
  'APPLICATION': 'Đăng ký',
  'INFORMATION_COLLECTION': 'Thu thập thông tin',
  'ASSESSMENT': 'Đánh giá',
  'RESIDENCE_TYPE_DETERMINATION': 'Xác định loại lưu trú',
  'CARE_DETERMINATION': 'Xác định chăm sóc',
  'SERVICE_DETERMINATION': 'Xác định dịch vụ',
  'CONDITION_CHECK': 'Kiểm tra điều kiện',
  'ADMISSION': 'Hoàn tất tiếp nhận',
};

/// Tiến trình tiếp nhận của người cao tuổi mà người đại diện đại diện (004 T019; US1, FR-003, FR-006): các bước suy ra từ
/// dữ liệu, chỉ xem, chỉ trực tuyến. Mã lượt đăng ký lấy từ nơi mở màn.
class AdmissionProgressScreen extends StatefulWidget {
  const AdmissionProgressScreen({super.key, required this.api, required this.applicationId});

  final ApiClient api;
  final int applicationId;

  @override
  State<AdmissionProgressScreen> createState() => _AdmissionProgressScreenState();
}

class _AdmissionProgressScreenState extends State<AdmissionProgressScreen> {
  List<Map<String, dynamic>>? _steps;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final data = await widget.api.get('/admission-applications/${widget.applicationId}/progress');
      if (!mounted) return;
      setState(() {
        _steps = ((data?['steps'] as List<dynamic>?) ?? const []).cast<Map<String, dynamic>>();
        _error = null;
      });
    } on Exception catch (e) {
      if (mounted) setState(() => _error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    if (_error != null) {
      return Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(backendMessage(_error!), style: text.bodyMedium),
            const SizedBox(height: 12),
            OutlinedButton(onPressed: _load, child: const Text('Tải lại')),
          ],
        ),
      );
    }
    final steps = _steps;
    if (steps == null) return const Center(child: CircularProgressIndicator());
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Tiến trình tiếp nhận', style: text.titleMedium),
        const SizedBox(height: 12),
        for (final step in steps)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Row(
              children: [
                Icon(step['done'] == true ? Icons.check_circle_outline : Icons.radio_button_unchecked, size: 20),
                const SizedBox(width: 12),
                Expanded(child: Text('${_stepLabels[step['name']] ?? step['name']}', style: text.bodyMedium)),
                Text(step['done'] == true ? 'Đã xong' : 'Chưa xong', style: text.bodySmall),
              ],
            ),
          ),
      ],
    );
  }
}
