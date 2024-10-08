CREATE TABLE pe_logs (
    log_id SERIAL PRIMARY KEY,                  
    pe_id VARCHAR(100) REFERENCES passport_entity(pe_id), 
    log_details TEXT NOT NULL,                
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
);


CREATE TABLE pe_datasheet (
    datasheet_id SERIAL PRIMARY KEY,
    template_entry JSON NOT NULL,
    status VARCHAR(50) NOT NULL,
    data_category VARCHAR(100),
    created_by VARCHAR(255) NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE passport_entity (
    pe_id VARCHAR(100) PRIMARY KEY,              
    pe_name VARCHAR(255) NOT NULL,                
    status VARCHAR(50) NOT NULL,                
    parent_pe VARCHAR(100)[] DEFAULT '{}',     
    created_by VARCHAR(255) NOT NULL,           
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP 
);


CREATE TABLE pe_ds_mapping (
    pe_id VARCHAR(100) REFERENCES passport_entity(pe_id) ON DELETE CASCADE,
    datasheet_id INT REFERENCES pe_datasheet(datasheet_id) ON DELETE CASCADE,
    PRIMARY KEY (pe_id, datasheet_id)  
);


CREATE SEQUENCE passport_entity_sequence;


CREATE OR REPLACE FUNCTION pe_custom_id()
RETURNS TRIGGER AS $$
DECLARE
    pe_id_prefix TEXT;
    new_id TEXT;
BEGIN
    SELECT hashvalue INTO pe_id_prefix FROM oc_config limit 1 ;

    new_id := pe_id_prefix || '-' || NEXTVAL('passport_entity_sequence');

    NEW.passport_entity_id := new_id;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;




CREATE TRIGGER before_insert_pe_id
BEFORE INSERT ON passport_entity
FOR EACH ROW
EXECUTE FUNCTION pe_custom_id();



