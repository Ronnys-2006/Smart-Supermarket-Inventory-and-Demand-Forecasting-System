// Grocery.java
// Represents a grocery/food item in the supermarket.
// The category-specific attribute here is the expiry date
// since perishable items need that tracked separately.
//
// Maps to products table where category = 'Grocery'
// and specific_attribute = expiry date (e.g. "2026-08-15")

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Grocery extends Product implements Forecastable {

    private LocalDate expiryDate;

    // Groceries turn over faster than other categories so I used a
    // slightly higher multiplier for the demand forecast.
    private static final double GROCERY_DEMAND_MULTIPLIER = 1.20;

    // Default constructor.
    public Grocery() {
        super();
        this.expiryDate = LocalDate.now().plusDays(30);
        super.setCategory("Grocery");
    }

    // Main constructor - super() runs first before anything else.
    public Grocery(String productId, String name, double price,
                   int quantity, LocalDate expiryDate) {
        super(productId, name, price, quantity, "Grocery");
        this.expiryDate = expiryDate;
    }

    // Overrides the abstract method from Product.
    // Each category shows its own specific details here -
    // that's the point of runtime polymorphism in this design.
    @Override
    public void displayDetails() {
        System.out.println("[Grocery] " + getProductId() + " | " + getName()
                + " | Price: Rs." + String.format("%.2f", getPrice())
                + " | Stock: " + getQuantity() + (isLowStock() ? " (LOW)" : "")
                + " | Expiry: " + expiryDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                + " | " + (isExpired() ? "EXPIRED" : "Fresh"));
    }

    // Returns expiry date string for the JDBC layer to write to specific_attribute column.
    @Override
    public String getSpecificAttribute() {
        return expiryDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    // Demand forecast for grocery items.
    // Formula: daily average * multiplier * number of days in next period.
    // The JDBC layer should pass in totalUnitsSoldInPeriod from:
    // SELECT SUM(quantity_sold) FROM sales_history WHERE product_id = ? AND sale_date >= ?
    @Override
    public double predictDemand(int totalUnitsSoldInPeriod, int numberOfDays) {
        double dailyRate = Forecastable.calculateDailyRate(totalUnitsSoldInPeriod, numberOfDays);
        double predictedDemand = dailyRate * GROCERY_DEMAND_MULTIPLIER * numberOfDays;
        System.out.println("Forecast - " + getName()
                + " | Avg daily: " + String.format("%.2f", dailyRate)
                + " | Level: " + getDemandLabel(dailyRate)
                + " | Predicted (" + numberOfDays + " days): "
                + String.format("%.0f", predictedDemand) + " units");
        return predictedDemand;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public long getDaysUntilExpiry() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        if (expiryDate == null) {
            System.err.println("Warning: Expiry date cannot be null. Ignoring.");
            return;
        }
        this.expiryDate = expiryDate;
    }

    // Convenience setter for when the date comes in as a string from the DB.
    public void setExpiryDateFromString(String dateString) {
        try {
            this.expiryDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            System.err.println("Warning: Invalid date format '" + dateString
                    + "'. Expected YYYY-MM-DD. Expiry not updated.");
        }
    }

    @Override
    public String toString() {
        return super.toString() + " | Expiry: " + expiryDate;
    }
}
