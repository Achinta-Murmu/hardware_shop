import 'package:flutter/material.dart';

import '../database/app_database.dart';
import 'add_product_screen.dart';
import 'product_qr_label_screen.dart';

class ProductsScreen extends StatefulWidget {
  const ProductsScreen({super.key});

  @override
  State<ProductsScreen> createState() => _ProductsScreenState();
}

class _ProductsScreenState extends State<ProductsScreen> {
  final AppDatabase database = AppDatabase();

  final TextEditingController searchController = TextEditingController();

  List<Product> allProducts = [];
  bool isLoading = true;

  String searchText = '';

  @override
  void initState() {
    super.initState();
    loadProducts();
  }

  @override
  void dispose() {
    searchController.dispose();
    database.close();
    super.dispose();
  }

  Future<void> loadProducts() async {
    final products = await database.getAllProducts();

    if (!mounted) return;

    setState(() {
      allProducts = products;
      isLoading = false;
    });
  }

  Future<void> addProduct() async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => AddProductScreen(database: database),
      ),
    );

    await loadProducts();
  }

  Future<void> editProduct(Product product) async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) =>
            AddProductScreen(database: database, product: product),
      ),
    );

    await loadProducts();
  }

  Future<void> showProductQr(Product product) async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ProductQrLabelScreen(product: product),
      ),
    );
  }

  Future<void> deleteProduct(Product product) async {
    final shouldDelete = await showDialog<bool>(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Delete Product?'),
          content: Text('Are you sure you want to delete "${product.name}"?'),
          actions: [
            TextButton(
              onPressed: () {
                Navigator.pop(context, false);
              },
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () {
                Navigator.pop(context, true);
              },
              child: const Text('Delete'),
            ),
          ],
        );
      },
    );

    if (shouldDelete == true) {
      await database.deleteProduct(product.id);

      await loadProducts();

      if (!mounted) return;

      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('Product deleted')));
    }
  }

  bool isLowStock(Product product) {
    return product.stockQuantity <= product.minimumStock;
  }

  List<Product> get filteredProducts {
    if (searchText.isEmpty) {
      return allProducts;
    }

    return allProducts.where((product) {
      final name = product.name.toLowerCase();
      final category = product.category?.toLowerCase() ?? '';
      final brand = product.brand?.toLowerCase() ?? '';
      final code = product.productCode?.toLowerCase() ?? '';

      return name.contains(searchText) ||
          category.contains(searchText) ||
          brand.contains(searchText) ||
          code.contains(searchText);
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final products = filteredProducts;

    double totalStockValue = 0;

    for (final product in allProducts) {
      totalStockValue += product.purchasePrice * product.stockQuantity;
    }

    final lowStockCount = allProducts.where(isLowStock).length;

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Stock',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
      ),

      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                // SEARCH
                Padding(
                  padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                  child: TextField(
                    controller: searchController,
                    onChanged: (value) {
                      setState(() {
                        searchText = value.toLowerCase();
                      });
                    },
                    decoration: InputDecoration(
                      hintText: 'Search products...',
                      prefixIcon: const Icon(Icons.search),
                      suffixIcon: searchText.isNotEmpty
                          ? IconButton(
                              onPressed: () {
                                searchController.clear();

                                setState(() {
                                  searchText = '';
                                });
                              },
                              icon: const Icon(Icons.clear),
                            )
                          : null,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                  ),
                ),

                // STOCK VALUE
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 8,
                  ),
                  child: Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.account_balance_wallet_outlined,
                            size: 32,
                          ),
                          const SizedBox(width: 14),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Total Stock Value',
                                style: TextStyle(color: Colors.grey.shade600),
                              ),
                              const SizedBox(height: 4),
                              Text(
                                '₹${totalStockValue.toStringAsFixed(2)}',
                                style: const TextStyle(
                                  fontSize: 22,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ),

                // COUNTS
                Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 4,
                  ),
                  child: Row(
                    children: [
                      Text(
                        '${products.length} product${products.length == 1 ? '' : 's'}',
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                      const Spacer(),
                      Text(
                        '$lowStockCount low stock',
                        style: TextStyle(
                          color: lowStockCount > 0
                              ? Colors.orange.shade800
                              : Colors.grey,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 4),

                // PRODUCT LIST
                Expanded(
                  child: products.isEmpty
                      ? Center(
                          child: Column(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              const Icon(Icons.search_off, size: 70),
                              const SizedBox(height: 12),
                              Text(
                                allProducts.isEmpty
                                    ? 'No products yet'
                                    : 'No matching products',
                                style: const TextStyle(fontSize: 20),
                              ),
                            ],
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.fromLTRB(12, 4, 12, 90),
                          itemCount: products.length,
                          itemBuilder: (context, index) {
                            final product = products[index];
                            final lowStock = isLowStock(product);

                            return Card(
                              margin: const EdgeInsets.only(bottom: 10),
                              child: Padding(
                                padding: const EdgeInsets.all(12),
                                child: Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    CircleAvatar(
                                      radius: 25,
                                      child: Text(
                                        product.name.isNotEmpty
                                            ? product.name[0].toUpperCase()
                                            : '?',
                                      ),
                                    ),

                                    const SizedBox(width: 12),

                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            product.name,
                                            style: const TextStyle(
                                              fontSize: 17,
                                              fontWeight: FontWeight.bold,
                                            ),
                                          ),

                                          if (product.brand != null &&
                                              product.brand!.isNotEmpty)
                                            Text(
                                              product.brand!,
                                              style: TextStyle(
                                                color: Colors.grey.shade600,
                                              ),
                                            ),

                                          if (product.category != null &&
                                              product.category!.isNotEmpty)
                                            Text(
                                              product.category!,
                                              style: TextStyle(
                                                color: Colors.grey.shade600,
                                              ),
                                            ),

                                          const SizedBox(height: 8),

                                          Row(
                                            children: [
                                              Icon(
                                                Icons.inventory_2_outlined,
                                                size: 18,
                                                color: lowStock
                                                    ? Colors.orange
                                                    : null,
                                              ),
                                              const SizedBox(width: 5),
                                              Text(
                                                '${product.stockQuantity} ${product.unit}',
                                                style: TextStyle(
                                                  fontWeight: FontWeight.bold,
                                                  color: lowStock
                                                      ? Colors.orange
                                                      : null,
                                                ),
                                              ),
                                            ],
                                          ),

                                          const SizedBox(height: 5),

                                          Text(
                                            'Buy: ₹${product.purchasePrice.toStringAsFixed(2)}   '
                                            'Sell: ₹${product.sellingPrice.toStringAsFixed(2)}',
                                          ),

                                          if (lowStock) ...[
                                            const SizedBox(height: 6),
                                            Text(
                                              'LOW STOCK • Minimum ${product.minimumStock}',
                                              style: TextStyle(
                                                color: Colors.orange.shade800,
                                                fontSize: 12,
                                                fontWeight: FontWeight.bold,
                                              ),
                                            ),
                                          ],
                                        ],
                                      ),
                                    ),

                                    PopupMenuButton<String>(
                                      onSelected: (value) {
                                        if (value == 'qr') {
                                          showProductQr(product);
                                        } else if (value == 'edit') {
                                          editProduct(product);
                                        } else if (value == 'delete') {
                                          deleteProduct(product);
                                        }
                                      },
                                      itemBuilder: (context) {
                                        return const [
                                          PopupMenuItem<String>(
                                            value: 'qr',
                                            child: Row(
                                              children: [
                                                Icon(Icons.qr_code_2),
                                                SizedBox(width: 8),
                                                Text('QR / Print Label'),
                                              ],
                                            ),
                                          ),
                                          PopupMenuItem<String>(
                                            value: 'edit',
                                            child: Row(
                                              children: [
                                                Icon(Icons.edit_outlined),
                                                SizedBox(width: 8),
                                                Text('Edit'),
                                              ],
                                            ),
                                          ),
                                          PopupMenuItem<String>(
                                            value: 'delete',
                                            child: Row(
                                              children: [
                                                Icon(Icons.delete_outline),
                                                SizedBox(width: 8),
                                                Text('Delete'),
                                              ],
                                            ),
                                          ),
                                        ];
                                      },
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                ),
              ],
            ),

      floatingActionButton: FloatingActionButton.extended(
        onPressed: addProduct,
        icon: const Icon(Icons.add),
        label: const Text('Add Product'),
      ),
    );
  }
}
