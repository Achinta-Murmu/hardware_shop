import 'package:drift/drift.dart';
import 'package:flutter/material.dart';

import '../database/app_database.dart';

class AddProductScreen extends StatefulWidget {
  final AppDatabase database;
  final Product? product;
  final bool fromPurchase;

  const AddProductScreen({
    super.key,
    required this.database,
    this.product,
    this.fromPurchase = false,
  });

  bool get isEditing => product != null;

  @override
  State<AddProductScreen> createState() => _AddProductScreenState();
}

class _AddProductScreenState extends State<AddProductScreen> {
  final _formKey = GlobalKey<FormState>();

  final nameController = TextEditingController();
  final categoryController = TextEditingController();
  final brandController = TextEditingController();
  final codeController = TextEditingController();
  final purchaseController = TextEditingController();
  final sellingController = TextEditingController();
  final stockController = TextEditingController();
  final minimumStockController = TextEditingController();

  String selectedUnit = 'pcs';

  @override
  void initState() {
    super.initState();

    final product = widget.product;

    if (product != null) {
      nameController.text = product.name;
      categoryController.text = product.category ?? '';
      brandController.text = product.brand ?? '';
      codeController.text = product.productCode ?? '';
      purchaseController.text = product.purchasePrice.toString();
      sellingController.text = product.sellingPrice.toString();
      stockController.text = product.stockQuantity.toString();
      minimumStockController.text = product.minimumStock.toString();
      selectedUnit = product.unit;
    } else {
      minimumStockController.text = '5';

      if (widget.fromPurchase) {
        stockController.text = '0';
      }
    }
  }

  @override
  void dispose() {
    nameController.dispose();
    categoryController.dispose();
    brandController.dispose();
    codeController.dispose();
    purchaseController.dispose();
    sellingController.dispose();
    stockController.dispose();
    minimumStockController.dispose();

    super.dispose();
  }

