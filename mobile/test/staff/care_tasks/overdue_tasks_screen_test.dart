import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/care_tasks/overdue_tasks_screen.dart';

void main() {
  testWidgets('FR_045_hienQuaHanVaSoLanNhacKhongTuGhiLenh', (t) async {
    await t.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: OverdueTasksScreen(
            accountId: 1,
            canView: true,
            load: (account, floor) async => [
              {
                'id': 7,
                'residentCard': {'fullName': '[Người kiểm thử]'},
                'taskType': 'Hỗ trợ uống nước',
                'plannedAt': '2026-11-02T03:00:00Z',
                'status': 'OVERDUE',
                'importance': 'IMPORTANT',
                'reminderCount': 2,
              },
            ],
          ),
        ),
      ),
    );
    await t.pumpAndSettle();
    expect(find.text('Hỗ trợ uống nước'), findsOneWidget);
    expect(find.text('[Người kiểm thử]'), findsOneWidget);
    expect(find.text('Đã nhắc và báo: 2'), findsOneWidget);
    expect(find.text('Ghi nhận hoàn thành'), findsNothing);
  });

  testWidgets('FR_049_khongCoQuyenKhongTaiDanhSach', (t) async {
    var calls = 0;
    await t.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: OverdueTasksScreen(
            accountId: 1,
            canView: false,
            load: (account, floor) async {
              calls++;
              return [];
            },
          ),
        ),
      ),
    );
    await t.pumpAndSettle();
    expect(calls, 0);
    expect(find.text('Bạn không có quyền xem công việc quá hạn.'), findsOneWidget);
  });

  testWidgets('FR_045_doiTaiKhoanBoDuLieuVaKetQuaCu', (t) async {
    final old = Completer<List<Map<String, dynamic>>>();
    Future<List<Map<String, dynamic>>> load(int account, int? floor) => account == 1 ? old.future : Future.value([]);
    Widget screen(int account) => MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: OverdueTasksScreen(accountId: account, canView: true, load: load),
      ),
    );
    await t.pumpWidget(screen(1));
    await t.pumpWidget(screen(2));
    await t.pumpAndSettle();
    old.complete([
      {'id': 9, 'taskType': 'Dữ liệu tài khoản cũ'},
    ]);
    await t.pumpAndSettle();
    expect(find.text('Dữ liệu tài khoản cũ'), findsNothing);
    expect(find.text('Không có công việc quá hạn'), findsOneWidget);
  });

  testWidgets('FR_045_matMangBaoLoiVaChoThuLai', (t) async {
    var fail = true;
    await t.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: OverdueTasksScreen(
            accountId: 1,
            canView: true,
            load: (account, floor) async {
              if (fail) throw ApiUnreachable();
              return [];
            },
          ),
        ),
      ),
    );
    await t.pumpAndSettle();
    expect(find.text('Không tải được công việc quá hạn. Kiểm tra kết nối rồi thử lại.'), findsOneWidget);
    fail = false;
    await t.tap(find.text('Tải lại'));
    await t.pumpAndSettle();
    expect(find.text('Không có công việc quá hạn'), findsOneWidget);
  });
}
