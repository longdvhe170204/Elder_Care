import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/residents/document_picker.dart';
import 'package:mobile/staff/residents/resident_status_screen.dart';

import '../../support/fake_backend.dart';

// 001 T052, T082 (tên logic trang_thai_test; US4; FR-044 → FR-047b): app nhân viên xem trạng thái, lịch sử và thực hiện
// bốn lệnh Cho tạm vắng, Ghi nhận trở về, Chuyển viện, Ghi nhận qua đời, chỉ trực tuyến (không vào hàng đợi ngoại tuyến).
// GET /residents/{id}, GET …/status-history, GET …/conditions/{command}, POST …/{lệnh}. Nút theo allowedCommands; điều
// kiện chưa thỏa hiện đủ và chặn nút xác nhận; mọi kiểm tra thật do backend. Đính kèm giấy tờ (chụp, tải tệp) chưa làm.

Map<String, dynamic> profile({
  String status = 'IN_RESIDENCE',
  List<String> commands = const ['GRANT_TEMPORARY_ABSENCE', 'TRANSFER_OUT', 'RECORD_DEATH', 'END_RESIDENCY'],
}) => {
  'id': 7,
  'profileCode': 'HS-0007',
  'personalInfo': {'fullName': '[Họ tên 1]', 'dateOfBirth': '1945-03-09', 'gender': 'FEMALE'},
  'status': status,
  'statusSince': '2026-10-01T02:00:00Z',
  'allowedCommands': commands,
  'hiddenFields': <String>[],
};

Map<String, dynamic> entry({
  String? from = 'ADMISSION_IN_PROGRESS',
  String to = 'IN_RESIDENCE',
  String command = 'COMPLETE_ADMISSION',
  String at = '2026-10-01T02:00:00Z',
  bool bySystem = false,
  String? reason = 'Đủ điều kiện',
  List<String> hidden = const [],
  Map<String, dynamic>? waiver,
}) => {
  'from': from,
  'to': to,
  'command': command,
  'at': at,
  'performedBy': '[Họ tên 2]',
  'bySystem': bySystem,
  'reason': reason,
  'conditionWaiver': waiver,
  'hiddenFields': hidden,
};

/// Bộ chọn giấy tờ giả: mỗi lần chụp trả một tệp có tên tăng dần.
class FakePicker implements DocumentPicker {
  int taken = 0;

  @override
  Future<PickedDocument?> capture() async {
    taken++;
    return PickedDocument('giay$taken.jpg', const [1, 2, 3], 'image/jpeg');
  }

  @override
  Future<PickedDocument?> pickFromGallery() => capture();
}

Future<void> pumpScreen(WidgetTester tester, FakeBackend backend, {DocumentPicker? picker}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: AppTheme.light(),
      home: ResidentStatusScreen(api: backend.client(), residentId: 7, picker: picker),
    ),
  );
  await tester.pumpAndSettle();
}

Finder button(String label) =>
    find.ancestor(of: find.text(label), matching: find.byWidgetPredicate((w) => w is ButtonStyleButton));

Future<void> tapButton(WidgetTester tester, String label) async {
  await tester.ensureVisible(button(label));
  await tester.pumpAndSettle();
  await tester.tap(button(label));
  await tester.pumpAndSettle();
}

Map<String, dynamic> lastBody(FakeBackend backend, String path) =>
    jsonDecode(backend.calls('POST', path).last.body) as Map<String, dynamic>;

