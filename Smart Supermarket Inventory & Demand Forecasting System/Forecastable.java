// Forecastable.java
// This interface defines what any product category needs to implement
// if it wants to support demand forecasting.
// I separated the forecasting logic here so it doesn't mix with the
// inventory management code - keeps things clean.

public interface Forecastable {

    // Each category will implement this differently based on how fast it sells.
    // The JDBC layer will pass in the totalUnitsSold by querying:
    // SELECT SUM(quantity_sold) FROM sales_history WHERE product_id = ? AND sale_date >= ?
    double predictDemand(int totalUnitsSoldInPeriod, int numberOfDays);

    // Default method - available to all classes that implement this interface.
    // Gives a simple label based on how fast something sells per day.
    default String getDemandLabel(double dailyRate) {
        if (dailyRate >= 10) {
            return "High";
        } else if (dailyRate >= 5) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    // Static utility so all category classes can use it without duplicating code.
    static double calculateDailyRate(int totalUnitsSold, int numberOfDays) {
        if (numberOfDays <= 0) {
            return 0.0;
        }
        return (double) totalUnitsSold / numberOfDays;
    }
}
