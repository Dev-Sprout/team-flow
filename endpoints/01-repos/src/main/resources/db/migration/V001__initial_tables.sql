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
    role ROLE NOT NULL,
    "position" JOB_POSITION NULL,
    password VARCHAR NULL,
    deleted_at TIMESTAMP WITH TIME ZONE NULL
);

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