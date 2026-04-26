// Product.java
// Abstract base class for all product types in the system.
// Grocery, Electronics, Clothing, and Household all extend this.
// I made it abstract because a plain "Product" without a category
// doesn't really make sense in a supermarket context.
//
// Maps to the 'products' table:
// products(product_id, name, price, quantity, category, specific_attribute)

public abstract class Product {

    // All fields are private - only accessible through getters/setters.
    // This way nothing outside can accidentally set a negative price or quantity.
    private String productId;
    private String name;
    private double price;
    private int quantity;
    private String category;

    // Tracks how many Product objects have been created this session.
    private static int productCount = 0;

    // Default constructor - sets placeholder values.
    public Product() {
        productCount++;
        this.productId = "PROD-" + productCount;
        this.name = "Unnamed Product";
        this.price = 0.0;
        this.quantity = 0;
        this.category = "General";
    }

    // Main constructor - called from subclasses using super().
    public Product(String productId, String name, double price, int quantity, String category) {
        productCount++;
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
    }

    // Adds stock to this product (e.g. after a new delivery).
    // After this runs, the JDBC layer should sync with:
    // UPDATE products SET quantity = ? WHERE product_id = ?
    public void addStock(int amount) throws InvalidStockException {
        if (amount <= 0) {
            throw new InvalidStockException(
                    "Cannot add zero or negative stock. Provided amount: " + amount,
                    this.productId, amount, this.quantity
            );
        }
        this.quantity += amount;
        System.out.println("Stock updated - " + this.name + ": +" + amount
                + " units. New quantity: " + this.quantity);
    }

    // Reduces stock when a sale happens.
    // After this runs, the JDBC layer should:
    // 1. UPDATE products SET quantity = ? WHERE product_id = ?
    // 2. INSERT INTO sales_history (product_id, quantity_sold, sale_date) VALUES (?, ?, NOW())
    public void sellProduct(int amount) throws InvalidStockException {
        if (amount <= 0) {
            throw new InvalidStockException(
                    "Sale quantity must be positive. Provided: " + amount,
                    this.productId, amount, this.quantity
            );
        }
        if (amount > this.quantity) {
            throw new InvalidStockException(
                    "Not enough stock for '" + this.name + "'. Tried to sell "
                            + amount + " but only " + this.quantity + " available.",
                    this.productId, amount, this.quantity
            );
        }
        this.quantity -= amount;
        System.out.println("Sale recorded - " + this.name + ": -" + amount
                + " units. Remaining: " + this.quantity);
    }

    // Anything at or below 10 units is considered low stock.
    public boolean isLowStock() {
        return this.quantity <= 10;
    }

    // Every subclass must override this to show its own category-specific details.
    // This is where runtime polymorphism actually happens in the system.
    public abstract void displayDetails();

    // Returns the category-specific attribute value.
    // Maps to the 'specific_attribute' column in the products table.
    // e.g. expiry date for Grocery, warranty months for Electronics.
    public abstract String getSpecificAttribute();

    // Static method - returns total products created this session.
    public static int getProductCount() {
        return productCount;
    }

    // Getters and setters below.
    // Setters include basic validation so invalid data can't sneak in.

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            System.err.println("Warning: Product ID cannot be empty. Ignoring.");
            return;
        }
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            System.err.println("Warning: Name cannot be empty. Ignoring.");
            return;
        }
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        if (price < 0) {
            System.err.println("Warning: Price cannot be negative. Ignoring.");
            return;
        }
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    // This setter is mainly for the JDBC layer to set quantity after reading from DB.
    // Prefer using addStock() and sellProduct() for any runtime changes.
    public void setQuantity(int quantity) {
        if (quantity < 0) {
            System.err.println("Warning: Quantity cannot be negative. Ignoring.");
            return;
        }
        this.quantity = quantity;
    }

    public String getCategory() {
        return category;
    }

    protected void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return String.format("Product[id='%s', name='%s', category='%s', price=%.2f, qty=%d]",
                productId, name, category, price, quantity);
    }
}
