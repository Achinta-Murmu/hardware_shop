import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';

import 'tables.dart';

part 'app_database.g.dart';

@DriftDatabase(tables: [Products, Purchases])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 2;

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
}
