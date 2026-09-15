CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          subcategory_id BIGINT NOT NULL REFERENCES subcategories(id) ON DELETE CASCADE,
                          name VARCHAR(150) NOT NULL,
                          title VARCHAR(200) NOT NULL,
                          description TEXT,
                          image_url VARCHAR(500),
                          price NUMERIC(10,2) NOT NULL,
                          created_at TIMESTAMP NOT NULL DEFAULT now()
);