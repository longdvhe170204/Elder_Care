import 'dart:convert';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/staff/care_tasks/care_offline_recording.dart';
import '../../support/fake_backend.dart';

void main() {
  test('BR_M15_20_onlineRecordingUsesOnePersistedEnvelopeBeforeSending', () async {
    final backend = FakeBackend();
    final api = backend.client();
    final db = OfflineDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    final queue = OfflineQueue(db, api, newKey: () => 'persisted-key');
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': 'persisted-key', 'result': 'ACCEPTED'},
      ],
    });
    expect(
      await CareOfflineRecording(api, queue, 7).recordAdHoc({'residentId': 9, 'taskTypeId': 3}, 'form-key'),
      isFalse,
    );
    expect(backend.calls('POST', '/ad-hoc-care-recordings'), isEmpty);
    final envelope = jsonDecode(backend.calls('POST', '/sync').single.body) as Map<String, dynamic>;
    expect((envelope['records'] as List<dynamic>).single, containsPair('idempotencyKey', 'persisted-key'));
    expect((await queue.synced(7)).single.idempotencyKey, 'persisted-key');
  });
  test('BR_M15_20_recordQueuesApprovedEnvelopeAndRejectedContentIsRetained', () async {
    final backend = FakeBackend()..down = true;
    final api = backend.client();
    final db = OfflineDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    final queue = OfflineQueue(db, api, clock: () => DateTime.utc(2026, 11, 6, 8));
    final recorder = CareOfflineRecording(api, queue, 7);
    expect(await recorder.recordTask(12, {'numericValue': 200}, 'online-key'), isTrue);
    final row = (await queue.unsynced(7)).single;
    expect(row.recordType, 'M04.CARE_TASK_RESULT');
    expect(jsonDecode(row.content), {
      'taskId': 12,
      'action': 'RECORD',
      'recording': {'numericValue': 200},
    });
    expect(await queue.unsynced(8), isEmpty);
    backend.down = false;
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': row.idempotencyKey, 'result': 'REJECTED', 'reason': 'Công việc đã đóng'},
      ],
    });
    await queue.sync(7);
    final rejected = (await queue.synced(7)).single;
    expect(rejected.content, row.content);
    expect(rejected.reason, 'Công việc đã đóng');
  });

  test('BR_M15_20_adHocQueuesTheApprovedBodyAndRetainsRejectedPermissionResult', () async {
    final backend = FakeBackend()..down = true;
    final api = backend.client();
    final db = OfflineDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    int sequence = 0;
    final queue = OfflineQueue(db, api, newKey: () => 'key-${++sequence}');
    final recorder = CareOfflineRecording(api, queue, 7);
    final body = {'residentId': 9, 'taskTypeId': 3, 'codeValue': 'SKIPPED', 'sameTypeTasksReviewed': true};
    expect(await recorder.recordAdHoc(body, 'ad-hoc'), isTrue);
    expect(jsonDecode((await queue.unsynced(7)).single.content), body);
    backend.down = false;
    backend.on('POST', '/sync', 200, {
      'results': [
        {'idempotencyKey': 'key-1', 'result': 'ACCEPTED'},
        {'idempotencyKey': 'key-2', 'result': 'REJECTED', 'reason': 'Không có quyền'},
      ],
    });
    await expectLater(recorder.recordTask(12, {'numericValue': 100}, 'denied'), throwsA(isA<Exception>()));
    expect(await queue.unsynced(7), isEmpty);
    expect((await queue.synced(7)).where((r) => r.result == 'REJECTED'), hasLength(1));
  });
}
