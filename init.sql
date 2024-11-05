
CREATE TABLE IF NOT EXISTS pe_datasheet
(
    datasheet_id SERIAL PRIMARY KEY,
    template_entry JSON NOT NULL,
    status VARCHAR(50) NOT NULL,
    data_category VARCHAR(100),
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS passport_entity
(
    passport_entity_id VARCHAR(100) PRIMARY KEY,
    pe_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    parent_pe_id VARCHAR(100) ,
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS pe_datasheet_mapping
(
	mapping_id SERIAL PRIMARY KEY,
    passport_entity_id VARCHAR(100) REFERENCES passport_entity(passport_entity_id) ON DELETE CASCADE,
    datasheet_id INT REFERENCES pe_datasheet(datasheet_id) ON DELETE CASCADE
    );

    
    
 CREATE TABLE IF NOT EXISTS pe_template
(
    template_id SERIAL PRIMARY KEY,
    template_name VARCHAR(50) NOT NULL UNIQUE,
    extracted_template JSON NOT NULL,
    data_category VARCHAR(100),
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

    