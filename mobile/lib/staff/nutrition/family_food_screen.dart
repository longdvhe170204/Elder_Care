import 'dart:math';

import 'package:flutter/material.dart';

import '../../common/api/api_client.dart';
import '../../common/api/problem_message.dart';

const _statusNames = {
  'NOT_ALLOWED': 'Không sử dụng',
  'NEEDS_CONFIRMATION': 'Cần xác nhận',
  'ALLOWED': 'Được sử dụng',
  'ENDED': 'Đã kết thúc',
};

const _reasonNames = {
  'FORBIDDEN_TYPE': 'Loại đồ ăn bị cấm theo chính sách của viện',
  'EXPIRED': 'Đã hết hạn dùng',
  'ALLERGY': 'Trùng dị ứng của người nhận',
  'PERSONAL_RESTRICTION': 'Trái hạn chế riêng của người nhận',
  'DIET_RESTRICTION': 'Trái hạn chế của chế độ ăn',
  'INGREDIENTS_UNKNOWN': 'Thành phần không rõ',
  'OTHER_INGREDIENT': 'Có thành phần loại khác',
  'RESIDENT_OTHER_ALLERGY': 'Người nhận có dị ứng loại khác',
  'TEXTURE_NOT_REGULAR': 'Kết cấu thức ăn của người nhận khác thường',
  'SENDER_NOT_RELATED': 'Người gửi không có trong danh sách người thân',
  'TREATMENT_DIET': 'Người nhận dùng chế độ ăn liên quan điều trị',
};

const _methodNames = {'RETURNED': 'Trả lại người gửi', 'DISCARDED': 'Hủy bỏ', 'CONSUMED': 'Đã dùng hết'};

/// Đồ ăn gia đình mang vào trên app nhân viên (011 T035; US7, FR-052 → FR-057, BR-M08-04). Chọn người cao tuổi, xem đồ ăn đã
/// ghi nhận, ghi nhận đồ ăn mới (hệ thống đối chiếu và gán trạng thái, nêu lý do), rồi xử lý theo trạng thái: Cần xác nhận
/// thì cho dùng (kèm ghi chú) hoặc không cho dùng (kèm lý do); Không sử dụng thì ghi xử lý (trả lại, hủy bỏ); Được sử dụng
/// thì kết thúc. Không có lệnh nào đưa đồ ăn Không sử dụng sang Được sử dụng. Thao tác trực tuyến, không có hàng đợi ngoại
/// tuyến (hợp đồng 011 không có bản ghi ngoại tuyến cho đồ ăn gia đình).
class FamilyFoodScreen extends StatefulWidget {
  const FamilyFoodScreen({super.key, required this.api});

  final ApiClient api;

  @override
  State<FamilyFoodScreen> createState() => _FamilyFoodScreenState();
}

class _FamilyFoodScreenState extends State<FamilyFoodScreen> {
  final _search = TextEditingController();
  List<Map<String, dynamic>>? _residents;
  Map<String, dynamic>? _resident;
  List<Map<String, dynamic>>? _foods;
  bool _creating = false;
  String? _loadError, _notice;

