import 'package:flutter/material.dart';
import 'package:drift/drift.dart' show Value;
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';

import '../database/app_database.dart';

class SalesHistoryScreen extends StatefulWidget {
  final AppDatabase database;

  const SalesHistoryScreen({super.key, required this.database});

  @override
  State<SalesHistoryScreen> createState() => _SalesHistoryScreenState();
}

class _SalesHistoryScreenState extends State<SalesHistoryScreen> {
  late Future<List<Sale>> salesFuture;

  @override
  void initState() {
    super.initState();
    salesFuture = widget.database.getAllSales();
  }

  void refreshSales() {
    setState(() {
      salesFuture = widget.database.getAllSales();
    });
  }

  String formatDate(DateTime date) {
    final local = date.toLocal();

    return '${local.day.toString().padLeft(2, '0')}/'
        '${local.month.toString().padLeft(2, '0')}/'
        '${local.year} '
        '${local.hour.toString().padLeft(2, '0')}:'
        '${local.minute.toString().padLeft(2, '0')}';
  }

  Future<void> openSaleDetails(Sale sale) async {
    final itemsFuture = widget.database.getSaleItems(sale.id);

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return AlertDialog(
          title: Text('Sale #${sale.id}'),
          content: SizedBox(
            width: 480,
            height: 380,
            child: FutureBuilder<List<SaleItem>>(
              future: itemsFuture,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }

                if (snapshot.hasError) {
                  return const Center(
                    child: Text('Could not load sale items.'),
                  );
                }

                final items = snapshot.data ?? [];

                if (items.isEmpty) {
                  return const Center(
                    child: Text('No items found for this sale.'),
                  );
                }

                return Column(
                  children: [
                    Text(
                      formatDate(sale.saleDate),
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    const Divider(),
                    Expanded(
                      child: ListView.separated(
                        itemCount: items.length,
                        separatorBuilder: (_, __) => const Divider(height: 1),
                        itemBuilder: (context, index) {
                          final item = items[index];

                          return ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: Text(item.productName),
                            subtitle: Text(
                              '${item.quantity} ${item.unit} × '
                              '₹${item.sellingPrice.toStringAsFixed(2)}',
                            ),
                            trailing: Text(
                              '₹${item.lineTotal.toStringAsFixed(2)}',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          );
                        },
                      ),
                    ),
                    const Divider(),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Total',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          '₹${sale.totalAmount.toStringAsFixed(2)}',
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ],
                );
              },
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext),
              child: const Text('Close'),
            ),
            FutureBuilder<List<SaleItem>>(
              future: itemsFuture,
              builder: (context, snapshot) {
                return FilledButton.icon(
                  onPressed: snapshot.hasData && snapshot.data!.isNotEmpty
                      ? () async {
                          Navigator.pop(dialogContext);

                          await printReceipt(sale, snapshot.data!);
                        }
                      : null,
                  icon: const Icon(Icons.print),
                  label: const Text('Print Receipt'),
                );
              },
            ),
          ],
        );
      },
    );
  }

  Future<void> printReceipt(Sale sale, List<SaleItem> items) async {
    try {
      final document = pw.Document();

      // A narrow receipt page sized for an 80 mm thermal printer.
      // Page height grows with the number of products.
      final pageHeight = (95 + items.length * 15) * PdfPageFormat.mm;

      document.addPage(
        pw.Page(
          pageFormat: PdfPageFormat(
            80 * PdfPageFormat.mm,
            pageHeight,
            marginAll: 5 * PdfPageFormat.mm,
          ),
          build: (context) {
            return pw.Column(
              crossAxisAlignment: pw.CrossAxisAlignment.start,
              children: [
                pw.Center(
                  child: pw.Column(
                    children: [
                      pw.Text(
                        'HARDWARE SHOP',
                        style: pw.TextStyle(
                          fontSize: 16,
                          fontWeight: pw.FontWeight.bold,
                        ),
                      ),
                      pw.SizedBox(height: 4),
                      pw.Text(
                        'Sales Receipt',
                        style: const pw.TextStyle(fontSize: 10),
                      ),
                    ],
                  ),
                ),
                pw.SizedBox(height: 12),
                pw.Divider(),
                pw.Text('Receipt No: ${sale.id}'),
                pw.Text('Date: ${formatDate(sale.saleDate)}'),
                pw.Divider(),
                pw.SizedBox(height: 5),
                pw.Row(
                  children: [
                    pw.Expanded(
                      flex: 5,
                      child: pw.Text(
                        'Item',
                        style: pw.TextStyle(fontWeight: pw.FontWeight.bold),
                      ),
                    ),
                    pw.Expanded(
                      flex: 2,
                      child: pw.Text(
                        'Qty',
                        textAlign: pw.TextAlign.right,
                        style: pw.TextStyle(fontWeight: pw.FontWeight.bold),
                      ),
                    ),
                    pw.Expanded(
                      flex: 3,
                      child: pw.Text(
                        'Amount',
                        textAlign: pw.TextAlign.right,
                        style: pw.TextStyle(fontWeight: pw.FontWeight.bold),
                      ),
                    ),
                  ],
                ),
                pw.SizedBox(height: 6),
                ...items.map((item) {
                  return pw.Padding(
                    padding: const pw.EdgeInsets.symmetric(vertical: 4),
                    child: pw.Row(
                      crossAxisAlignment: pw.CrossAxisAlignment.start,
                      children: [
                        pw.Expanded(
                          flex: 5,
                          child: pw.Text(
                            item.productName,
                            style: const pw.TextStyle(fontSize: 9),
                          ),
                        ),
                        pw.Expanded(
                          flex: 2,
                          child: pw.Text(
                            '${item.quantity} ${item.unit}',
                            textAlign: pw.TextAlign.right,
                            style: const pw.TextStyle(fontSize: 9),
                          ),
                        ),
                        pw.Expanded(
                          flex: 3,
                          child: pw.Text(
                            item.lineTotal.toStringAsFixed(2),
                            textAlign: pw.TextAlign.right,
                            style: const pw.TextStyle(fontSize: 9),
                          ),
                        ),
                      ],
                    ),
                  );
                }),
                pw.Divider(),
                pw.SizedBox(height: 5),
                pw.Row(
                  mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                  children: [
                    pw.Text(
                      'TOTAL',
                      style: pw.TextStyle(
                        fontSize: 13,
                        fontWeight: pw.FontWeight.bold,
                      ),
                    ),
                    pw.Text(
                      'Rs. ${sale.totalAmount.toStringAsFixed(2)}',
                      style: pw.TextStyle(
                        fontSize: 13,
                        fontWeight: pw.FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                pw.SizedBox(height: 18),
                pw.Center(
                  child: pw.Text(
                    'Thank you for shopping!',
                    style: const pw.TextStyle(fontSize: 10),
                  ),
                ),
              ],
            );
          },
        ),
      );

      await Printing.layoutPdf(
        onLayout: (_) async => document.save(),
        name: 'receipt_${sale.id}.pdf',
      );
    } catch (error) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Could not print receipt. Please try again.'),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Sales History',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        actions: [
          IconButton(
            tooltip: 'Refresh',
            onPressed: refreshSales,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: FutureBuilder<List<Sale>>(
        future: salesFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          if (snapshot.hasError) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, size: 50),
                    const SizedBox(height: 12),
                    const Text('Could not load sales history.'),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: refreshSales,
                      child: const Text('Try Again'),
                    ),
                  ],
                ),
              ),
            );
          }

          final sales = snapshot.data ?? [];

          if (sales.isEmpty) {
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.receipt_long_outlined, size: 80),
                  SizedBox(height: 16),
                  Text(
                    'No completed sales yet',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 8),
                  Text('Your completed bills will appear here.'),
                ],
              ),
            );
          }

          final totalRevenue = sales.fold<double>(
            0,
            (sum, sale) => sum + sale.totalAmount,
          );

          return Column(
            children: [
              Card(
                margin: const EdgeInsets.all(12),
                child: ListTile(
                  leading: const Icon(Icons.currency_rupee),
                  title: const Text('Recorded Sales Total'),
                  subtitle: Text('${sales.length} completed bill(s)'),
                  trailing: Text(
                    '₹${totalRevenue.toStringAsFixed(2)}',
                    style: const TextStyle(
                      fontSize: 17,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),
              Expanded(
                child: ListView.separated(
                  itemCount: sales.length,
                  separatorBuilder: (_, __) => const Divider(height: 1),
                  itemBuilder: (context, index) {
                    final sale = sales[index];

                    return ListTile(
                      leading: const CircleAvatar(
                        child: Icon(Icons.receipt_long),
                      ),
                      title: Text('Sale #${sale.id}'),
                      subtitle: Text(formatDate(sale.saleDate)),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Text(
                            '₹${sale.totalAmount.toStringAsFixed(2)}',
                            style: const TextStyle(fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(width: 4),
                          const Icon(Icons.chevron_right),
                        ],
                      ),
                      onTap: () => openSaleDetails(sale),
                    );
                  },
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
