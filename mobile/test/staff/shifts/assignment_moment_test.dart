import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/shifts/assignment_moment.dart';

void main() {
  testWidgets('FR_025_ngày giờ Việt Nam, không chấp nhận ngày tràn hoặc giờ sai', (tester) async {
    String? result;
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Builder(
          builder: (context) => Scaffold(
            body: TextButton(
              onPressed: () async {
                result = await assignmentMoment(context, DateTime.utc(2026, 11, 7, 22), withTime: true);
              },
              child: const Text('Mở'),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('Mở'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextFormField).first, '31/02/2026');
    await tester.tap(find.text('Chọn'));
    await tester.pumpAndSettle();
    expect(find.text('Ngày không hợp lệ.'), findsOneWidget);
    expect(result, isNull);
    await tester.enterText(find.byType(TextFormField).first, '07/11/2026');
    await tester.enterText(find.byType(TextFormField).last, '25:00');
    await tester.tap(find.text('Chọn'));
    await tester.pumpAndSettle();
    expect(find.text('Giờ không hợp lệ.'), findsOneWidget);
    await tester.enterText(find.byType(TextFormField).last, '06:15');
    await tester.tap(find.text('Chọn'));
    await tester.pumpAndSettle();
    expect(result, '2026-11-07T06:15:00+07:00');
  });
}
