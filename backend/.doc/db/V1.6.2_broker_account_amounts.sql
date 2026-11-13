BEGIN;

ALTER TABLE broker_account
    ADD COLUMN IF NOT EXISTS total_principal numeric(15, 2) DEFAULT 0 NOT NULL,
    ADD COLUMN IF NOT EXISTS realized_profit_loss numeric(15, 2) DEFAULT 0 NOT NULL;

COMMENT ON COLUMN broker_account.total_principal IS '总本金';
COMMENT ON COLUMN broker_account.realized_profit_loss IS '清仓盈亏，亏损为负数';

COMMIT;
