import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';

/// Phiếu bữa ăn của tầng/khu trên app nhân viên (011 T031; FR-047, FR-048, FR-048b, BR-M08-11): chọn bữa hôm nay đã chốt,
/// xem phiếu trong phạm vi, nhận phiếu đã giao hoặc báo sai lệch (kể cả phần bổ sung). Chỉ trực tuyến: lệnh nhận phiếu
/// cần trạng thái phiếu hiện tại của bếp. Nút theo trạng thái phiếu; backend kiểm lại quyền và phạm vi tầng/khu.
class MealSlipScreen extends StatefulWidget {
  const MealSlipScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<MealSlipScreen> createState() => _MealSlipScreenState();
}

const _slipStatus = {
  'CLOSED': 'Đã chốt',
  'PREPARED': 'Đã chuẩn bị',
  'DELIVERED': 'Đã giao',
  'HAS_DISCREPANCY': 'Có sai lệch',
  'RECEIVED': 'Đã nhận',
};

class _MealSlipScreenState extends State<MealSlipScreen> {
  List<Map<String, dynamic>>? _meals;
  Map<String, dynamic>? _meal;
  List<Map<String, dynamic>>? _slips;
  String? _error;
  String? _notice;
  // (loại, mã) của đối tượng đang mở ô nhập nội dung sai lệch
  (String, int)? _reporting;
  final _content = TextEditingController();
  bool _busy = false;
  final _keys = <String, String>{};

  @override
  void initState() {
    super.initState();
    _loadMeals();
  }

  @override
  void dispose() {
    _content.dispose();
    super.dispose();
  }

  String _today() {
    final d = DateTime.now().toUtc().add(const Duration(hours: 7));
    String two(int v) => v.toString().padLeft(2, '0');
    return '${d.year}-${two(d.month)}-${two(d.day)}';
  }

