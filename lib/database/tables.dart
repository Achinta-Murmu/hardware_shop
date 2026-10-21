import 'package:drift/drift.dart';

class Products extends Table {
  IntColumn get id => integer().autoIncrement()();

  TextColumn get name => text()();

  TextColumn get category => text().nullable()();

  TextColumn get brand => text().nullable()();

  TextColumn get productCode => text().nullable()();

  RealColumn get purchasePrice => real()();

  RealColumn get sellingPrice => real()();

  IntColumn get stockQuantity => integer()();

  IntColumn get minimumStock => integer().withDefault(const Constant(5))();

  TextColumn get unit => text().withDefault(const Constant('pcs'))();

  DateTimeColumn get createdAt => dateTime().withDefault(currentDateAndTime)();
}

class Purchases extends Table {
  IntColumn get id => integer().autoIncrement()();

  IntColumn get productId => integer()();

  IntColumn get quantity => integer()();

  RealColumn get purchasePrice => real()();

  TextColumn get supplier => text().nullable()();

  DateTimeColumn get purchaseDate =>
      dateTime().withDefault(currentDateAndTime)();
}

// One record for each completed bill.
class Sales extends Table {
  IntColumn get id => integer().autoIncrement()();

  RealColumn get totalAmount => real()();

  DateTimeColumn get saleDate => dateTime().withDefault(currentDateAndTime)();
}

// The products and quantities included in each bill.
// Product details are saved as snapshots so sale history
// remains readable if a product is later edited or deleted.
class SaleItems extends Table {
  IntColumn get id => integer().autoIncrement()();

  IntColumn get saleId => integer()();

  IntColumn get productId => integer()();

  TextColumn get productName => text()();

  TextColumn get unit => text()();

  IntColumn get quantity => integer()();

  RealColumn get sellingPrice => real()();

  RealColumn get lineTotal => real()();
}
