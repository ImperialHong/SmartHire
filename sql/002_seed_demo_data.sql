-- SmartHire demo seed data / SmartHire 演示种子数据
-- Target: MySQL 8.0+ / 目标版本：MySQL 8.0+
-- Purpose: create reusable demo accounts, jobs, applications, interviews and notifications
-- 用途：初始化可重复执行的演示账号、岗位、投递、面试和通知数据
-- Demo password / 演示密码：password123
-- Safe to rerun after Flyway schema migrations / 在执行 Flyway 结构迁移后可重复运行

USE smarthire;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @demo_password_hash = '$2y$10$UAPEr0pYEkIQbnHMjHfHHujB3c/Z.FoWET3yp/s47zXory/8K8MAO';

SET @candidate_email = 'candidate@example.com';
SET @candidate_two_email = 'candidate2@example.com';
SET @candidate_three_email = 'candidate3@example.com';
SET @candidate_four_email = 'candidate4@example.com';
SET @candidate_five_email = 'candidate5@example.com';
SET @hr_email = 'hr@example.com';
SET @hr_two_email = 'hr2@example.com';
SET @admin_email = 'admin@example.com';

DELETE FROM notifications
WHERE recipient_user_id IN (
  SELECT id
  FROM users
  WHERE email IN (
    @candidate_email,
    @candidate_two_email,
    @candidate_three_email,
    @candidate_four_email,
    @candidate_five_email,
    @hr_email,
    @hr_two_email,
    @admin_email
  )
);

DELETE i
FROM interviews i
JOIN applications a ON a.id = i.application_id
JOIN jobs j ON j.id = a.job_id
JOIN users u ON u.id = j.created_by
WHERE u.email IN (@hr_email, @hr_two_email);

DELETE a
FROM applications a
JOIN jobs j ON j.id = a.job_id
JOIN users u ON u.id = j.created_by
WHERE u.email IN (@hr_email, @hr_two_email);

DELETE ur
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
WHERE u.email IN (
  @candidate_email,
  @candidate_two_email,
  @candidate_three_email,
  @candidate_four_email,
  @candidate_five_email,
  @hr_email,
  @hr_two_email,
  @admin_email
);

DELETE j
FROM jobs j
JOIN users u ON u.id = j.created_by
WHERE u.email IN (@hr_email, @hr_two_email);

