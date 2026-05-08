INSERT INTO categories (id, name, description) VALUES
                                                   ('a1b2c3d4-0000-0000-0000-000000000001', 'Electronics', 'Electronic devices and components'),
                                                   ('a1b2c3d4-0000-0000-0000-000000000002', 'Office Supplies', 'Stationery and office equipment'),
                                                   ('a1b2c3d4-0000-0000-0000-000000000003', 'Furniture', 'Office and warehouse furniture');

INSERT INTO suppliers (id, name, email, phone, country, active) VALUES
                                                                    ('b1b2c3d4-0000-0000-0000-000000000001', 'TechParts SA', 'contact@techparts.com', '+52-555-0001', 'Mexico', true),
                                                                    ('b1b2c3d4-0000-0000-0000-000000000002', 'Office World', 'sales@officeworld.com', '+52-555-0002', 'Mexico', true),
                                                                    ('b1b2c3d4-0000-0000-0000-000000000003', 'Global Furniture', 'info@globalfurniture.com', '+1-800-0003', 'USA', true);

INSERT INTO products (id, sku, name, description, price, stock_current, stock_minimum, category_id, supplier_id) VALUES
                                                                                                                     ('c1b2c3d4-0000-0000-0000-000000000001', 'ELEC-001', 'Laptop Pro 15"', 'High performance laptop', 25000.00, 50, 10, 'a1b2c3d4-0000-0000-0000-000000000001', 'b1b2c3d4-0000-0000-0000-000000000001'),
                                                                                                                     ('c1b2c3d4-0000-0000-0000-000000000002', 'ELEC-002', 'Wireless Mouse', 'Ergonomic wireless mouse', 450.00, 200, 30, 'a1b2c3d4-0000-0000-0000-000000000001', 'b1b2c3d4-0000-0000-0000-000000000001'),
                                                                                                                     ('c1b2c3d4-0000-0000-0000-000000000003', 'OFFI-001', 'A4 Paper Ream', '500 sheets A4 paper', 120.00, 8, 20, 'a1b2c3d4-0000-0000-0000-000000000002', 'b1b2c3d4-0000-0000-0000-000000000002'),
                                                                                                                     ('c1b2c3d4-0000-0000-0000-000000000004', 'FURN-001', 'Executive Chair', 'Ergonomic office chair', 3500.00, 15, 5, 'a1b2c3d4-0000-0000-0000-000000000003', 'b1b2c3d4-0000-0000-0000-000000000003');