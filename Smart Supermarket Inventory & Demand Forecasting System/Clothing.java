// Clothing.java
// Represents a clothing product in the supermarket.
// The category-specific attribute is size (XS/S/M/L/XL/XXL)
// since that's the key differentiator for clothing inventory.
//
// Maps to products table where category = 'Clothing'
// and specific_attribute = size (e.g. "M")

public class Clothing extends Product implements Forecastable {

    private String size;
    private String fabricType;

    // Using a neutral multiplier as baseline since clothing demand
    // can be seasonal and vary a lot.
    private static final double CLOTHING_DEMAND_MULTIPLIER = 1.05;

    private static final String[] VALID_SIZES = {"XS", "S", "M", "L", "XL", "XXL"};

    // Default constructor.
    public Clothing() {
        super();
        this.size = "M";
        this.fabricType = "Cotton";
        super.setCategory("Clothing");
    }

    // Full constructor with both size and fabric.
    public Clothing(String productId, String name, double price,
                    int quantity, String size, String fabricType) {
        super(productId, name, price, quantity, "Clothing");
        this.size = isValidSize(size) ? size.toUpperCase() : "M";
        this.fabricType = (fabricType != null && !fabricType.trim().isEmpty())
                ? fabricType : "Unknown";
    }

    // Shorter constructor when fabric type isn't needed - delegates to the full one.
    public Clothing(String productId, String name, double price, int quantity, String size) {
        this(productId, name, price, quantity, size, "Cotton");
    }

    // Overrides the abstract method from Product.
    @Override
    public void displayDetails() {
        System.out.println("[Clothing] " + getProductId() + " | " + getName()
                + " | Price: Rs." + String.format("%.2f", getPrice())
                + " | Stock: " + getQuantity() + (isLowStock() ? " (LOW)" : "")
                + " | Size: " + size
                + " | Fabric: " + fabricType);
    }

    // Returns size for the specific_attribute column in the DB.
    @Override
    public String getSpecificAttribute() {
        return size;
    }

    // Demand forecast for clothing items.
    @Override
    public double predictDemand(int totalUnitsSoldInPeriod, int numberOfDays) {
        double dailyRate = Forecastable.calculateDailyRate(totalUnitsSoldInPeriod, numberOfDays);
        double predictedDemand = dailyRate * CLOTHING_DEMAND_MULTIPLIER * numberOfDays;
        System.out.println("Forecast - " + getName() + " (Size: " + size + ")"
                + " | Avg daily: " + String.format("%.2f", dailyRate)
                + " | Level: " + getDemandLabel(dailyRate)
                + " | Predicted (" + numberOfDays + " days): "
                + String.format("%.0f", predictedDemand) + " units");
        return predictedDemand;
    }

    // Validates size before setting it.
    private boolean isValidSize(String size) {
        if (size == null) return false;
        for (String valid : VALID_SIZES) {
            if (valid.equalsIgnoreCase(size)) return true;
        }
        System.err.println("Warning: Invalid size '" + size + "'. Defaulting to M.");
        return false;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        if (isValidSize(size)) {
            this.size = size.toUpperCase();
        }
    }

    public String getFabricType() {
        return fabricType;
    }

    public void setFabricType(String fabricType) {
        if (fabricType == null || fabricType.trim().isEmpty()) {
            System.err.println("Warning: Fabric type cannot be empty. Ignoring.");
            return;
        }
        this.fabricType = fabricType;
    }

    @Override
    public String toString() {
        return super.toString() + " | Size: " + size + " | Fabric: " + fabricType;
    }
}
