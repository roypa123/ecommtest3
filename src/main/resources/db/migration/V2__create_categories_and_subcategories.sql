CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(150) NOT NULL UNIQUE,
                            image_url VARCHAR(500),
                            created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE subcategories (
                               id BIGSERIAL PRIMARY KEY,
                               category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE CASCADE,
                               name VARCHAR(150) NOT NULL,
                               image_url VARCHAR(500),
                               created_at TIMESTAMP NOT NULL DEFAULT now(),
                               UNIQUE(category_id, name)
);
