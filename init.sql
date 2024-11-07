
CREATE TABLE IF NOT EXISTS datasheet
(
    id SERIAL PRIMARY KEY,
    template_entry JSON NOT NULL,
    data_category VARCHAR(100),
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS passport_entity
(
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    parent_id VARCHAR(100) ,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS datasheet_mapping
(
	id SERIAL PRIMARY KEY,
    passport_entity_id VARCHAR(100) REFERENCES passport_entity(id) ON DELETE CASCADE,
    datasheet_id INT REFERENCES datasheet(id) ON DELETE CASCADE
    );

    
    
 CREATE TABLE IF NOT EXISTS template
(
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    template JSON NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

    
