// Main.java
// This is just a demo/test class to verify that all the OOP logic works correctly.
// In the final project, the JDBC team's entry point will replace this,
// and the JavaFX team's GUI will replace the console output.

import java.time.LocalDate;

public class Main {

    public static void main(String[] args) {

        System.out.println("Smart Supermarket - Core OOP Demo");
        System.out.println("SIT Pune, AIML B/C\n");

        // Get the single InventoryManager instance (Singleton pattern).
        InventoryManager manager = InventoryManager.getInstance();

        // Create products using parameterized constructors.
        // Each subclass calls super() internally to set the common Product fields.
        // When these get added to the manager, they're stored as Product (upcasting).

        Grocery rice = new Grocery("GRC-001", "Basmati Rice 5kg", 349.00,
                150, LocalDate.of(2026, 12, 31));
        Grocery milk = new Grocery("GRC-002", "Amul Full Cream Milk 1L", 68.00,
                80, LocalDate.now().plusDays(5));

        Electronics fan = new Electronics("ELC-001", "Havells Ceiling Fan", 2499.00, 30, 24);
        Electronics bulb = new Electronics("ELC-002", "Philips LED Bulb 9W", 149.00, 8, 12);

        Clothing shirt = new Clothing("CLO-001", "Men's Formal Shirt - White", 799.00, 45, "L", "Cotton");
        Clothing tshirt = new Clothing("CLO-002", "Kids T-Shirt - Blue", 299.00, 25, "S", "Polyester");

        Household cooker = new Household("HSD-001", "Prestige Pressure Cooker 5L", 1899.00, 20, "Stainless Steel", false);
        Household glassSet = new Household("HSD-002", "Borosil Glass Set (6 pcs)", 649.00, 7, "Borosilicate Glass", true);

        // Add all products to inventory.
        manager.addProduct(rice);
        manager.addProduct(milk);
        manager.addProduct(fan);
        manager.addProduct(bulb);
        manager.addProduct(shirt);
        manager.addProduct(tshirt);
        manager.addProduct(cooker);
        manager.addProduct(glassSet);

        System.out.println("\nTotal Product objects created this session: " + Product.getProductCount());

        // displayAllProducts() calls displayDetails() on each product.
        // The JVM picks the right subclass version at runtime - that's dynamic dispatch.
        manager.displayAllProducts();

        // Category-wise display uses instanceof before downcasting.
        manager.displayByCategory("Grocery");
        manager.displayByCategory("Electronics");

        // Exception handling demo.
        System.out.println("\n--- Exception Handling ---");

        // Valid sale - should work fine.
        System.out.println("\nValid sale, 10 units of rice:");
        manager.recordSale("GRC-001", 10);

        // Trying to sell more than available - InvalidStockException gets caught.
        System.out.println("\nOversell attempt, 999 units of rice:");
        manager.recordSale("GRC-001", 999);

        // Negative quantity - also throws InvalidStockException.
        System.out.println("\nNegative sale quantity:");
        manager.recordSale("ELC-001", -5);

        // Valid restock.
        System.out.println("\nRestock LED Bulb by 50 units:");
        manager.restockProduct("ELC-002", 50);

        // Zero units - should fail.
        System.out.println("\nInvalid restock with 0 units:");
        manager.restockProduct("HSD-002", 0);

        // Direct try-catch-finally to show how the checked exception works.
        System.out.println("\nDirect exception catch example:");
        try {
            milk.sellProduct(200); // only 80 in stock
        } catch (InvalidStockException e) {
            System.err.println("Caught: " + e.getMessage());
        } finally {
            System.out.println("Sell attempt on '" + milk.getName() + "' complete.");
        }

        // Low stock report.
        manager.displayLowStockReport();

        // Demand forecasting - each category uses its own multiplier.
        // In the real system, the JDBC team passes in the actual sales_history totals.
        System.out.println("\n--- Demand Forecasting (last 30 days) ---");
        rice.predictDemand(420, 30);
        fan.predictDemand(15, 30);
        shirt.predictDemand(90, 30);
        cooker.predictDemand(45, 30);

        // instanceof + downcast example - safe way to access subclass-specific methods.
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
            if (p instanceof Household) {
                Household h = (Household) p;
                if (h.isFragile()) {
                    System.out.println("Fragile item: " + h.getName()
                            + " [" + h.getMaterialType() + "]");
                }
            }
        }

        System.out.println("\nDemo complete. Ready for JDBC and JavaFX integration.");
    }
}
