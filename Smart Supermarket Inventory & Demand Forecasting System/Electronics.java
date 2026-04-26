// Electronics.java
// Represents an electronics product in the supermarket.
// The category-specific attribute is warranty period in months
// since that's the most relevant detail for electronics.
//
// Maps to products table where category = 'Electronics'
// and specific_attribute = warranty months (e.g. "24")

public class Electronics extends Product implements Forecastable {

    private int warrantyMonths;

    // Electronics sell slower than groceries so a lower multiplier makes sense.
    private static final double ELECTRONICS_DEMAND_MULTIPLIER = 0.85;

    // Default constructor.
    public Electronics() {
        super();
        this.warrantyMonths = 12;
        super.setCategory("Electronics");
    }

    // Main constructor - calls super() first as required.
    public Electronics(String productId, String name, double price,
                       int quantity, int warrantyMonths) {
        super(productId, name, price, quantity, "Electronics");
        this.warrantyMonths = warrantyMonths;
    }

    // Shows electronics-specific info.
    // Overrides the abstract displayDetails() from Product.
    @Override
    public void displayDetails() {
        System.out.println("[Electronics] " + getProductId() + " | " + getName()
                + " | Price: Rs." + String.format("%.2f", getPrice())
                + " | Stock: " + getQuantity() + (isLowStock() ? " (LOW)" : "")
                + " | Warranty: " + warrantyMonths + " months");
    }

    // Returns warranty as string for the specific_attribute column in DB.
    @Override
    public String getSpecificAttribute() {
        return String.valueOf(warrantyMonths);
    }

    // Demand forecast for electronics.
    // Lower multiplier because high-value items don't sell as frequently.
    @Override
    public double predictDemand(int totalUnitsSoldInPeriod, int numberOfDays) {
        double dailyRate = Forecastable.calculateDailyRate(totalUnitsSoldInPeriod, numberOfDays);
        double predictedDemand = dailyRate * ELECTRONICS_DEMAND_MULTIPLIER * numberOfDays;
        System.out.println("Forecast - " + getName()
                + " | Avg daily: " + String.format("%.2f", dailyRate)
                + " | Level: " + getDemandLabel(dailyRate)
                + " | Predicted (" + numberOfDays + " days): "
                + String.format("%.0f", predictedDemand) + " units");
        return predictedDemand;
    }

    // Helper to display warranty in years for readability.
    public int getWarrantyYears() {
        return warrantyMonths / 12;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        if (warrantyMonths <= 0) {
            System.err.println("Warning: Warranty months must be positive. Ignoring.");
            return;
        }
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String toString() {
        return super.toString() + " | Warranty: " + warrantyMonths + " months";
    }
}