DELETE FROM users
WHERE email IN (
  @candidate_email,
  @candidate_two_email,
  @candidate_three_email,
  @candidate_four_email,
  @candidate_five_email,
  @hr_email,
  @hr_two_email,
  @admin_email
);

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
    @candidate_three_email,
    @demo_password_hash,
    'Third Candidate',
    '13800138004',
    'ACTIVE',
    '2026-03-15 09:18:00',
    '2026-03-14 09:12:00',
    '2026-03-15 09:18:00'
  ),
  (
    @candidate_four_email,
    @demo_password_hash,
    'Fourth Candidate',
    '13800138005',
    'ACTIVE',
    '2026-03-15 09:22:00',
    '2026-03-14 09:14:00',
    '2026-03-15 09:22:00'
  ),
  (
    @candidate_five_email,
    @demo_password_hash,
    'Archived Candidate',
    '13800138006',
    'DISABLED',
    NULL,
    '2026-03-14 09:16:00',
    '2026-03-15 09:16:00'
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
    @hr_two_email,
    @demo_password_hash,
    'Platform HR',
    '13800138007',
    'ACTIVE',
    '2026-03-15 09:24:00',
    '2026-03-14 08:55:00',
    '2026-03-15 09:24:00'
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
WHERE u.email IN (
  @candidate_email,
  @candidate_two_email,
  @candidate_three_email,
  @candidate_four_email,
  @candidate_five_email
);

INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, '2026-03-14 09:31:00'
FROM users u
JOIN roles r ON r.code = 'HR'
WHERE u.email IN (@hr_email, @hr_two_email);

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
    'Build SmartHire backend services with Spring Boot, MySQL, Redis and message-driven notification delivery.',
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
  ),
  (
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    'QA Automation Engineer',
    'Own regression automation, API contract checks and CI quality gates for product squads.',
    'Suzhou',
    'Engineering',
    'FULL_TIME',
    'MID',
    17000.00,
    25000.00,
    'OPEN',
    '2026-10-31 23:59:59',
    '2026-03-14 10:12:00',
    '2026-03-15 10:12:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    'Data Analyst',
    'Analyze hiring funnel metrics, candidate conversion and interview throughput for business reporting.',
    'Chengdu',
    'Analytics',
    'FULL_TIME',
    'JUNIOR',
    15000.00,
    22000.00,
    'EXPIRED',
    '2026-02-28 23:59:59',
    '2026-03-14 10:15:00',
    '2026-03-15 10:15:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1),
    'DevOps Engineer',
    'Maintain CI/CD pipelines, cloud infrastructure and release automation for multiple delivery teams.',
    'Shenzhen',
    'Platform',
    'FULL_TIME',
    'MID',
    22000.00,
    32000.00,
    'OPEN',
    '2026-12-15 23:59:59',
    '2026-03-14 10:20:00',
    '2026-03-15 10:20:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1),
    'Product Operations Intern',
    'Support campaign execution, data checking and cross-team follow-ups in the operations team.',
    'Guangzhou',
    'Operations',
    'INTERNSHIP',
    'ENTRY',
    3500.00,
    5000.00,
    'OPEN',
    '2026-09-30 23:59:59',
    '2026-03-14 10:25:00',
    '2026-03-15 10:25:00'
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
    'I have hands-on experience with Spring Boot, JWT, MySQL and Redis backed services.',
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
    'I can collaborate well on front-end delivery and API integration tasks.',
    NULL,
    '2026-03-15 08:10:00',
    '2026-03-15 08:10:00',
    NULL,
    '2026-03-15 08:10:00',
    '2026-03-15 08:10:00'
  ),
  (
    (
      SELECT id
      FROM jobs
      WHERE title = 'Frontend Engineer'
        AND created_by = (SELECT id FROM users WHERE email = @hr_email LIMIT 1)
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @candidate_three_email LIMIT 1),
    'REJECTED',
    '/resume/third-candidate-frontend.pdf',
    'I enjoy collaborating on design systems and component libraries.',
    'Portfolio did not match the current role direction.',
    '2026-03-15 10:00:00',
    '2026-03-15 16:30:00',
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    '2026-03-15 10:00:00',
    '2026-03-15 16:30:00'
  ),
  (
    (
      SELECT id
      FROM jobs
      WHERE title = 'QA Automation Engineer'
        AND created_by = (SELECT id FROM users WHERE email = @hr_email LIMIT 1)
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @candidate_four_email LIMIT 1),
    'OFFERED',
    '/resume/fourth-candidate-qa.pdf',
    'I have been building API and UI automation suites for internal tools.',
    'Offer approved after practical task review.',
    '2026-03-15 09:20:00',
    '2026-03-16 13:45:00',
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    '2026-03-15 09:20:00',
    '2026-03-16 13:45:00'
  ),
  (
    (
      SELECT id
      FROM jobs
      WHERE title = 'DevOps Engineer'
        AND created_by = (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1)
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @candidate_two_email LIMIT 1),
    'APPLIED',
    '/resume/second-candidate-devops.pdf',
    'I want to move deeper into cloud delivery and pipeline automation.',
    NULL,
    '2026-03-15 11:05:00',
    '2026-03-15 11:05:00',
    NULL,
    '2026-03-15 11:05:00',
    '2026-03-15 11:05:00'
  ),
  (
    (
      SELECT id
      FROM jobs
      WHERE title = 'DevOps Engineer'
        AND created_by = (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1)
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @candidate_three_email LIMIT 1),
    'INTERVIEW',
    '/resume/third-candidate-devops.pdf',
    'I have been maintaining CI jobs, containers and deployment scripts for student projects.',
    'Technical round completed, recommend moving ahead.',
    '2026-03-15 11:30:00',
    '2026-03-16 10:20:00',
    (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1),
    '2026-03-15 11:30:00',
    '2026-03-16 10:20:00'
  ),
  (
    (
      SELECT id
      FROM jobs
      WHERE title = 'Product Operations Intern'
        AND created_by = (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1)
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @candidate_four_email LIMIT 1),
    'REVIEWING',
    '/resume/fourth-candidate-ops.pdf',
    'I enjoy campaign execution, reporting and cross-team coordination.',
    'Initial review looks positive.',
    '2026-03-15 13:15:00',
    '2026-03-16 09:15:00',
    (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1),
    '2026-03-15 13:15:00',
    '2026-03-16 09:15:00'
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
  ),
  (
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'DevOps Engineer'
        AND c.email = @candidate_three_email
      LIMIT 1
    ),
    (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1),
    '2026-03-18 10:30:00',
    'Online',
    'https://meet.example.com/devops-final',
    'COMPLETED',
    'PASSED',
    'Candidate performed well on CI/CD and deployment troubleshooting questions.',
    '2026-03-16 10:20:00',
    '2026-03-17 14:10:00'
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
    (SELECT id FROM users WHERE email = @candidate_three_email LIMIT 1),
    'APPLICATION_STATUS_CHANGED',
    'Application status updated',
    'Your application for Frontend Engineer is now REJECTED.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'Frontend Engineer'
        AND c.email = @candidate_three_email
      LIMIT 1
    ),
    1,
    '2026-03-15 16:35:00',
    '2026-03-15 16:30:00',
    '2026-03-15 16:35:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_email LIMIT 1),
    'APPLICATION_SUBMITTED',
    'New application received',
    'Fourth Candidate applied for QA Automation Engineer.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'QA Automation Engineer'
        AND c.email = @candidate_four_email
      LIMIT 1
    ),
    0,
    NULL,
    '2026-03-15 09:20:00',
    '2026-03-15 09:20:00'
  ),
  (
    (SELECT id FROM users WHERE email = @candidate_four_email LIMIT 1),
    'APPLICATION_STATUS_CHANGED',
    'Application status updated',
    'Your application for QA Automation Engineer is now OFFERED.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'QA Automation Engineer'
        AND c.email = @candidate_four_email
      LIMIT 1
    ),
    0,
    NULL,
    '2026-03-16 13:45:00',
    '2026-03-16 13:45:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1),
    'APPLICATION_SUBMITTED',
    'New application received',
    'Second Candidate applied for DevOps Engineer.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'DevOps Engineer'
        AND c.email = @candidate_two_email
      LIMIT 1
    ),
    0,
    NULL,
    '2026-03-15 11:05:00',
    '2026-03-15 11:05:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1),
    'APPLICATION_SUBMITTED',
    'New application received',
    'Third Candidate applied for DevOps Engineer.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'DevOps Engineer'
        AND c.email = @candidate_three_email
      LIMIT 1
    ),
    1,
    '2026-03-15 11:32:00',
    '2026-03-15 11:30:00',
    '2026-03-15 11:32:00'
  ),
  (
    (SELECT id FROM users WHERE email = @candidate_three_email LIMIT 1),
    'INTERVIEW_UPDATED',
    'Interview updated',
    'Your interview for DevOps Engineer has been updated to COMPLETED.',
    'INTERVIEW',
    (
      SELECT i.id
      FROM interviews i
      JOIN applications a ON a.id = i.application_id
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'DevOps Engineer'
        AND c.email = @candidate_three_email
      LIMIT 1
    ),
    0,
    NULL,
    '2026-03-17 14:10:00',
    '2026-03-17 14:10:00'
  ),
  (
    (SELECT id FROM users WHERE email = @hr_two_email LIMIT 1),
    'APPLICATION_SUBMITTED',
    'New application received',
    'Fourth Candidate applied for Product Operations Intern.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'Product Operations Intern'
        AND c.email = @candidate_four_email
      LIMIT 1
    ),
    0,
    NULL,
    '2026-03-15 13:15:00',
    '2026-03-15 13:15:00'
  ),
  (
    (SELECT id FROM users WHERE email = @candidate_four_email LIMIT 1),
    'APPLICATION_STATUS_CHANGED',
    'Application status updated',
    'Your application for Product Operations Intern is now REVIEWING.',
    'APPLICATION',
    (
      SELECT a.id
      FROM applications a
      JOIN jobs j ON j.id = a.job_id
      JOIN users c ON c.id = a.candidate_id
      WHERE j.title = 'Product Operations Intern'
        AND c.email = @candidate_four_email
      LIMIT 1
    ),
    0,
    NULL,
    '2026-03-16 09:15:00',
    '2026-03-16 09:15:00'
  ),
  (
    (SELECT id FROM users WHERE email = @admin_email LIMIT 1),
    'SYSTEM',
    'Demo environment ready',
    'Expanded SmartHire demo data has been loaded successfully.',
    'SYSTEM',
    NULL,
    0,
    NULL,
    '2026-03-15 09:30:00',
    '2026-03-15 09:30:00'
  );
