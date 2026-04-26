# Database Schema - Smart Supermarket Inventory & Demand Forecasting System
# SIT Pune | AIML B/C | Guide: Dr. Wasim Khan
# Share this with the JDBC team member before they start setting up the connection.

---

## How the Java classes map to the database

The OOP layer (Product and its subclasses) is designed to directly reflect the
two tables below. When the JDBC layer reads a row from `products`, it should
create the matching Java object (Grocery, Electronics, Clothing, or Household)
and load it into the InventoryManager. When a sale happens in Java,
the JDBC layer needs to update both tables accordingly.

---

## Table 1: products

Stores every product in the supermarket, regardless of category.

```sql
CREATE TABLE products (
    product_id       VARCHAR(20)    PRIMARY KEY,
    name             VARCHAR(100)   NOT NULL,
    price            DECIMAL(10,2)  NOT NULL,
    quantity         INT            NOT NULL DEFAULT 0,
    category         VARCHAR(20)    NOT NULL,
    specific_attribute VARCHAR(100) NOT NULL
);
```

| Column             | Type           | Description                                                     |
|--------------------|----------------|-----------------------------------------------------------------|
| product_id         | VARCHAR(20)    | Primary key. e.g. "GRC-001", "ELC-002". Set manually for now.  |
| name               | VARCHAR(100)   | Product name. e.g. "Basmati Rice 5kg"                          |
| price              | DECIMAL(10,2)  | Price in rupees                                                 |
| quantity           | INT            | Current stock count. Updated on every sale or restock.          |
| category           | VARCHAR(20)    | One of: Grocery, Electronics, Clothing, Household               |
| specific_attribute | VARCHAR(100)   | Category-specific value. See the mapping table below.           |

### What goes in specific_attribute per category

| category    | specific_attribute value           | Example       |
|-------------|------------------------------------|---------------|
| Grocery     | Expiry date in YYYY-MM-DD format   | "2026-12-31"  |
| Electronics | Warranty period in months          | "24"          |
| Clothing    | Clothing size                      | "L"           |
| Household   | Material type                      | "Stainless Steel" |

### Sample data

```sql
INSERT INTO products VALUES ('GRC-001', 'Basmati Rice 5kg',    349.00, 150, 'Grocery',     '2026-12-31');
INSERT INTO products VALUES ('GRC-002', 'Amul Full Cream Milk', 68.00,  80, 'Grocery',     '2026-04-30');
INSERT INTO products VALUES ('ELC-001', 'Havells Ceiling Fan', 2499.00, 30, 'Electronics', '24');
INSERT INTO products VALUES ('ELC-002', 'Philips LED Bulb 9W',  149.00,  8, 'Electronics', '12');
INSERT INTO products VALUES ('CLO-001', 'Mens Formal Shirt',    799.00, 45, 'Clothing',    'L');
INSERT INTO products VALUES ('CLO-002', 'Kids T-Shirt Blue',    299.00, 25, 'Clothing',    'S');
INSERT INTO products VALUES ('HSD-001', 'Prestige Cooker 5L',  1899.00, 20, 'Household',  'Stainless Steel');
INSERT INTO products VALUES ('HSD-002', 'Borosil Glass Set',    649.00,  7, 'Household',  'Borosilicate Glass');
```

---

## Table 2: sales_history

Stores a record every time a product is sold. This is what the forecasting
module reads to predict demand. The Java layer calls sellProduct() and then
the JDBC layer inserts a row here.

```sql
CREATE TABLE sales_history (
    sale_id         INT            PRIMARY KEY AUTO_INCREMENT,
    product_id      VARCHAR(20)    NOT NULL,
    quantity_sold   INT            NOT NULL,
    sale_date       DATE           NOT NULL DEFAULT (CURRENT_DATE),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);
```

| Column       | Type        | Description                                          |
|--------------|-------------|------------------------------------------------------|
| sale_id      | INT         | Auto-incremented primary key. No need to set manually. |
| product_id   | VARCHAR(20) | Foreign key linking to products.product_id           |
| quantity_sold| INT         | How many units were sold in this transaction         |
| sale_date    | DATE        | Date the sale happened. Default is today's date.     |

### Sample data

```sql
INSERT INTO sales_history (product_id, quantity_sold, sale_date) VALUES ('GRC-001', 50, '2026-04-01');
INSERT INTO sales_history (product_id, quantity_sold, sale_date) VALUES ('GRC-001', 30, '2026-04-10');
INSERT INTO sales_history (product_id, quantity_sold, sale_date) VALUES ('ELC-001',  5, '2026-04-05');
INSERT INTO sales_history (product_id, quantity_sold, sale_date) VALUES ('CLO-001', 20, '2026-04-15');
```

---

## Queries the JDBC layer will need

These are the main SQL operations. The Java side handles the logic,
the JDBC side just needs to run these at the right moments.

```sql
-- Load all products from DB into InventoryManager on startup
SELECT product_id, name, price, quantity, category, specific_attribute FROM products;

-- After restockProduct() succeeds in Java
UPDATE products SET quantity = ? WHERE product_id = ?;

-- After recordSale() succeeds in Java (run both of these together)
UPDATE products SET quantity = ? WHERE product_id = ?;
INSERT INTO sales_history (product_id, quantity_sold, sale_date) VALUES (?, ?, NOW());

-- Add a new product
INSERT INTO products (product_id, name, price, quantity, category, specific_attribute)
VALUES (?, ?, ?, ?, ?, ?);

-- Remove a product
DELETE FROM products WHERE product_id = ?;

-- Get total units sold for a product in the last N days (used for demand forecasting)
SELECT SUM(quantity_sold)
FROM sales_history
WHERE product_id = ? AND sale_date >= DATE_SUB(NOW(), INTERVAL ? DAY);
```

---

## Notes for the team

- The `category` column must exactly match one of: Grocery, Electronics, Clothing, Household.
  The Java code uses this string to decide which subclass object to create.

- The `specific_attribute` column always stores a string. The Java layer handles
  converting it to the right type (e.g. LocalDate for Grocery, int for Electronics).

- Do not change `product_id` format without updating the Java side too.
  Current format is [PREFIX]-[NUMBER] e.g. GRC-001, ELC-002.

- The forecasting module only reads from sales_history. It never writes to it.
  Only sellProduct() triggers a new insert into sales_history.
