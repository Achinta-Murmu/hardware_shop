import 'package:flutter/material.dart';

import '../database/app_database.dart';
import 'add_purchase_screen.dart';

class PurchasesScreen extends StatefulWidget {
  const PurchasesScreen({super.key});

  @override
  State<PurchasesScreen> createState() => _PurchasesScreenState();
}

class _PurchasesScreenState extends State<PurchasesScreen> {
  final AppDatabase database = AppDatabase();

  List<Purchase> purchases = [];
  Map<int, Product> productsById = {};

  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    loadPurchases();
  }

  @override
  void dispose() {
    database.close();
    super.dispose();
  }

  Future<void> loadPurchases() async {
    final purchaseResult = await database.getAllPurchases();

    final productResult = await database.getAllProducts();

    if (!mounted) return;

    setState(() {
      purchases = purchaseResult;

      productsById = {for (final product in productResult) product.id: product};

      isLoading = false;
    });
  }

  Future<void> addPurchase() async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AddPurchaseScreen(database: database),
      ),
    );

    await loadPurchases();
  }

  String formatDate(DateTime date) {
    final day = date.day.toString().padLeft(2, '0');
    final month = date.month.toString().padLeft(2, '0');
    final year = date.year.toString();

    return '$day/$month/$year';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Purchases',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
      ),

      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : purchases.isEmpty
          ? const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.shopping_cart_outlined, size: 70),
                  SizedBox(height: 12),
                  Text('No purchases yet', style: TextStyle(fontSize: 20)),
                ],
              ),
            )
          : ListView.builder(
              padding: const EdgeInsets.fromLTRB(12, 12, 12, 90),
              itemCount: purchases.length,
              itemBuilder: (context, index) {
                final purchase = purchases[index];

                final product = productsById[purchase.productId];

                final total = purchase.quantity * purchase.purchasePrice;

                return Card(
                  margin: const EdgeInsets.only(bottom: 10),
                  child: Padding(
                    padding: const EdgeInsets.all(14),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        CircleAvatar(child: const Icon(Icons.shopping_cart)),

                        const SizedBox(width: 12),

                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                product?.name ?? 'Unknown Product',
                                style: const TextStyle(
                                  fontSize: 17,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),

                              const SizedBox(height: 5),

                              Text(
                                '${purchase.quantity} '
                                '${product?.unit ?? 'units'} × '
                                '₹${purchase.purchasePrice.toStringAsFixed(2)}',
                                style: TextStyle(color: Colors.grey[700]),
                              ),

                              const SizedBox(height: 4),

                              Text(
                                formatDate(purchase.purchaseDate),
                                style: TextStyle(
                                  color: Colors.grey[600],
                                  fontSize: 13,
                                ),
                              ),

                              if (purchase.supplier != null &&
                                  purchase.supplier!.isNotEmpty)
                                Padding(
                                  padding: const EdgeInsets.only(top: 4),
                                  child: Text(
                                    'Supplier: '
                                    '${purchase.supplier}',
                                    style: TextStyle(
                                      color: Colors.grey[700],
                                      fontSize: 13,
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),

                        const SizedBox(width: 8),

                        Text(
                          '₹${total.toStringAsFixed(2)}',
                          style: const TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),

      floatingActionButton: FloatingActionButton.extended(
        onPressed: addPurchase,
        icon: const Icon(Icons.add_shopping_cart),
        label: const Text('New Purchase'),
      ),
    );
  }
}
