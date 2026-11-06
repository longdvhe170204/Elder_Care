import 'package:image_picker/image_picker.dart';

/// Chụp một ảnh, trả (nội dung, tên tệp); `null` khi người dùng bỏ chụp. Test truyền bản giả.
typedef PhotoPicker = Future<(List<int>, String)?> Function();

/// Máy ảnh của thiết bị qua `image_picker` (G3, A duyệt 2026-11-05); ảnh thu nhỏ để dưới giới hạn 10 MB của `POST /files`.
Future<(List<int>, String)?> cameraPhoto() async {
  final file = await ImagePicker().pickImage(source: ImageSource.camera, maxWidth: 1920, imageQuality: 85);
  if (file == null) return null;
  return (await file.readAsBytes(), file.name);
}
