CREATE TABLE accounts (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT NOT NULL,
                          balance NUMERIC(10, 2) NOT NULL
)
