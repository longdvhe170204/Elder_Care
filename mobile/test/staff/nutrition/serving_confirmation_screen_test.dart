import 'dart:convert';

import 'package:drift/drift.dart' show driftRuntimeOptions;
import 'package:drift/native.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/offline/offline_database.dart';
import 'package:mobile/common/offline/offline_queue.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/nutrition/serving_confirmation_screen.dart';

import '../../support/fake_backend.dart';

// 011 T032 (US6; FR-049 → FR-051; quickstart Q38 → Q41): nhân viên chọn bữa, chọn suất theo nhãn, chọn người đang được
// phục vụ rồi xác nhận. Sai người hiện lý do; suất có dị ứng yêu cầu khai đã ăn/chưa ăn (đã ăn kèm sự cố); mất mạng thì
// lưu vào hàng đợi ngoại tuyến với loại bản ghi M08.SERVING_CONFIRMATION.

const _meals = [
  {'dailyMealId': 31, 'mealId': 3, 'mealName': 'Trưa', 'closed': true},
  {'dailyMealId': 32, 'mealId': 4, 'mealName': 'Tối', 'closed': false},
];

const _slips = [
  {
    'id': 70,
    'floorArea': 'Tầng 2',
    'meal': 'Trưa',
    'status': 'DELIVERED',
    'labels': [
      {
        'id': 501,
        'fullName': 'Nguyễn Thị Lan',
        'room': 'P201',
        'diet': 'Tiểu đường',
        'texture': 'SOFT',
        'dishes': 'Cháo thịt',
        'flags': <String>[],
        'prepared': true,
      },
    ],
  },
];

const _residents = {
  'items': [
    {
      'id': 5,
      'profileCode': 'NCT-0005',
      'personalInfo': {'fullName': 'Nguyễn Thị Lan'},
    },
    {
      'id': 6,
      'profileCode': 'NCT-0006',
      'personalInfo': {'fullName': 'Trần Văn Bình'},
    },
  ],
  'nextCursor': null,
};

FakeBackend backend() => FakeBackend()
  ..on('GET', '/daily-meals', 200, _meals)
  ..on('GET', '/meal-slips', 200, _slips)
  ..on('GET', '/residents', 200, _residents);

OfflineQueue queueOf(ApiClient api) {
  driftRuntimeOptions.dontWarnAboutMultipleDatabases = true;
  return OfflineQueue(OfflineDatabase(NativeDatabase.memory()), api);
}

Future<OfflineQueue> pumpScreen(WidgetTester tester, ApiClient api) async {
  final queue = queueOf(api);
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: Scaffold(
        body: ServingConfirmationScreen(api: api, queue: queue, accountId: 7),
      ),
    ),
  );
  await tester.pumpAndSettle();
  return queue;
}

Future<void> tapText(WidgetTester tester, String text) async {
  await tester.ensureVisible(find.text(text).first);
  await tester.pumpAndSettle();
  await tester.tap(find.text(text).first);
  await tester.pumpAndSettle();
}

Future<void> openLabel(WidgetTester tester) async {
  await tapText(tester, 'Trưa');
  await tapText(tester, 'Nguyễn Thị Lan');
  expect(find.text('Suất của Nguyễn Thị Lan'), findsOneWidget);
}

