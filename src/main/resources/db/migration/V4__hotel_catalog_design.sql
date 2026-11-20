CREATE TABLE hotels (
 id BIGSERIAL PRIMARY KEY,name VARCHAR(150) NOT NULL,address VARCHAR(255) NOT NULL,
 contact_email VARCHAR(150) NOT NULL,phone VARCHAR(20) NOT NULL,
 latitude NUMERIC(10,7),longitude NUMERIC(10,7),timezone VARCHAR(50) NOT NULL,
 check_in_time TIME NOT NULL,check_out_time TIME NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK(latitude BETWEEN -90 AND 90),CHECK(longitude BETWEEN -180 AND 180)
);
CREATE TABLE room_types (
 id BIGSERIAL PRIMARY KEY,hotel_id BIGINT NOT NULL REFERENCES hotels(id),name VARCHAR(100) NOT NULL,
 description TEXT,capacity INT NOT NULL CHECK(capacity>0),base_price NUMERIC(12,2) NOT NULL CHECK(base_price>0),
 active BOOLEAN NOT NULL DEFAULT TRUE,created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_room_types_hotel ON room_types(hotel_id,id);
CREATE TABLE amenities(id BIGSERIAL PRIMARY KEY,name VARCHAR(100) NOT NULL UNIQUE,icon VARCHAR(255),active BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE room_type_amenities(id BIGSERIAL PRIMARY KEY,room_type_id BIGINT NOT NULL REFERENCES room_types(id),amenity_id BIGINT NOT NULL REFERENCES amenities(id),UNIQUE(room_type_id,amenity_id));
CREATE TABLE hotel_services (
 id BIGSERIAL PRIMARY KEY,hotel_id BIGINT NOT NULL REFERENCES hotels(id),name VARCHAR(120) NOT NULL,
 description TEXT,unit_price NUMERIC(12,2) NOT NULL CHECK(unit_price>=0),unit VARCHAR(30) NOT NULL,
 image_url VARCHAR(500),active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_services_hotel_active ON hotel_services(hotel_id,active,id);
CREATE TABLE nearby_suggestions (
 id BIGSERIAL PRIMARY KEY,hotel_id BIGINT NOT NULL REFERENCES hotels(id),name VARCHAR(150) NOT NULL,
 category VARCHAR(50) NOT NULL,description TEXT,address VARCHAR(255),latitude NUMERIC(10,7),longitude NUMERIC(10,7),
 image_url VARCHAR(500),active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK(latitude BETWEEN -90 AND 90),CHECK(longitude BETWEEN -180 AND 180)
);
CREATE INDEX ix_suggestions_hotel_active ON nearby_suggestions(hotel_id,active,id);
-- Expansion only: never assign legacy rooms to a fabricated hotel or room type.
ALTER TABLE rooms ADD COLUMN room_type_id BIGINT REFERENCES room_types(id);
ALTER TABLE rooms ADD COLUMN floor_number INT;
CREATE INDEX ix_rooms_room_type ON rooms(room_type_id);
