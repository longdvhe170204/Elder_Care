import 'dart:async';
import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/care_tasks/care_checklist_cache.dart';
import 'package:mobile/staff/care_tasks/care_checklist_repository.dart';
import 'package:mobile/staff/care_tasks/care_checklist_screen.dart';
import 'package:mobile/staff/care_tasks/care_offline_recording.dart';
import '../../support/fake_backend.dart';
import 'care_checklist_cache_test.dart' show FixtureChecklistGateway;

void main() {
  late CareChecklistCache cache;
  late FixtureChecklistGateway gateway;
  late CareChecklistRepository repository;
  setUp(() {
    cache = CareChecklistCache(NativeDatabase.memory());
    gateway = FixtureChecklistGateway();
    repository = CareChecklistRepository(gateway, cache);
    gateway.body['tasks'] = [
      {
        'id': 1,
        'taskType': 'Việc đến hạn',
        'status': 'DUE',
        'residentId': 77,
        'residentCard': {'fullName': 'Người kiểm thử'},
        'allowedCommands': ['notPerform'],
      },
      {
        'id': 2,
        'taskType': 'Việc quá hạn',
        'status': 'OVERDUE',
        'residentId': 77,
        'residentCard': {'fullName': 'Người kiểm thử'},
        'allowedCommands': ['claim'],
      },
    ];
    gateway.body['riskAgreements'] = [
      {
        'residentId': 77,
        'agreements': [
          {'choice': 'Lựa chọn đã trao đổi', 'reviewDate': '2026-11-08'},
        ],
      },
    ];
    gateway.body['nonPerformanceReasonTypes'] = [
      {'id': 1, 'name': 'Người cao tuổi từ chối', 'status': 'ACTIVE', 'fixed': true},
    ];
  });
  tearDown(() => cache.close());
  Future<void> view(WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: CareChecklistScreen(repository: repository, accountId: 1, shiftId: 9),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('FR_030_quaHanLenTruocVaTheHienLuaChon', (tester) async {
    await view(tester);
    expect(tester.getTopLeft(find.text('Việc quá hạn')).dy, lessThan(tester.getTopLeft(find.text('Việc đến hạn')).dy));
    expect(find.text('Lựa chọn đã trao đổi'), findsNWidgets(2));
  });
  testWidgets('FR_034_sleepFormUsesResultCatalogAndStoresMinutesWithQuality', (tester) async {
    gateway.body['tasks'] = [
      {
        'id': 1,
        'taskType': 'Giấc ngủ',
        'status': 'DUE',
        'resultType': 'SLEEP',
        'allowedCommands': ['record'],
      },
    ];
    gateway.body['resultValues'] = [
      {'type': 'SLEEP_QUALITY', 'code': 'QUIET', 'name': 'Ngủ yên', 'status': 'ACTIVE'},
    ];
    await view(tester);
    await tester.tap(find.text('Ghi nhận kết quả'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Thời gian ngủ (phút)'), '60');
    await tester.tap(find.text('Ngủ yên'));
    await tester.ensureVisible(find.text('Lưu kết quả'));
    await tester.tap(find.text('Lưu kết quả'));
    await tester.pumpAndSettle();
    expect(gateway.recordings.single['numericValue'], 60);
    expect(gateway.recordings.single['codeValue'], 'QUIET');
  });
  testWidgets('FR_035_matMangHienChuaCapNhatVaKhongGuiLenh', (tester) async {
    await repository.load(1, 9);
    gateway.failure = ApiUnreachable();
    await view(tester);
    expect(find.textContaining('Chưa cập nhật'), findsOneWidget);
    expect(find.text('Nhận công việc'), findsNothing);
    expect(find.text('Không thực hiện được…'), findsNothing);
  });
  testWidgets('BR_M15_20_offlineChecklistRecordsIntoSharedQueueAndShowsUnsynced', (tester) async {
    final backend = FakeBackend()..down = true;
    final api = backend.client();
    final db = OfflineDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    final queue = OfflineQueue(db, api);
    repository = CareChecklistRepository(gateway, cache, recorder: CareOfflineRecording(api, queue, 1));
    gateway.body['tasks'] = [
      {
        'id': 1,
        'taskType': 'Uống nước',
        'status': 'DUE',
        'resultType': 'ML',
        'allowedCommands': ['record'],
        'residentId': 77,
      },
    ];
    await repository.load(1, 9);
    gateway.failure = ApiUnreachable();
    await view(tester);
    await tester.tap(find.text('Ghi nhận kết quả'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Lượng nước (ml)'), '200');
    await tester.ensureVisible(find.text('Lưu kết quả'));
    await tester.tap(find.text('Lưu kết quả'));
    await tester.pumpAndSettle();
    expect(await queue.unsynced(1), hasLength(1));
    expect(find.textContaining('Chưa đồng bộ'), findsWidgets);
  });
  testWidgets('FR_028a_phaiChonLoaiLyDoTruocKhiLuu', (tester) async {
    await view(tester);
    await tester.tap(find.text('Không thực hiện được…'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Lưu Không thực hiện'));
    await tester.pumpAndSettle();
    expect(find.text('Chọn loại lý do Không thực hiện.'), findsOneWidget);
    expect(find.text('Người cao tuổi từ chối'), findsOneWidget);
  });
  testWidgets('FR_036_loiKhongMatGhiChuVaThuLaiCungUuid', (tester) async {
    gateway.commandFailure = ApiProblem(409, 'CARE_TASK_CLOSED', 'Công việc đã được ghi nhận.');
    await view(tester);
    await tester.tap(find.text('Không thực hiện được…'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Người cao tuổi từ chối'));
    await tester.enterText(find.byType(TextField), 'Lời người cao tuổi đã nói');
    await tester.tap(find.text('Lưu Không thực hiện'));
    await tester.pumpAndSettle();
    expect(find.text('Công việc đã được ghi nhận.'), findsOneWidget);
    expect(find.text('Lời người cao tuổi đã nói'), findsOneWidget);
    await tester.tap(find.text('Lưu Không thực hiện'));
    await tester.pumpAndSettle();
    expect(gateway.attempts.length, 2);
    expect(
      gateway.attempts.first.last,
      matches(RegExp(r'^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$')),
    );
    expect(gateway.attempts.first.last, gateway.attempts.last.last);
  });
  testWidgets('FR_038_ghiNhanMlTheoSnapshotVaGiuGiaTriKhiLoi', (tester) async {
    gateway.body['tasks'] = [
      {
        'id': 1,
        'taskType': 'Tên loại đã đổi',
        'resultType': 'ML',
        'status': 'DUE',
        'allowedCommands': ['record'],
      },
    ];
    gateway.commandFailure = ApiProblem(409, 'CARE_TASK_CLOSED', 'Công việc đã được ghi nhận.');
    await view(tester);
    await tester.tap(find.text('Ghi nhận kết quả'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Lượng nước (ml)'), '200');
    await tester.tap(find.text('Lưu kết quả'));
    await tester.pumpAndSettle();
    expect(gateway.recordings.single, {'numericValue': 200});
    expect(find.text('200'), findsOneWidget);
    expect(find.text('Công việc đã được ghi nhận.'), findsOneWidget);
  });
  testWidgets('FR_035_guiGioThucHienTheoGioVietNam', (tester) async {
    gateway.body['tasks'] = [
      {
        'id': 1,
        'taskType': 'Uống nước kiểm thử',
        'resultType': 'ML',
        'status': 'DUE',
        'allowedCommands': ['record'],
      },
    ];
    await view(tester);
    await tester.tap(find.text('Ghi nhận kết quả'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Lượng nước (ml)'), '200');
    await tester.enterText(
      find.widgetWithText(TextField, 'Thời điểm thực hiện (dd/MM/yyyy HH:mm)'),
      '01/11/2026 10:15',
    );
    await tester.tap(find.text('Lưu kết quả'));
    await tester.pumpAndSettle();
    expect(gateway.recordings.single, {'numericValue': 200, 'performedAt': '2026-11-01T03:15:00.000Z'});
  });
  testWidgets('BR_M15_20_doiTaiKhoanXoaHienThiCuVaBoResponseCu', (tester) async {
    await view(tester);
    final oldRequest = Completer<Map<String, dynamic>>();
    final newRequest = Completer<Map<String, dynamic>>();
    gateway.loader = (id) => id == 9 ? oldRequest.future : newRequest.future;
    await tester.tap(find.text('Tải lại checklist'));
    await tester.pump();
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: CareChecklistScreen(repository: repository, accountId: 2, shiftId: 10),
      ),
    );
    await tester.pump();
    expect(find.text('Việc đến hạn'), findsNothing);
    newRequest.complete({
      'shiftId': 10,
      'loadedAt': '2026-11-01T04:00:00Z',
      'tasks': [
        {'id': 10, 'taskType': 'Việc tài khoản mới', 'status': 'DUE'},
      ],
    });
    await tester.pumpAndSettle();
    expect(find.text('Việc tài khoản mới'), findsOneWidget);
    oldRequest.complete(gateway.body);
    await tester.pumpAndSettle();
    expect(find.text('Việc tài khoản mới'), findsOneWidget);
    expect(find.text('Việc đến hạn'), findsNothing);
  });
  testWidgets('FR_030_hienThuocCanhBaoVaBanGiao', (tester) async {
    gateway.body['tasks'] = [];
    gateway.body['medicationDoses'] = [
      {
        'doseKey': 'dose-1',
        'medication': {'description': 'Liều thuốc kiểm thử'},
      },
    ];
    gateway.body['openAlerts'] = [
      {'id': 22, 'residentId': 77},
    ];
    gateway.body['handoverTasks'] = {'receiveAtStart': true, 'prepareAtEnd': true};
    await view(tester);
    expect(find.text('Liều thuốc kiểm thử'), findsOneWidget);
    expect(find.text('Cảnh báo #22'), findsOneWidget);
    expect(find.text('Nhận bàn giao đầu ca'), findsOneWidget);
    expect(find.text('Lập bàn giao cuối ca'), findsOneWidget);
  });
}