void main() {
  late FakeBackend backend;

  setUp(() {
    backend = FakeBackend();
    backend.on('GET', '/residents/7', 200, profile());
    backend.on('GET', '/residents/7/status-history', 200, [entry()]);
    for (final c in ['GRANT_TEMPORARY_ABSENCE', 'RECORD_RETURN', 'TRANSFER_OUT', 'RECORD_DEATH']) {
      backend.on('GET', '/residents/7/conditions/$c', 200, <Object>[]);
    }
    backend.on('POST', '/residents/7/grant-temporary-absence', 200, {
      'status': 'TEMPORARILY_ABSENT',
      'warnings': <Object>[],
    });
    backend.on('POST', '/residents/7/transfer-out', 200, {'status': 'HOSPITALIZED', 'warnings': <Object>[]});
    backend.on('POST', '/residents/7/record-return', 200, {'status': 'IN_RESIDENCE', 'warnings': <Object>[]});
    backend.on('POST', '/residents/7/record-death', 200, {'status': 'DECEASED', 'warnings': <Object>[]});
  });

  testWidgets('FR_044_current_showsStatusSinceAndOnlyTheFourAppCommandsInOrder', (tester) async {
    await pumpScreen(tester, backend);

    expect(find.text('Trạng thái hiện tại'), findsOneWidget);
    expect(find.text('Đang lưu trú'), findsWidgets);
    expect(find.text('từ 01/10/2026'), findsOneWidget);
    expect(button('Cho tạm vắng'), findsOneWidget);
    expect(button('Chuyển viện'), findsOneWidget);
    expect(button('Ghi nhận qua đời'), findsOneWidget);
    expect(button('Ghi nhận trở về'), findsNothing);
    expect(button('Kết thúc lưu trú'), findsNothing);
    final order = [
      'Cho tạm vắng',
      'Chuyển viện',
      'Ghi nhận qua đời',
    ].map((l) => tester.getTopLeft(button(l)).dx + tester.getTopLeft(button(l)).dy * 1000).toList();
    expect(order, [...order]..sort());
  });

  testWidgets('FR_044_hospitalized_offersRecordReturn', (tester) async {
    backend.on(
      'GET',
      '/residents/7',
      200,
      profile(status: 'HOSPITALIZED', commands: ['RECORD_RETURN', 'RECORD_DEATH']),
    );
    await pumpScreen(tester, backend);

    expect(button('Ghi nhận trở về'), findsOneWidget);
    expect(button('Cho tạm vắng'), findsNothing);
  });

  testWidgets('FR_047b_recordReturn_uploadsCapturedDocumentsThenSendsAttachmentFileIds', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    backend.on('GET', '/residents/7', 200, profile(status: 'HOSPITALIZED', commands: ['RECORD_RETURN']));
    backend.on('POST', '/files', 201, {'id': 91});
    final picker = FakePicker();
    await pumpScreen(tester, backend, picker: picker);

    await tapButton(tester, 'Ghi nhận trở về');
    await tapButton(tester, 'Chụp giấy tờ');
    await tapButton(tester, 'Chụp giấy tờ');
    expect(find.text('giay1.jpg'), findsOneWidget);
    expect(find.text('giay2.jpg'), findsOneWidget);
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Ra viện về lại');
    await tapButton(tester, 'Xác nhận ghi nhận trở về');

    expect(backend.calls('POST', '/files'), hasLength(2));
    expect(lastBody(backend, '/residents/7/record-return')['attachmentFileIds'], [91, 91]);
  });

  testWidgets('FR_047b_removedDocument_isNotUploadedOrSent', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    backend.on('GET', '/residents/7', 200, profile(status: 'HOSPITALIZED', commands: ['RECORD_RETURN']));
    await pumpScreen(tester, backend, picker: FakePicker());

    await tapButton(tester, 'Ghi nhận trở về');
    await tapButton(tester, 'Chụp giấy tờ');
    await tester.tap(find.byTooltip('Bỏ giay1.jpg'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Ra viện');
    await tapButton(tester, 'Xác nhận ghi nhận trở về');

    expect(backend.calls('POST', '/files'), isEmpty);
    expect(lastBody(backend, '/residents/7/record-return').containsKey('attachmentFileIds'), isFalse);
  });

  testWidgets('FR_047b_recordDeath_uploadsEvidenceAsDetailsEvidenceFileId', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    backend.on('POST', '/files', 201, {'id': 55});
    await pumpScreen(tester, backend, picker: FakePicker());

    await tapButton(tester, 'Ghi nhận qua đời');
    await tapButton(tester, 'Chụp giấy tờ');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Mất tại viện');
    await tapButton(tester, 'Xác nhận ghi nhận qua đời');

    expect((lastBody(backend, '/residents/7/record-death')['details'] as Map)['evidenceFileId'], 55);
  });

  testWidgets('FR_047b_uploadRefused_showsBackendMessageAndDoesNotSendCommand', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    backend.on('GET', '/residents/7', 200, profile(status: 'HOSPITALIZED', commands: ['RECORD_RETURN']));
    backend.on('POST', '/files', 422, {'status': 422, 'errorCode': 'FILE_TOO_LARGE', 'detail': 'Tệp quá 10 MB'});
    await pumpScreen(tester, backend, picker: FakePicker());

    await tapButton(tester, 'Ghi nhận trở về');
    await tapButton(tester, 'Chụp giấy tờ');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Ra viện');
    await tapButton(tester, 'Xác nhận ghi nhận trở về');

    expect(find.text('Tệp quá 10 MB'), findsOneWidget);
    expect(backend.calls('POST', '/residents/7/record-return'), isEmpty);
  });

  testWidgets('FR_044_finalStatus_saysReadOnlyAndHasNoCommands', (tester) async {
    backend.on('GET', '/residents/7', 200, profile(status: 'DECEASED', commands: ['GRANT_TEMPORARY_ABSENCE']));
    await pumpScreen(tester, backend);

    expect(find.text('Hồ sơ ở trạng thái cuối, chỉ xem. Không còn lệnh nào thực hiện được.'), findsOneWidget);
    expect(button('Cho tạm vắng'), findsNothing);
  });

  testWidgets('FR_044_noCommandAllowed_saysSo', (tester) async {
    backend.on('GET', '/residents/7', 200, profile(commands: ['END_RESIDENCY']));
    await pumpScreen(tester, backend);

    expect(find.text('Bạn không có lệnh nào dùng được ở trạng thái này.'), findsOneWidget);
  });

  testWidgets('FR_046_history_newestFirstWithActorReasonAndWaiverNote', (tester) async {
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    backend.on('GET', '/residents/7/status-history', 200, [
      entry(
        from: null,
        to: 'ADMISSION_IN_PROGRESS',
        command: 'CREATE_PROFILE',
        at: '2026-09-30T02:00:00Z',
        reason: null,
      ),
      entry(waiver: {'approvalRequestId': 12}),
      entry(
        from: 'IN_RESIDENCE',
        to: 'TEMPORARILY_ABSENT',
        command: 'ROLL_CALL_DEPARTURE',
        at: '2026-10-05T02:00:00Z',
        bySystem: true,
        reason: null,
      ),
    ]);
    await pumpScreen(tester, backend);

    expect(find.text('Hệ thống (điểm danh), người điểm danh [Họ tên 2]'), findsOneWidget);
    expect(find.text('[Họ tên 2], tạo hồ sơ'), findsOneWidget);
    final newest = tester.getTopLeft(find.text('05/10/2026 09:00')).dy;
    final older = tester.getTopLeft(find.text('01/10/2026 09:00')).dy;
    expect(newest, lessThan(older));
    expect(find.text('Theo yêu cầu ngoại lệ đã duyệt số 12'), findsOneWidget);
    expect(find.text('Miễn điều kiện'), findsOneWidget);
  });

  testWidgets('FR_046_history_hiddenReason_showsHealthViewerNote', (tester) async {
    backend.on('GET', '/residents/7/status-history', 200, [
      entry(reason: null, hidden: ['reason']),
    ]);
    await pumpScreen(tester, backend);

    expect(
      find.text('Lý do và căn cứ chỉ hiện với người được xem thông tin sức khỏe của người cao tuổi này.'),
      findsOneWidget,
    );
  });

  testWidgets('FR_046_history_forbidden_saysNoPermission', (tester) async {
    backend.on('GET', '/residents/7/status-history', 403, {'errorCode': 'FORBIDDEN', 'detail': 'Không đủ quyền.'});
    await pumpScreen(tester, backend);

    expect(find.text('Không có quyền xem lịch sử trạng thái'), findsOneWidget);
  });

  testWidgets('FR_044_loadOffline_saysNeedsConnectionAndRetries', (tester) async {
    backend.down = true;
    await pumpScreen(tester, backend);
    expect(find.textContaining('Không kết nối được máy chủ'), findsOneWidget);

    backend.down = false;
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(find.text('Trạng thái hiện tại'), findsOneWidget);
  });

  testWidgets('FR_045_command_previewsConditionsAndUnmetBlocksConfirm', (tester) async {
    backend.on('GET', '/residents/7/conditions/GRANT_TEMPORARY_ABSENCE', 200, [
      {'code': 'BR-M03-06', 'description': 'Còn thuốc chưa bàn giao', 'basis': 'BR-M03-06', 'waivable': true},
    ]);
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');

    expect(backend.calls('GET', '/residents/7/conditions/GRANT_TEMPORARY_ABSENCE'), hasLength(1));
    expect(find.text('Chưa đủ điều kiện để thực hiện'), findsOneWidget);
    expect(find.text('Còn thuốc chưa bàn giao'), findsOneWidget);
    expect(find.text('Miễn được'), findsOneWidget);
    expect(tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Xác nhận cho tạm vắng')).onPressed, isNull);
  });

  testWidgets('FR_045_command_conditionsMet_saysEnoughAndShowsEffect', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');

    expect(find.text('Đủ điều kiện để thực hiện.'), findsOneWidget);
    expect(
      find.text('Người cao tuổi chuyển sang Tạm vắng; công việc, suất ăn, liều thuốc trong thời gian vắng được xử lý.'),
      findsOneWidget,
    );
  });

  testWidgets('FR_047_command_missingReason_showsErrorAndSendsNothing', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tapButton(tester, 'Xác nhận cho tạm vắng');

    expect(find.text('Nhập lý do của lệnh'), findsOneWidget);
    expect(backend.calls('POST', '/residents/7/grant-temporary-absence'), isEmpty);
  });

  testWidgets('FR_044_grantAbsence_sendsReasonKeyThenShowsNewStatusAndReloads', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), ' Về thăm nhà ');
    backend.on('GET', '/residents/7', 200, profile(status: 'TEMPORARILY_ABSENT', commands: ['RECORD_RETURN']));
    await tapButton(tester, 'Xác nhận cho tạm vắng');

    expect(lastBody(backend, '/residents/7/grant-temporary-absence'), {
      'reason': 'Về thăm nhà',
      'details': <String, dynamic>{},
    });
    expect(backend.calls('POST', '/residents/7/grant-temporary-absence').last.headers['Idempotency-Key'], isNotEmpty);
    expect(find.text('Đã chuyển sang Tạm vắng.'), findsOneWidget);
    expect(backend.calls('GET', '/residents/7/status-history'), hasLength(2));
    expect(button('Ghi nhận trở về'), findsOneWidget);
    expect(button('Cho tạm vắng'), findsNothing);
  });

  testWidgets('FR_044_command_warningsFromBackend_showInBanner', (tester) async {
    backend.on('POST', '/residents/7/grant-temporary-absence', 200, {
      'status': 'TEMPORARILY_ABSENT',
      'warnings': [
        {'code': 'X', 'description': 'Có liều thuốc cần bàn giao'},
      ],
    });
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Về nhà');
    await tapButton(tester, 'Xác nhận cho tạm vắng');

    expect(find.textContaining('Có liều thuốc cần bàn giao'), findsOneWidget);
  });

  testWidgets('FR_044_transferOut_requiresBasisThenSendsNumericIncidentId', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Chuyển viện');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Khó thở');
    await tapButton(tester, 'Xác nhận chuyển viện');
    expect(find.text('Chọn và nhập căn cứ chuyển viện'), findsOneWidget);
    expect(backend.calls('POST', '/residents/7/transfer-out'), isEmpty);

    await tester.tap(find.widgetWithText(ChoiceChip, 'Sự cố'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Nội dung căn cứ'), '12');
    await tapButton(tester, 'Xác nhận chuyển viện');

    expect(lastBody(backend, '/residents/7/transfer-out'), {
      'reason': 'Khó thở',
      'details': {
        'basis': {'incidentId': 12},
      },
    });
  });

  testWidgets('FR_044_transferOut_doctorOrder_sendsText', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Chuyển viện');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Khó thở');
    await tester.tap(find.widgetWithText(ChoiceChip, 'Chỉ định của Bác sĩ'));
    await tester.pumpAndSettle();
    await tester.enterText(find.widgetWithText(TextField, 'Nội dung căn cứ'), 'Chuyển viện cấp cứu');
    await tapButton(tester, 'Xác nhận chuyển viện');

    expect((lastBody(backend, '/residents/7/transfer-out')['details'] as Map)['basis'], {
      'doctorOrder': 'Chuyển viện cấp cứu',
    });
  });

  testWidgets('FR_047b_recordDeath_evidenceRequired422_showsMessageAtEvidenceField', (tester) async {
    backend.on('POST', '/residents/7/record-death', 422, {'errorCode': 'EVIDENCE_REQUIRED', 'detail': 'Thiếu giấy tờ'});
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Ghi nhận qua đời');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Qua đời tại viện');
    await tapButton(tester, 'Xác nhận ghi nhận qua đời');

    expect(
      find.textContaining('Phải kèm giấy tờ bằng chứng khi Hành chính ghi nhận qua đời ngoài viện'),
      findsOneWidget,
    );
  });

  testWidgets('FR_047_command_reasonRequired422_showsMessageAtReasonField', (tester) async {
    backend.on('POST', '/residents/7/grant-temporary-absence', 422, {
      'errorCode': 'REASON_REQUIRED',
      'detail': 'Cần lý do rõ hơn',
    });
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'x');
    await tapButton(tester, 'Xác nhận cho tạm vắng');

    expect(find.text('Cần lý do rõ hơn'), findsOneWidget);
  });

  testWidgets('FR_048_command_conflict409_showsMessageAndReloadsConditions', (tester) async {
    backend.on('POST', '/residents/7/grant-temporary-absence', 409, {
      'errorCode': 'UNMET_CONDITIONS',
      'detail': 'Điều kiện vừa thay đổi',
    });
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Về nhà');
    await tapButton(tester, 'Xác nhận cho tạm vắng');

    expect(find.text('Điều kiện vừa thay đổi'), findsOneWidget);
    expect(backend.calls('GET', '/residents/7/conditions/GRANT_TEMPORARY_ABSENCE'), hasLength(2));
  });

  testWidgets('R_13_command_offline_saysNeedsConnectionAndQueuesNothing', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tester.enterText(find.widgetWithText(TextField, 'Lý do của lệnh (bắt buộc)'), 'Về nhà');
    backend.down = true;
    await tapButton(tester, 'Xác nhận cho tạm vắng');

    expect(find.textContaining('Không kết nối được máy chủ'), findsOneWidget);
    expect(find.widgetWithText(FilledButton, 'Xác nhận cho tạm vắng'), findsOneWidget);
  });

  testWidgets('FR_044_command_cancel_closesSheetWithoutSending', (tester) async {
    await pumpScreen(tester, backend);

    await tapButton(tester, 'Cho tạm vắng');
    await tapButton(tester, 'Thôi');

    expect(find.text('Xác nhận cho tạm vắng'), findsNothing);
    expect(backend.requests.where((r) => r.method == 'POST'), isEmpty);
  });
}