  @override
  void initState() {
    super.initState();
    _find();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
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

  Future<void> _loadFoods() async {
    try {
      final rows = await widget.api.getList('/residents/${_resident!['id']}/family-foods');
      if (mounted) {
        setState(() {
          _foods = rows.cast<Map<String, dynamic>>();
          _loadError = null;
        });
      }
    } on Exception catch (e) {
      if (mounted) setState(() => _loadError = backendMessage(e));
    }
  }

  void _pick(Map<String, dynamic> resident) {
    setState(() {
      _resident = resident;
      _foods = null;
      _notice = null;
    });
    _loadFoods();
  }

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    final resident = _resident;
    if (resident != null && _creating) {
      return _NewFoodForm(
        api: widget.api,
        resident: resident,
        onBack: () => setState(() => _creating = false),
        onSaved: (result) {
          setState(() {
            _creating = false;
            _notice = 'Đã ghi nhận: ${_statusNames[result['status']] ?? result['status']}.';
          });
          _loadFoods();
        },
      );
    }
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        if (resident == null) ...[
          Text('Chọn người cao tuổi nhận đồ ăn', style: text.titleMedium),
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
          ] else if (_residents == null)
            const Center(child: CircularProgressIndicator())
          else
            for (final r in _residents!)
              ListTile(
                minTileHeight: 56,
                title: Text((r['personalInfo'] as Map)['fullName'] as String),
                subtitle: Text('${r['profileCode']}'),
                onTap: () => _pick(r),
              ),
        ] else ...[
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(onPressed: () => setState(() => _resident = null), child: const Text('Chọn người khác')),
          ),
          Text('Đồ ăn gia đình của ${(resident['personalInfo'] as Map)['fullName']}', style: text.titleMedium),
          const SizedBox(height: 8),
          FilledButton(onPressed: () => setState(() => _creating = true), child: const Text('Ghi nhận đồ ăn mới')),
          if (_notice != null)
            Semantics(
              liveRegion: true,
              child: Padding(
                padding: const EdgeInsets.only(top: 12),
                child: Text(_notice!, style: text.bodyMedium),
              ),
            ),
          const SizedBox(height: 12),
          if (_loadError != null)
            Text(_loadError!, style: text.bodyMedium)
          else if (_foods == null)
            const Center(child: CircularProgressIndicator())
          else if (_foods!.isEmpty)
            Text('Chưa có đồ ăn gia đình nào được ghi nhận.', style: text.bodyMedium)
          else
            for (final food in _foods!)
              _FoodCard(key: ValueKey(food['id']), api: widget.api, food: food, onChanged: _loadFoods),
        ],
      ],
    );
  }
}

/// Một đồ ăn đã ghi nhận: trạng thái, lý do hệ thống và các lệnh hợp lệ với trạng thái đó.
class _FoodCard extends StatefulWidget {
  const _FoodCard({super.key, required this.api, required this.food, required this.onChanged});

  final ApiClient api;
  final Map<String, dynamic> food;
  final Future<void> Function() onChanged;

  @override
  State<_FoodCard> createState() => _FoodCardState();
}

class _FoodCardState extends State<_FoodCard> {
  final _note = TextEditingController();
  final _returnedTo = TextEditingController();
  final _keys = <String, String>{};
  String? _action, _method, _error;
  bool _busy = false;

  @override
  void dispose() {
    _note.dispose();
    _returnedTo.dispose();
    super.dispose();
  }

  List<(String, String)> get _actions => switch (widget.food['status']) {
    'NEEDS_CONFIRMATION' => [('approve-use', 'Cho dùng'), ('reject-use', 'Không cho dùng')],
    'NOT_ALLOWED' => [('record-disposal', 'Ghi xử lý')],
    'ALLOWED' => [('end', 'Kết thúc')],
    _ => const [],
  };

  Map<String, dynamic>? _body() {
    switch (_action) {
      case 'approve-use':
        if (_note.text.trim().isEmpty) return _fail('Nhập ghi chú xác nhận.');
        return {'note': _note.text.trim()};
      case 'reject-use':
        if (_note.text.trim().isEmpty) return _fail('Nhập lý do không cho dùng.');
        return {'reason': _note.text.trim()};
      default:
        if (_method == null) return _fail('Chọn cách xử lý.');
        if (_method == 'RETURNED' && _returnedTo.text.trim().isEmpty) return _fail('Nhập người nhận lại.');
        return {'method': _method, if (_method == 'RETURNED') 'returnedTo': _returnedTo.text.trim()};
    }
  }

  Map<String, dynamic>? _fail(String message) {
    setState(() => _error = message);
    return null;
  }

