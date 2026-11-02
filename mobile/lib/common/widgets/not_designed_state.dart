import 'package:flutter/material.dart';

/// Trạng thái màn "chưa có" (DESIGN.md 7.3 `NotDesignedState`): thẻ trắng, "[tên]: chưa có màn", nói rõ chức năng
/// chưa hoạt động. Không đặt chữ trực tiếp trên nền trời.
class NotDesignedState extends StatelessWidget {
  const NotDesignedState({super.key, required this.title, required this.description});

  final String title;
  final String description;

  @override
  Widget build(BuildContext context) {
    final text = Theme.of(context).textTheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(title, style: text.titleMedium),
            const SizedBox(height: 8),
            Text(description, style: text.bodyMedium),
          ],
        ),
      ),
    );
  }
}
