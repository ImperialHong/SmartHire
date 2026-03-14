-- SmartHire demo seed data / SmartHire 演示种子数据
-- Target: MySQL 8.0+ / 目标版本：MySQL 8.0+
-- Purpose: create reusable demo accounts, jobs, applications, interviews and notifications
-- 用途：初始化可重复执行的演示账号、岗位、投递、面试和通知数据
-- Demo password / 演示密码：password123
-- Safe to rerun after 001_init_schema.sql / 在执行 001_init_schema.sql 后可重复运行

USE smarthire;

SET NAMES utf8mb4;

SET @demo_password_hash = '$2y$10$UAPEr0pYEkIQbnHMjHfHHujB3c/Z.FoWET3yp/s47zXory/8K8MAO';

SET @candidate_email = 'candidate@example.com';
SET @candidate_two_email = 'candidate2@example.com';
SET @hr_email = 'hr@example.com';
SET @admin_email = 'admin@example.com';

DELETE FROM notifications
WHERE recipient_user_id IN (
  SELECT id
  FROM users
  WHERE email IN (@candidate_email, @candidate_two_email, @hr_email, @admin_email)
);

DELETE i
FROM interviews i
JOIN applications a ON a.id = i.application_id
JOIN jobs j ON j.id = a.job_id
JOIN users u ON u.id = j.created_by
WHERE u.email = @hr_email;

DELETE a
FROM applications a
JOIN jobs j ON j.id = a.job_id
JOIN users u ON u.id = j.created_by
WHERE u.email = @hr_email;

DELETE ur
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
WHERE u.email IN (@candidate_email, @candidate_two_email, @hr_email, @admin_email);

DELETE j
FROM jobs j
JOIN users u ON u.id = j.created_by
WHERE u.email = @hr_email;

DELETE FROM users
WHERE email IN (@candidate_email, @candidate_two_email, @hr_email, @admin_email);

INSERT INTO users (
  email,
  password_hash,
  full_name,
  phone,
  status,
  last_login_at,
  created_at,
  updated_at
)
VALUES
  (
    @candidate_email,
    @demo_password_hash,
    'Candidate User',
    '13800138000',
    'ACTIVE',
    '2026-03-15 09:05:00',
    '2026-03-14 09:00:00',
    '2026-03-15 09:05:00'
  ),
  (
    @candidate_two_email,
    @demo_password_hash,
    'Second Candidate',
    '13800138001',
    'ACTIVE',
    '2026-03-15 09:15:00',
    '2026-03-14 09:10:00',
    '2026-03-15 09:15:00'
  ),
  (
    @hr_email,
    @demo_password_hash,
    'Hiring HR',
    '13800138002',
    'ACTIVE',
    '2026-03-15 09:20:00',
    '2026-03-14 08:50:00',
    '2026-03-15 09:20:00'
  ),
  (
    @admin_email,
    @demo_password_hash,
    'System Admin',
    '13800138003',
    'ACTIVE',
    '2026-03-15 09:25:00',
    '2026-03-14 08:40:00',
    '2026-03-15 09:25:00'
  );

INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, '2026-03-14 09:30:00'
FROM users u
JOIN roles r ON r.code = 'CANDIDATE'
WHERE u.email IN (@candidate_email, @candidate_two_email);

INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, '2026-03-14 09:31:00'
FROM users u
JOIN roles r ON r.code = 'HR'
WHERE u.email = @hr_email;

INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, '2026-03-14 09:32:00'
FROM users u
JOIN roles r ON r.code = 'ADMIN'
WHERE u.email = @admin_email;

INSERT INTO jobs (
  created_by,
  title,
  description,
  city,
  category,
  employment_type,
  experience_level,
  salary_min,
  salary_max,
  status,
  application_deadline,
  created_at,
  updated_at
)
VALUES
  (
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    'Backend Engineer',
    'Build SmartHire backend services with Spring Boot, MySQL and JWT based security.',
    'Shanghai',
    'Engineering',
    'FULL_TIME',
    'JUNIOR',
    18000.00,
    28000.00,
    'OPEN',
    '2026-12-31 23:59:59',
    '2026-03-14 10:00:00',
    '2026-03-15 10:00:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    'Frontend Engineer',
    'Build candidate and recruiter workflows with React, routing and API integration.',
    'Hangzhou',
    'Engineering',
    'FULL_TIME',
    'JUNIOR',
    16000.00,
    24000.00,
    'OPEN',
    '2026-11-30 23:59:59',
    '2026-03-14 10:05:00',
    '2026-03-15 10:05:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    'Java Intern',
    'Support test automation, API development and data maintenance for internal systems.',
    'Nanjing',
    'Internship',
    'INTERNSHIP',
    'ENTRY',
    3000.00,
    5000.00,
    'CLOSED',
    '2026-05-31 23:59:59',
    '2026-03-14 10:10:00',
    '2026-03-15 10:10:00'
  );

