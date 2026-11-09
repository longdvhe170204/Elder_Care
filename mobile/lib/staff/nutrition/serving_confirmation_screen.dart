import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';
import '../../common/offline/offline_queue.dart';

/// Loại bản ghi ngoại tuyến của xác nhận phục vụ (`OfflineHandler.recordType` của 011).
const servingRecordType = 'M08.SERVING_CONFIRMATION';

/// Xác nhận phục vụ suất đặc biệt trên app nhân viên (011 T032, T033; US6, FR-049 → FR-051, BR-M08-14). Chọn bữa hôm nay
/// đã chốt, chọn suất theo nhãn trên phiếu của tầng/khu mình, chọn người đang được phục vụ rồi xác nhận. Backend kiểm đúng
/// người, dị ứng và hạn chế hiện hành tại lúc phục vụ: sai người hoặc trái hạn chế thì báo lý do, không tạo sự cố; suất có
/// thành phần gây dị ứng thì yêu cầu khai đã ăn hay chưa ăn (đã ăn kèm mức độ, triệu chứng để ghi sự cố). Mất mạng thì lưu
/// vào hàng đợi ngoại tuyến (Q-01); kiểm tra lại xung đột khi đồng bộ, thời điểm thiết bị lưu riêng. Không chọn sẵn giá trị nào.
class ServingConfirmationScreen extends StatefulWidget {
  const ServingConfirmationScreen({super.key, required this.api, required this.queue, required this.accountId});

  final ApiClient api;
  final OfflineQueue queue;
  final int accountId;

  @override
  State<ServingConfirmationScreen> createState() => _ServingConfirmationScreenState();
}

class _ServingConfirmationScreenState extends State<ServingConfirmationScreen> {
  List<Map<String, dynamic>>? _meals;
  Map<String, dynamic>? _meal;
  List<Map<String, dynamic>>? _slips;
  Map<String, dynamic>? _label;
  String? _loadError;

  @override
  void initState() {
    super.initState();
    _loadMeals();
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
          _loadError = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = backendMessage(e));
    }
  }

  Future<void> _openMeal(Map<String, dynamic> meal) async {
    setState(() {
      _meal = meal;
      _slips = null;
      _loadError = null;
    });
    try {
      final rows = await widget.api.getList('/meal-slips?dailyMealId=${meal['dailyMealId']}');
      if (mounted) setState(() => _slips = rows.cast<Map<String, dynamic>>());
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = backendMessage(e));
    }
  }

  @override
  Widget build(BuildContext context) {
    final label = _label;
    if (label != null) {
      return _ServingForm(
        key: ValueKey(label['id']),
        api: widget.api,
        queue: widget.queue,
        accountId: widget.accountId,
        label: label,
        onBack: () => setState(() => _label = null),
      );
    }
    final text = Theme.of(context).textTheme;
    final meal = _meal;
    final slips = _slips;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (meal == null) ...[
          Text('Chọn bữa cần xác nhận phục vụ', style: text.titleMedium),
          const SizedBox(height: 12),
          if (_loadError != null) ...[
            Text(_loadError!, style: text.bodyMedium),
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
              }),
              child: const Text('Chọn bữa khác'),
            ),
          ),
          Text('Suất đặc biệt bữa ${meal['mealName']}', style: text.titleMedium),
          const SizedBox(height: 12),
          if (_loadError != null)
            Text(_loadError!, style: text.bodyMedium)
          else if (slips == null)
            const Center(child: CircularProgressIndicator())
          else if (slips.every((s) => ((s['labels'] as List?) ?? const []).isEmpty))
            Text('Phiếu của tầng/khu bạn không có suất đặc biệt.', style: text.bodyMedium)
          else
            for (final slip in slips)
              for (final l in ((slip['labels'] as List?) ?? const []).cast<Map<String, dynamic>>())
                ListTile(
                  minTileHeight: 64,
                  title: Text('${l['fullName']}'),
                  subtitle: Text('${l['room'] ?? ''} · ${l['diet']} · ${l['dishes']}'.trim()),
                  onTap: () => setState(() => _label = l),
                ),
        ],
      ],
    );
  }
}

