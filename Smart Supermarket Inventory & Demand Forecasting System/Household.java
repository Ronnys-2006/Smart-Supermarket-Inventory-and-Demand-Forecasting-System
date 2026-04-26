// Household.java
// Represents a household product in the supermarket.
// The category-specific attribute is material type (e.g. "Steel", "Plastic")
// since that matters for storage and logistics.
//
// Maps to products table where category = 'Household'
// and specific_attribute = material type (e.g. "Stainless Steel")

public class Household extends Product implements Forecastable {

    private String materialType;
    private boolean isFragile;

    // Household items have steady but slow demand so I used a
    // slightly conservative multiplier to avoid overstocking.
    private static final double HOUSEHOLD_DEMAND_MULTIPLIER = 0.95;

    // Default constructor.
    public Household() {
        super();
        this.materialType = "Plastic";
        this.isFragile = false;
        super.setCategory("Household");
    }

    // Full constructor.
    public Household(String productId, String name, double price,
                     int quantity, String materialType, boolean isFragile) {
        super(productId, name, price, quantity, "Household");
        this.materialType = (materialType != null && !materialType.trim().isEmpty())
                ? materialType : "Unknown";
        this.isFragile = isFragile;
    }

    // Shorter constructor when fragile flag isn't needed.
    public Household(String productId, String name, double price,
                     int quantity, String materialType) {
        this(productId, name, price, quantity, materialType, false);
    }

    // Overrides the abstract method from Product.
    @Override
    public void displayDetails() {
        System.out.println("[Household] " + getProductId() + " | " + getName()
                + " | Price: Rs." + String.format("%.2f", getPrice())
                + " | Stock: " + getQuantity() + (isLowStock() ? " (LOW)" : "")
                + " | Material: " + materialType
                + " | Fragile: " + (isFragile ? "Yes" : "No"));
    }

    // Returns material type for the specific_attribute column in the DB.
    @Override
    public String getSpecificAttribute() {
        return materialType;
    }

    // Demand forecast for household items.
    @Override
    public double predictDemand(int totalUnitsSoldInPeriod, int numberOfDays) {
        double dailyRate = Forecastable.calculateDailyRate(totalUnitsSoldInPeriod, numberOfDays);
        double predictedDemand = dailyRate * HOUSEHOLD_DEMAND_MULTIPLIER * numberOfDays;
        System.out.println("Forecast - " + getName()
                + " | Avg daily: " + String.format("%.2f", dailyRate)
                + " | Level: " + getDemandLabel(dailyRate)
                + " | Predicted (" + numberOfDays + " days): "
                + String.format("%.0f", predictedDemand) + " units");
        return predictedDemand;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        if (materialType == null || materialType.trim().isEmpty()) {
            System.err.println("Warning: Material type cannot be empty. Ignoring.");
            return;
        }
        this.materialType = materialType;
    }

    public boolean isFragile() {
        return isFragile;
    }

    public void setFragile(boolean fragile) {
        this.isFragile = fragile;
    }

    @Override
    public String toString() {
        return super.toString() + " | Material: " + materialType + " | Fragile: " + isFragile;
    }
}
