import 'package:image_picker/image_picker.dart';

/// Một giấy tờ đã chụp hoặc chọn, chờ tải lên (`POST /files`: PDF, JPEG, PNG, tối đa 10 MB do backend kiểm).
class PickedDocument {
  PickedDocument(this.name, this.bytes, this.contentType);

  final String name;
  final List<int> bytes;
  final String contentType;
}

/// Nguồn giấy tờ của lệnh trạng thái (001 T082): chụp bằng camera hoặc chọn ảnh có sẵn. Tách ra để test widget dùng bản giả.
abstract class DocumentPicker {
  Future<PickedDocument?> capture();

  Future<PickedDocument?> pickFromGallery();
}

/// Bản dùng thật, qua gói `image_picker` (G3 đã duyệt).
class ImageDocumentPicker implements DocumentPicker {
  ImageDocumentPicker({ImagePicker? picker}) : _picker = picker ?? ImagePicker();

  final ImagePicker _picker;

  @override
  Future<PickedDocument?> capture() => _pick(ImageSource.camera);

  @override
  Future<PickedDocument?> pickFromGallery() => _pick(ImageSource.gallery);

  Future<PickedDocument?> _pick(ImageSource source) async {
    final file = await _picker.pickImage(source: source, maxWidth: 2400, imageQuality: 85);
    if (file == null) return null;
    final name = file.name;
    final lower = name.toLowerCase();
    final type = file.mimeType ?? (lower.endsWith('.png') ? 'image/png' : 'image/jpeg');
    return PickedDocument(name, await file.readAsBytes(), type);
  }
}
