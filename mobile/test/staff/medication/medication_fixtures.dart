import 'package:drift/native.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/staff/medication/medication_round_cache.dart';
import 'package:mobile/staff/medication/medication_round_gateway.dart';
import 'package:mobile/staff/medication/medication_round_repository.dart';

import '../../support/fake_backend.dart';

/// Dữ liệu minh họa theo hình dạng api-006 `Dose`, `Prescription`, `Medication` và api-001 `Profile` (họ tên hư cấu,
/// DESIGN.md 8). Liều 10 nguồn viện Đến giờ; liều 11 thuốc kiểm soát đặc biệt Trễ; liều 12 Chưa đến giờ.
void serveRound(FakeBackend backend) {
  backend.on('GET', '/medication-doses/rounds', 200, [
    dose('10-202611050800', prescriptionId: 10, status: 'DUE', scheduled: '2026-11-05T01:00:00Z'),
    dose(
      '11-202611050730',
      prescriptionId: 11,
      status: 'LATE',
      scheduled: '2026-11-05T00:30:00Z',
      requiresOnline: true,
    ),
    dose('10-202611051200', prescriptionId: 10, status: 'UPCOMING', scheduled: '2026-11-05T05:00:00Z'),
  ]);
  backend.on('GET', '/prescriptions/10', 200, {
    'id': 10,
    'medicationId': 20,
    'dosage': 1,
    'route': 'đường uống',
    'instructionNote': 'Uống sau ăn',
  });
  backend.on('GET', '/prescriptions/11', 200, {'id': 11, 'medicationId': 21, 'dosage': 2.5, 'route': 'tiêm dưới da'});
  backend.on('GET', '/medications/20', 200, {
    'id': 20,
    'name': 'Paracetamol',
    'concentration': '500 mg',
    'usageUnit': 'viên',
  });
  backend.on('GET', '/medications/21', 200, {
    'id': 21,
    'name': 'Morphin',
    'concentration': '10 mg/ml',
    'usageUnit': 'mg',
    'specialControl': true,
  });
  backend.on('GET', '/residents/77', 200, {
    'id': 77,
    'personalInfo': {'fullName': 'Nguyễn Thị Lan'},
    'activeHealthItems': [
      {
        'id': 1,
        'type': 'ALLERGY',
        'status': 'ACTIVE',
        'allergen': {'id': 5, 'name': 'Penicillin'},
      },
      {'id': 2, 'type': 'CHRONIC_CONDITION', 'status': 'ACTIVE', 'freeText': 'Tăng huyết áp'},
    ],
  });
}

Map<String, dynamic> dose(
  String key, {
  required int prescriptionId,
  required String status,
  required String scheduled,
  bool requiresOnline = false,
}) {
  final at = DateTime.parse(scheduled);
  return {
    'doseKey': key,
    'residentId': 77,
    'prescriptionId': prescriptionId,
    'scheduledTime': scheduled,
    'windowFrom': at.subtract(const Duration(minutes: 30)).toIso8601String(),
    'windowTo': at.add(const Duration(minutes: 30)).toIso8601String(),
    'status': status,
    'requiresOnline': requiresOnline,
    'medicationSource': 'FACILITY',
    'floorShared': false,
  };
}

class RoundFixture {
  RoundFixture() {
    backend = FakeBackend();
    final api = backend.client();
    queue = OfflineQueue(OfflineDatabase(NativeDatabase.memory()), api, newKey: () => 'queued-${++_n}');
    cache = MedicationRoundCache(NativeDatabase.memory());
    repository = MedicationRoundRepository(
      ApiMedicationRoundGateway(api),
      cache,
      queue,
      clock: () => DateTime.utc(2026, 11, 5, 1, 5),
    );
  }

  late final FakeBackend backend;
  late final OfflineQueue queue;
  late final MedicationRoundCache cache;
  late final MedicationRoundRepository repository;
  int _n = 0;
}
