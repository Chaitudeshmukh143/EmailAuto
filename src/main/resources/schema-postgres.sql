create table if not exists users (
    id bigserial primary key,
    email varchar(320) not null unique,
    encrypted_access_token text not null,
    encrypted_refresh_token text,
    access_token_expires_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists campaigns (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    subject varchar(255) not null,
    sent_count integer not null default 0,
    failed_count integer not null default 0,
    created_at timestamptz not null default now()
);

create table if not exists emails (
    id bigserial primary key,
    campaign_id bigint not null references campaigns(id) on delete cascade,
    recipient_email varchar(320) not null,
    recipient_name varchar(255),
    company varchar(255),
    success boolean not null,
    error_message text,
    created_at timestamptz not null default now()
);

create index if not exists idx_campaigns_user_created_at on campaigns(user_id, created_at desc);
create index if not exists idx_emails_campaign_created_at on emails(campaign_id, created_at desc);
create index if not exists idx_emails_success_created_at on emails(success, created_at desc);
