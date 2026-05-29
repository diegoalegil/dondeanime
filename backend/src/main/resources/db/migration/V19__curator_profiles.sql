CREATE TABLE curator_profile (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(180) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    approved BOOLEAN NOT NULL DEFAULT TRUE,
    approved_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_curator_profile_email UNIQUE (email)
);

CREATE INDEX idx_curator_profile_approved ON curator_profile (approved);
