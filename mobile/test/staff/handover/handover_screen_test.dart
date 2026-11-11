import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/auth/secure_store.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/handover/handover_cache.dart';
import 'package:mobile/staff/handover/handover_screen.dart';

import '../../support/fake_backend.dart';

Map<String, dynamic> handover({List<String> commands = const []}) => {
  'id': 21,
  'shiftId': 12,
  'status': 'PREPARED',
  'generalAssessment': 'Nội dung bàn giao kiểm thử',
  'residentAssessments': <Object>[],
  'items': [
    {'id': 30, 'group': 'TASK', 'description': 'Việc cần theo dõi kiểm thử', 'section': 'FINALIZED'},
  ],
  'comments': <Object>[],
  'allowedCommands': commands,
};

FakeBackend backend({List<String> commands = const []}) => FakeBackend()
  ..on('GET', '/me/permissions', 200, [
    {'function': 'SHIFT_HANDOVER', 'operation': 'VIEW'},
  ])
  ..on('GET', '/shifts/12', 200, {
    'id': 12,
    'startsAt': '2026-11-07T07:00:00Z',
    'endsAt': '2026-11-07T15:00:00Z',
    'allowedCommands': <Object>[],
  })
  ..on('GET', '/shifts/12/handovers', 200, handover(commands: commands))
  ..on('GET', '/handovers/21', 200, handover(commands: commands), {'etag': '"5"'})
  ..on('GET', '/handovers/21/source-records', 200, <Object>[]);

