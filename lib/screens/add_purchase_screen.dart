import 'package:drift/drift.dart';
import 'package:flutter/material.dart';

import '../database/app_database.dart';
import 'add_product_screen.dart';

class AddPurchaseScreen extends StatefulWidget {
  final AppDatabase database;

  const AddPurchaseScreen({super.key, required this.database});

  @override
  State<AddPurchaseScreen> createState() => _AddPurchaseScreenState();
}

class _AddPurchaseScreenState extends State<AddPurchaseScreen> {
  final _formKey = GlobalKey<FormState>();

  List<Product> products = [];
  Product? selectedProduct;

  final quantityController = TextEditingController();
  final priceController = TextEditingController();
  final supplierController = TextEditingController();

  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    loadProducts();
  }

  @override
  void dispose() {
    quantityController.dispose();
    priceController.dispose();
    supplierController.dispose();

    super.dispose();
  }

  Future<void> loadProducts() async {
    final result = await widget.database.getAllProducts();

    if (!mounted) return;

    setState(() {
      products = result;
      isLoading = false;
    });
  }

  Future<void> addNewProduct() async {
    final newProductId = await Navigator.push<int>(
      context,
      MaterialPageRoute(
        builder: (context) =>
            AddProductScreen(database: widget.database, fromPurchase: true),
      ),
    );

    if (newProductId == null) {
      return;
    }

    await loadProducts();

    if (!mounted) return;

    Product? newProduct;

    for (final product in products) {
      if (product.id == newProductId) {
        newProduct = product;
        break;
      }
    }

    if (newProduct != null) {
      setState(() {
        selectedProduct = newProduct;

        priceController.text = newProduct!.purchasePrice.toString();
      });
    }
  }

  void selectProduct(Product? product) {
    setState(() {
      selectedProduct = product;

      if (product != null) {
        priceController.text = product.purchasePrice.toString();
      }
    });
  }

  double get totalAmount {
    final quantity = int.tryParse(quantityController.text) ?? 0;

    final price = double.tryParse(priceController.text) ?? 0;

    return quantity * price;
  }

  Future<void> savePurchase() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    if (selectedProduct == null) {
      return;
    }

    final quantity = int.parse(quantityController.text.trim());

    final price = double.parse(priceController.text.trim());

    final supplier = supplierController.text.trim().isEmpty
        ? null
        : supplierController.text.trim();

    // Add purchase record
    await widget.database.addPurchase(
      PurchasesCompanion.insert(
        productId: selectedProduct!.id,
        quantity: quantity,
        purchasePrice: price,
        supplier: Value(supplier),
      ),
    );

    // Increase product stock
    final updatedProduct = selectedProduct!.copyWith(
      stockQuantity: selectedProduct!.stockQuantity + quantity,
      purchasePrice: price,
    );

    await widget.database.updateProduct(updatedProduct);

    if (!mounted) return;

    Navigator.pop(context, true);
  }

  String? quantityValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Required';
    }

    final quantity = int.tryParse(value);

    if (quantity == null || quantity <= 0) {
      return 'Enter a valid quantity';
    }

    return null;
  }

  String? priceValidator(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Required';
    }

    final price = double.tryParse(value);

    if (price == null || price <= 0) {
      return 'Enter a valid price';
    }

    return null;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'New Purchase',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : products.isEmpty
          ? const Center(
              child: Text(
                'Add a product first',
                style: TextStyle(fontSize: 18),
              ),
            )
          : Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Row(
                    children: [
                      const Expanded(
                        child: Text(
                          'Product',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      TextButton.icon(
                        onPressed: addNewProduct,
                        icon: const Icon(Icons.add),
                        label: const Text('New Product'),
                      ),
                    ],
                  ),

                  const SizedBox(height: 8),
                  DropdownButtonFormField<Product>(
                    initialValue: selectedProduct,
                    decoration: const InputDecoration(
                      labelText: 'Select Product *',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.inventory_2_outlined),
                    ),
                    items: products.map((product) {
                      return DropdownMenuItem<Product>(
                        value: product,
                        child: Text(product.name),
                      );
                    }).toList(),
                    onChanged: selectProduct,
                    validator: (value) {
                      if (value == null) {
                        return 'Select a product';
                      }

                      return null;
                    },
                  ),

                  const SizedBox(height: 15),

                  if (selectedProduct != null)
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Row(
                          children: [
                            const Icon(Icons.inventory),
                            const SizedBox(width: 10),
                            Text(
                              'Current Stock: '
                              '${selectedProduct!.stockQuantity} '
                              '${selectedProduct!.unit}',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),

                  const SizedBox(height: 15),

                  TextFormField(
                    controller: quantityController,
                    keyboardType: TextInputType.number,
                    onChanged: (_) {
                      setState(() {});
                    },
                    decoration: const InputDecoration(
                      labelText: 'Purchase Quantity *',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.add_box_outlined),
                    ),
                    validator: quantityValidator,
                  ),

                  const SizedBox(height: 15),

                  TextFormField(
                    controller: priceController,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    onChanged: (_) {
                      setState(() {});
                    },
                    decoration: const InputDecoration(
                      labelText: 'Purchase Price *',
                      prefixText: '₹ ',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.currency_rupee),
                    ),
                    validator: priceValidator,
                  ),

                  const SizedBox(height: 15),

                  TextFormField(
                    controller: supplierController,
                    decoration: const InputDecoration(
                      labelText: 'Supplier',
                      hintText: 'Optional',
                      border: OutlineInputBorder(),
                      prefixIcon: Icon(Icons.business_outlined),
                    ),
                  ),

                  const SizedBox(height: 20),

                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          const Text(
                            'Total Purchase',
                            style: TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const Spacer(),
                          Text(
                            '₹${totalAmount.toStringAsFixed(2)}',
                            style: const TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                  const SizedBox(height: 25),

                  SizedBox(
                    height: 52,
                    child: FilledButton.icon(
                      onPressed: savePurchase,
                      icon: const Icon(Icons.shopping_cart),
                      label: const Text(
                        'Save Purchase',
                        style: TextStyle(fontSize: 17),
                      ),
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}
