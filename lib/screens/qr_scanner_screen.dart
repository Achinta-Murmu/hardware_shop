import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

import '../database/app_database.dart';

class QrScannerScreen extends StatefulWidget {
  final AppDatabase database;

  const QrScannerScreen({super.key, required this.database});

  @override
  State<QrScannerScreen> createState() => _QrScannerScreenState();
}

class _QrScannerScreenState extends State<QrScannerScreen> {
  bool _isProcessing = false;
  String _message = 'Point the camera at a product QR code';

  Future<void> _handleDetection(BarcodeCapture capture) async {
    if (_isProcessing) return;

    final String? value = capture.barcodes.firstOrNull?.rawValue;
    if (value == null || value.isEmpty) return;

    const prefix = 'hardware_shop:product:';

    if (!value.startsWith(prefix)) {
      setState(() {
        _message = 'Invalid QR code. Scan a hardware shop product label.';
      });
      return;
    }

    final int? productId = int.tryParse(value.substring(prefix.length));

    if (productId == null) {
      setState(() {
        _message = 'Invalid product ID.';
      });
      return;
    }

    _isProcessing = true;

    try {
      final products = await widget.database.getAllProducts();

      Product? product;
      for (final item in products) {
        if (item.id == productId) {
          product = item;
          break;
        }
      }

      if (!mounted) return;

      if (product == null) {
        setState(() {
          _message = 'Product not found. It may have been deleted.';
        });
        _isProcessing = false;
        return;
      }

      // Return the scanned product to the previous screen.
      Navigator.pop(context, product);
    } catch (error) {
      if (!mounted) return;

      setState(() {
        _message = 'Could not load product. Please try again.';
      });
      _isProcessing = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Scan Product QR')),
      body: Column(
        children: [
          Expanded(child: MobileScanner(onDetect: _handleDetection)),
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              children: [
                const Icon(Icons.qr_code_scanner, size: 36),
                const SizedBox(height: 8),
                Text(_message, textAlign: TextAlign.center),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
