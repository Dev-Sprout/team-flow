CREATE TYPE JOB_POSITION AS ENUM (
    'project_manager',
    'fullstack_developer',
    'frontend_developer',
    'backend_developer',
    'ai_engineer',
    'data_analyst',
    'data_engineer',
    'qa',
    'designer',
    'hr',
    'accountant',
    'other'
);

CREATE TYPE ROLE AS ENUM ('admin', 'manager', 'developer');

CREATE TABLE assets (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    s3_key VARCHAR NOT NULL,
    file_name VARCHAR NULL,
    content_type VARCHAR NULL
);

CREATE TABLE users (
	id UUID PRIMARY KEY,
	created_at TIMESTAMP WITH TIME ZONE NOT NULL,
	first_name VARCHAR NOT NULL,
	last_name VARCHAR NOT NULL,
	email VARCHAR NOT NULL,
	username VARCHAR NOT NULL UNIQUE,
	is_github_member BOOLEAN NOT NULL,
    role ROLE NOT NULL,
    "position" JOB_POSITION NULL,
    password VARCHAR NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL
);

INSERT INTO users (id, created_at, first_name, last_name, email, username, is_github_member, role, password) VALUES
('f43385b5-0ee6-4c8d-a0bf-6f8da8e5b907', NOW(), 'Admin', 'User', 'admin@example.com', 'admin', false, 'admin', '$s0$e0801$5JK3Ogs35C2h5htbXQoeEQ==$N7HgNieSnOajn1FuEB7l4PhC6puBSq+e1E8WUaSJcGY=');

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR NULL,
    url VARCHAR NOT NULL UNIQUE
);

CREATE TABLE agents (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    name VARCHAR NOT NULL UNIQUE,
    prompt VARCHAR NOT NULL,
    description VARCHAR NULL
)

CREATE TABLE analyses (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    project_id UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    agent_id UUID NOT NULL REFERENCES agents (id) ON DELETE CASCADE,
    response VARCHAR NOT NULL
);

CREATE TABLE user_analyses (
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    analysis_id UUID NOT NULL REFERENCES analyses (id) ON DELETE CASCADE
);