  Future<void> saveProduct() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }

    final name = nameController.text.trim();

    final category = categoryController.text.trim().isEmpty
        ? null
        : categoryController.text.trim();

    final brand = brandController.text.trim().isEmpty
        ? null
        : brandController.text.trim();

    final productCode = codeController.text.trim().isEmpty
        ? null
        : codeController.text.trim();

    final purchasePrice = double.parse(purchaseController.text.trim());

    final sellingPrice = double.parse(sellingController.text.trim());

    final stockQuantity = int.parse(stockController.text.trim());

    final minimumStock = int.parse(minimumStockController.text.trim());

    int productId;

    if (widget.product == null) {
      productId = await widget.database.addProduct(
        ProductsCompanion.insert(
          name: name,
          category: Value(category),
          brand: Value(brand),
          productCode: Value(productCode),
          purchasePrice: purchasePrice,
          sellingPrice: sellingPrice,
          stockQuantity: stockQuantity,
          minimumStock: Value(minimumStock),
          unit: Value(selectedUnit),
        ),
      );
    } else {
      final updatedProduct = widget.product!.copyWith(
        name: name,
        category: Value(category),
        brand: Value(brand),
        productCode: Value(productCode),
        purchasePrice: purchasePrice,
        sellingPrice: sellingPrice,
        stockQuantity: stockQuantity,
        minimumStock: minimumStock,
        unit: selectedUnit,
      );

      await widget.database.updateProduct(updatedProduct);

      productId = widget.product!.id;
    }

    if (!mounted) return;

    Navigator.pop(context, productId);
  }

  String? requiredField(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Required';
    }

    return null;
  }

  String? numberField(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Required';
    }

    if (double.tryParse(value) == null) {
      return 'Enter a valid number';
    }

    if (double.parse(value) < 0) {
      return 'Cannot be negative';
    }

    return null;
  }

  String? integerField(String? value) {
    if (value == null || value.trim().isEmpty) {
      return 'Required';
    }

    final number = int.tryParse(value);

    if (number == null) {
      return 'Enter a whole number';
    }

    if (number < 0) {
      return 'Cannot be negative';
    }

    return null;
  }

  @override
  Widget build(BuildContext context) {
    final isEditing = widget.isEditing;

    return Scaffold(
      appBar: AppBar(
        title: Text(
          isEditing ? 'Edit Product' : 'Add Product',
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // PRODUCT NAME
            TextFormField(
              controller: nameController,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(
                labelText: 'Product Name *',
                hintText: 'Enter product name',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.inventory_2_outlined),
              ),
              validator: requiredField,
            ),

            const SizedBox(height: 15),

            // CATEGORY
            TextFormField(
              controller: categoryController,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(
                labelText: 'Category',
                hintText: 'e.g. Electrical, Plumbing, Tools',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.category_outlined),
              ),
            ),

            const SizedBox(height: 15),

            // BRAND
            TextFormField(
              controller: brandController,
              textCapitalization: TextCapitalization.words,
              decoration: const InputDecoration(
                labelText: 'Brand',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.branding_watermark_outlined),
              ),
            ),

            const SizedBox(height: 15),

            // PRODUCT CODE
            TextFormField(
              controller: codeController,
              decoration: const InputDecoration(
                labelText: 'Product Code',
                hintText: 'Barcode / SKU / Code',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.qr_code),
              ),
            ),

            const SizedBox(height: 15),

            // PURCHASE PRICE
            TextFormField(
              controller: purchaseController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(
                labelText: 'Purchase Price *',
                prefixText: '₹ ',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.currency_rupee),
              ),
              validator: numberField,
            ),

            const SizedBox(height: 15),

            // SELLING PRICE
            TextFormField(
              controller: sellingController,
              keyboardType: const TextInputType.numberWithOptions(
                decimal: true,
              ),
              decoration: const InputDecoration(
                labelText: 'Selling Price *',
                prefixText: '₹ ',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.sell_outlined),
              ),
              validator: numberField,
            ),

            const SizedBox(height: 15),

            // STOCK + UNIT
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: TextFormField(
                    controller: stockController,
                    keyboardType: TextInputType.number,
                    readOnly: widget.fromPurchase,
                    decoration: InputDecoration(
                      labelText: 'Stock *',
                      border: const OutlineInputBorder(),
                      prefixIcon: const Icon(Icons.inventory_2_outlined),
                      helperText: widget.fromPurchase ? 'Starts at 0' : null,
                    ),
                    validator: integerField,
                  ),
                ),

                const SizedBox(width: 12),

                Expanded(
                  child: DropdownButtonFormField<String>(
                    initialValue: selectedUnit,
                    decoration: const InputDecoration(
                      labelText: 'Unit',
                      border: OutlineInputBorder(),
                    ),
                    items: const [
                      DropdownMenuItem(
                        value: 'pcs',
                        child: Text('Piece (pcs)'),
                      ),
                      DropdownMenuItem(value: 'box', child: Text('Box')),
                      DropdownMenuItem(
                        value: 'pkt',
                        child: Text('Packet (pkt)'),
                      ),
                      DropdownMenuItem(value: 'set', child: Text('Set')),
                      DropdownMenuItem(value: 'pair', child: Text('Pair')),
                      DropdownMenuItem(
                        value: 'doz',
                        child: Text('Dozen (doz)'),
                      ),
                      DropdownMenuItem(
                        value: 'kg',
                        child: Text('Kilogram (kg)'),
                      ),
                      DropdownMenuItem(value: 'g', child: Text('Gram (g)')),
                      DropdownMenuItem(
                        value: 'qtl',
                        child: Text('Quintal (qtl)'),
                      ),
                      DropdownMenuItem(value: 'ton', child: Text('Ton')),
                      DropdownMenuItem(value: 'm', child: Text('Meter (m)')),
                      DropdownMenuItem(
                        value: 'cm',
                        child: Text('Centimeter (cm)'),
                      ),
                      DropdownMenuItem(value: 'ft', child: Text('Foot (ft)')),
                      DropdownMenuItem(value: 'in', child: Text('Inch (in)')),
                      DropdownMenuItem(value: 'l', child: Text('Liter (L)')),
                      DropdownMenuItem(
                        value: 'ml',
                        child: Text('Milliliter (ml)'),
                      ),
                      DropdownMenuItem(value: 'roll', child: Text('Roll')),
                      DropdownMenuItem(value: 'bundle', child: Text('Bundle')),
                      DropdownMenuItem(value: 'bag', child: Text('Bag')),
                      DropdownMenuItem(value: 'sheet', child: Text('Sheet')),
                      DropdownMenuItem(value: 'bar', child: Text('Bar')),
                      DropdownMenuItem(value: 'bottle', child: Text('Bottle')),
                      DropdownMenuItem(value: 'can', child: Text('Can')),
                      DropdownMenuItem(value: 'drum', child: Text('Drum')),
                    ],
                    onChanged: (value) {
                      if (value != null) {
                        setState(() {
                          selectedUnit = value;
                        });
                      }
                    },
                  ),
                ),
              ],
            ),

            const SizedBox(height: 15),

            // MINIMUM STOCK
            TextFormField(
              controller: minimumStockController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Minimum Stock',
                hintText: 'Alert when stock goes below this',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.warning_amber_outlined),
              ),
              validator: integerField,
            ),

            const SizedBox(height: 30),

            // SAVE / UPDATE
            SizedBox(
              height: 52,
              child: FilledButton.icon(
                onPressed: saveProduct,
                icon: Icon(isEditing ? Icons.update : Icons.save),
                label: Text(
                  isEditing ? 'Update Product' : 'Save Product',
                  style: const TextStyle(fontSize: 17),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
