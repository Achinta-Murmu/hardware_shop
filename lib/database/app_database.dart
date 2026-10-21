import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';

import 'tables.dart';

part 'app_database.g.dart';

@DriftDatabase(tables: [Products, Purchases, Sales, SaleItems])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 3;

  @override
  MigrationStrategy get migration {
    return MigrationStrategy(
      onCreate: (Migrator m) async {
        await m.createAll();
      },
      onUpgrade: (Migrator m, int from, int to) async {
        if (from < 2) {
          await m.createTable(purchases);
        }

        if (from < 3) {
          await m.createTable(sales);
          await m.createTable(saleItems);
        }
      },
    );
  }

  static QueryExecutor _openConnection() {
    return driftDatabase(name: 'hardware_manager');
  }

  // -------------------------
  // PRODUCTS
  // -------------------------

  Future<List<Product>> getAllProducts() {
    return select(products).get();
  }

  Future<int> addProduct(ProductsCompanion product) {
    return into(products).insert(product);
  }

  Future<bool> updateProduct(Product product) {
    return update(products).replace(product);
  }

  Future<int> deleteProduct(int id) {
    return (delete(products)..where((p) => p.id.equals(id))).go();
  }

  // -------------------------
  // PURCHASES
  // -------------------------

  Future<List<Purchase>> getAllPurchases() {
    return select(purchases).get();
  }

  Future<int> addPurchase(PurchasesCompanion purchase) {
    return into(purchases).insert(purchase);
  }

  // -------------------------
  // SALES
  // -------------------------

  Future<List<Sale>> getAllSales() {
    return (select(sales)..orderBy([
          (s) => OrderingTerm(expression: s.saleDate, mode: OrderingMode.desc),
        ]))
        .get();
  }

  Future<List<SaleItem>> getSaleItems(int saleId) {
    return (select(
      saleItems,
    )..where((item) => item.saleId.equals(saleId))).get();
  }

  /// Saves a complete sale and deducts stock in one transaction.
  ///
  /// [quantities] maps product IDs to quantities being sold.
  /// Returns the new sale ID if successful.
  /// Throws StateError if a product is missing or stock is insufficient.
  Future<int> completeSale(Map<int, int> quantities) {
    return transaction(() async {
      if (quantities.isEmpty) {
        throw StateError('The bill is empty.');
      }

      // Validate every product before making any changes.
      final List<Product> saleProducts = [];

      for (final entry in quantities.entries) {
        final productId = entry.key;
        final quantity = entry.value;

        if (quantity <= 0) {
          throw StateError('Sale quantities must be greater than zero.');
        }

        final product = await (select(
          products,
        )..where((p) => p.id.equals(productId))).getSingleOrNull();

        if (product == null) {
          throw StateError('A product in this bill no longer exists.');
        }

        if (quantity > product.stockQuantity) {
          throw StateError(
            'Only ${product.stockQuantity} ${product.unit} of '
            '${product.name} are available.',
          );
        }

        saleProducts.add(product);
      }

      // Calculate the total using current database prices.
      double totalAmount = 0;

      for (final product in saleProducts) {
        final quantity = quantities[product.id]!;
        totalAmount += product.sellingPrice * quantity;
      }

      // Create the sale record.
      final saleId = await into(sales)
          .insert(SalesCompanion.insert(totalAmount: totalAmount));

      // Save each bill item and deduct its stock.
      for (final product in saleProducts) {
        final quantity = quantities[product.id]!;
        final lineTotal = product.sellingPrice * quantity;

        await into(saleItems).insert(
          SaleItemsCompanion.insert(
            saleId: saleId,
            productId: product.id,
            productName: product.name,
            unit: product.unit,
            quantity: quantity,
            sellingPrice: product.sellingPrice,
            lineTotal: lineTotal,
          ),
        );

        await (update(products)..where((p) => p.id.equals(product.id))).write(
          ProductsCompanion(
            stockQuantity: Value(product.stockQuantity - quantity),
          ),
        );
      }

      return saleId;
    });
  }
}
