// InventoryManager.java
// This is the central class that manages all products at runtime.
// It holds everything in an ArrayList<Product> which works because of
// polymorphism - Grocery, Electronics etc. can all be stored as Product.
//
// Uses the Singleton pattern so there's only ever one instance
// of this manager running.

import java.util.ArrayList;
import java.util.List;

public class InventoryManager {

    // Singleton instance.
    private static InventoryManager instance;

    // All products stored here.
    private final ArrayList<Product> inventory;
    
    // NEW: Reference to the database manager
    private DatabaseManager dbManager;

    // Private constructor.
    private InventoryManager() {
        this.inventory = new ArrayList<>();
        // NEW: Initialize the database connection when the manager is created
        this.dbManager = new DatabaseManager();
    }

    // The only way to get the InventoryManager.
    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
            System.out.println("InventoryManager instance created.");
        }
        return instance;
    }

    // NEW: Initialize the inventory from the database on startup
    public void loadInventoryFromDB() {
        List<Product> productsFromDB = dbManager.loadAllProducts();
        inventory.clear(); // Clear memory to avoid duplicates if called multiple times
        inventory.addAll(productsFromDB);
        System.out.println("Database Sync: Loaded " + inventory.size() + " products into memory.");
    }

    // MODIFIED: Adds a product to memory AND the database.
    public void addProduct(Product product) {
        if (product == null) {
            System.err.println("Cannot add a null product.");
            return;
        }
        if (findProductById(product.getProductId()) != null) {
            System.err.println("Product ID '" + product.getProductId() + "' already exists. Skipping.");
            return;
        }
        
        // 1. Add to in-memory list
        inventory.add(product);
        
        // 2. Sync with Database
        boolean dbSuccess = dbManager.addProductDB(product);
        if (dbSuccess) {
            System.out.println("Added: " + product.getName() + " [" + product.getCategory() + "] (Synced to DB)");
        } else {
            inventory.remove(product);
            System.err.println("Warning: Failed to save " + product.getName() + " to the database.");
        }
    }

    // Removes a product from the list.
    // Removes a product from the list and the database.
    public boolean removeProduct(String productId) {
        Product target = findProductById(productId);
        if (target == null) {
            System.err.println("Product ID '" + productId + "' not found.");
            return false;
        }
        
        // 1. Sync with Database FIRST
        boolean dbSuccess = dbManager.removeProductDB(productId);
        
        // 2. If DB delete was successful, remove from Java memory
        if (dbSuccess) {
            inventory.remove(target);
            System.out.println("Removed: " + target.getName() + " from memory and database.");
            return true;
        } else {
            System.err.println("Warning: Failed to delete product from database.");
            return false;
        }
    }

    // Searches by product ID. Used internally and by the JDBC layer.
    public Product findProductById(String productId) {
        for (Product p : inventory) {
            if (p.getProductId().equalsIgnoreCase(productId)) {
                return p;
            }
        }
        return null;
    }

    // Searches by name - partial match, not case sensitive.
    public List<Product> searchByName(String name) {
        List<Product> results = new ArrayList<>();
        if (name == null || name.trim().isEmpty()) return results;
        for (Product p : inventory) {
            if (p.getName().toLowerCase().contains(name.toLowerCase())) {
                results.add(p);
            }
        }
        return results;
    }

    // MODIFIED: Adds stock in memory and updates the DB.
    public boolean restockProduct(String productId, int amount) {
        Product product = findProductById(productId);
        try {
            if (product == null) {
                throw new IllegalArgumentException("Product ID '" + productId + "' not found.");
            }
            
            // 1. In-memory update (Validation happens here)
            product.addStock(amount);
            
            // 2. Database Sync
            boolean dbSuccess = dbManager.updateStockDB(productId, product.getQuantity());
            if (!dbSuccess) {
                // <-- ADD THIS CATCH to roll back memory
                product.sellProduct(amount); // Reverts the addStock
                System.err.println("Warning: Database sync failed. Restock reverted.");
                return false; 
            }
            return true;

        } catch (InvalidStockException e) {
            System.err.println("Restock failed: " + e.getMessage());
            return false;

        } catch (IllegalArgumentException e) {
            System.err.println("Restock failed: " + e.getMessage());
            return false;

        } finally {
            System.out.println("Restock operation finished for ID: " + productId);
        }
    }

    // MODIFIED: Records a sale in memory and updates both products and sales_history tables.
    public boolean recordSale(String productId, int amount) {
        Product product = findProductById(productId);
        try {
            if (product == null) {
                throw new IllegalArgumentException("Product ID '" + productId + "' not found.");
            }
            
            // 1. In-memory update (Throws exception if not enough stock)
            product.sellProduct(amount);
            
            // 2. Database Sync (Handles transaction for stock update + sales history)
            boolean dbSuccess = dbManager.recordSaleDB(productId, product.getQuantity(), amount);
            
            if (!dbSuccess) {
                // Rollback in-memory if DB fails
                product.addStock(amount); 
                System.err.println("Database sync failed. Sale reverted in memory.");
                return false;
            }
            return true;

        } catch (InvalidStockException e) {
            System.err.println("Sale failed: " + e.getMessage());
            return false;

        } catch (IllegalArgumentException e) {
            System.err.println("Sale failed: " + e.getMessage());
            return false;

        } finally {
            System.out.println("Sale operation finished for ID: " + productId);
        }
    }

    // Displays all products.
    public void displayAllProducts() {
        if (inventory.isEmpty()) {
            System.out.println("Inventory is empty.");
            return;
        }
        System.out.println("\nFull Inventory (" + inventory.size() + " products):");
        for (Product p : inventory) {
            p.displayDetails();
        }
    }

    // Filters and displays products by category.
    public void displayByCategory(String category) {
        System.out.println("\nCategory: " + category);
        boolean found = false;
        for (Product p : inventory) {
            switch (category.toLowerCase()) {
                case "grocery":
                    if (p instanceof Grocery) { p.displayDetails(); found = true; }
                    break;
                case "electronics":
                    if (p instanceof Electronics) { p.displayDetails(); found = true; }
                    break;
                case "clothing":
                    if (p instanceof Clothing) { p.displayDetails(); found = true; }
                    break;
                case "household":
                    if (p instanceof Household) { p.displayDetails(); found = true; }
                    break;
                default:
                    System.err.println("Unknown category: " + category);
                    return;
            }
        }
        if (!found) {
            System.out.println("No products found in category: " + category);
        }
    }

    // Prints a report of anything that's running low on stock.
    public void displayLowStockReport() {
        System.out.println("\nLow Stock Report:");
        boolean found = false;
        for (Product p : inventory) {
            if (p.isLowStock()) {
                System.out.println("LOW: [" + p.getCategory() + "] "
                        + p.getName() + " - " + p.getQuantity() + " units left.");
                found = true;
            }
        }
        if (!found) {
            System.out.println("All products have sufficient stock.");
        }
    }

    // MODIFIED: Uses the database to get real historical sales data for forecasting.
    // Removed the manual 'totalUnitsSold' parameter since we pull it directly now.
    public void runForecastingForAll(int numberOfDays) {
        System.out.println("\nDemand Forecast Report (Last " + numberOfDays + " Days):");
        for (Product p : inventory) {
            if (p instanceof Forecastable) {
                Forecastable forecastable = (Forecastable) p;
                
                // Fetch actual historical units sold from sales_history table via DatabaseManager
                int actualUnitsSold = dbManager.getSalesDataForForecasting(p.getProductId(), numberOfDays);
                
                forecastable.predictDemand(actualUnitsSold, numberOfDays);
            }
        }
    }

    // Returns a copy of the inventory list.
    public List<Product> getAllProducts() {
        return new ArrayList<>(inventory);
    }

    public int getInventorySize() {
        return inventory.size();
    }
    
    // NEW: Safely close the database connection
    public void shutdown() {
        if (dbManager != null) {
            dbManager.closeConnection();
        }
    }
}
