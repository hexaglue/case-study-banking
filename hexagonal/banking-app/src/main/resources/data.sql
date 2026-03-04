-- Pre-loaded demo data for Swagger UI demonstration

-- Customers
INSERT INTO customer (id, first_name, last_name, email, phone, street, city, zip_code, country)
VALUES (1, 'Alice', 'Martin', 'alice.martin@example.com', '+33612345678', '12 Rue de la Paix', 'Paris', '75002', 'FR');
INSERT INTO customer (id, first_name, last_name, email, phone, street, city, zip_code, country)
VALUES (2, 'Bob', 'Dupont', 'bob.dupont@example.com', '+33698765432', '45 Avenue des Champs', 'Lyon', '69001', 'FR');
INSERT INTO customer (id, first_name, last_name, email, phone, street, city, zip_code, country)
VALUES (3, 'Claire', 'Bernard', 'claire.bernard@example.com', '+33655443322', '8 Boulevard Victor Hugo', 'Marseille', '13001', 'FR');

-- Accounts (checking and savings)
INSERT INTO account (id, account_number, type, customer_id, active, amount, currency)
VALUES (1, 'FR7612345000010000000001', 'CHECKING', 1, true, 2500.00, 'EUR');
INSERT INTO account (id, account_number, type, customer_id, active, amount, currency)
VALUES (2, 'FR7612345000010000000002', 'SAVINGS', 1, true, 15000.00, 'EUR');
INSERT INTO account (id, account_number, type, customer_id, active, amount, currency)
VALUES (3, 'FR7612345000020000000001', 'CHECKING', 2, true, 1800.00, 'EUR');
INSERT INTO account (id, account_number, type, customer_id, active, amount, currency)
VALUES (4, 'FR7612345000030000000001', 'CHECKING', 3, true, 5200.00, 'EUR');

-- Transactions (history for existing accounts)
INSERT INTO transactions (id, type, description, reference, account_id, created_at, amount, currency)
VALUES (1, 'DEPOSIT', 'Initial deposit', 'INIT-001', 1, '2026-01-15 10:00:00', 3000.00, 'EUR');
INSERT INTO transactions (id, type, description, reference, account_id, created_at, amount, currency)
VALUES (2, 'WITHDRAWAL', 'ATM withdrawal', 'ATM-001', 1, '2026-02-01 14:30:00', -500.00, 'EUR');
INSERT INTO transactions (id, type, description, reference, account_id, created_at, amount, currency)
VALUES (3, 'DEPOSIT', 'Salary deposit', 'SAL-001', 3, '2026-01-20 09:00:00', 1800.00, 'EUR');
INSERT INTO transactions (id, type, description, reference, account_id, created_at, amount, currency)
VALUES (4, 'DEPOSIT', 'Savings transfer', 'SAV-001', 2, '2026-01-10 11:00:00', 15000.00, 'EUR');
