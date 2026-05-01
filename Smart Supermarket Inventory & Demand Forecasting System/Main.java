// Main.java
// Entry point for the Smart Supermarket System.
// This version integrates the JDBC Data Access Layer.
// The in-memory OOP logic now syncs seamlessly with the MySQL database.

public class Main {

    public static void main(String[] args) {

        System.out.println("==================================================");
        System.out.println("Smart Supermarket - Database Integrated Version");
        System.out.println("SIT Pune, AIML B/C | Guide: Dr. Wasim Khan");
        System.out.println("==================================================\n");

        // 1. Get the single InventoryManager instance (Singleton pattern).
        // This also internally initializes the DatabaseManager.
        InventoryManager manager = InventoryManager.getInstance();

        // 2. Load data from MySQL instead of creating it manually.
        System.out.println("\n--- Initializing System ---");
        manager.loadInventoryFromDB();

        // 3. Display what was loaded from the DB.
        // The JVM picks the right subclass version at runtime (dynamic dispatch).
        manager.displayAllProducts();

        // Category-wise display uses instanceof before downcasting.
        manager.displayByCategory("Grocery");

        // 4. Exception handling and DB Sync Demo
        System.out.println("\n--- Testing Transactions & Exception Handling ---");

        if (manager.getInventorySize() > 0) {
            // Valid sale - Updates both Java memory and MySQL tables (products & sales_history)
            System.out.println("\nValid sale, 5 units of GRC-001:");
            manager.recordSale("GRC-001", 5);

            // Trying to sell more than available - InvalidStockException gets caught (No DB change)
            System.out.println("\nOversell attempt, 999 units of GRC-001:");
            manager.recordSale("GRC-001", 999);

            // Valid restock - Updates Java memory and MySQL (products table)
            System.out.println("\nRestock ELC-001 by 20 units:");
            manager.restockProduct("ELC-001", 20);
        } else {
            System.out.println("No products loaded. Please check your database connection and tables.");
        }

        // Low stock report.
        manager.displayLowStockReport();

        // 5. Demand forecasting 
        // This now queries the MySQL sales_history table for REAL data instead of hardcoded numbers.
        System.out.println("\n--- Demand Forecasting ---");
        manager.runForecastingForAll(30); // Forecast based on the last 30 days

        // 6. instanceof + downcast example - safe way to access subclass-specific methods.
        System.out.println("\n--- instanceof and Downcast Demo ---");
        for (Product p : manager.getAllProducts()) {
            if (p instanceof Grocery) {
                Grocery g = (Grocery) p;
                if (g.isExpired()) {
                    System.out.println("EXPIRED: " + g.getName());
                } else {
                    System.out.println("Grocery OK: " + g.getName() 
                            + " - " + g.getDaysUntilExpiry() + " days to expiry.");
                }
            }
        }

        // 7. Graceful Shutdown
        System.out.println("\n--- Shutting Down ---");
        manager.shutdown();
        System.out.println("Demo complete. Ready for JavaFX GUI integration.");
    }
}
