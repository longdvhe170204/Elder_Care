import 'dart:math';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:share_plus/share_plus.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/format/date_time.dart';
import '../../common/widgets/page_body.dart';

/// Mở, lưu, gửi một tệp tải về qua bảng chia sẻ của hệ điều hành (017 T036, T039; share_plus, A duyệt 2026-11-08).
typedef FileSharer = Future<void> Function(Uint8List bytes, String name, String mimeType);

Future<void> shareFile(Uint8List bytes, String name, String mimeType) async {
  await SharePlus.instance.share(
    ShareParams(
      files: [XFile.fromData(bytes, mimeType: mimeType, name: name)],
      fileNameOverrides: [name],
    ),
  );
}

const _mime = {'PDF': 'application/pdf', 'XLSX': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'};

// Cùng chữ với `frontend/src/features/balance` (mobile/README luật 1 cấm import chéo giữa staff, family).
const _typeLabels = {'PAYMENT_NOTICE': 'Phiếu báo phí', 'SETTLEMENT_STATEMENT': 'Bảng quyết toán'};

/// Chứng từ và sao kê trên app người thân (017 T036, T039; FR-026, FR-038; api-017 `GET
/// /family-portal/residents/{residentId}/documents`, `GET /family-portal/documents/{type}/{id}/file`, `POST
/// /family-portal/residents/{residentId}/balance-statement`): phiếu báo phí, phiên bản hiện hành của bảng quyết toán
/// tải đúng file đã lưu; sao kê theo khoảng ngày (không dài hơn CFG-M14-01) ở Excel hoặc PDF. Backend kiểm quyền xem chi
/// phí mỗi lần.
class FamilyDocumentsScreen extends StatefulWidget {
  const FamilyDocumentsScreen({
    super.key,
    required this.api,
    required this.residentId,
    required this.residentName,
    this.share = shareFile,
    this.today,
  });

  final ApiClient api;
  final int residentId;
  final String residentName;
  final FileSharer share;

  /// Ngày hiện tại của máy, đặt được trong test.
  final DateTime? today;

  @override
  State<FamilyDocumentsScreen> createState() => _FamilyDocumentsScreenState();
}

class _FamilyDocumentsScreenState extends State<FamilyDocumentsScreen> {
  List<Map<String, dynamic>>? _documents;
  String? _error;
  String? _message;
  bool _busy = false;
  late DateTime _from;
  late DateTime _to;
  String _format = 'PDF';

  @override
  void initState() {
    super.initState();
    _to = widget.today ?? DateTime.now();
    _from = _to.subtract(const Duration(days: 30));
    _load();
  }

  Future<void> _load() async {
    try {
      final data = await widget.api.getList('/family-portal/residents/${widget.residentId}/documents');
      if (mounted) setState(() => _documents = data.cast<Map<String, dynamic>>());
    } on ApiProblem catch (e) {
      if (!mounted) return;
      setState(
        () => _error = e.status == 403
            ? 'Bạn chưa có quyền xem chi phí của ${widget.residentName}. Người đại diện có thể đề nghị viện cấp quyền.'
            : backendMessage(e),
      );
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() {
      _busy = true;
      _message = null;
    });
    try {
      await action();
    } on Exception catch (e) {
      if (mounted) setState(() => _message = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _download(Map<String, dynamic> document, String format) => _run(() async {
    final bytes = await widget.api.getBytes(
      '/family-portal/documents/${document['type']}/${document['id']}/file?format=$format',
    );
    await widget.share(bytes, '${document['code']}.${format.toLowerCase()}', _mime[format]!);
  });

  Future<void> _statement() => _run(() async {
    final bytes = await widget.api.postBytes(
      '/family-portal/residents/${widget.residentId}/balance-statement',
      {'from': _isoDate(_from), 'to': _isoDate(_to), 'format': _format},
      headers: {'Idempotency-Key': _uuidV4()},
    );
    await widget.share(bytes, 'sao-ke-${_isoDate(_from)}-${_isoDate(_to)}.${_format.toLowerCase()}', _mime[_format]!);
  });

  Future<void> _pick(bool from) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: from ? _from : _to,
      firstDate: DateTime(2020),
      lastDate: widget.today ?? DateTime.now(),
    );
    if (picked != null && mounted) setState(() => from ? _from = picked : _to = picked);
  }

  static String _isoDate(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  static String _display(DateTime d) => formatDate(_isoDate(d));

  static String _uuidV4() {
    final random = Random.secure();
    final bytes = List<int>.generate(16, (_) => random.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final hex = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-'
        '${hex.substring(16, 20)}-${hex.substring(20)}';
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    if (_error != null) return PageBody(child: Text(_error!));
    final documents = _documents;
    if (documents == null) return const PageBody(child: Center(child: CircularProgressIndicator()));
    return PageBody(
      child: ListView(
        children: [
          Text('Phiếu báo phí và bảng quyết toán', style: text.titleMedium),
          if (documents.isEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text('Chưa có phiếu báo phí hay bảng quyết toán nào.', style: text.bodyMedium),
            )
          else
            for (final d in documents)
              Container(
                constraints: const BoxConstraints(minHeight: 56),
                padding: const EdgeInsets.symmetric(vertical: 8),
                decoration: BoxDecoration(
                  border: Border(bottom: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${_typeLabels[d['type']] ?? d['type']} ${d['code']}', style: text.bodyLarge),
                          Text(
                            '${formatDate(d['date'] as String)}${d['period'] == null ? '' : ', kỳ ${d['period']}'}',
                            style: text.bodySmall,
                          ),
                        ],
                      ),
                    ),
                    for (final format in (d['formats'] as List<dynamic>).cast<String>())
                      TextButton(
                        onPressed: _busy ? null : () => _download(d, format),
                        child: Text('Tải $format', semanticsLabel: 'Tải ${d['code']} dạng $format'),
                      ),
                  ],
                ),
              ),
          const SizedBox(height: 24),
          Text('Sao kê số dư', style: text.titleMedium),
          Text('Khoảng ngày không dài hơn giới hạn của viện (tối đa 12 tháng).', style: text.bodySmall),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(onPressed: () => _pick(true), child: Text('Từ ${_display(_from)}')),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: OutlinedButton(onPressed: () => _pick(false), child: Text('Đến ${_display(_to)}')),
              ),
            ],
          ),
          const SizedBox(height: 8),
          SegmentedButton<String>(
            segments: const [
              ButtonSegment(value: 'PDF', label: Text('PDF')),
              ButtonSegment(value: 'XLSX', label: Text('Excel')),
            ],
            selected: {_format},
            onSelectionChanged: (value) => setState(() => _format = value.first),
          ),
          const SizedBox(height: 8),
          FilledButton(onPressed: _busy ? null : _statement, child: const Text('Tải sao kê')),
          if (_message != null)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(_message!, style: text.bodyMedium),
            ),
        ],
      ),
    );
  }
}