  Future<void> _loadMeals() async {
    try {
      final rows = await widget.api.getList('/daily-meals?date=${_today()}');
      if (mounted) {
        setState(() {
          _meals = rows.cast<Map<String, dynamic>>();
          _error = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Future<void> _loadSlips() async {
    final meal = _meal;
    if (meal == null) return;
    try {
      final rows = await widget.api.getList('/meal-slips?dailyMealId=${meal['dailyMealId']}');
      if (mounted) setState(() => _slips = rows.cast<Map<String, dynamic>>());
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  Future<void> _openMeal(Map<String, dynamic> meal) async {
    setState(() {
      _meal = meal;
      _slips = null;
      _error = null;
      _notice = null;
    });
    await _loadSlips();
  }

  Future<void> _send(String path, Map<String, dynamic> body, String done) async {
    final signature = '$path $body';
    final key = _keys.putIfAbsent(signature, _uuid);
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.post(path, body, headers: {'Idempotency-Key': key});
      _keys.remove(signature);
      if (mounted) {
        setState(() {
          _notice = done;
          _reporting = null;
          _content.clear();
        });
      }
      await _loadSlips();
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _report(String path) async {
    if (_content.text.trim().isEmpty) {
      setState(() => _error = 'Nhập nội dung sai lệch.');
      return;
    }
    await _send(path, {'content': _content.text.trim()}, 'Đã báo sai lệch.');
  }

  Widget _reportForm(String path) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: _content,
          minLines: 2,
          maxLines: 4,
          decoration: const InputDecoration(labelText: 'Nội dung sai lệch'),
        ),
        const SizedBox(height: 8),
        Row(
          mainAxisAlignment: MainAxisAlignment.end,
          children: [
            TextButton(onPressed: () => setState(() => _reporting = null), child: const Text('Thôi')),
            FilledButton(onPressed: _busy ? null : () => _report(path), child: const Text('Gửi báo sai lệch')),
          ],
        ),
      ],
    );
  }

  Widget _supplement(Map<String, dynamic> s) {
    final id = s['id'] as int;
    final delivered = s['deliveredAt'] != null;
    final received = s['receivedAt'] != null;
    final text = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.only(top: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Phần bổ sung ${s['portionCount']} suất: ${received ? 'đã nhận' : (delivered ? 'đã giao' : 'chưa giao')}',
            style: text.bodyMedium,
          ),
          if (delivered && !received)
            if (_reporting == ('supplement', id))
              _reportForm('/supplementary-portions/$id/report-discrepancy')
            else
              Wrap(
                spacing: 8,
                children: [
                  FilledButton(
                    onPressed: _busy
                        ? null
                        : () => _send('/supplementary-portions/$id/receive', {}, 'Đã nhận phần bổ sung.'),
                    child: const Text('Nhận phần bổ sung'),
                  ),
                  OutlinedButton(
                    onPressed: () => setState(() {
                      _reporting = ('supplement', id);
                      _content.clear();
                    }),
                    child: const Text('Báo sai lệch phần bổ sung'),
                  ),
                ],
              ),
        ],
      ),
    );
  }

  Widget _slip(Map<String, dynamic> slip) {
    final text = Theme.of(context).textTheme;
    final id = slip['id'] as int;
    final status = slip['status'] as String;
    final portions = ((slip['portionsByDiet'] as List?) ?? const []).cast<Map<String, dynamic>>();
    final labels = (slip['labels'] as List?) ?? const [];
    final supplements = ((slip['supplementaryPortions'] as List?) ?? const []).cast<Map<String, dynamic>>();
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${slip['floorArea']}: ${_slipStatus[status] ?? status}', style: text.titleMedium),
            const SizedBox(height: 4),
            for (final p in portions) Text('${p['diet']}: ${p['portionCount']} suất', style: text.bodyMedium),
            if (labels.isNotEmpty) Text('Suất đặc biệt: ${labels.length}', style: text.bodyMedium),
            if (status == 'DELIVERED') ...[
              const SizedBox(height: 8),
              if (_reporting == ('slip', id))
                _reportForm('/meal-slips/$id/report-discrepancy')
              else
                Wrap(
                  spacing: 8,
                  children: [
                    FilledButton(
                      onPressed: _busy ? null : () => _send('/meal-slips/$id/receive', {}, 'Đã nhận phiếu.'),
                      child: const Text('Nhận phiếu'),
                    ),
                    OutlinedButton(
                      onPressed: () => setState(() {
                        _reporting = ('slip', id);
                        _content.clear();
                      }),
                      child: const Text('Báo sai lệch'),
                    ),
                  ],
                ),
            ],
            for (final s in supplements) _supplement(s),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final meal = _meal;
    final slips = _slips;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (meal == null) ...[
          Text('Chọn bữa cần nhận phiếu', style: text.titleMedium),
          const SizedBox(height: 12),
          if (_error != null) ...[
            Text(_error!, style: text.bodyMedium),
            const SizedBox(height: 8),
            OutlinedButton(onPressed: _loadMeals, child: const Text('Tải lại')),
          ] else if (_meals == null)
            const Center(child: CircularProgressIndicator())
          else if (_meals!.isEmpty)
            Text('Hôm nay chưa có bữa nào.', style: text.bodyMedium)
          else
            for (final m in _meals!)
              ListTile(
                minTileHeight: 64,
                title: Text('${m['mealName']}'),
                subtitle: Text(m['closed'] == true ? 'Đã chốt suất' : 'Chưa chốt suất'),
                enabled: m['closed'] == true,
                onTap: () => _openMeal(m),
              ),
        ] else ...[
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(
              onPressed: () => setState(() {
                _meal = null;
                _slips = null;
                _notice = null;
              }),
              child: const Text('Chọn bữa khác'),
            ),
          ),
          Text('Phiếu bữa ${meal['mealName']}', style: text.titleMedium),
          const SizedBox(height: 12),
          if (_notice != null) Semantics(liveRegion: true, child: Text(_notice!, style: text.bodyMedium)),
          if (_error != null) Semantics(liveRegion: true, child: Text(_error!, style: text.bodyMedium)),
          if (slips == null && _error == null)
            const Center(child: CircularProgressIndicator())
          else if (slips != null && slips.isEmpty)
            Text('Không có phiếu nào của bữa này trong phạm vi của bạn.', style: text.bodyMedium)
          else if (slips != null)
            for (final s in slips) _slip(s),
        ],
      ],
    );
  }
}

String _uuid() {
  final r = Random.secure(), b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 0x0f) | 0x40;
  b[8] = (b[8] & 0x3f) | 0x80;
  String h(int v) => v.toRadixString(16).padLeft(2, '0');
  final s = b.map(h).join();
  return '${s.substring(0, 8)}-${s.substring(8, 12)}-${s.substring(12, 16)}-${s.substring(16, 20)}-${s.substring(20)}';
}
