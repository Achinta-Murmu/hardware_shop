import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';

import '../database/app_database.dart';

class ProductQrLabelScreen extends StatelessWidget {
  final Product product;

  const ProductQrLabelScreen({super.key, required this.product});

  String get qrData => 'hardware_shop:product:${product.id}';

  Future<void> printLabel() async {
    final document = pw.Document();

    // Compact 50 x 30 mm thermal label.
    final pageFormat = PdfPageFormat(
      50 * PdfPageFormat.mm,
      30 * PdfPageFormat.mm,
      marginAll: 2 * PdfPageFormat.mm,
    );

    document.addPage(
      pw.Page(
        pageFormat: pageFormat,
        build: (context) {
          return pw.Row(
            crossAxisAlignment: pw.CrossAxisAlignment.center,
            children: [
              pw.BarcodeWidget(
                barcode: pw.Barcode.qrCode(),
                data: qrData,
                width: 23 * PdfPageFormat.mm,
                height: 23 * PdfPageFormat.mm,
              ),
              pw.SizedBox(width: 2 * PdfPageFormat.mm),
              pw.Expanded(
                child: pw.Column(
                  mainAxisAlignment: pw.MainAxisAlignment.center,
                  crossAxisAlignment: pw.CrossAxisAlignment.start,
                  children: [
                    pw.Text(
                      product.name,
                      maxLines: 3,
                      style: pw.TextStyle(
                        fontSize: 8,
                        fontWeight: pw.FontWeight.bold,
                      ),
                    ),
                    pw.SizedBox(height: 2),
                    pw.Text(
                      'ID: ${product.id}',
                      style: const pw.TextStyle(fontSize: 6),
                    ),
                    if (product.productCode != null &&
                        product.productCode!.isNotEmpty)
                      pw.Text(
                        product.productCode!,
                        maxLines: 1,
                        style: const pw.TextStyle(fontSize: 6),
                      ),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );

    await Printing.layoutPdf(
      name: 'product_${product.id}_qr_label.pdf',
      onLayout: (_) async => document.save(),
      format: pageFormat,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Product QR Label')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      const Text(
                        'HARDWARE SHOP',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          letterSpacing: 1,
                        ),
                      ),
                      const SizedBox(height: 12),
                      QrImageView(
                        data: qrData,
                        version: QrVersions.auto,
                        size: 200,
                        backgroundColor: Colors.white,
                      ),
                      const SizedBox(height: 12),
                      Text(
                        product.name,
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text('Product ID: ${product.id}'),
                      if (product.productCode != null &&
                          product.productCode!.isNotEmpty)
                        Text('Code: ${product.productCode}'),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: printLabel,
                  icon: const Icon(Icons.print),
                  label: const Text('Print QR Sticker'),
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                'Designed for a 50 × 30 mm label. '
                'Actual output depends on printer settings.',
                textAlign: TextAlign.center,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
