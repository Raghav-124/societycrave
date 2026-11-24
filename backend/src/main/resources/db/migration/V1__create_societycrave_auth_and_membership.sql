CREATE TABLE societycrave_users (
    id uuid PRIMARY KEY,
    provider varchar(32) NOT NULL,
    provider_subject varchar(255) NOT NULL,
    email varchar(320) NOT NULL,
    display_name varchar(255),
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT uk_sc_user_provider_subject UNIQUE (provider, provider_subject)
);

CREATE TABLE societycrave_societies (
    id uuid PRIMARY KEY,
    name varchar(255) NOT NULL,
    locality varchar(255) NOT NULL,
    city varchar(255) NOT NULL,
    pin_code varchar(32) NOT NULL,
    latitude double precision,
    longitude double precision,
    geohash varchar(12)
);

CREATE INDEX idx_sc_society_search ON societycrave_societies (city, locality, name);
CREATE INDEX idx_sc_society_geohash ON societycrave_societies (geohash);

CREATE TABLE societycrave_memberships (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES societycrave_users (id),
    society_id uuid NOT NULL REFERENCES societycrave_societies (id),
    unit_number varchar(128) NOT NULL,
    status varchar(32) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT uk_sc_membership_user_society UNIQUE (user_id, society_id)
);
