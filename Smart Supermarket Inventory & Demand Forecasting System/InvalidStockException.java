// InvalidStockException.java
// Custom checked exception for invalid stock operations.
// I made it checked (extends Exception, not RuntimeException) so the compiler
// forces whoever calls addStock() or sellProduct() to handle it properly.

public class InvalidStockException extends Exception {

    private final String productId;
    private final int requestedQuantity;
    private final int availableQuantity;

    // Main constructor - takes full context so we know exactly what went wrong.
    public InvalidStockException(String message, String productId,
                                 int requestedQuantity, int availableQuantity) {
        super(message);
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    // Second constructor for wrapping lower-level exceptions if needed.
    public InvalidStockException(String message, Throwable cause) {
        super(message, cause);
        this.productId = "UNKNOWN";
        this.requestedQuantity = -1;
        this.availableQuantity = -1;
    }

    public String getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    @Override
    public String toString() {
        return "InvalidStockException {"
                + " productId='" + productId + '\''
                + ", requestedQuantity=" + requestedQuantity
                + ", availableQuantity=" + availableQuantity
                + ", message='" + getMessage() + '\''
                + " }";
    }
}
