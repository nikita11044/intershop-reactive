CREATE TABLE products (
                          id SERIAL PRIMARY KEY,
                          title VARCHAR(255) NOT NULL,
                          description TEXT,
                          img_path VARCHAR(255),
                          count INTEGER NOT NULL,
                          price NUMERIC(10, 2) NOT NULL
);

CREATE TABLE orders (
                        id SERIAL PRIMARY KEY,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
                             id SERIAL PRIMARY KEY,
                             order_id INTEGER NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
                             product_id INTEGER NOT NULL REFERENCES products(id),
                             quantity INTEGER NOT NULL CHECK (quantity > 0),
                             price_at_purchase NUMERIC(10, 2) NOT NULL
);
