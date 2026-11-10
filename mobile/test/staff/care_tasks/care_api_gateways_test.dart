import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/staff/care_tasks/care_api_gateways.dart';
import '../../support/fake_backend.dart';

void main() {
  test('FR_017_FR_012_adaptersReadResidentNameFromProfilePersonalInfo', () async {
    final backend = FakeBackend();
    backend.on('GET', '/residents', 200, {
      'items': [
        {
          'id': 7,
          'personalInfo': {'fullName': 'Trần An Kiểm'},
        },
      ],
      'nextCursor': null,
    });
    backend.on('GET', '/me/permissions', 200, []);
    backend.on('GET', '/shifts/9/source-data', 200, {});
    backend.on('GET', '/shifts/9', 200, {'staff': []});
    backend.on('GET', '/shifts/9/care-tasks', 200, []);
    backend.on('GET', '/task-types', 200, []);
    backend.on('GET', '/day-care-attendances', 200, []);
    final api = backend.client();
    final planning = await ApiCareTaskPlanningGateway(api).load(9);
    final attendance = await ApiDayCareAttendanceGateway(api).load('2026-11-10');
    expect(planning.residents.single['fullName'], 'Trần An Kiểm');
    expect(attendance.residents.single['fullName'], 'Trần An Kiểm');
  });
  test('FR_017_adapterSendsArrayAndKeepsRowErrors', () async {
    final backend = FakeBackend();
    final gateway = ApiCareTaskPlanningGateway(backend.client());
    backend.on('POST', '/shifts/9/care-tasks/plan', 409, {
      'errorCode': 'UNMET_CONDITIONS',
      'detail': 'Chưa lưu lô',
      'rowErrors': [
        {
          'row': 2,
          'codes': ['INVALID_BASIS'],
        },
      ],
    });
    try {
      await gateway.plan(9, [
        {'taskTypeId': 3},
      ], 'stable-key');
      fail('Phải trả lỗi từng dòng');
    } on CareTaskPlanningError catch (error) {
      expect(error.rowErrors.single['row'], 2);
    }
    final request = backend.calls('POST', '/shifts/9/care-tasks/plan').single;
    expect(jsonDecode(request.body), [
      {'taskTypeId': 3},
    ]);
    expect(request.headers['Idempotency-Key'], 'stable-key');
  });

  test('FR_012_adapterUsesAttendanceCommandsAndIdempotency', () async {
    final backend = FakeBackend();
    final gateway = ApiDayCareAttendanceGateway(backend.client());
    backend.on('POST', '/day-care-attendances/check-in', 201, {});
    backend.on('POST', '/day-care-attendances/4/check-out', 200, {});
    await gateway.checkIn(7, 'arrival');
    await gateway.checkOut(4, 8, 'departure');
    expect(
      (jsonDecode(backend.calls('POST', '/day-care-attendances/check-in').single.body)
          as Map<String, dynamic>)['residentId'],
      7,
    );
    expect(
      (jsonDecode(backend.calls('POST', '/day-care-attendances/4/check-out').single.body)
          as Map<String, dynamic>)['pickupRecordId'],
      8,
    );
    expect(backend.calls('POST', '/day-care-attendances/4/check-out').single.headers['Idempotency-Key'], 'departure');
  });
}
