-- H2 compatible syntax for altering transaction nullable fields

ALTER TABLE transactions 
ALTER COLUMN receiver_id SET NULL;

ALTER TABLE transactions 
ALTER COLUMN destination_wallet_id SET NULL;
