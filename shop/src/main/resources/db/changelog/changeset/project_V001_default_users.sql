ALTER TABLE users ADD COLUMN password TEXT;

ALTER TABLE users ADD COLUMN role TEXT;

INSERT INTO users (id, name, password, role)
VALUES (1, 'ADMIN', '$2a$12$1.GS8r9/.G8PscCAVdW/OOUoZflA4SFcmXQfUuJyv/0hYrPscbyVe', 'ADMIN');

INSERT INTO users (id, name, password, role)
VALUES (2, 'John Doe', '$2a$12$1.GS8r9/.G8PscCAVdW/OOUoZflA4SFcmXQfUuJyv/0hYrPscbyVe', 'CUSTOMER');