Future<void> pump(WidgetTester tester, FakeBackend b, {bool dark = false, double scale = 1}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: dark ? AppTheme.dark() : AppTheme.light(),
      builder: (context, child) => MediaQuery(
        data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(scale)),
        child: child!,
      ),
      home: Scaffold(body: HandoverScreen(api: b.client(), initialShiftId: 12)),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('FR_054_chỉ hiện nội dung và lệnh được backend cho phép', (tester) async {
    final b = backend();
    await pump(tester, b);
    expect(find.text('Nội dung bàn giao kiểm thử'), findsOneWidget);
    expect(find.text('Việc cần theo dõi kiểm thử'), findsOneWidget);
    expect(find.text('Xác nhận bàn giao'), findsNothing);
    expect(find.text('Hoàn tất bàn giao'), findsNothing);
    expect(b.calls('POST', '/handovers/21/confirm'), isEmpty);
  });

  testWidgets('FR_060_lỗi mạng khi tải lại xóa nội dung, không dùng bản không có hạn phạm vi', (tester) async {
    final b = backend();
    await pump(tester, b);
    b.on('GET', '/shifts/12/handovers', 503, {'detail': 'Chưa đọc được bàn giao'});
    await tester.tap(find.text('Tải lại'));
    await tester.pumpAndSettle();
    expect(find.text('Nội dung bàn giao kiểm thử'), findsNothing);
    expect(find.text('Việc cần theo dõi kiểm thử'), findsNothing);
    expect(find.text('Chưa đọc được bàn giao'), findsOneWidget);
  });

  testWidgets('FR_054_mất quyền không giữ nội dung đã mở', (tester) async {
    final b = backend();
    await pump(tester, b);
    b.on('GET', '/me/permissions', 403, {'detail': 'Quyền xem đã thu hồi'});
    await tester.tap(find.text('Tải lại'));
    await tester.pumpAndSettle();
    expect(find.text('Nội dung bàn giao kiểm thử'), findsNothing);
    expect(find.text('Quyền xem đã thu hồi'), findsOneWidget);
  });

  testWidgets('FR_042_gửi ý kiến giữ nội dung và khóa lệnh khi chưa nhận xác nhận', (tester) async {
    final b = backend(commands: ['comment'])
      ..on('POST', '/handovers/21/comments', 503, {'detail': 'Chưa nhận xác nhận'});
    await pump(tester, b);
    await tester.scrollUntilVisible(find.text('Ý kiến bàn giao'), 150, scrollable: find.byType(Scrollable).first);
    await tester.enterText(find.byType(TextField), 'Ý kiến kiểm thử');
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Gửi ý kiến'), 150, scrollable: find.byType(Scrollable).first);
    await tester.tap(find.text('Gửi ý kiến'));
    await tester.pumpAndSettle();
    final first = b.calls('POST', '/handovers/21/comments').single;
    expect(jsonDecode(first.body), {'content': 'Ý kiến kiểm thử'});
    expect(find.text('Ý kiến kiểm thử'), findsOneWidget);
    await tester.ensureVisible(find.text('Gửi ý kiến'));
    await tester.tap(find.text('Gửi ý kiến'));
    await tester.pumpAndSettle();
    expect(b.calls('POST', '/handovers/21/comments').last.headers['Idempotency-Key'], first.headers['Idempotency-Key']);
  });

  testWidgets('FR_042_xác nhận cần thao tác xác nhận và đọc lại trạng thái máy chủ', (tester) async {
    final b = backend(commands: ['confirm'])..on('POST', '/handovers/21/confirm', 200, {});
    await pump(tester, b);
    await tester.scrollUntilVisible(find.text('Xác nhận bàn giao'), 150);
    await tester.tap(find.text('Xác nhận bàn giao'));
    await tester.pumpAndSettle();
    expect(b.calls('POST', '/handovers/21/confirm'), isEmpty);
    b.on('GET', '/shifts/12/handovers', 200, {...handover(), 'status': 'CONFIRMED'});
    b.on('GET', '/handovers/21', 200, {...handover(), 'status': 'CONFIRMED'}, {'etag': '"6"'});
    await tester.tap(find.text('Tiếp tục'));
    await tester.pumpAndSettle();
    expect(b.calls('POST', '/handovers/21/confirm'), hasLength(1));
    expect(find.text('Đã xác nhận'), findsOneWidget);
    expect(find.text('Xác nhận bàn giao'), findsNothing);
  });

  testWidgets('FR_041_hoàn tất bị chặn giữ nội dung và lý do backend', (tester) async {
    final draft = {
      ...handover(commands: ['complete']),
      'status': 'DRAFT',
    };
    final b = backend()
      ..on('GET', '/shifts/12/handovers', 200, draft)
      ..on('GET', '/handovers/21', 200, draft, {'etag': '"5"'})
      ..on('POST', '/handovers/21/complete', 409, {'detail': 'Cần ghi chú mục nghiêm trọng'});
    await pump(tester, b);
    await tester.scrollUntilVisible(find.text('Hoàn tất bàn giao'), 150);
    await tester.tap(find.text('Hoàn tất bàn giao'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tiếp tục'));
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Cần ghi chú mục nghiêm trọng'), -150);
    expect(find.text('Cần ghi chú mục nghiêm trọng'), findsOneWidget);
    expect(find.text('Bản nháp'), findsOneWidget);
    expect(b.calls('POST', '/handovers/21/complete'), hasLength(1));
  });

  testWidgets('FR_038_chỉ lập bàn giao bằng lệnh khi backend cho phép', (tester) async {
    final b = backend()
      ..on('GET', '/shifts/12/handovers', 404, {'detail': 'Chưa có bàn giao'})
      ..on('GET', '/shifts/12', 200, {
        'id': 12,
        'allowedCommands': ['createHandover'],
      })
      ..on('POST', '/shifts/12/handovers', 201, handover());
    await pump(tester, b);
    expect(b.calls('POST', '/shifts/12/handovers'), isEmpty);
    b.on('GET', '/shifts/12/handovers', 200, handover());
    b.on('GET', '/handovers/21', 200, handover(), {'etag': '"6"'});
    await tester.tap(find.text('Lập bàn giao'));
    await tester.pumpAndSettle();
    expect(b.calls('POST', '/shifts/12/handovers'), hasLength(1));
    expect(find.text('Nội dung bàn giao kiểm thử'), findsOneWidget);
  });

  testWidgets('FR_042_gửi lại phản hồi đúng ý kiến, không tự tạo mục bổ sung', (tester) async {
    final value = {
      ...handover(commands: ['supplementAndResubmit']),
      'status': 'HAS_COMMENTS',
      'comments': [
        {'id': 31, 'content': 'Ý kiến cần phản hồi', 'replies': <Object>[]},
      ],
    };
    final b = backend()
      ..on('GET', '/shifts/12/handovers', 200, value)
      ..on('GET', '/handovers/21', 200, value, {'etag': '"6"'})
      ..on('POST', '/handovers/21/supplement-and-resubmit', 200, {});
    await pump(tester, b);
    await tester.scrollUntilVisible(find.text('Phản hồi ý kiến'), 150, scrollable: find.byType(Scrollable).first);
    await tester.enterText(find.byType(TextFormField), ' Phản hồi kiểm thử ');
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(find.text('Bổ sung và gửi lại'), 150, scrollable: find.byType(Scrollable).first);
    b.on('GET', '/shifts/12/handovers', 200, handover());
    b.on('GET', '/handovers/21', 200, handover(), {'etag': '"6"'});
    await tester.tap(find.text('Bổ sung và gửi lại'));
    await tester.pumpAndSettle();
    expect(jsonDecode(b.calls('POST', '/handovers/21/supplement-and-resubmit').single.body), {
      'replies': [
        {'commentId': 31, 'content': 'Phản hồi kiểm thử'},
      ],
    });
  });

  for (final dark in [false, true]) {
    testWidgets('FR_054_bàn giao chữ200_sáng_tối_$dark', (tester) async {
      tester.view.physicalSize = const Size(390, 844);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      await pump(tester, backend(), dark: dark, scale: 2);
      await tester.scrollUntilVisible(find.text('Việc cần theo dõi kiểm thử'), 200);
      expect(tester.takeException(), isNull);
    });
  }

  testWidgets('FR_060_bản lưu có hạn phạm vi xem được khi mất mạng, không có lệnh', (tester) async {
    final store = MemoryStore();
    final b = backend(commands: ['confirm'])
      ..on('GET', '/me/shifts', 200, [
        {
          'id': 12,
          'startsAt': '2026-11-07T07:00:00Z',
          'endsAt': '2026-11-07T15:00:00Z',
          'scopeEndsAt': DateTime.now().toUtc().add(const Duration(hours: 2)).toIso8601String(),
        },
      ]);
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: HandoverScreen(api: b.client(), cache: HandoverCache(store, 7)),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.byType(DropdownButtonFormField<int>));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(DropdownMenuItem<int>).last);
    await tester.pumpAndSettle();
    expect(find.text('Xác nhận bàn giao'), findsOneWidget);
    expect(store.values.keys, contains('handover.7.12'));

    b.down = true;
    await tester.tap(find.text('Tải lại'));
    await tester.pumpAndSettle();
    expect(find.text('Nội dung bàn giao kiểm thử'), findsOneWidget);
    expect(find.textContaining('Bản lưu trên máy lúc'), findsOneWidget);
    expect(find.text('Xác nhận bàn giao'), findsNothing);
  });

  testWidgets('FR_060_bản lưu quá hạn bị xóa, mất mạng không hiện nội dung', (tester) async {
    final store = MemoryStore()
      ..values['handover.index.7'] = '[12]'
      ..values['handover.7.12'] = jsonEncode({
        'record': handover(),
        'scopeEndsAt': DateTime.now().toUtc().subtract(const Duration(minutes: 1)).toIso8601String(),
        'savedAt': '2026-11-07T08:00:00Z',
      });
    final b = backend()..down = true;
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: HandoverScreen(api: b.client(), initialShiftId: 12, cache: HandoverCache(store, 7)),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Nội dung bàn giao kiểm thử'), findsNothing);
    expect(store.values.containsKey('handover.7.12'), isFalse);
  });

  testWidgets('FR_040_thêm mục từ nguồn gửi If-Match theo ETag', (tester) async {
    final b = backend(commands: ['addItem'])
      ..on('GET', '/handovers/21/source-records', 200, [
        {'group': 'ALERT', 'sourceType': 'M05.ALERT', 'sourceId': '81', 'residentId': 5, 'description': 'Cảnh báo sốt'},
      ])
      ..on('POST', '/handovers/21/items', 201);
    await pump(tester, b);
    await tester.scrollUntilVisible(find.text('Thêm'), 150, scrollable: find.byType(Scrollable).first);
    await tester.tap(find.text('Thêm'));
    await tester.pumpAndSettle();
    final post = b.calls('POST', '/handovers/21/items').single;
    expect(post.headers['If-Match'], '"5"');
    expect(post.headers['Idempotency-Key'], isNotEmpty);
    expect(jsonDecode(post.body), {
      'group': 'ALERT',
      'sourceType': 'M05.ALERT',
      'sourceId': '81',
      'residentId': 5,
      'description': 'Cảnh báo sốt',
    });
  });
}
