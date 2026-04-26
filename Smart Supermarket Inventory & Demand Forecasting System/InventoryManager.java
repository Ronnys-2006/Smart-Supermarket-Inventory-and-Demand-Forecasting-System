// InventoryManager.java
// This is the central class that manages all products at runtime.
// It holds everything in an ArrayList<Product> which works because of
// polymorphism - Grocery, Electronics etc. can all be stored as Product.
//
// I used the Singleton pattern here so there's only ever one instance
// of this manager running. Creating multiple instances would cause
// the inventory state to go out of sync.
//
// Note for JDBC team: this class doesn't touch the database directly.
// It just manages the in-memory state. After any stock change here,
// you need to sync it back to the DB using the queries mentioned in the comments.

import java.util.ArrayList;
import java.util.List;

public class InventoryManager {

    // Singleton instance.
    private static InventoryManager instance;

    // Private constructor so nobody can do new InventoryManager() from outside.
    private InventoryManager() {
        this.inventory = new ArrayList<>();
    }

    // The only way to get the InventoryManager.
    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
            System.out.println("InventoryManager instance created.");
        }
        return instance;
    }

    // All products stored here. Using the base type Product so any subclass fits in.
    // This is upcasting - a Grocery or Electronics object gets stored as Product.
    private final ArrayList<Product> inventory;

    // Adds a product to the in-memory list.
    // Called by the JDBC layer after reading a row from the products table.
    public void addProduct(Product product) {
        if (product == null) {
            System.err.println("Cannot add a null product.");
            return;
        }
        if (findProductById(product.getProductId()) != null) {
            System.err.println("Product ID '" + product.getProductId() + "' already exists. Skipping.");
            return;
        }
        inventory.add(product);
        System.out.println("Added: " + product.getName() + " [" + product.getCategory() + "]");
    }

    // Removes a product from the list.
    // JDBC team: after calling this, run DELETE FROM products WHERE product_id = ?
    public boolean removeProduct(String productId) {
        Product target = findProductById(productId);
        if (target == null) {
            System.err.println("Product ID '" + productId + "' not found.");
            return false;
        }
        inventory.remove(target);
        System.out.println("Removed: " + target.getName());
        return true;
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

    // Adds stock to a product and handles the exception if the amount is invalid.
    // JDBC team: on success, run UPDATE products SET quantity = ? WHERE product_id = ?
    public boolean restockProduct(String productId, int amount) {
        Product product = findProductById(productId);
        try {
            if (product == null) {
                throw new IllegalArgumentException("Product ID '" + productId + "' not found.");
            }
            product.addStock(amount);
            return true;

        } catch (InvalidStockException e) {
            System.err.println("Restock failed: " + e.getMessage());
            return false;

        } catch (IllegalArgumentException e) {
            System.err.println("Restock failed: " + e.getMessage());
            return false;

        } finally {
            // finally block always runs - good for logging regardless of outcome.
            System.out.println("Restock operation finished for ID: " + productId);
        }
    }

    // Records a sale by reducing stock and handles any stock-related exceptions.
    // JDBC team: on success, run:
    // 1. UPDATE products SET quantity = ? WHERE product_id = ?
    // 2. INSERT INTO sales_history (product_id, quantity_sold, sale_date) VALUES (?, ?, NOW())
    public boolean recordSale(String productId, int amount) {
        Product product = findProductById(productId);
        try {
            if (product == null) {
                throw new IllegalArgumentException("Product ID '" + productId + "' not found.");
            }
            product.sellProduct(amount);
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

    // Displays all products - each call to displayDetails() dispatches
    // to the correct subclass method at runtime (dynamic dispatch).
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
    // Uses instanceof before downcasting to avoid ClassCastException.
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

    // Runs demand forecasting across all products that implement Forecastable.
    // Uses instanceof to check before casting - safe downcast.
    // JDBC team: pass in aggregate sales data from:
    // SELECT SUM(quantity_sold) FROM sales_history WHERE sale_date >= ?
    public void runForecastingForAll(int totalUnitsSold, int numberOfDays) {
        System.out.println("\nDemand Forecast Report:");
        for (Product p : inventory) {
            if (p instanceof Forecastable) {
                Forecastable forecastable = (Forecastable) p;
                forecastable.predictDemand(totalUnitsSold, numberOfDays);
            }
        }
    }

    // Returns a copy of the inventory list so the internal list can't be modified directly.
    public List<Product> getAllProducts() {
        return new ArrayList<>(inventory);
    }

    public int getInventorySize() {
        return inventory.size();
    }
}
