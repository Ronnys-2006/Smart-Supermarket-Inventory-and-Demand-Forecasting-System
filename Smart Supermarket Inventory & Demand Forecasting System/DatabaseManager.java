import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    // IMPORTANT: Update these to match your local MySQL setup
    private static final String URL = "jdbc:mysql://localhost:3306/javaproject";
    private static final String USER = "root"; 
    private static final String PASSWORD = ""; 

    private Connection connection;

    // Constructor establishes the connection
    public DatabaseManager() {
        try {
            // Ensures the driver is loaded (optional in newer JDBC, but good practice)
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("JDBC connected to MySQL successfully.");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database Connection Failed: " + e.getMessage());
        }
    }

    // 1. Load all products on startup
    public List<Product> loadAllProducts() {
        List<Product> products = new ArrayList<>();
        String query = "SELECT product_id, name, price, quantity, category, specific_attribute FROM products";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String id = rs.getString("product_id");
                String name = rs.getString("name");
                double price = rs.getDouble("price");
                int qty = rs.getInt("quantity");
                String category = rs.getString("category");
                String attr = rs.getString("specific_attribute");

                Product p = null;
                try {
                    // Rebuild the correct subclass based on the category string
                    switch (category) {
                        case "Grocery":
                            p = new Grocery(id, name, price, qty, LocalDate.parse(attr));
                            break;
                        case "Electronics":
                            p = new Electronics(id, name, price, qty, Integer.parseInt(attr));
                            break;
                        case "Clothing":
                            p = new Clothing(id, name, price, qty, attr);
                            break;
                        case "Household":
                            p = new Household(id, name, price, qty, attr);
                            break;
                    }
                    if (p != null) products.add(p);

                } catch (DateTimeParseException | NumberFormatException e) {
                    System.err.println("Error parsing specific_attribute for product: " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading products: " + e.getMessage());
        }
        return products;
    }

    // 2. Add a completely new product
    public boolean addProductDB(Product p) {
        String query = "INSERT INTO products (product_id, name, price, quantity, category, specific_attribute) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, p.getProductId());
            ps.setString(2, p.getName());
            ps.setDouble(3, p.getPrice());
            ps.setInt(4, p.getQuantity());
            ps.setString(5, p.getCategory());
            ps.setString(6, p.getSpecificAttribute());
            
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error inserting product: " + e.getMessage());
            return false;
        }
    }

    // 3. Update stock (used for restock)
    public boolean updateStockDB(String productId, int newQuantity) {
        String query = "UPDATE products SET quantity = ? WHERE product_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, newQuantity);
            ps.setString(2, productId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error updating stock: " + e.getMessage());
            return false;
        }
    }

    // 4. Record a sale (Updates stock AND logs the sale in a single transaction)
    public boolean recordSaleDB(String productId, int newQuantity, int amountSold) {
        String updateStockQuery = "UPDATE products SET quantity = ? WHERE product_id = ?";
        String insertSaleQuery = "INSERT INTO sales_history (product_id, quantity_sold, sale_date) VALUES (?, ?, NOW())";

        try {
            // Start Transaction
            connection.setAutoCommit(false);

            // Step A: Update the products table
            try (PreparedStatement psUpdate = connection.prepareStatement(updateStockQuery)) {
                psUpdate.setInt(1, newQuantity);
                psUpdate.setString(2, productId);
                psUpdate.executeUpdate();
            }

            // Step B: Insert into sales_history table
            try (PreparedStatement psInsert = connection.prepareStatement(insertSaleQuery)) {
                psInsert.setString(1, productId);
                psInsert.setInt(2, amountSold);
                psInsert.executeUpdate();
            }

            // Commit Transaction
            connection.commit();
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback(); // Revert changes if anything fails
                System.err.println("Transaction failed. Changes rolled back.");
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Reset to default state
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 5. Get total sales for forecasting
    public int getSalesDataForForecasting(String productId, int days) {
        String query = "SELECT SUM(quantity_sold) AS total_sold FROM sales_history WHERE product_id = ? AND sale_date >= DATE_SUB(NOW(), INTERVAL ? DAY)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, productId);
            ps.setInt(2, days);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_sold");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching sales data: " + e.getMessage());
        }
        return 0; // Return 0 if no sales or error
    }

    // 6. Delete a product from the database
    public boolean removeProductDB(String productId) {
        // Because of the Foreign Key, we MUST delete sales history first.
        String deleteHistoryQuery = "DELETE FROM sales_history WHERE product_id = ?";
        String deleteProductQuery = "DELETE FROM products WHERE product_id = ?";

        try {
            // Start Transaction to ensure both delete together, or neither do
            connection.setAutoCommit(false);

            // Step A: Clear out the sales history for this product
            try (PreparedStatement psHistory = connection.prepareStatement(deleteHistoryQuery)) {
                psHistory.setString(1, productId);
                psHistory.executeUpdate();
            }

            // Step B: Delete the actual product
            try (PreparedStatement psProduct = connection.prepareStatement(deleteProductQuery)) {
                psProduct.setString(1, productId);
                int rowsAffected = psProduct.executeUpdate();
                
                // If 0 rows were affected, the product ID didn't exist in the DB
                if (rowsAffected == 0) {
                    connection.rollback();
                    return false;
                }
            }

            // Commit Transaction
            connection.commit();
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback(); // Undo everything if an error occurs
                System.err.println("Deletion failed. Rolled back: " + e.getMessage());
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(true); // Reset to default
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Call this when the app closes
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // This will trigger the constructor and try to connect
        DatabaseManager db = new DatabaseManager(); 
    }
}
