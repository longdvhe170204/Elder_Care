import 'package:flutter/material.dart';
import '../../common/api/api_client.dart';
import '../../common/offline/offline_queue.dart';
import '../../common/widgets/page_body.dart';
import 'care_checklist_screen.dart' show careCommandKey;
import 'care_offline_recording.dart';
import 'care_result_form.dart';

/// FR-025/T040: form cùng loại kết quả với checklist, phát sinh chỉ từ lệnh người dùng.
class AdHocCareRecordingScreen extends StatefulWidget {
  const AdHocCareRecordingScreen({super.key, required this.api, required this.queue, required this.accountId});
  final ApiClient api;
  final OfflineQueue queue;
  final int accountId;
  @override
  State<AdHocCareRecordingScreen> createState() => _AdHocCareRecordingScreenState();
}

class _AdHocCareRecordingScreenState extends State<AdHocCareRecordingScreen> {
  late final recorder = CareOfflineRecording(widget.api, widget.queue, widget.accountId);
  List<Map<String, dynamic>>? residents, types, values;
  int? resident, type;
  bool reviewed = false;
  String? error, message;
  Future<void> load() async {
    try {
      final people = await widget.api.get('/residents');
      final catalog = await widget.api.getList('/task-types');
      final results = await widget.api.getList('/result-values');
      if (mounted) {
        setState(() {
          residents = (people?['items'] as List<dynamic>? ?? [])
              .map((r) => Map<String, dynamic>.from(r as Map))
              .toList();
          types = catalog
              .map((r) => Map<String, dynamic>.from(r as Map))
              .where((r) => r['status'] == 'ACTIVE')
              .toList();
          values = results.map((r) => Map<String, dynamic>.from(r as Map)).toList();
          error = null;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(
          () => error = e is ApiProblem
              ? e.detail
              : 'Không tải được người cao tuổi và danh mục. Kiểm tra kết nối rồi thử lại.',
        );
      }
    }
  }

  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  Widget build(BuildContext context) {
    final selected = type == null ? null : types!.firstWhere((t) => t['id'] == type);
    return Scaffold(
      appBar: AppBar(title: const Text('Ghi nhận phát sinh')),
      body: PageBody(
        child: ListView(
          children: [
            if (error != null) ...[
              Semantics(liveRegion: true, child: Text(error!)),
              OutlinedButton(onPressed: load, child: const Text('Thử lại')),
            ],
            if (message != null) Semantics(liveRegion: true, child: Text(message!)),
            if (residents == null && error == null) const Center(child: CircularProgressIndicator()),
            if (residents != null) ...[
              DropdownButtonFormField<int>(
                initialValue: resident,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Người cao tuổi'),
                items: [
                  for (final r in residents!)
                    DropdownMenuItem(value: r['id'] as int, child: Text(r['fullName'] as String? ?? '${r['id']}')),
                ],
                onChanged: (v) => setState(() {
                  resident = v;
                  reviewed = false;
                }),
              ),
              DropdownButtonFormField<int>(
                initialValue: type,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Loại công việc'),
                items: [
                  for (final t in types!) DropdownMenuItem(value: t['id'] as int, child: Text(t['name'] as String)),
                ],
                onChanged: (v) => setState(() {
                  type = v;
                  reviewed = false;
                }),
              ),
              CheckboxListTile(
                value: reviewed,
                onChanged: (v) => setState(() => reviewed = v ?? false),
                title: const Text('Đã xem công việc cùng loại đang mở trong checklist'),
              ),
              if (resident != null && selected != null)
                CareResultForm(
                  key: ValueKey('$resident:$type'),
                  task: {'resultType': selected['resultType']},
                  newKey: careCommandKey,
                  commandContext: reviewed.toString(),
                  resultValues: values ?? [],
                  onRecord: (body, key) async {
                    final queued = await recorder.recordAdHoc({
                      'residentId': resident,
                      'taskTypeId': type,
                      ...body,
                      'sameTypeTasksReviewed': reviewed,
                    }, key);
                    if (mounted) {
                      setState(
                        () => message = queued
                            ? 'Chưa đồng bộ. Ghi nhận phát sinh đã lưu trên máy.'
                            : 'Đã lưu ghi nhận phát sinh.',
                      );
                    }
                  },
                ),
            ],
          ],
        ),
      ),
    );
  }
}
