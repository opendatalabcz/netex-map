CREATE EXTENSION IF NOT EXISTS postgis;

-- Sequences
CREATE SEQUENCE physical_stop_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE route_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE line_version_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE operating_period_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE journey_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE stop_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE operator_seq START WITH 1 INCREMENT BY 20;

-- Tables
CREATE TABLE physical_stop (
    relational_id BIGINT PRIMARY KEY DEFAULT nextval('physical_stop_seq'),
    external_id TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    position GEOGRAPHY(Point, 4326) NOT NULL,
    tags JSONB NOT NULL
);

CREATE TABLE route (
    relational_id BIGINT PRIMARY KEY DEFAULT nextval('route_seq'),
    external_id TEXT NOT NULL,
    point_sequence GEOGRAPHY(LineString, 4326) NOT NULL,
    total_distance DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_route_external_id ON route(external_id);
CREATE INDEX idx_route_point_sequence ON route USING GIST(point_sequence);

CREATE TABLE operator (
    relational_id BIGINT PRIMARY KEY DEFAULT nextval('operator_seq'),
    public_code TEXT NOT NULL UNIQUE,
    legal_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT NOT NULL,
    url TEXT NOT NULL,
    address_line TEXT NOT NULL
);

CREATE TABLE line_version (
    relational_id BIGINT PRIMARY KEY DEFAULT nextval('line_version_seq'),
    public_code TEXT NOT NULL,
    name TEXT NOT NULL,
    short_name TEXT NOT NULL,
    transport_mode TEXT NOT NULL,
    line_type TEXT NOT NULL,
    is_detour BOOLEAN NOT NULL,
    operator_id BIGINT NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(public_code, valid_from, valid_to, is_detour),
    FOREIGN KEY (operator_id) REFERENCES operator(relational_id)
);

CREATE TABLE active_period (
    line_version_id BIGINT NOT NULL,
    from_date TIMESTAMP WITH TIME ZONE NOT NULL,
    to_date TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (line_version_id, from_date) INCLUDE (to_date),
    FOREIGN KEY (line_version_id) REFERENCES line_version(relational_id) ON DELETE CASCADE
);

CREATE INDEX idx_active_period_dates ON active_period(from_date, to_date);

CREATE TABLE operating_period (
    relational_id BIGINT PRIMARY KEY DEFAULT nextval('operating_period_seq'),
    from_date TIMESTAMP NOT NULL,
    to_date TIMESTAMP NOT NULL,
    valid_days BOOLEAN[] NOT NULL
);

CREATE INDEX idx_operating_period_dates ON operating_period(from_date, to_date);

CREATE TABLE stop (
    relational_id BIGINT PRIMARY KEY DEFAULT nextval('stop_seq'),
    name TEXT NOT NULL,
    line_public_code TEXT NOT NULL,
    bistro BOOLEAN NOT NULL,
    border_crossing BOOLEAN NOT NULL,
    displays_for_visually_impaired BOOLEAN NOT NULL,
    low_floor_access BOOLEAN NOT NULL,
    park_and_ride_park BOOLEAN NOT NULL,
    suitable_for_heavily_disabled BOOLEAN NOT NULL,
    toilet BOOLEAN NOT NULL,
    wheel_chair_access_toilet BOOLEAN NOT NULL,
    other_transport_modes TEXT,
    UNIQUE(line_public_code, name)
);

CREATE TABLE tariff_stop (
    line_version_id BIGINT NOT NULL,
    tariff_order INT NOT NULL,
    tariff_zone TEXT,
    stop_id BIGINT NOT NULL,
    PRIMARY KEY (line_version_id, tariff_order),
    FOREIGN KEY (line_version_id) REFERENCES line_version(relational_id) ON DELETE CASCADE,
    FOREIGN KEY (stop_id) REFERENCES stop(relational_id)
);

CREATE TABLE journey_pattern (
    line_version_id BIGINT NOT NULL,
    pattern_number INT NOT NULL,
    direction TEXT NOT NULL,
    route_id BIGINT,
    PRIMARY KEY (line_version_id, pattern_number),
    FOREIGN KEY (line_version_id) REFERENCES line_version(relational_id) ON DELETE CASCADE,
    FOREIGN KEY (route_id) REFERENCES route(relational_id) ON DELETE SET NULL
);

CREATE TABLE journey_pattern_stop (
    line_version_id BIGINT NOT NULL,
    pattern_number INT NOT NULL,
    stop_order INT NOT NULL,
    distance_to_next_stop DOUBLE PRECISION NOT NULL,
    tariff_order INT NOT NULL,
    for_boarding BOOLEAN NOT NULL,
    for_alighting BOOLEAN NOT NULL,
    requires_ordering BOOLEAN NOT NULL,
    stop_on_request BOOLEAN NOT NULL,
    PRIMARY KEY (line_version_id, pattern_number, stop_order),
    FOREIGN KEY (line_version_id, tariff_order) REFERENCES tariff_stop(line_version_id, tariff_order),
    FOREIGN KEY (line_version_id, pattern_number) REFERENCES journey_pattern(line_version_id, pattern_number) ON DELETE CASCADE
);

CREATE TABLE within_region_transport_ban (
    line_version_id BIGINT NOT NULL,
    pattern_number INT NOT NULL,
    stop_order INT NOT NULL,
    ban_group_number INT NOT NULL,
    PRIMARY KEY (line_version_id, pattern_number, stop_order, ban_group_number),
    FOREIGN KEY (line_version_id, pattern_number, stop_order) REFERENCES journey_pattern_stop(line_version_id, pattern_number, stop_order) ON DELETE CASCADE
);

CREATE TABLE journey (
    relational_id BIGINT PRIMARY KEY DEFAULT nextval('journey_seq'),
    journey_number TEXT NOT NULL,
    line_version_id BIGINT NOT NULL,
    operating_period_id BIGINT NOT NULL,
    pattern_number INT NOT NULL,
    requires_ordering BOOLEAN NOT NULL,
    baggage_storage BOOLEAN NOT NULL,
    cycles_allowed BOOLEAN NOT NULL,
    low_floor_access BOOLEAN NOT NULL,
    reservation_compulsory BOOLEAN NOT NULL,
    reservation_possible BOOLEAN NOT NULL,
    snacks_on_board BOOLEAN NOT NULL,
    unaccompanied_minor_assistance BOOLEAN NOT NULL,
    next_day_first_stop_index INTEGER,
    begin_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone TEXT NOT NULL,
    UNIQUE(line_version_id, journey_number),
    FOREIGN KEY (operating_period_id) REFERENCES operating_period(relational_id),
    FOREIGN KEY (line_version_id, pattern_number) REFERENCES journey_pattern(line_version_id, pattern_number) ON DELETE CASCADE
);

CREATE TABLE route_stop (
    route_id BIGINT NOT NULL,
    stop_order INTEGER NOT NULL,
    physical_stop_id BIGINT NOT NULL,
    route_fraction DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (route_id, stop_order),
    FOREIGN KEY (route_id) REFERENCES route(relational_id) ON DELETE CASCADE,
    FOREIGN KEY (physical_stop_id) REFERENCES physical_stop(relational_id)
);

CREATE TABLE scheduled_stop (
    journey_id BIGINT NOT NULL,
    stop_order INTEGER NOT NULL,
    arrival TIME,
    departure TIME,
    PRIMARY KEY (journey_id, stop_order),
    FOREIGN KEY (journey_id) REFERENCES journey(relational_id) ON DELETE CASCADE
);

-- Link sequences to their columns
ALTER SEQUENCE physical_stop_seq OWNED BY physical_stop.relational_id;
ALTER SEQUENCE route_seq OWNED BY route.relational_id;
ALTER SEQUENCE line_version_seq OWNED BY line_version.relational_id;
ALTER SEQUENCE operating_period_seq OWNED BY operating_period.relational_id;
ALTER SEQUENCE journey_seq OWNED BY journey.relational_id;
ALTER SEQUENCE stop_seq OWNED BY stop.relational_id;
ALTER SEQUENCE operator_seq OWNED BY operator.relational_id;
