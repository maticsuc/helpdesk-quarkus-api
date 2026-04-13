-- Seed data for HelpDesk API
-- Password for all accounts: password123

-- Users
INSERT INTO users (id, username, password_hash) VALUES
  (1, 'janez', '$2a$10$TDAVq9rb/hk9cJZCrscxp.5bjoWXrgRh0hNVfPqE8/ngpjOwncY3W'),
  (2, 'ana',   '$2a$10$uQTVMF4VNHr6kTi5m8wDv.WL..zfsibd0c2pURhuZ9hbPHRI5/.r2');

-- Operators
INSERT INTO operators (id, username, password_hash, email) VALUES
  (1, 'operator1', '$2a$10$WSivVDdTvbt2vExl5PG/.eKQ2dKk4xNer7a.GLzCmJ8gfhU8c8ZcO', 'op1@helpdesk.si'),
  (2, 'operator2', '$2a$10$eqBoy/jYcX2EIJg/DxWYT.dkbtrO/Nt22x/ec1l7sZ8Yrk030.S5y', 'op2@helpdesk.si');

-- Reset sequences
ALTER SEQUENCE users_id_seq RESTART WITH 10;
ALTER SEQUENCE operators_id_seq RESTART WITH 10;
