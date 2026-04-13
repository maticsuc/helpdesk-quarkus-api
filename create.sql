-- HelpDesk API — schema creation script
-- Run once against a fresh database before starting the application.

-- Sequences
CREATE SEQUENCE conversations_id_seq;
CREATE SEQUENCE messages_id_seq;
CREATE SEQUENCE operators_id_seq;
CREATE SEQUENCE users_id_seq;

-- Tables
CREATE TABLE users (
    id            BIGINT       NOT NULL,
    username      VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE operators (
    id            BIGINT       NOT NULL,
    username      VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE conversations (
    id          BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    operator_id BIGINT,
    status      VARCHAR(255) NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'CLOSED')),
    room        VARCHAR(255) NOT NULL CHECK (room IN ('TEHNIKA', 'STORITVE', 'POGOVOR')),
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE messages (
    id              BIGINT       NOT NULL,
    conversation_id BIGINT       NOT NULL,
    sender_id       BIGINT       NOT NULL,
    sender_type     VARCHAR(255) NOT NULL CHECK (sender_type IN ('USER', 'OPERATOR')),
    content         TEXT         NOT NULL,
    sent_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id)
);

-- Foreign keys
ALTER TABLE conversations ADD CONSTRAINT fk_conversations_user     FOREIGN KEY (user_id)         REFERENCES users;
ALTER TABLE conversations ADD CONSTRAINT fk_conversations_operator FOREIGN KEY (operator_id)     REFERENCES operators;
ALTER TABLE messages      ADD CONSTRAINT fk_messages_conversation  FOREIGN KEY (conversation_id) REFERENCES conversations;

-- Seed data (dev/staging only — omit in production)
INSERT INTO users (id, username, password_hash) VALUES
    (1, 'janez', '$2a$10$TDAVq9rb/hk9cJZCrscxp.5bjoWXrgRh0hNVfPqE8/ngpjOwncY3W'),
    (2, 'ana',   '$2a$10$uQTVMF4VNHr6kTi5m8wDv.WL..zfsibd0c2pURhuZ9hbPHRI5/.r2');

INSERT INTO operators (id, username, password_hash, email) VALUES
    (1, 'operator1', '$2a$10$WSivVDdTvbt2vExl5PG/.eKQ2dKk4xNer7a.GLzCmJ8gfhU8c8ZcO', 'op1@helpdesk.si'),
    (2, 'operator2', '$2a$10$eqBoy/jYcX2EIJg/DxWYT.dkbtrO/Nt22x/ec1l7sZ8Yrk030.S5y', 'op2@helpdesk.si');

ALTER SEQUENCE users_id_seq     RESTART WITH 10;
ALTER SEQUENCE operators_id_seq RESTART WITH 10;