INSERT INTO applications (
  job_id,
  candidate_id,
  status,
  resume_file_path,
  cover_letter,
  hr_note,
  applied_at,
  status_updated_at,
  last_updated_by,
  created_at,
  updated_at
)
VALUES
  (
    (
      SELECT id
      FROM jobs
      WHERE title = 'Backend Engineer'
        AND created_by = (SELECT id FROM users WHERE email = @hr_email LIMIT 1)
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @candidate_email LIMIT 1),
    'INTERVIEW',
    '/resume/candidate-user-backend.pdf',
    'I have hands-on experience with Spring Boot, JWT and MySQL projects.',
    'Strong backend fundamentals, proceed to interview.',
    '2026-03-14 11:00:00',
    '2026-03-15 09:40:00',
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    '2026-03-14 11:00:00',
    '2026-03-15 09:40:00'
  ),
  (
    (
      SELECT id
      FROM jobs
      WHERE title = 'Backend Engineer'
        AND created_by = (SELECT id FROM users WHERE email = @hr_email LIMIT 1)
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @candidate_two_email LIMIT 1),
    'REVIEWING',
    '/resume/second-candidate-backend.pdf',
    'I am interested in backend infrastructure and API development.',
    'Resume is under review.',
    '2026-03-14 12:30:00',
    '2026-03-15 08:50:00',
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    '2026-03-14 12:30:00',
    '2026-03-15 08:50:00'
  ),
  (
    (
      SELECT id
      FROM jobs
      WHERE title = 'Frontend Engineer'
        AND created_by = (SELECT id FROM users WHERE email = @hr_email LIMIT 1)
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @candidate_email LIMIT 1),
    'APPLIED',
    '/resume/candidate-user-frontend.pdf',
    'I can also collaborate well on front-end and API integration tasks.',
    NULL,
    '2026-03-15 08:10:00',
    '2026-03-15 08:10:00',
    NULL,
    '2026-03-15 08:10:00',
    '2026-03-15 08:10:00'
  );

INSERT INTO interviews (
  application_id,
  scheduled_by,
  interview_at,
  location,
  meeting_link,
  status,
  result,
  remark,
  created_at,
  updated_at
)
VALUES
  (
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'Backend Engineer'
        AND c.email = @candidate_email
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    '2026-03-20 15:00:00',
    'Meeting Room A',
    'https://meet.example.com/smarthire-backend',
    'SCHEDULED',
    'PENDING',
    'Please prepare a 5-minute project walkthrough.',
    '2026-03-15 09:40:00',
    '2026-03-15 09:40:00'
  );

INSERT INTO notifications (
  recipient_user_id,
  type,
  title,
  content,
  related_type,
  related_id,
  is_read,
  read_at,
  created_at,
  updated_at
)
VALUES
  (
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    'APPLICATION_SUBMITTED',
    'New application received',
    'Candidate User applied for Backend Engineer.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'Backend Engineer'
        AND c.email = @candidate_email
      LIMIT 1
    ),
    0,
    NULL,
    '2026-03-14 11:00:00',
    '2026-03-14 11:00:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    'APPLICATION_SUBMITTED',
    'New application received',
    'Second Candidate applied for Backend Engineer.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'Backend Engineer'
        AND c.email = @candidate_two_email
      LIMIT 1
    ),
    1,
    '2026-03-14 12:35:00',
    '2026-03-14 12:30:00',
    '2026-03-14 12:35:00'
  ),
  (
    (SELECT id FROM users WHERE email = @candidate_email LIMIT 1),
    'APPLICATION_STATUS_CHANGED',
    'Application status updated',
    'Your application for Backend Engineer is now INTERVIEW.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'Backend Engineer'
        AND c.email = @candidate_email
      LIMIT 1
    ),
    1,
    '2026-03-15 09:41:00',
    '2026-03-15 09:40:00',
    '2026-03-15 09:41:00'
  ),
  (
    (SELECT id FROM users WHERE email = @candidate_email LIMIT 1),
    'INTERVIEW_SCHEDULED',
    'Interview scheduled',
    'Your interview for Backend Engineer is scheduled at 2026-03-20 15:00:00.',
    'INTERVIEW',
    (
      SELECT i.id
      FROM interviews i
      JOIN applications a ON a.id = i.application_id
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'Backend Engineer'
        AND c.email = @candidate_email
      LIMIT 1
    ),
    0,
    NULL,
    '2026-03-15 09:40:00',
    '2026-03-15 09:40:00'
  ),
  (
    (SELECT id FROM users WHERE email = @candidate_two_email LIMIT 1),
    'APPLICATION_STATUS_CHANGED',
    'Application status updated',
    'Your application for Backend Engineer is now REVIEWING.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'Backend Engineer'
        AND c.email = @candidate_two_email
      LIMIT 1
    ),
    0,
    NULL,
    '2026-03-15 08:50:00',
    '2026-03-15 08:50:00'
  ),
  (
    (SELECT id FROM users WHERE email = @admin_email LIMIT 1),
    'SYSTEM',
    'Demo environment ready',
    'SmartHire demo data has been loaded successfully.',
    'SYSTEM',
    NULL,
    0,
    NULL,
    '2026-03-15 09:30:00',
    '2026-03-15 09:30:00'
  );
