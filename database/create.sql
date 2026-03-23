-- Sequences
CREATE SEQUENCE physical_stop_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE route_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE line_version_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE operating_period_seq START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE journey_seq START WITH 1 INCREMENT BY 20;

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
    external_id TEXT NOT NULL UNIQUE,
    point_sequence GEOGRAPHY(LineString, 4326) NOT NULL,
    total_distance DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_route_point_sequence ON route USING GIST(point_sequence);

CREATE TABLE line_version (
    relational_id BIGINT PRIMARY KEY DEFAULT nextval('line_version_seq'),
    external_id TEXT NOT NULL,
    public_code TEXT NOT NULL,
    name TEXT NOT NULL,
    short_name TEXT NOT NULL,
    transport_mode TEXT NOT NULL,
    is_detour BOOLEAN NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE NOT NULL,
    active_from TIMESTAMP WITH TIME ZONE,
    active_to TIMESTAMP WITH TIME ZONE,
    UNIQUE(external_id, valid_from, valid_to, is_detour)
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

CREATE TABLE journey (
    relational_id BIGINT PRIMARY KEY DEFAULT nextval('journey_seq'),
    external_id TEXT NOT NULL,
    journey_pattern_id TEXT NOT NULL,
    line_version_id BIGINT NOT NULL,
    route_id BIGINT,
    next_day_first_stop_index INTEGER,
    operating_period_id BIGINT NOT NULL,
    begin_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone TEXT NOT NULL,
    UNIQUE(line_version_id, external_id),
    FOREIGN KEY (line_version_id) REFERENCES line_version(relational_id) ON DELETE CASCADE,
    FOREIGN KEY (route_id) REFERENCES route(relational_id) ON DELETE SET NULL,
    FOREIGN KEY (operating_period_id) REFERENCES operating_period(relational_id)
);

CREATE TABLE route_stop (
    route_id BIGINT NOT NULL,
    stop_order INTEGER NOT NULL,
    physical_stop_id BIGINT NOT NULL,
    point_sequence_index INTEGER NOT NULL,
    distance_to_next_stop DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (route_id, stop_order),
    FOREIGN KEY (route_id) REFERENCES route(relational_id) ON DELETE CASCADE,
    FOREIGN KEY (physical_stop_id) REFERENCES physical_stop(relational_id)
);

CREATE TABLE scheduled_stop (
    journey_id BIGINT NOT NULL,
    stop_order INTEGER NOT NULL,
    name TEXT NOT NULL,
    stop_on_request BOOLEAN NOT NULL,
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
