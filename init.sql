
CREATE TABLE IF NOT EXISTS datasheets
(
    id SERIAL PRIMARY KEY,
    data JSON NOT NULL,
    dictionary VARCHAR(100),
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS passports
(
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    parent_id VARCHAR(100) ,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS passport_datasheet_mappings
(
    id SERIAL PRIMARY KEY,
    passport_id VARCHAR(100) REFERENCES passports(id) ON DELETE CASCADE,
    datasheet_id INT REFERENCES datasheets(id) ON DELETE CASCADE
    );



 CREATE TABLE IF NOT EXISTS passport_templates
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    template JSON NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users
(
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    password VARCHAR(500) NOT NULL,
    role VARCHAR(25) NOT NULL,
    is_active BOOLEAN NOT NULL,
    refresh_token VARCHAR(100),
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS passport_logs
(
    id SERIAL PRIMARY KEY,
    passport_id VARCHAR(100) NOT NULL,
    data JSON NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passport_id) REFERENCES passports(id)
);

CREATE TABLE IF NOT EXISTS passport_lifecycles
(
    id SERIAL PRIMARY KEY,
    passport_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    data JSON NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (passport_id) REFERENCES passports(id)
);