class _ServingForm extends StatefulWidget {
  const _ServingForm({
    super.key,
    required this.api,
    required this.queue,
    required this.accountId,
    required this.label,
    required this.onBack,
  });

  final ApiClient api;
  final OfflineQueue queue;
  final int accountId;
  final Map<String, dynamic> label;
  final VoidCallback onBack;

  @override
  State<_ServingForm> createState() => _ServingFormState();
}

class _ServingFormState extends State<_ServingForm> {
  final _search = TextEditingController();
  final _severity = TextEditingController();
  final _symptoms = TextEditingController();
  final _treatment = TextEditingController();
  final _keys = <String, String>{};
  List<Map<String, dynamic>>? _residents;
  Map<String, dynamic>? _served;
  bool _allergen = false;
  bool? _hasEaten;
  bool _busy = false;
  String? _error, _done, _loadError;

  @override
  void initState() {
    super.initState();
    _find();
  }

  @override
  void dispose() {
    _search.dispose();
    _severity.dispose();
    _symptoms.dispose();
    _treatment.dispose();
    super.dispose();
  }

  String _uuid() {
    final r = Random.secure(), b = List<int>.generate(16, (_) => r.nextInt(256));
    b[6] = (b[6] & 15) | 64;
    b[8] = (b[8] & 63) | 128;
    final h = b.map((v) => v.toRadixString(16).padLeft(2, '0')).join();
    return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
  }

