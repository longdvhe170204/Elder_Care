import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/staff/shifts/my_shifts_controller.dart';

import '../../support/fake_backend.dart';

void main() {
  late FakeBackend backend;
  late MyShiftsController controller;

  setUp(() {
    backend = FakeBackend()
      ..on('GET', '/me/shifts', 200, [])
      ..on('GET', '/staff', 200, []);
    controller = MyShiftsController(api: backend.client(), now: DateTime.utc(2026, 12, 31, 18));
  });

  tearDown(() => controller.dispose());

  test('FR_033_initialMonthUsesVietnamTimeAndExclusiveNextMonth', () async {
    await controller.load();
    expect(controller.month, DateTime.utc(2027, 1));
    expect(backend.calls('GET', '/me/shifts').single.url.queryParameters, {'from': '2027-01-01', 'to': '2027-02-01'});
    expect(controller.loading, isFalse);
    expect(controller.error, isNull);
    expect(controller.shifts, isEmpty);
  });

  test('FR_033_previousAndNextMonthCrossYearBoundary', () async {
    await controller.changeMonth(-1);
    expect(backend.calls('GET', '/me/shifts').last.url.queryParameters, {'from': '2026-12-01', 'to': '2027-01-01'});
    await controller.changeMonth(1);
    expect(controller.month, DateTime.utc(2027, 1));
    expect(backend.requests.every((r) => r.method == 'GET'), isTrue);
    expect(backend.requests.every((r) => ['/api/v1/me/shifts', '/api/v1/staff'].contains(r.url.path)), isTrue);
  });

  test('FR_054_usesOnlyOwnEndpointSortsChronologicallyAndKeepsBackendStatus', () async {
    backend.on('GET', '/me/shifts', 200, [
      {'id': 2, 'startsAt': '2027-01-01T02:00:00Z', 'status': 'CANCELLED'},
      {'id': 1, 'startsAt': '2027-01-01T08:00:00+07:00', 'status': 'DRAFT'},
    ]);
    backend.on('GET', '/staff', 200, [
      {'id': 11, 'fullName': '[Họ tên NV 1]', 'phoneNumber': 'không hiển thị'},
    ]);
    await controller.load();
    expect(controller.shifts.map((s) => s['id']), [1, 2]);
    expect(controller.shifts.map((s) => s['status']), ['DRAFT', 'CANCELLED']);
    expect(controller.staffNames, {11: '[Họ tên NV 1]'});
    expect(backend.calls('GET', '/shifts'), isEmpty);
  });

  test('FR_054_forbiddenRefreshRemovesPreviouslyLoadedDataAndKeepsProblem', () async {
    backend.on('GET', '/me/shifts', 200, [
      {'id': 1, 'startsAt': '2027-01-01T00:00:00Z'},
    ]);
    await controller.load();
    backend.on('GET', '/me/shifts', 403, {'detail': 'Bạn không có quyền xem lịch ca.'});
    await controller.load();
    expect(controller.shifts, isEmpty);
    expect(controller.staffNames, isEmpty);
    expect(controller.error, isA<ApiProblem>().having((p) => p.status, 'status', 403));
    expect(controller.loading, isFalse);
  });

  test('FR_033_networkFailureCanBeRetried', () async {
    backend.down = true;
    await controller.load();
    expect(controller.error, isA<ApiUnreachable>());
    backend.down = false;
    await controller.load();
    expect(controller.error, isNull);
    expect(controller.shifts, isEmpty);
  });

  test('FR_054_lateResponseCannotOverwriteNewerMonth', () async {
    final oldReply = Completer<http.Response>();
    final api = ApiClient(
      baseUrl: 'http://backend.test',
      httpClient: MockClient((request) async {
        if (request.url.path.endsWith('/staff')) return http.Response('[]', 200);
        if (request.url.queryParameters['from'] == '2026-10-01') return oldReply.future;
        return http.Response(
          jsonEncode([
            {'id': 2, 'startsAt': '2026-11-02T00:00:00Z'},
          ]),
          200,
        );
      }),
    );
    final subject = MyShiftsController(api: api, now: DateTime.utc(2026, 10, 15));
    addTearDown(subject.dispose);
    final oldLoad = subject.load();
    expect(subject.loading, isTrue);
    await subject.changeMonth(1);
    oldReply.complete(
      http.Response(
        jsonEncode([
          {'id': 1, 'startsAt': '2026-10-02T00:00:00Z'},
        ]),
        200,
      ),
    );
    await oldLoad;
    expect(subject.month, DateTime.utc(2026, 11));
    expect(subject.shifts.single['id'], 2);
    expect(subject.loading, isFalse);
  });

  test('FR_033_finishingRequestAfterDisposeDoesNotNotify', () async {
    final reply = Completer<http.Response>();
    final api = ApiClient(baseUrl: 'http://backend.test', httpClient: MockClient((_) => reply.future));
    final subject = MyShiftsController(api: api);
    var notifications = 0;
    subject.addListener(() => notifications++);
    final pending = subject.load();
    subject.dispose();
    reply.complete(http.Response('[]', 200));
    await pending;
    expect(notifications, 1);
  });
}
