import 'package:flutter/material.dart';

import '../database/app_database.dart';
import 'qr_scanner_screen.dart';
import 'sales_history_screen.dart';

class SalesScreen extends StatefulWidget {
  const SalesScreen({super.key});

  @override
  State<SalesScreen> createState() => _SalesScreenState();
}

class _SalesScreenState extends State<SalesScreen> {
  late final AppDatabase database;

  // Products in the current bill, keyed by product ID.
  final Map<int, Product> billProducts = {};
  final Map<int, int> quantities = {};

  bool isScanning = false;
  bool isPickingProduct = false;

  @override
  void initState() {
    super.initState();
    database = AppDatabase();
  }

  @override
  void dispose() {
    database.close();
    super.dispose();
  }

  double get totalAmount {
    double total = 0;

    for (final entry in billProducts.entries) {
      final product = entry.value;
      final quantity = quantities[entry.key] ?? 0;
      total += product.sellingPrice * quantity;
    }

    return total;
  }

  int get totalUnits {
    return quantities.values.fold(0, (sum, quantity) => sum + quantity);
  }

  // Add a scanned or manually selected product to the bill.
  void addProductToBill(Product product) {
    final currentQuantity = quantities[product.id] ?? 0;
    final availableStock = product.stockQuantity;

    if (currentQuantity >= availableStock) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Only $availableStock ${product.unit} available in stock '
            'for ${product.name}.',
          ),
        ),
      );
      return;
    }

    setState(() {
      billProducts[product.id] = product;
      quantities[product.id] = currentQuantity + 1;
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('${product.name} added to bill'),
        duration: const Duration(seconds: 1),
      ),
    );
  }

  // Scan a product QR code.
  Future<void> scanProduct() async {
    if (isScanning) return;

    setState(() => isScanning = true);

    try {
      final Product? product = await Navigator.push<Product>(
        context,
        MaterialPageRoute(builder: (_) => QrScannerScreen(database: database)),
      );

      if (!mounted) return;

      if (product != null) {
        addProductToBill(product);
      }
    } catch (error) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Could not open the QR scanner.')),
      );
    } finally {
      if (mounted) {
        setState(() => isScanning = false);
      }
    }
  }

  // Search existing products and add one without needing a QR label.
  Future<void> addProductManually() async {
    if (isPickingProduct) return;

    setState(() => isPickingProduct = true);

    try {
      final products = await database.getAllProducts();

      if (!mounted) return;

      final Product? product = await showDialog<Product>(
        context: context,
        builder: (_) => _ManualProductPicker(products: products),
      );

      if (!mounted) return;

      if (product != null) {
        addProductToBill(product);
      }
    } catch (error) {
      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Could not load products. Please try again.'),
        ),
      );
    } finally {
      if (mounted) {
        setState(() => isPickingProduct = false);
      }
    }
  }

  // Increase or decrease the quantity of a product.
  void changeQuantity(int productId, int change) {
    final product = billProducts[productId];

    if (product == null) return;

    final currentQuantity = quantities[productId] ?? 0;
    final newQuantity = currentQuantity + change;
    final availableStock = product.stockQuantity;

    if (newQuantity > availableStock) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Only $availableStock ${product.unit} available in stock '
            'for ${product.name}.',
          ),
        ),
      );
      return;
    }

    setState(() {
      if (newQuantity <= 0) {
        quantities.remove(productId);
        billProducts.remove(productId);
      } else {
        quantities[productId] = newQuantity;
      }
    });
  }

  void clearBill() {
    setState(() {
      billProducts.clear();
      quantities.clear();
    });
  }

  void showSaleStatus() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text(
          'This is a bill preview. Saving the sale and deducting '
          'stock have not been connected yet.',
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final items = billProducts.values.toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'New Sale',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),

        actions: [
          IconButton(
            tooltip: 'Sales History',
            icon: const Icon(Icons.history),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => SalesHistoryScreen(database: database),
                ),
              );
            },
          ),
          if (items.isNotEmpty)
            IconButton(
              tooltip: 'Clear bill',
              onPressed: clearBill,
              icon: const Icon(Icons.delete_outline),
            ),
        ],
      ),
      body: items.isEmpty ? _buildEmptyBill() : _buildBill(items),
      floatingActionButton: items.isEmpty
          ? null
          : FloatingActionButton(
              onPressed: isScanning ? null : scanProduct,
              tooltip: 'Scan another product',
              child: const Icon(Icons.qr_code_scanner),
            ),
    );
  }

  Widget _buildEmptyBill() {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.point_of_sale, size: 80),
            const SizedBox(height: 16),
            const Text(
              'Start a New Bill',
              style: TextStyle(fontSize: 23, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              'Scan a product QR code or find a product manually.',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: 240,
              child: FilledButton.icon(
                onPressed: isScanning ? null : scanProduct,
                icon: const Icon(Icons.qr_code_scanner),
                label: const Text('Scan QR'),
              ),
            ),
            const SizedBox(height: 12),
            SizedBox(
              width: 240,
              child: OutlinedButton.icon(
                onPressed: isPickingProduct ? null : addProductManually,
                icon: const Icon(Icons.search),
                label: const Text('Add Manually'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBill(List<Product> items) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              const Icon(Icons.shopping_cart_outlined),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  '${items.length} product type(s) · $totalUnits units',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
              ),
              Wrap(
                spacing: 8,
                children: [
                  IconButton(
                    tooltip: 'Scan QR',
                    onPressed: isScanning ? null : scanProduct,
                    icon: const Icon(Icons.qr_code_scanner),
                  ),
                  IconButton(
                    tooltip: 'Add manually',
                    onPressed: isPickingProduct ? null : addProductManually,
                    icon: const Icon(Icons.add),
                  ),
                ],
              ),
            ],
          ),
        ),
        Expanded(
          child: ListView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            itemCount: items.length,
            itemBuilder: (context, index) {
              final product = items[index];
              final quantity = quantities[product.id] ?? 0;
              final lineTotal = product.sellingPrice * quantity;

              return Card(
                margin: const EdgeInsets.only(bottom: 10),
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Icon(Icons.inventory_2_outlined),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  product.name,
                                  style: const TextStyle(
                                    fontSize: 17,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  '₹${product.sellingPrice.toStringAsFixed(2)} '
                                  'per ${product.unit}',
                                ),
                                if (product.productCode != null &&
                                    product.productCode!.isNotEmpty)
                                  Text(
                                    'Code: ${product.productCode}',
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodySmall,
                                  ),
                              ],
                            ),
                          ),
                          IconButton(
                            tooltip: 'Remove product',
                            onPressed: () =>
                                changeQuantity(product.id, -quantity),
                            icon: const Icon(Icons.close),
                          ),
                        ],
                      ),
                      const Divider(),
                      Row(
                        children: [
                          IconButton(
                            tooltip: 'Decrease quantity',
                            onPressed: () => changeQuantity(product.id, -1),
                            icon: const Icon(Icons.remove_circle_outline),
                          ),
                          Text(
                            '$quantity ${product.unit}',
                            style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                            ),
                          ),
                          IconButton(
                            tooltip: 'Increase quantity',
                            onPressed: () => changeQuantity(product.id, 1),
                            icon: const Icon(Icons.add_circle_outline),
                          ),
                          const Spacer(),
                          Text(
                            '₹${lineTotal.toStringAsFixed(2)}',
                            style: const TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
        SafeArea(
          top: false,
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceContainer,
              border: Border(
                top: BorderSide(color: Theme.of(context).dividerColor),
              ),
            ),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Total',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      '₹${totalAmount.toStringAsFixed(2)}',
                      style: const TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: FilledButton.icon(
                    onPressed: showSaleStatus,
                    icon: const Icon(Icons.receipt_long),
                    label: const Text('Confirm Sale'),
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

// Searchable product picker for manual billing.
class _ManualProductPicker extends StatefulWidget {
  final List<Product> products;

  const _ManualProductPicker({required this.products});

  @override
  State<_ManualProductPicker> createState() => _ManualProductPickerState();
}

class _ManualProductPickerState extends State<_ManualProductPicker> {
  final TextEditingController searchController = TextEditingController();

  String searchText = '';

  @override
  void dispose() {
    searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final query = searchText.trim().toLowerCase();

    final filteredProducts = widget.products.where((product) {
      return product.name.toLowerCase().contains(query) ||
          (product.productCode ?? '').toLowerCase().contains(query) ||
          (product.category ?? '').toLowerCase().contains(query) ||
          (product.brand ?? '').toLowerCase().contains(query);
    }).toList();

    return AlertDialog(
      title: const Text('Add Product Manually'),
      content: SizedBox(
        width: 450,
        height: 420,
        child: Column(
          children: [
            TextField(
              controller: searchController,
              autofocus: true,
              decoration: const InputDecoration(
                hintText: 'Search name, code, category...',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
              ),
              onChanged: (value) {
                setState(() => searchText = value);
              },
            ),
            const SizedBox(height: 12),
            Expanded(
              child: filteredProducts.isEmpty
                  ? const Center(child: Text('No matching products found'))
                  : ListView.builder(
                      itemCount: filteredProducts.length,
                      itemBuilder: (context, index) {
                        final product = filteredProducts[index];

                        return ListTile(
                          leading: const Icon(Icons.inventory_2_outlined),
                          title: Text(product.name),
                          subtitle: Text(
                            '₹${product.sellingPrice.toStringAsFixed(2)} '
                            'per ${product.unit}'
                            '${product.productCode == null || product.productCode!.isEmpty ? '' : '\nCode: ${product.productCode}'}',
                          ),
                          isThreeLine:
                              product.productCode != null &&
                              product.productCode!.isNotEmpty,
                          onTap: () {
                            Navigator.pop(context, product);
                          },
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
      ],
    );
  }
}