void main() {
  testWidgets('Q38: chọn suất theo nhãn, chọn người phục vụ, sai người thì hiện lý do', (tester) async {
    final b = backend()
      ..on('POST', '/special-portions/501/confirm-serving', 409, {
        'errorCode': 'PORTION_WRONG_RESIDENT',
        'detail': 'Suất không đúng người được phục vụ',
      });
    await pumpScreen(tester, b.client());
    expect(find.text('Tối'), findsOneWidget);
    await openLabel(tester);

    await tapText(tester, 'Xác nhận phục vụ');
    expect(find.text('Chọn người đang được phục vụ.'), findsOneWidget);
    expect(b.calls('POST', '/special-portions/501/confirm-serving'), isEmpty);

    await tapText(tester, 'Trần Văn Bình');
    await tapText(tester, 'Xác nhận phục vụ');
    expect(find.text('Suất không đúng người được phục vụ'), findsOneWidget);
    final sent = jsonDecode(b.calls('POST', '/special-portions/501/confirm-serving').single.body) as Map;
    expect(sent, {'residentId': 6});
  });

  testWidgets('đúng người thì báo đã xác nhận', (tester) async {
    final b = backend()..on('POST', '/special-portions/501/confirm-serving', 201);
    await pumpScreen(tester, b.client());
    await openLabel(tester);
    await tapText(tester, 'NCT-0005');
    await tapText(tester, 'Xác nhận phục vụ');
    expect(find.text('Đã xác nhận phục vụ đúng suất.'), findsOneWidget);
  });

  testWidgets('Q39: suất có dị ứng yêu cầu khai đã ăn hay chưa ăn; đã ăn gửi kèm sự cố', (tester) async {
    final b = backend()
      ..on('POST', '/special-portions/501/confirm-serving', 409, {
        'errorCode': 'PORTION_HAS_ALLERGEN',
        'detail': 'Suất có thành phần gây dị ứng với người được phục vụ: khai đã ăn hoặc chưa ăn',
      });
    await pumpScreen(tester, b.client());
    await openLabel(tester);
    await tapText(tester, 'NCT-0005');
    await tapText(tester, 'Xác nhận phục vụ');
    expect(find.text('Suất có thành phần gây dị ứng với người này'), findsOneWidget);

    await tapText(tester, 'Ghi nhận');
    expect(find.text('Chọn người đó đã ăn hay chưa ăn.'), findsOneWidget);

    await tapText(tester, 'Đã ăn');
    await tapText(tester, 'Ghi nhận');
    expect(find.text('Khai đã ăn cần nhập mức độ và triệu chứng.'), findsOneWidget);

    await tester.enterText(find.widgetWithText(TextField, 'Mức độ'), 'MEDIUM');
    await tester.enterText(find.widgetWithText(TextField, 'Triệu chứng'), 'Nổi mề đay');
    b.on('POST', '/special-portions/501/confirm-serving', 201);
    await tapText(tester, 'Ghi nhận');
    expect(find.text('Đã ghi nhận. Cảnh báo đã gửi tới điều dưỡng phụ trách.'), findsOneWidget);
    final sent = jsonDecode(b.calls('POST', '/special-portions/501/confirm-serving').last.body) as Map;
    expect(sent['hasEaten'], true);
    expect(sent['incident'], {'severity': 'MEDIUM', 'symptoms': 'Nổi mề đay'});
  });

  testWidgets('Q39: chưa ăn thì không gửi dữ liệu sự cố', (tester) async {
    final b = backend()
      ..on('POST', '/special-portions/501/confirm-serving', 409, {
        'errorCode': 'PORTION_HAS_ALLERGEN',
        'detail': 'Có dị ứng',
      });
    await pumpScreen(tester, b.client());
    await openLabel(tester);
    await tapText(tester, 'NCT-0005');
    await tapText(tester, 'Xác nhận phục vụ');
    await tapText(tester, 'Chưa ăn');
    b.on('POST', '/special-portions/501/confirm-serving', 201);
    await tapText(tester, 'Ghi nhận');
    final sent = jsonDecode(b.calls('POST', '/special-portions/501/confirm-serving').last.body) as Map;
    expect(sent, {'residentId': 5, 'hasEaten': false});
  });

  testWidgets('Q41: mất mạng thì lưu vào hàng đợi ngoại tuyến', (tester) async {
    final b = backend();
    final api = b.client();
    final queue = await pumpScreen(tester, api);
    await openLabel(tester);
    await tapText(tester, 'NCT-0005');
    b.down = true;
    await tapText(tester, 'Xác nhận phục vụ');
    expect(find.textContaining('Đã lưu để gửi khi có mạng'), findsOneWidget);
    final waiting = await queue.unsynced(7);
    expect(waiting, hasLength(1));
    expect(waiting.single.recordType, servingRecordType);
    expect(jsonDecode(waiting.single.content), {'specialPortionId': 501, 'residentId': 5});
  });
}
