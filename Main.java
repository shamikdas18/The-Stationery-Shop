import java.sql.*;
import java.util.Scanner;

/**
 * Stationery Shop Inventory System
 * Core Java Console Application — MySQL + JDBC
 * Location: Kolkata Retail Store
 */
public class Main {

    // ═══════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═══════════════════════════════════════════════════════════════
    public static void main(String[] args) {

        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Welcome to Stationery Shop Inventory System");
            System.out.println("Location: Kolkata Retail Store");
            System.out.println("------------------------------------------------");
            while (true) {
                printMenu();
                System.out.print("Enter your choice: ");
                String input;
                try {
                    input = scanner.nextLine().trim();
                } catch (java.util.NoSuchElementException e) {
                    System.out.println("\nInput stream ended. Exiting.");
                    return;
                }

                switch (input) {
                    case "1" -> addProduct(scanner);
                    case "2" -> recordSale(scanner);
                    case "3" -> restockProduct(scanner);
                    case "4" -> showProfitReport();
                    case "5" -> viewProducts();
                    case "6" -> {
                        System.out.println("\nThank you! Closing system. Goodbye.");
                        return;
                    }
                    default  -> System.out.println("  [!] Invalid choice. Please enter 1-6.\n");
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  MENU
    // ═══════════════════════════════════════════════════════════════
    private static void printMenu() {
        System.out.println("\n──────────────────────────────────────────────");
        System.out.println("  MAIN MENU");
        System.out.println("──────────────────────────────────────────────");
        System.out.println("  1. Add Product");
        System.out.println("  2. Record Sale");
        System.out.println("  3. Restock Product");
        System.out.println("  4. Today's Profit Report");
        System.out.println("  5. View Products & Stock");
        System.out.println("  6. Exit");
        System.out.println("──────────────────────────────────────────────");
    }

    // ═══════════════════════════════════════════════════════════════
    //  1. ADD PRODUCT
    // ═══════════════════════════════════════════════════════════════
    private static void addProduct(Scanner sc) {
        System.out.println("\n--- Add New Product ---");

        System.out.print("Product ID (0 = auto-assign): ");
        int prodId = readInt(sc);

        System.out.print("Product Name      : ");
        String name = sc.nextLine().trim();

        System.out.print("Category          : ");
        String category = sc.nextLine().trim();

        System.out.print("Initial Stock     : ");
        int stock = readInt(sc);

        System.out.print("Selling Price (Rs): ");
        double price = readDouble(sc);

        System.out.print("Cost Price (Rs)   : ");
        double cost = readDouble(sc);

        String sql = (prodId == 0)
            ? "INSERT INTO products (name, category, stock, price, cost) VALUES (?, ?, ?, ?, ?)"
            : "INSERT INTO products (prod_id, name, category, stock, price, cost) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (prodId == 0) {
                ps.setString(1, name);
                ps.setString(2, category);
                ps.setInt(3, stock);
                ps.setDouble(4, price);
                ps.setDouble(5, cost);
            } else {
                ps.setInt(1, prodId);
                ps.setString(2, name);
                ps.setString(3, category);
                ps.setInt(4, stock);
                ps.setDouble(5, price);
                ps.setDouble(6, cost);
            }

            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet keys = ps.getGeneratedKeys();
                int assignedId = (keys.next()) ? keys.getInt(1) : prodId;
                System.out.println("  Product added successfully! Product ID: " + assignedId);
            }

        } catch (SQLException e) {
            System.out.println("  Error adding product: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  2. RECORD SALE
    // ═══════════════════════════════════════════════════════════════
    private static void recordSale(Scanner sc) {
        System.out.println("\n--- Record Sale ---");

        System.out.print("Enter Product ID : ");
        int prodId = readInt(sc);

        System.out.print("Quantity Sold    : ");
        int qty = readInt(sc);

        // Use a transaction: check stock → deduct → insert sale
        String checkStockSql = "SELECT name, stock FROM products WHERE prod_id = ?";
        String updateStockSql = "UPDATE products SET stock = stock - ? WHERE prod_id = ?";
        String insertSaleSql  = "INSERT INTO sales (prod_id, qty_sold) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);  // begin transaction

            try (PreparedStatement checkPs = conn.prepareStatement(checkStockSql)) {
                checkPs.setInt(1, prodId);
                ResultSet rs = checkPs.executeQuery();

                if (!rs.next()) {
                    System.out.println("  [✗] Product ID " + prodId + " not found.");
                    conn.rollback();
                    return;
                }

                String productName  = rs.getString("name");
                int    currentStock = rs.getInt("stock");

                if (currentStock < qty) {
                    System.out.println("  [✗] Insufficient stock! Available: " + currentStock + " units of '" + productName + "'.");
                    conn.rollback();
                    return;
                }
            }

            // Deduct stock
            try (PreparedStatement updatePs = conn.prepareStatement(updateStockSql)) {
                updatePs.setInt(1, qty);
                updatePs.setInt(2, prodId);
                updatePs.executeUpdate();
            }

            // Insert sale record
            try (PreparedStatement salePs = conn.prepareStatement(insertSaleSql)) {
                salePs.setInt(1, prodId);
                salePs.setInt(2, qty);
                salePs.executeUpdate();
            }

            conn.commit();
            System.out.println("   Sale recorded successfully! Sold " + qty + " unit(s) of Product ID " + prodId + ".");

        } catch (SQLException e) {
            System.out.println("   Error recording sale: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  3. RESTOCK PRODUCT
    // ═══════════════════════════════════════════════════════════════
    private static void restockProduct(Scanner sc) {
        System.out.println("\n--- Restock Product ---");

        System.out.print("Enter Product ID      : ");
        int prodId = readInt(sc);

        System.out.print("Quantity to Add       : ");
        int qty = readInt(sc);

        String sql = "UPDATE products SET stock = stock + ? WHERE prod_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, qty);
            ps.setInt(2, prodId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("   Stock updated! Added " + qty + " unit(s) to Product ID " + prodId + ".");
            } else {
                System.out.println("   Product ID " + prodId + " not found.");
            }

        } catch (SQLException e) {
            System.out.println("  Error restocking: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  4. TODAY'S PROFIT REPORT
    //     Revenue = SUM(qty_sold × price)
    //     Cost    = SUM(qty_sold × cost)
    //     Profit  = Revenue − Cost
    // ═══════════════════════════════════════════════════════════════
    private static void showProfitReport() {
        System.out.println("\n════════════════════════════════════════════════");
        System.out.println("        TODAY'S PROFIT REPORT");
        System.out.println("════════════════════════════════════════════════");

        String sql =
            "SELECT " +
            "    COALESCE(SUM(s.qty_sold * p.price), 0) AS revenue, " +
            "    COALESCE(SUM(s.qty_sold * p.cost),  0) AS total_cost, " +
            "    COALESCE(SUM(s.qty_sold * (p.price - p.cost)), 0) AS profit " +
            "FROM sales s " +
            "JOIN products p ON s.prod_id = p.prod_id " +
            "WHERE DATE(s.sale_time) = CURDATE()";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double revenue = rs.getDouble("revenue");
                double cost    = rs.getDouble("total_cost");
                double profit  = rs.getDouble("profit");

                System.out.printf("  Total Revenue  : ₹%,.2f%n", revenue);
                System.out.printf("  Total Cost     : ₹%,.2f%n", cost);
                System.out.println("  ─────────────────────────────────────");
                System.out.printf("  Net Profit     : ₹%,.2f%n", profit);
            }
            System.out.println("════════════════════════════════════════════════");

        } catch (SQLException e) {
            System.out.println("   Error generating report: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  5. VIEW PRODUCTS & STOCK
    // ═══════════════════════════════════════════════════════════════
    private static void viewProducts() {
        System.out.println("\n================================================================================================================");
        System.out.println("  PRODUCT LIST");
        System.out.println("================================================================================================================");
        System.out.printf("  %-6s  %-35s  %-15s  %8s  %10s  %10s%n",
                "ID", "Name", "Category", "Stock", "Price (₹)", "Cost (₹)");
        System.out.println("  ------  -----------------------------------  ---------------  --------  ----------  ----------");

        String sql = "SELECT prod_id, name, category, stock, price, cost FROM products ORDER BY category, name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                System.out.printf("  %-6d  %-35s  %-15s  %8d  %10.2f  %10.2f%n",
                        rs.getInt("prod_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getInt("stock"),
                        rs.getDouble("price"),
                        rs.getDouble("cost"));
                count++;
            }
            System.out.println("================================================================================================================");
            System.out.println("  Total Products: " + count);

        } catch (SQLException e) {
            System.out.println("  Error fetching products: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS — safe numeric input readers
    // ═══════════════════════════════════════════════════════════════
    private static int readInt(Scanner sc) {
        while (true) {
            try {
                String line = sc.nextLine();
                int value = Integer.parseInt(line.trim());
                if (value < 0) {
                    System.out.print("  [!] Value must be 0 or greater. Try again: ");
                    continue;
                }
                return value;
            } catch (java.util.NoSuchElementException e) {
                System.out.println("\n  [!] Input stream ended. Exiting.");
                System.exit(0);
            } catch (NumberFormatException e) {
                System.out.print("  [!] Invalid number. Try again: ");
            }
        }
    }

    private static double readDouble(Scanner sc) {
        while (true) {
            try {
                String line = sc.nextLine();
                double value = Double.parseDouble(line.trim());
                if (value < 0) {
                    System.out.print("  [!] Value must be 0 or greater. Try again: ");
                    continue;
                }
                return value;
            } catch (java.util.NoSuchElementException e) {
                System.out.println("\n  [!] Input stream ended. Exiting.");
                System.exit(0);
            } catch (NumberFormatException e) {
                System.out.print("  [!] Invalid number. Try again: ");
            }
        }
    }
}
