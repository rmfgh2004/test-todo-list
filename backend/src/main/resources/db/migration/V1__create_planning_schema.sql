CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    priority VARCHAR(16) NOT NULL,
    estimate_minutes INTEGER NOT NULL,
    due_date DATE,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_task_priority CHECK (
        priority = 'LOW' OR priority = 'MEDIUM' OR priority = 'HIGH'
    ),
    CONSTRAINT ck_task_estimate CHECK (
        estimate_minutes BETWEEN 15 AND 840 AND MOD(estimate_minutes, 15) = 0
    ),
    CONSTRAINT ck_task_status CHECK (status = 'TODO' OR status = 'COMPLETED')
);

CREATE TABLE schedule_slots (
    task_id UUID PRIMARY KEY,
    schedule_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT fk_schedule_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT ck_schedule_order CHECK (start_time < end_time),
    CONSTRAINT ck_schedule_window CHECK (start_time >= TIME '08:00:00' AND end_time <= TIME '22:00:00')
);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL,
    action VARCHAR(24) NOT NULL,
    actor VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    changed_fields VARCHAR(512) NOT NULL
);

CREATE INDEX idx_tasks_backlog
    ON tasks(status, due_date, priority, created_at, id);
CREATE INDEX idx_schedule_range
    ON schedule_slots(schedule_date, start_time, end_time, task_id);
CREATE INDEX idx_audit_task_time
    ON audit_events(task_id, occurred_at, id);
