-- Alter Transaction table to allow NULL for receiver_id and destination_wallet_id

ALTER TABLE transactions
ALTER COLUMN receiver_id DROP NOT NULL,
ALTER COLUMN destination_wallet_id DROP NOT NULL;
