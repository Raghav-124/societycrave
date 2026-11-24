CREATE TABLE societycrave_chefs (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL UNIQUE REFERENCES societycrave_users (id),
    society_id uuid NOT NULL REFERENCES societycrave_societies (id),
    display_name varchar(255) NOT NULL,
    profile_photo_url varchar(2048),
    bio varchar(2000) NOT NULL,
    cuisine_specialties varchar(1000) NOT NULL,
    food_categories varchar(1000) NOT NULL,
    pickup_instructions varchar(1000) NOT NULL,
    created_at timestamp with time zone NOT NULL
);
CREATE INDEX idx_sc_chef_society ON societycrave_chefs (society_id);

CREATE TABLE societycrave_listings (
    id uuid PRIMARY KEY,
    chef_id uuid NOT NULL REFERENCES societycrave_chefs (id),
    society_id uuid NOT NULL REFERENCES societycrave_societies (id),
    title varchar(255) NOT NULL,
    description varchar(2000) NOT NULL,
    price_paise integer NOT NULL,
    pickup_instructions varchar(1000),
    status varchar(32) NOT NULL,
    created_at timestamp with time zone NOT NULL
);
CREATE INDEX idx_sc_listing_society_status ON societycrave_listings (society_id, status);

CREATE TABLE societycrave_orders (
    id uuid PRIMARY KEY,
    resident_user_id uuid NOT NULL REFERENCES societycrave_users (id),
    chef_id uuid NOT NULL REFERENCES societycrave_chefs (id),
    society_id uuid NOT NULL REFERENCES societycrave_societies (id),
    listing_id uuid NOT NULL REFERENCES societycrave_listings (id),
    quantity integer NOT NULL,
    status varchar(32) NOT NULL,
    created_at timestamp with time zone NOT NULL
);
