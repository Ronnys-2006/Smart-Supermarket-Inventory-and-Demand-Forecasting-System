import java.time.LocalDate;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class SupermarketApp extends Application {

    private InventoryManager manager;
    private TableView<Product> table;
    private ObservableList<Product> productData;

    @Override
    public void start(Stage primaryStage) {
        manager = InventoryManager.getInstance();
        manager.loadInventoryFromDB();
        productData = FXCollections.observableArrayList(manager.getAllProducts());

        primaryStage.setTitle("Smart Supermarket System - SIT Pune");

        TabPane tabPane = new TabPane();
        Tab inventoryTab = new Tab("Inventory Control", createInventoryUI());
        inventoryTab.setClosable(false);
        
        Tab forecastTab = new Tab("Demand Forecasting", createForecastingUI());
        forecastTab.setClosable(false);

        tabPane.getTabs().addAll(inventoryTab, forecastTab);

        Scene scene = new Scene(tabPane, 1000, 650);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> manager.shutdown()); 
        primaryStage.show();
    }

    private BorderPane createInventoryUI() {
        table = new TableView<>();
        setupTableColumns();
        table.setItems(productData);

        VBox sidebar = createSidebar();
        BorderPane pane = new BorderPane();
        pane.setCenter(table);
        pane.setRight(sidebar);
        pane.setPadding(new Insets(10));
        return pane;
    }

    private void setupTableColumns() {
        TableColumn<Product, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("productId"));
        
        TableColumn<Product, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Product, Double> priceCol = new TableColumn<>("Price (Rs.)");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));

        TableColumn<Product, Integer> qtyCol = new TableColumn<>("Stock");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        
        qtyCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    if (item <= 10) {
                        setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: #ffe6e6;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        table.getColumns().addAll(idCol, nameCol, catCol, priceCol, qtyCol);
    }

    private VBox createSidebar() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(0, 0, 0, 15));
        vbox.setMinWidth(280);

        Label lblTitle = new Label("Product Actions");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TextField txtId = new TextField(); txtId.setPromptText("Enter Product ID");
        TextField txtQty = new TextField(); txtQty.setPromptText("Quantity");

        Button btnSell = new Button("Record Sale");
        btnSell.setMaxWidth(Double.MAX_VALUE);
        btnSell.setOnAction(e -> handleUpdate(txtId.getText(), txtQty.getText(), "SALE"));

        Button btnRestock = new Button("Restock Item");
        btnRestock.setMaxWidth(Double.MAX_VALUE);
        btnRestock.setOnAction(e -> handleUpdate(txtId.getText(), txtQty.getText(), "RESTOCK"));

        Button btnDelete = new Button("🗑 Delete Product");
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");
        // NEW: Now calls the actual delete method
        btnDelete.setOnAction(e -> handleDelete(txtId.getText()));

        Button btnAdd = new Button("➕ Add New Product");
        btnAdd.setMaxWidth(Double.MAX_VALUE);
        btnAdd.setStyle("-fx-background-color: #28a745; -fx-text-fill: white;");
        // NEW: Now opens the Add Product form
        btnAdd.setOnAction(e -> showAddProductDialog());
        vbox.getChildren().addAll(lblTitle, new Label("ID:"), txtId, new Label("Qty:"), txtQty, 
                                 btnSell, btnRestock, new Separator(), btnDelete, btnAdd);
        return vbox;
    }

    private void handleUpdate(String id, String qtyStr, String type) {
        try {
            int qty = Integer.parseInt(qtyStr);
            boolean success = type.equals("SALE") ? manager.recordSale(id, qty) : manager.restockProduct(id, qty);
            if (success) {
                productData.setAll(manager.getAllProducts());
                table.refresh();
            } else {
                showAlert("Action Failed", "Please check stock levels or Product ID.");
            }
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Enter a valid numeric quantity.");
        }
    }

    private VBox createForecastingUI() {
        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.TOP_CENTER);

        // 1. Set up the Axes for the Bar Chart
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Products");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Predicted Demand (Units)");

        // 2. Create the Bar Chart
        BarChart<String, Number> forecastChart = new BarChart<>(xAxis, yAxis);
        forecastChart.setTitle("30-Day Smart Demand Forecast");
        forecastChart.setAnimated(true); // Gives a nice growing animation when it loads

        // 3. The Run Button
        Button runBtn = new Button("🧠 Generate Visual Forecast");
        runBtn.setStyle("-fx-background-color: #8e44ad; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        runBtn.setOnAction(e -> {
            // Get the math from your backend
            java.util.Map<String, Double> forecastData = manager.getForecastData(30);
            
            // Create a new data series for the chart
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Units Needed Next 30 Days");

            // Loop through the map and add it to the chart
            for (java.util.Map.Entry<String, Double> entry : forecastData.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            // Clear old data and show the new data
            forecastChart.getData().clear();
            forecastChart.getData().add(series);
        });

        vbox.getChildren().addAll(new Label("AI-Assisted Demand Dashboard"), runBtn, forecastChart);
        return vbox;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ==========================================
    // ADD & DELETE LOGIC
    // ==========================================

    private void handleDelete(String id) {
        if (id == null || id.trim().isEmpty()) {
            showAlert("Missing Input", "Please enter a Product ID to delete.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + id + " from the database?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                if (manager.removeProduct(id)) {
                    productData.setAll(manager.getAllProducts());
                    table.refresh();
                } else {
                    showAlert("Error", "Product ID not found or deletion failed.");
                }
            }
        });
    }

    private void showAddProductDialog() {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle("Add New Product");
        dialog.setHeaderText("Enter details to sync with MySQL");
        
        ButtonType addButtonType = new ButtonType("Add to DB", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField id = new TextField(); id.setPromptText("e.g. GRC-101");
        TextField name = new TextField(); name.setPromptText("e.g. Wheat Bread");
        TextField price = new TextField(); price.setPromptText("e.g. 45.0");
        TextField qty = new TextField(); qty.setPromptText("e.g. 50");
        ComboBox<String> category = new ComboBox<>(FXCollections.observableArrayList("Grocery", "Electronics", "Clothing", "Household"));
        category.setValue("Grocery");
        TextField attribute = new TextField(); attribute.setPromptText("Expiry(YYYY-MM-DD) or Warranty(Months)");

        grid.add(new Label("Product ID:"), 0, 0); grid.add(id, 1, 0);
        grid.add(new Label("Name:"), 0, 1); grid.add(name, 1, 1);
        grid.add(new Label("Price (₹):"), 0, 2); grid.add(price, 1, 2);
        grid.add(new Label("Initial Qty:"), 0, 3); grid.add(qty, 1, 3);
        grid.add(new Label("Category:"), 0, 4); grid.add(category, 1, 4);
        grid.add(new Label("Special Attr:"), 0, 5); grid.add(attribute, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                try {
                    String cat = category.getValue();
                    String pid = id.getText();
                    String n = name.getText();
                    double p = Double.parseDouble(price.getText());
                    int q = Integer.parseInt(qty.getText());
                    String attr = attribute.getText();

                    switch (cat) {
                        case "Grocery": return new Grocery(pid, n, p, q, LocalDate.parse(attr));
                        case "Electronics": return new Electronics(pid, n, p, q, Integer.parseInt(attr));
                        case "Clothing": return new Clothing(pid, n, p, q, attr);
                        case "Household": return new Household(pid, n, p, q, attr);
                    }
                } catch (Exception ex) { 
                    showAlert("Format Error", "Ensure Price/Qty are numbers and Date is YYYY-MM-DD."); 
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(product -> {
            manager.addProduct(product); // Calls your backend method
            productData.setAll(manager.getAllProducts()); // Refreshes the table
            table.refresh();
        });
    }
    public static void main(String[] args) { launch(args); }
}