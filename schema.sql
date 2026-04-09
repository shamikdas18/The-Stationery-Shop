-- ============================================================
--  Stationery Shop Inventory System - Database Schema
--  Database : stationery_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS stationery_db;
USE stationery_db;

-- ------------------------------------------------------------
--  Table: products
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    prod_id   INT            NOT NULL AUTO_INCREMENT,
    name      VARCHAR(150)   NOT NULL,
    category  VARCHAR(100)   NOT NULL,
    stock     INT            NOT NULL DEFAULT 0,
    price     DECIMAL(10, 2) NOT NULL,   -- selling price per unit
    cost      DECIMAL(10, 2) NOT NULL,   -- purchase cost per unit
    PRIMARY KEY (prod_id)
);

-- ------------------------------------------------------------
--  Table: sales
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sales (
    sale_id   INT      NOT NULL AUTO_INCREMENT,
    prod_id   INT      NOT NULL,
    qty_sold  INT      NOT NULL,
    sale_time DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (sale_id),
    CONSTRAINT fk_sales_product FOREIGN KEY (prod_id)
        REFERENCES products (prod_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);