  Future<void> _submit() async {
    if (_busy) return;
    final body = _body();
    if (body == null) return;
    final signature = '$_action $body';
    final key = _keys.putIfAbsent(signature, _uuid);
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await widget.api.post('/family-foods/${widget.food['id']}/$_action', body, headers: {'Idempotency-Key': key});
      _keys.remove(signature);
      if (mounted) setState(() => _action = null);
      await widget.onChanged();
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final text = theme.textTheme;
    final food = widget.food;
    final reasons = ((food['systemReasons'] as List?) ?? const []).cast<String>();
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${food['type']}', style: text.titleSmall),
            Text('${_statusNames[food['status']] ?? food['status']} · gửi bởi ${food['senderName']}'),
            Text('Hạn dùng: ${food['expiresAt']} · Bảo quản: ${food['storageMethod']}', style: text.bodySmall),
            for (final r in reasons) Text('• ${_reasonNames[r] ?? r}', style: text.bodySmall),
            if (_actions.isNotEmpty) ...[
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                children: [
                  for (final (command, label) in _actions)
                    OutlinedButton(
                      onPressed: _busy
                          ? null
                          : () => setState(() {
                              _action = command;
                              _error = null;
                              _method = null;
                              _note.clear();
                              _returnedTo.clear();
                            }),
                      child: Text(label),
                    ),
                ],
              ),
            ],
            if (_action == 'approve-use' || _action == 'reject-use') ...[
              const SizedBox(height: 8),
              TextField(
                controller: _note,
                decoration: InputDecoration(
                  labelText: _action == 'approve-use' ? 'Ghi chú xác nhận' : 'Lý do không cho dùng',
                ),
              ),
            ] else if (_action != null) ...[
              const SizedBox(height: 8),
              RadioGroup<String>(
                groupValue: _method,
                onChanged: (v) => setState(() => _method = v),
                child: Column(
                  children: [
                    for (final m in _action == 'record-disposal' ? ['RETURNED', 'DISCARDED'] : _methodNames.keys)
                      RadioListTile<String>(value: m, title: Text(_methodNames[m]!)),
                  ],
                ),
              ),
              if (_method == 'RETURNED')
                TextField(
                  controller: _returnedTo,
                  decoration: const InputDecoration(labelText: 'Người nhận lại'),
                ),
            ],
            if (_action != null) ...[
              const SizedBox(height: 8),
              FilledButton(onPressed: _busy ? null : _submit, child: const Text('Xác nhận lệnh')),
            ],
            if (_error != null)
              Semantics(
                liveRegion: true,
                child: Padding(
                  padding: const EdgeInsets.only(top: 8),
                  child: Text(_error!, style: text.bodyMedium?.copyWith(color: theme.colorScheme.error)),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// Biểu mẫu ghi nhận đồ ăn mới (FR-052): thành phần chọn từ danh mục dị nguyên nhóm thực phẩm, hoặc "không rõ".
class _NewFoodForm extends StatefulWidget {
  const _NewFoodForm({required this.api, required this.resident, required this.onBack, required this.onSaved});

  final ApiClient api;
  final Map<String, dynamic> resident;
  final VoidCallback onBack;
  final void Function(Map<String, dynamic> result) onSaved;

  @override
  State<_NewFoodForm> createState() => _NewFoodFormState();
}

class _NewFoodFormState extends State<_NewFoodForm> {
  final _sender = TextEditingController();
  final _type = TextEditingController();
  final _description = TextEditingController();
  final _expires = TextEditingController();
  final _storage = TextEditingController();
  final _keys = <String, String>{};
  final _chosen = <int>{};
  List<Map<String, dynamic>>? _ingredients;
  bool _unknown = false;
  bool _busy = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadIngredients();
  }

  @override
  void dispose() {
    for (final c in [_sender, _type, _description, _expires, _storage]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _loadIngredients() async {
    try {
      final rows = await widget.api.getList('/catalogs/allergens?group=FOOD&status=ACTIVE');
      if (mounted) setState(() => _ingredients = rows.cast<Map<String, dynamic>>());
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    }
  }

  /// "dd/MM/yyyy HH:mm" giờ Việt Nam (UTC+7) → ISO UTC; `null` khi sai định dạng.
  static String? _parseExpires(String input) {
    final m = RegExp(r'^(\d{1,2})/(\d{1,2})/(\d{4})\s+(\d{1,2}):(\d{2})$').firstMatch(input.trim());
    if (m == null) return null;
    final v = [for (var i = 1; i <= 5; i++) int.parse(m.group(i)!)];
    final local = DateTime.utc(v[2], v[1], v[0], v[3], v[4]);
    if (local.day != v[0] || local.month != v[1]) return null;
    return local.subtract(const Duration(hours: 7)).toIso8601String();
  }

  String _uuidKey(String signature) => _keys.putIfAbsent(signature, _uuid);

  Future<void> _submit() async {
    if (_busy) return;
    if (_sender.text.trim().isEmpty ||
        _type.text.trim().isEmpty ||
        _description.text.trim().isEmpty ||
        _storage.text.trim().isEmpty) {
      setState(() => _error = 'Nhập đủ người gửi, loại đồ ăn, mô tả và cách bảo quản.');
      return;
    }
    final expires = _parseExpires(_expires.text);
    if (expires == null) {
      setState(() => _error = 'Hạn dùng theo dạng ngày/tháng/năm giờ:phút, ví dụ 25/12/2026 18:00.');
      return;
    }
    final body = {
      'senderName': _sender.text.trim(),
      'type': _type.text.trim(),
      'description': _description.text.trim(),
      'ingredients': _chosen.toList()..sort(),
      'ingredientsUnknown': _unknown,
      'expiresAt': expires,
      'storageMethod': _storage.text.trim(),
    };
    final key = _uuidKey(body.toString());
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final created = await widget.api.post(
        '/residents/${widget.resident['id']}/family-foods',
        body,
        headers: {'Idempotency-Key': key},
      );
      widget.onSaved(created ?? const {});
    } on Exception catch (e) {
      if (mounted) setState(() => _error = backendMessage(e));
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final text = theme.textTheme;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(onPressed: widget.onBack, child: const Text('Quay lại danh sách')),
          ),
          Text('Ghi nhận đồ ăn mới', style: text.titleLarge),
          const SizedBox(height: 12),
          TextField(
            controller: _sender,
            decoration: const InputDecoration(labelText: 'Người gửi (họ tên, quan hệ)'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _type,
            decoration: const InputDecoration(labelText: 'Loại đồ ăn'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _description,
            decoration: const InputDecoration(labelText: 'Mô tả'),
          ),
          const SizedBox(height: 12),
          Text('Thành phần chính', style: text.titleSmall),
          if (_ingredients == null)
            const Padding(
              padding: EdgeInsets.all(8),
              child: Center(child: CircularProgressIndicator()),
            )
          else
            Wrap(
              spacing: 8,
              children: [
                for (final i in _ingredients!)
                  FilterChip(
                    label: Text('${i['name']}'),
                    selected: _chosen.contains(i['id']),
                    onSelected: (on) => setState(() => on ? _chosen.add(i['id'] as int) : _chosen.remove(i['id'])),
                  ),
              ],
            ),
          SwitchListTile(
            value: _unknown,
            onChanged: (v) => setState(() => _unknown = v),
            title: const Text('Thành phần không rõ'),
          ),
          TextField(
            controller: _expires,
            keyboardType: TextInputType.datetime,
            decoration: const InputDecoration(labelText: 'Hạn dùng (ngày/tháng/năm giờ:phút)'),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _storage,
            decoration: const InputDecoration(labelText: 'Cách bảo quản'),
          ),
          if (_error != null)
            Semantics(
              liveRegion: true,
              child: Padding(
                padding: const EdgeInsets.only(top: 16),
                child: Text(_error!, style: text.bodyMedium?.copyWith(color: theme.colorScheme.error)),
              ),
            ),
          const SizedBox(height: 16),
          FilledButton(onPressed: _busy ? null : _submit, child: const Text('Ghi nhận')),
        ],
      ),
    );
  }
}

String _uuid() {
  final r = Random.secure(), b = List<int>.generate(16, (_) => r.nextInt(256));
  b[6] = (b[6] & 15) | 64;
  b[8] = (b[8] & 63) | 128;
  final h = b.map((v) => v.toRadixString(16).padLeft(2, '0')).join();
  return '${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20)}';
}