  Future<void> _find() async {
    final q = _search.text.trim();
    try {
      final page = await widget.api.get(q.isEmpty ? '/residents' : '/residents?q=${Uri.encodeQueryComponent(q)}');
      final items = ((page?['items'] as List?) ?? const []).cast<Map<String, dynamic>>();
      if (mounted) {
        setState(() {
          _residents = items;
          _loadError = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = backendMessage(e));
    }
  }

  Map<String, dynamic> _body() => {
    'residentId': _served!['id'],
    if (_allergen && _hasEaten != null) 'hasEaten': _hasEaten,
    if (_allergen && _hasEaten == true)
      'incident': {
        'severity': _severity.text.trim(),
        'symptoms': _symptoms.text.trim(),
        if (_treatment.text.trim().isNotEmpty) 'treatment': _treatment.text.trim(),
      },
  };

  Future<void> _submit() async {
    if (_busy) return;
    if (_served == null) {
      setState(() => _error = 'Chọn người đang được phục vụ.');
      return;
    }
    if (_allergen) {
      if (_hasEaten == null) {
        setState(() => _error = 'Chọn người đó đã ăn hay chưa ăn.');
        return;
      }
      if (_hasEaten == true && (_severity.text.trim().isEmpty || _symptoms.text.trim().isEmpty)) {
        setState(() => _error = 'Khai đã ăn cần nhập mức độ và triệu chứng.');
        return;
      }
    }
    final body = _body();
    final signature = body.toString();
    final key = _keys.putIfAbsent(signature, _uuid);
    setState(() {
      _busy = true;
      _error = null;
      _done = null;
    });
    final id = widget.label['id'];
    try {
      await widget.api.post('/special-portions/$id/confirm-serving', body, headers: {'Idempotency-Key': key});
      _keys.remove(signature);
      if (mounted) {
        setState(
          () => _done = _allergen
              ? 'Đã ghi nhận. Cảnh báo đã gửi tới điều dưỡng phụ trách.'
              : 'Đã xác nhận phục vụ đúng suất.',
        );
      }
    } on ApiProblem catch (e) {
      if (mounted) {
        setState(() {
          _error = backendMessage(e);
          if (e.errorCode == 'PORTION_HAS_ALLERGEN') _allergen = true;
        });
      }
    } on ApiUnreachable {
      await _saveOffline(body);
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  /// Mất mạng (Q-01): lưu để gửi khi có mạng; xung đột dị ứng được kiểm lại khi đồng bộ nên chưa khai đã ăn/chưa ăn ở đây.
  Future<void> _saveOffline(Map<String, dynamic> body) async {
    await widget.queue.enqueue(
      accountId: widget.accountId,
      recordType: servingRecordType,
      content: {'specialPortionId': widget.label['id'], ...body},
    );
    if (mounted) {
      setState(() => _done = 'Đã lưu để gửi khi có mạng. Hệ thống kiểm tra lại dị ứng, hạn chế khi đồng bộ.');
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final text = theme.textTheme;
    final residents = _residents;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(onPressed: widget.onBack, child: const Text('Chọn suất khác')),
          ),
          Text('Suất của ${widget.label['fullName']}', style: text.titleLarge),
          const SizedBox(height: 4),
          Text('${widget.label['diet']} · ${widget.label['dishes']}', style: text.bodyMedium),
          const SizedBox(height: 4),
          Text(
            'Chọn đúng người đang được phục vụ. Người có dị ứng loại khác trong hồ sơ cần được hỏi lại trước khi phục vụ.',
            style: text.bodySmall,
          ),
          const SizedBox(height: 16),
          Text('Người đang được phục vụ', style: text.titleSmall),
          const SizedBox(height: 8),
          TextField(
            controller: _search,
            textInputAction: TextInputAction.search,
            onSubmitted: (_) => _find(),
            decoration: const InputDecoration(labelText: 'Tìm theo họ tên hoặc mã hồ sơ'),
          ),
          const SizedBox(height: 8),
          if (_loadError != null) ...[
            Text(_loadError!, style: text.bodyMedium),
            OutlinedButton(onPressed: _find, child: const Text('Tải lại')),
          ] else if (residents == null)
            const Center(child: CircularProgressIndicator())
          else
            for (final r in residents)
              ListTile(
                minTileHeight: 56,
                selected: identical(r, _served) || (_served != null && r['id'] == _served!['id']),
                title: Text((r['personalInfo'] as Map)['fullName'] as String),
                subtitle: Text('${r['profileCode']}'),
                onTap: _busy ? null : () => setState(() => _served = r),
              ),
          if (_allergen) ...[
            const SizedBox(height: 16),
            Text('Suất có thành phần gây dị ứng với người này', style: text.titleSmall),
            const SizedBox(height: 8),
            RadioGroup<bool>(
              groupValue: _hasEaten,
              onChanged: (v) => setState(() => _hasEaten = v),
              child: const Column(
                children: [
                  RadioListTile<bool>(value: false, title: Text('Chưa ăn')),
                  RadioListTile<bool>(value: true, title: Text('Đã ăn')),
                ],
              ),
            ),
            if (_hasEaten == true) ...[
              TextField(
                controller: _severity,
                decoration: const InputDecoration(labelText: 'Mức độ'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _symptoms,
                decoration: const InputDecoration(labelText: 'Triệu chứng'),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _treatment,
                decoration: const InputDecoration(labelText: 'Xử trí ban đầu'),
              ),
            ],
          ],
          if (_error != null)
            Semantics(
              liveRegion: true,
              child: Padding(
                padding: const EdgeInsets.only(top: 16),
                child: Text(_error!, style: text.bodyMedium?.copyWith(color: theme.colorScheme.error)),
              ),
            ),
          if (_done != null)
            Semantics(
              liveRegion: true,
              child: Padding(
                padding: const EdgeInsets.only(top: 16),
                child: Text(_done!, style: text.bodyMedium),
              ),
            ),
          const SizedBox(height: 16),
          FilledButton(onPressed: _busy ? null : _submit, child: Text(_allergen ? 'Ghi nhận' : 'Xác nhận phục vụ')),
        ],
      ),
    );
  }
}
