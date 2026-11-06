import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/staff/shifts/assignments_controller.dart';
import '../../support/fake_backend.dart';

Map<String, dynamic> shiftFixture({List<String> commands = const []}) => {
  'id': 12,
  'startsAt': '2026-11-07T15:00:00Z',
  'endsAt': '2026-11-07T23:00:00Z',
  'status': 'DRAFT',
  'allowedCommands': commands,
  'staff': [
    {'staffId': 9},
  ],
};
FakeBackend fixture({bool manage = true}) {
  final b = FakeBackend()
    ..on('GET', '/me', 200, {
      'roles': ['AC-01'],
    })
    ..on('GET', '/me/permissions', 200, [
      {'function': 'SHIFT_SCHEDULE', 'operation': 'VIEW'},
      if (manage) {'function': 'SHIFT_SCHEDULE', 'operation': 'UPDATE'},
    ])
    ..on('GET', '/shift-schedules', 200, [
      {
        'id': 1,
        'scopeType': 'FLOOR',
        'scopeId': 2,
        'shifts': [
          shiftFixture(commands: ['updateDraftAssignment']),
        ],
      },
    ])
    ..on('GET', '/me/shifts', 200, [shiftFixture()])
    ..on('GET', '/shifts/12', 200, shiftFixture(commands: ['updateDraftAssignment']))
    ..on('GET', '/shifts/12/assignments', 200, [])
    ..on('GET', '/staff', 200, [
      {'id': 9, 'fullName': '[Nhân viên 1]'},
    ]);
  return b;
}

void main() {
  test('FR_032_sao chép giữ từng lý do không chép được từ backend', () async {
    final b = fixture()
      ..on('GET', '/shifts/12', 200, shiftFixture(commands: ['copyAssignments']))
      ..on('POST', '/shifts/12/copy-assignments', 200, {
        'copiedIds': [44],
        'notCopied': [
          {'sourceAssignmentId': 20, 'reason': 'Nhân viên không có mặt'},
        ],
      });
    final c = AssignmentsController(b.client());
    await c.load();
    await c.selectShift(12);
    final result = await c.copy(8);
    expect(result?['notCopied'], [
      {'sourceAssignmentId': 20, 'reason': 'Nhân viên không có mặt'},
    ]);
    expect(jsonDecode(b.calls('POST', '/shifts/12/copy-assignments').single.body), {'sourceShiftId': 8});
    c.dispose();
  });
  test('FR_024_quyền CREATE của Trưởng tầng không cho giao Trưởng tầng', () async {
    final b = fixture()
      ..on('GET', '/me', 200, {
        'roles': ['AC-02'],
      })
      ..on('GET', '/me/permissions', 200, [
        {'function': 'SHIFT_SCHEDULE', 'operation': 'VIEW'},
        {'function': 'SHIFT_SCHEDULE', 'operation': 'CREATE'},
      ]);
    final c = AssignmentsController(b.client());
    await c.load();
    expect(await c.assignFloor({'staffId': 9, 'floorAreaId': 2, 'from': '2026-11-07'}), false);
    expect(b.calls('POST', '/floor-manager-assignments'), isEmpty);
    c.dispose();
  });
  test('FR_054_mất quyền khi tải lại phải xóa dữ liệu và lệnh cũ', () async {
    final b = fixture();
    final c = AssignmentsController(b.client());
    await c.load();
    await c.selectShift(12);
    b.on('GET', '/me/permissions', 403, {'detail': 'Đã thu hồi quyền'});
    await c.load();
    expect(c.view, false);
    expect(c.manage, false);
    expect(c.shifts, isEmpty);
    expect(c.shift, isNull);
    c.dispose();
  });
  test('FR_054_quyền cá nhân chỉ đọc ca của mình trong đúng tháng Việt Nam', () async {
    final b = fixture(manage: false);
    final c = AssignmentsController(b.client(), initialMonth: DateTime(2026, 11));
    await c.load();
    expect(b.calls('GET', '/shift-schedules'), isEmpty);
    expect(b.calls('GET', '/me/shifts').single.url.queryParameters, {'from': '2026-11-01', 'to': '2026-12-01'});
    expect(c.shifts, hasLength(1));
    c.dispose();
  });
  test('FR_025_sửa Nháp dùng preview riêng, If-Match và giữ khóa khi mất mạng', () async {
    final b = fixture()
      ..on('GET', '/assignments/22', 200, {'id': 22, 'version': 3, 'status': 'DRAFT'})
      ..on('POST', '/assignments/22/draft-update-preview', 200, {'warnings': []})
      ..on('POST', '/assignments/22/update-draft', 200, {
        'assignment': {'id': 22},
        'warnings': [],
      });
    final c = AssignmentsController(b.client(), initialMonth: DateTime(2026, 11));
    await c.load();
    await c.selectShift(12);
    final original = await c.draft(22);
    final body = <String, dynamic>{
      'staffId': 9,
      'targetType': 'FLOOR',
      'targetId': 2,
      'role': 'CAREGIVER',
      'assignmentKind': 'PRIMARY',
    };
    await c.preview(body, original: original);
    expect(b.calls('POST', '/assignments/22/draft-update-preview').single.headers['If-Match'], '"3"');
    b.down = true;
    expect(await c.save(body, original: original), false);
    b.down = false;
    expect(await c.save(Map.fromEntries(body.entries.toList().reversed), original: original), true);
    final calls = b.calls('POST', '/assignments/22/update-draft');
    expect(calls[0].headers['Idempotency-Key'], calls[1].headers['Idempotency-Key']);
    expect(calls[0].body, calls[1].body);
    expect(calls[1].headers['If-Match'], '"3"');
    expect(b.calls('DELETE', '/assignments/22'), isEmpty);
    c.dispose();
  });
  test('FR_027_xóa khác hủy, kết thúc bắt buộc lý do và phải được backend cho phép', () async {
    final b = fixture()
      ..on('DELETE', '/assignments/22', 204)
      ..on('POST', '/assignments/22/cancel', 200);
    final c = AssignmentsController(b.client());
    await c.load();
    await c.selectShift(12);
    expect(await c.finish({'id': 22, 'status': 'DRAFT'}, 'delete'), false);
    b.on('GET', '/shifts/12', 200, shiftFixture(commands: ['deleteAssignment', 'cancelAssignment', 'endAssignment']));
    await c.selectShift(12);
    expect(await c.finish({'id': 22, 'status': 'DRAFT'}, 'delete'), true);
    expect(await c.finish({'id': 22, 'status': 'SCHEDULED'}, 'cancel', reason: ''), false);
    expect(await c.finish({'id': 22, 'status': 'SCHEDULED'}, 'cancel', reason: 'Điều chỉnh nhân sự'), true);
    expect(jsonDecode(b.calls('POST', '/assignments/22/cancel').single.body), {'reason': 'Điều chỉnh nhân sự'});
    c.dispose();
  });
  test('FR_028_kết quả chỉ đọc từ backend, không suy người chính từ bảng', () async {
    final b = fixture()
      ..on('GET', '/shifts/12/assignment-resolution', 200, {
        'primaryStaffId': 77,
        'assignedNurseStaffIds': [80],
      });
    final c = AssignmentsController(b.client());
    await c.load();
    await c.selectShift(12);
    expect((await c.resolve(5, '2026-11-07T22:00:00+07:00', 'CAREGIVER'))?['primaryStaffId'], 77);
    expect(
      b.calls('GET', '/shifts/12/assignment-resolution').single.url.queryParameters['at'],
      '2026-11-07T22:00:00+07:00',
    );
    c.dispose();
  });
}
