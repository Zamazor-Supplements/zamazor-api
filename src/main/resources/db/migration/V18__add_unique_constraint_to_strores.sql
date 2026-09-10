ALTER TABLE stores
    ADD CONSTRAINT uc_stores_name UNIQUE (name);