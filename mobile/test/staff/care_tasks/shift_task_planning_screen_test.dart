import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/care_tasks/care_task_planning_gateway.dart';
import 'package:mobile/staff/care_tasks/shift_task_planning_screen.dart';

class FixtureGateway implements CareTaskPlanningGateway {
  bool canCreate = true;
  int writes = 0;
  CareTaskPlanningError? failure;
  List<Map<String, dynamic>>? writtenRows;
  @override
  Future<List<Map<String, dynamic>>> shifts() async => [];
  @override
  Future<CareTaskPlanningData> load(int shiftId) async => CareTaskPlanningData(
    source: {
      'shift': {'id': 5, 'startsAt': '2026-11-01T00:00:00Z', 'endsAt': '2026-11-01T08:00:00Z', 'floorAreaId': 3},
      'residents': [
        {
          'residentId': 77,
          'assignments': [
            {'staffId': 22, 'fullName': '[Nhân viên kiểm thử]'},
          ],
          'planItems': [
            {'id': 90, 'content': 'Mục kế hoạch kiểm thử'},
          ],
          'otherSources': [],
        },
      ],
    },
    residents: [
      {'id': 77, 'fullName': '[Họ tên kiểm thử]'},
    ],
    taskTypes: [
      {'id': 9, 'name': 'Loại kiểm thử', 'status': 'ACTIVE'},
    ],
    tasks: [],
    canCreate: canCreate,
    shiftMembers: [
      {'staffId': 22},
      {'staffId': 33},
    ],
  );
  @override
  Future<void> plan(int shiftId, List<Map<String, dynamic>> rows, String idempotencyKey) async {
    writes++;
    writtenRows = rows;
    if (failure != null) throw failure!;
  }
}

void main() {
  testWidgets('FR_017_nguonKhongTuDienCongViecVaKhongTuGuiLenh', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(body: ShiftTaskPlanningScreen(gateway: gateway, shiftId: 5)),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Chưa có dòng công việc.'), findsOneWidget);
    expect(gateway.writes, 0);
    await tester.tap(find.text('Thêm dòng công việc'));
    await tester.pumpAndSettle();
    expect(find.byKey(const ValueKey('content-1')), findsOneWidget);
    expect(find.text('Mục kế hoạch kiểm thử'), findsOneWidget);
    expect(gateway.writes, 0);
  });
  testWidgets('FR_017_khongCoQuyenThiKhongHienLenhLap', (tester) async {
    final gateway = FixtureGateway()..canCreate = false;
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(body: ShiftTaskPlanningScreen(gateway: gateway, shiftId: 5)),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Công việc đã lập (0)'), findsOneWidget);
    expect(find.text('Thêm dòng công việc'), findsNothing);
  });
  testWidgets('FR_017_boDongTruocKhongDoiNoiDungDongSau', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(body: ShiftTaskPlanningScreen(gateway: gateway, shiftId: 5)),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Thêm dòng công việc'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byKey(const ValueKey('content-1')));
    await tester.enterText(find.byKey(const ValueKey('content-1')), 'Nội dung dòng trước');
    await tester.ensureVisible(find.text('Thêm dòng công việc'));
    await tester.tap(find.text('Thêm dòng công việc'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byKey(const ValueKey('content-2')));
    await tester.enterText(find.byKey(const ValueKey('content-2')), 'Nội dung cần giữ');
    await tester.ensureVisible(find.text('Bỏ dòng 1'));
    await tester.tap(find.text('Bỏ dòng 1'));
    await tester.pumpAndSettle();
    final editor = find.descendant(of: find.byKey(const ValueKey('content-1')), matching: find.byType(EditableText));
    expect(tester.widget<EditableText>(editor).controller.text, 'Nội dung cần giữ');
  });
  testWidgets('BR_M04_01_loiTheoDongGiuNoiDungVaPayloadDungMuiGio', (tester) async {
    final gateway = FixtureGateway()
      ..failure = const CareTaskPlanningError(
        'Chưa lưu lô công việc.',
        rowErrors: [
          {
            'row': 1,
            'codes': ['DUPLICATE_BASIS'],
          },
        ],
      );
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(body: ShiftTaskPlanningScreen(gateway: gateway, shiftId: 5)),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Thêm dòng công việc'));
    await tester.pumpAndSettle();
    Future<void> select(String field, String text) async {
      final dropdown = find.byKey(ValueKey('$field-1-'));
      await tester.ensureVisible(dropdown);
      await tester.tap(dropdown);
      await tester.pumpAndSettle();
      await tester.tap(find.text(text).last);
      await tester.pumpAndSettle();
    }

    await select('residentId', '[Họ tên kiểm thử]');
    await select('taskTypeId', 'Loại kiểm thử');
    await select('basisType', 'Mục kế hoạch');
    await select('basisId', 'Mục kế hoạch kiểm thử');
    await select('importance', 'Thường');
    await select('performerId', '[Nhân viên kiểm thử]');
    await tester.ensureVisible(find.byKey(const ValueKey('plannedAt-1')));
    await tester.enterText(find.byKey(const ValueKey('plannedAt-1')), '01/11/2026 08:00');
    await tester.ensureVisible(find.byKey(const ValueKey('content-1')));
    await tester.enterText(find.byKey(const ValueKey('content-1')), 'Nội dung tự nhập');
    tester.testTextInput.hide();
    await tester.pumpAndSettle();
    final submit = find.widgetWithText(FilledButton, 'Lập công việc của ca');
    await tester.ensureVisible(submit);
    await tester.pumpAndSettle();
    await tester.tap(submit);
    await tester.pumpAndSettle();
    expect(gateway.writes, 1);
    expect(gateway.writtenRows!.single['plannedAt'], '2026-11-01T08:00:00+07:00');
    expect(find.text('Trùng căn cứ và thời điểm dự kiến.'), findsOneWidget);
    expect(find.text('Nội dung tự nhập'), findsOneWidget);
    expect(find.text('Đã lập công việc của ca.'), findsNothing);
  });
  testWidgets('BR_M04_24_chonDuocThanhVienCaKhongCoPhanCongTheoNguoi', (tester) async {
    final gateway = FixtureGateway();
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(body: ShiftTaskPlanningScreen(gateway: gateway, shiftId: 5)),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Thêm dòng công việc'));
    await tester.pumpAndSettle();
    final location = find.byKey(const ValueKey('locationType-1-'));
    await tester.ensureVisible(location);
    await tester.pumpAndSettle();
    await tester.tap(location);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tầng của ca').last);
    await tester.pumpAndSettle();
    final performer = find.byKey(const ValueKey('performerId-1-'));
    await tester.ensureVisible(performer);
    await tester.pumpAndSettle();
    await tester.tap(performer);
    await tester.pumpAndSettle();
    expect(find.text('Nhân viên · mã 33'), findsOneWidget);
  });
}
