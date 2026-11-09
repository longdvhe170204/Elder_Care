import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:mobile/common/api/api_client.dart';
import 'package:mobile/common/theme/app_theme.dart';
import 'package:mobile/staff/beds/bed_readiness_screen.dart';
import 'package:mobile/staff/beds/bed_transfer_screen.dart';
import 'package:mobile/staff/beds/beds_screen.dart';
import 'package:mobile/staff/beds/room_isolation_screen.dart';

void main() {
  late ApiClient api;
  late List<http.Request> commands;
  late bool writable, offline, blocked, retired, doctorOnly, targetGone, shiftLead, noDuty;
  late List<Map<String, dynamic>> history;
  setUp(() {
    commands = [];
    history = [];
    writable = true;
    offline = blocked = false;
    retired = false;
    doctorOnly = targetGone = false;
    shiftLead = noDuty = false;
    api = ApiClient(
      baseUrl: 'http://fixture',
      httpClient: MockClient((r) async {
        Object body;
        if ((shiftLead || noDuty) && r.url.path == '/api/v1/beds' ||
            noDuty && r.url.path == '/api/v1/bed-readiness/beds') {
          return http.Response(
            jsonEncode({'detail': 'Không có quyền'}),
            403,
            headers: {'content-type': 'application/problem+json; charset=utf-8'},
          );
        }
        if (doctorOnly &&
            (r.url.path == '/api/v1/rooms/2' ||
                r.url.path == '/api/v1/beds' ||
                r.url.path == '/api/v1/bed-allocations')) {
          return http.Response(
            jsonEncode({'detail': 'Không có quyền'}),
            403,
            headers: {'content-type': 'application/problem+json; charset=utf-8'},
          );
        }
        if (r.method == 'POST') {
          commands.add(r);
          if (offline) throw http.ClientException('Ngoại tuyến');
          if (blocked) {
            return http.Response(
              jsonEncode({
                'detail': 'Giường đã được phân bổ',
                'errorCode': 'UNMET_CONDITIONS',
                'unmetConditions': [
                  {'description': 'Giường đích không còn trống'},
                ],
              }),
              409,
              headers: {'content-type': 'application/problem+json; charset=utf-8'},
            );
          }
          if (r.url.path == '/api/v1/rooms/2/isolate') {
            history.add({
              'action': 'ISOLATE',
              'occurredAt': '2026-11-08T03:00:00Z',
              'actor': '[Bác sĩ đặt]',
              'reason': (jsonDecode(r.body) as Map)['reason'],
            });
          }
          return http.Response(jsonEncode({'state': 'EMPTY'}), 200);
        }
        switch (r.url.path) {
          case '/api/v1/me/permissions':
            // Ô ⁴⁰ của Điều dưỡng chỉ có ở lớp nhiệm vụ nên /me/permissions không có BED_READINESS.
            body = [
              if (!shiftLead && !noDuty)
                for (final f in [
                  if (!doctorOnly) 'ROOM_BED_CONFIGURATION',
                  if (!doctorOnly) 'BED_READINESS',
                  if (!doctorOnly) 'BED_ALLOCATION',
                  'ROOM_ISOLATION',
                ])
                  for (final op in ['VIEW', if (writable) 'CREATE', if (writable) 'UPDATE'])
                    {'function': f, 'operation': op},
            ];
          case '/api/v1/me':
            body = {
              'roles': ['AC-03'],
            };
          case '/api/v1/beds':
            body = {
              'beds': [
                {
                  'id': 3,
                  'code': 'G1',
                  'state': retired
                      ? 'RETIRED'
                      : doctorOnly
                      ? 'OCCUPIED'
                      : 'AWAITING_READY',
                  'location': {'roomId': 2, 'label': 'Phòng 201, giường G1'},
                },
              ],
            };
          case '/api/v1/bed-allocations':
            body = [
              {
                'id': 9,
                'residentId': 7,
                'status': 'ACTIVE',
                'location': {'roomId': 2},
                'flags': ['AWAITING_DOCTOR_CONFIRMATION'],
              },
            ];
          case '/api/v1/bed-readiness/beds':
            body = [
              {
                'id': 3,
                'state': 'AWAITING_READY',
                'readyDeadline': '2026-11-09T01:00:00Z',
                'readinessOverdue': true,
                'location': {'bedId': 3, 'roomId': 2, 'floorId': 1, 'label': 'Phòng 201, giường G1'},
              },
            ];
          case '/api/v1/beds/3/readiness-conditions':
            body = {'allowed': true, 'disinfectionRequired': true, 'departedResidentId': 7};
          case '/api/v1/bed-allocations/9':
            body = {
              'id': 9,
              'residentId': 7,
              'location': {'label': 'Phòng 201, giường G1'},
            };
          case '/api/v1/bed-allocations/suitable-beds':
            body = [
              {
                'location': {
                  'bedId': targetGone ? 8 : 4,
                  'label': targetGone ? 'Phòng 203, giường G3' : 'Phòng 202, giường G2',
                },
              },
            ];
          case '/api/v1/bed-transfer-reasons':
            body = [
              {'id': 5, 'name': 'Theo nhu cầu', 'status': 'ACTIVE'},
            ];
          case '/api/v1/bed-allocations/prescribing-doctors':
            body = [
              {'id': 6, 'fullName': '[Bác sĩ kiểm thử]'},
            ];
          case '/api/v1/rooms/2':
            body = {'id': 2, 'name': 'Phòng 201', 'isolated': false};
          case '/api/v1/room-isolation/rooms':
            body = [
              {'id': 2, 'name': 'Phòng 201', 'isolated': false},
            ];
          case '/api/v1/room-isolation/pending-stays':
            body = [
              {
                'id': 9,
                'residentId': 7,
                'status': 'ACTIVE',
                'location': {'bedId': 3, 'roomId': 2},
                'flags': ['AWAITING_DOCTOR_CONFIRMATION'],
              },
            ];
          case '/api/v1/rooms/2/isolation-history':
            body = history;
          default:
            throw StateError('Yêu cầu ngoài fixture: ${r.method} ${r.url}');
        }
        return http.Response(jsonEncode(body), 200, headers: {'content-type': 'application/json; charset=utf-8'});
      }),
    );
  });

  Future<void> open(WidgetTester t, Widget child, {bool dark = false, double scale = 1}) async {
    await t.pumpWidget(
      MaterialApp(
        theme: dark ? AppTheme.dark() : AppTheme.light(),
        builder: (context, child) => MediaQuery(
          data: MediaQuery.of(context).copyWith(textScaler: TextScaler.linear(scale)),
          child: child!,
        ),
        home: child,
      ),
    );
    await t.pumpAndSettle();
  }

  testWidgets('FR_045_batBuocKhuKhuanVaBaoHoTruocKhiXacNhan', (t) async {
    await open(t, BedReadinessScreen(api: api, bedId: 3));
    expect(commands, isEmpty);
    expect(t.widget<FilledButton>(find.widgetWithText(FilledButton, 'Xác nhận sẵn sàng')).onPressed, isNull);
    await t.tap(find.text('Đã khử khuẩn'));
    await t.tap(find.text('Đã dùng bảo hộ'));
    await t.pump();
    await t.tap(find.text('Xác nhận sẵn sàng'));
    await t.pumpAndSettle();
    expect(commands.single.url.path, '/api/v1/beds/3/confirm-readiness');
    expect(jsonDecode(commands.single.body), containsPair('disinfected', true));
    expect(jsonDecode(commands.single.body), containsPair('ppeUsed', true));
    expect(commands.single.headers['Idempotency-Key'], isNotEmpty);
  });

  testWidgets('FR_044_khongCoQuyenThiKhongCoNutXacNhan', (t) async {
    writable = false;
    await open(t, BedReadinessScreen(api: api, bedId: 3));
    expect(find.widgetWithText(FilledButton, 'Xác nhận sẵn sàng'), findsNothing);
    expect(commands, isEmpty);
  });

  Future<void> selectTransfer(WidgetTester t) async {
    await t.tap(find.widgetWithText(DropdownButtonFormField<int>, 'Giường mới'));
    await t.pumpAndSettle();
    await t.tap(find.text('Phòng 202, giường G2').last);
    await t.pumpAndSettle();
    await t.tap(find.widgetWithText(DropdownButtonFormField<int>, 'Lý do chuyển'));
    await t.pumpAndSettle();
    await t.tap(find.text('Theo nhu cầu').last);
    await t.pumpAndSettle();
    await t.enterText(find.widgetWithText(TextField, 'Lý do (bắt buộc)'), 'Theo đề nghị');
    await t.pump();
  }

  testWidgets('FR_028_tuChonGiuongVaLyDoGuiDungHopDongKhongChonSan', (t) async {
    await open(t, BedTransferScreen(api: api, allocationId: 9));
    expect(commands, isEmpty);
    expect(t.widget<FilledButton>(find.widgetWithText(FilledButton, 'Chuyển giường')).onPressed, isNull);
    await selectTransfer(t);
    await t.tap(find.widgetWithText(FilledButton, 'Chuyển giường'));
    await t.pumpAndSettle();
    expect(commands.single.url.path, '/api/v1/bed-allocations/9/transfer');
    expect(jsonDecode(commands.single.body), {'newBedId': 4, 'transferReasonId': 5, 'reason': 'Theo đề nghị'});
  });

  testWidgets('FR_020_biChanGiuLyDoVaHienDieuKienChuaThoa', (t) async {
    blocked = true;
    await open(t, BedTransferScreen(api: api, allocationId: 9));
    await selectTransfer(t);
    await t.tap(find.widgetWithText(FilledButton, 'Chuyển giường'));
    await t.pumpAndSettle();
    expect(find.textContaining('Giường đích không còn trống'), findsOneWidget);
    expect(find.text('Theo đề nghị'), findsOneWidget);
  });

  testWidgets('FR_008_datCachLyCoLyDoVaKhongTuGuiLenh', (t) async {
    await open(t, RoomIsolationScreen(api: api, roomId: 2));
    expect(commands, isEmpty);
    await t.enterText(find.widgetWithText(TextField, 'Lý do (bắt buộc)'), 'Theo dõi lây nhiễm');
    await t.pump();
    await t.tap(find.widgetWithText(FilledButton, 'Đặt cách ly'));
    await t.pumpAndSettle();
    expect(commands.single.url.path, '/api/v1/rooms/2/isolate');
    expect(jsonDecode(commands.single.body), {'reason': 'Theo dõi lây nhiễm'});
    expect(find.text('Đang cách ly'), findsOneWidget);
    expect(find.text('Không cách ly'), findsNothing);
  });

  testWidgets('FR_008_lichSuGhiNguoiThoiDiemLyDoVaTaiLaiSauLenh', (t) async {
    history.add({
      'action': 'LIFT',
      'occurredAt': '2026-11-01T03:00:00Z',
      'actor': '[Quản lý gỡ]',
      'reason': 'Hết theo dõi',
    });
    await open(t, RoomIsolationScreen(api: api, roomId: 2));
    expect(find.textContaining('[Quản lý gỡ]'), findsOneWidget);
    await t.enterText(find.widgetWithText(TextField, 'Lý do (bắt buộc)'), 'Nghi nhiễm cúm');
    await t.pump();
    await t.tap(find.widgetWithText(FilledButton, 'Đặt cách ly'));
    await t.pumpAndSettle();
    expect(find.textContaining('[Bác sĩ đặt]'), findsOneWidget);
    expect(find.descendant(of: find.byType(ListTile), matching: find.textContaining('Nghi nhiễm cúm')), findsOneWidget);
  });

  testWidgets('FR_044_cachLyNgoaiTuyenBaoLoiKhongGiaThanhCong', (t) async {
    offline = true;
    await open(t, RoomIsolationScreen(api: api, roomId: 2));
    await t.enterText(find.widgetWithText(TextField, 'Lý do (bắt buộc)'), 'Theo dõi lây nhiễm');
    await t.pump();
    await t.tap(find.widgetWithText(FilledButton, 'Đặt cách ly'));
    await t.pumpAndSettle();
    expect(find.textContaining('Kiểm tra mạng'), findsOneWidget);
    expect(find.text('Không cách ly'), findsOneWidget);
    expect(find.text('Đã ghi nhận.'), findsNothing);
  });

  testWidgets('FR_044_ngoaiTuyenBaoLoiKhongGiaThanhCong', (t) async {
    offline = true;
    await open(t, BedReadinessScreen(api: api, bedId: 3));
    await t.tap(find.text('Đã khử khuẩn'));
    await t.tap(find.text('Đã dùng bảo hộ'));
    await t.pump();
    await t.tap(find.text('Xác nhận sẵn sàng'));
    await t.pumpAndSettle();
    expect(find.textContaining('Kiểm tra mạng'), findsOneWidget);
    expect(find.text('Đã xác nhận giường sẵn sàng.'), findsNothing);
  });

  testWidgets('BR_M03_21_chuyenKiemSoatGuiBacSiDaChonTuDanhSach', (t) async {
    await open(t, BedTransferScreen(api: api, allocationId: 9, infectionControl: true));
    await t.tap(find.widgetWithText(DropdownButtonFormField<int>, 'Giường mới'));
    await t.pumpAndSettle();
    await t.tap(find.text('Phòng 202, giường G2').last);
    await t.pumpAndSettle();
    await t.tap(find.widgetWithText(DropdownButtonFormField<int>, 'Bác sĩ chỉ định'));
    await t.pumpAndSettle();
    await t.tap(find.text('[Bác sĩ kiểm thử]').last);
    await t.pumpAndSettle();
    await t.enterText(find.widgetWithText(TextField, 'Lý do (bắt buộc)'), 'Kiểm soát lây nhiễm');
    await t.pump();
    await t.tap(find.widgetWithText(FilledButton, 'Chuyển theo chỉ định'));
    await t.pumpAndSettle();
    expect(commands.single.url.path, '/api/v1/bed-allocations/9/infection-control-transfer');
    expect(jsonDecode(commands.single.body), {
      'newBedId': 4,
      'prescribingDoctorId': 6,
      'reason': 'Kiểm soát lây nhiễm',
    });
  });

  testWidgets('FR_024a_bacSiXacNhanOLaiCoLyDo', (t) async {
    await open(t, RoomIsolationScreen(api: api, roomId: 2, allocationId: 9));
    await t.enterText(find.widgetWithText(TextField, 'Lý do (bắt buộc)'), 'Tiếp tục theo dõi tại phòng');
    await t.pump();
    await t.tap(find.widgetWithText(FilledButton, 'Xác nhận ở lại giường'));
    await t.pumpAndSettle();
    expect(commands.single.url.path, '/api/v1/bed-allocations/9/confirm-isolated-stay');
  });

  testWidgets('FR_060_bangGiuongMoManXacNhanKhongTuGuiLenh', (t) async {
    await open(t, BedsScreen(api: api));
    await t.tap(find.text('Phòng 201, giường G1'));
    await t.pumpAndSettle();
    await t.tap(find.text('Xác nhận sẵn sàng'));
    await t.pumpAndSettle();
    expect(find.text('Đã khử khuẩn'), findsOneWidget);
    expect(commands, isEmpty);
  });
  testWidgets('FR_044_dieuDuongPhuTrachCaThayGiuongChoSanSangCuaTangVaMoXacNhan', (t) async {
    shiftLead = true;
    await open(t, BedsScreen(api: api));
    expect(find.text('Phòng 201, giường G1'), findsOneWidget);
    expect(find.text('Chờ sẵn sàng, quá hạn'), findsOneWidget);
    await t.tap(find.text('Phòng 201, giường G1'));
    await t.pumpAndSettle();
    expect(find.text('Đã khử khuẩn'), findsOneWidget);
    expect(commands, isEmpty);
  });

  testWidgets('FR_044_khongCoNhiemVuThiKhongLoGiuong', (t) async {
    noDuty = true;
    await open(t, BedsScreen(api: api));
    expect(find.text('Phòng 201, giường G1'), findsNothing);
    expect(find.text('Chưa có giường trong phạm vi được xem.'), findsOneWidget);
  });

  testWidgets('FR_008_bangGiuongLayMaPhongTuLocationDungHopDong', (t) async {
    await open(t, BedsScreen(api: api));
    await t.tap(find.text('Phòng 201, giường G1'));
    await t.pumpAndSettle();
    expect(find.text('Cách ly phòng'), findsOneWidget);
  });

  testWidgets('FR_011_giuongThanhLyHienKhongSuDung', (t) async {
    retired = true;
    await open(t, BedsScreen(api: api));
    expect(find.text('Không sử dụng'), findsOneWidget);
  });

  testWidgets('FR_008_bacSiKhongCanQuyenCauHinhVanDatCachLy', (t) async {
    doctorOnly = true;
    await open(t, RoomIsolationScreen(api: api, roomId: 2));
    expect(find.widgetWithText(TextField, 'Lý do (bắt buộc)'), findsOneWidget);
    expect(find.text('Không có quyền'), findsNothing);
  });

  testWidgets('FR_024a_bacSiMoXacNhanOLaiKhongDocDanhSachPhanBo', (t) async {
    doctorOnly = true;
    await open(t, BedsScreen(api: api));
    expect(find.text('Xác nhận ở lại hồ sơ 7'), findsOneWidget);
    await t.tap(find.text('Xác nhận ở lại hồ sơ 7'));
    await t.pumpAndSettle();
    expect(find.widgetWithText(TextField, 'Lý do (bắt buộc)'), findsOneWidget);
  });

  testWidgets('FR_022_taiLaiBoGiuongDaKhongConTrongDanhSach', (t) async {
    await open(t, BedTransferScreen(api: api, allocationId: 9));
    await selectTransfer(t);
    targetGone = true;
    await t.ensureVisible(find.text('Tải lại giường phù hợp'));
    await t.tap(find.text('Tải lại giường phù hợp'));
    await t.pumpAndSettle();
    expect(t.takeException(), isNull);
    expect(t.widget<FilledButton>(find.widgetWithText(FilledButton, 'Chuyển giường')).onPressed, isNull);
    expect(commands, isEmpty);
  });

  testWidgets('FR_044_manHepCheDoToiChu200PhanTramKhongTran', (t) async {
    t.view.physicalSize = const Size(360, 800);
    t.view.devicePixelRatio = 1;
    addTearDown(t.view.resetPhysicalSize);
    addTearDown(t.view.resetDevicePixelRatio);
    await open(t, BedReadinessScreen(api: api, bedId: 3), dark: true, scale: 2);
    expect(t.takeException(), isNull);
  });

  testWidgets('FR_008_cachLyManHepCheDoToiChu200PhanTramKhongTran', (t) async {
    history.add({
      'action': 'ISOLATE',
      'occurredAt': '2026-11-01T03:00:00Z',
      'actor': '[Bác sĩ đặt]',
      'reason': 'Ho sốt',
    });
    t.view.physicalSize = const Size(360, 800);
    t.view.devicePixelRatio = 1;
    addTearDown(t.view.resetPhysicalSize);
    addTearDown(t.view.resetDevicePixelRatio);
    await open(t, RoomIsolationScreen(api: api, roomId: 2), dark: true, scale: 2);
    expect(t.takeException(), isNull);
  });
}
