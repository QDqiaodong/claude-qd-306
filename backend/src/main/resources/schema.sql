-- 印刷厂 · 印版与工单
SET NAMES utf8mb4;

DROP TABLE IF EXISTS color_proof;
DROP TABLE IF EXISTS print_job;
DROP TABLE IF EXISTS paper;
DROP TABLE IF EXISTS plate;
DROP TABLE IF EXISTS press;

CREATE TABLE press (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  press_code  VARCHAR(24) NOT NULL,
  press_name  VARCHAR(64) NOT NULL,
  model_text  VARCHAR(32) NULL,
  operator    VARCHAR(32) NULL,
  press_state VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_press_code (press_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE plate (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  plate_code VARCHAR(24) NOT NULL,
  plate_name VARCHAR(64) NOT NULL,
  plate_size VARCHAR(16) NULL,
  press_id   BIGINT      NULL,
  plate_date DATE        NULL,
  plate_state VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_plate_code (plate_code),
  KEY idx_plate_press (press_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE paper (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  paper_code  VARCHAR(24) NOT NULL,
  paper_name  VARCHAR(64) NOT NULL,
  gram_weight INT         NULL,
  stock       INT         NOT NULL DEFAULT 0,
  warn_line   INT         NULL,
  paper_state VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_paper_code (paper_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE print_job (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  job_no      VARCHAR(24) NOT NULL,
  client_name VARCHAR(64) NOT NULL,
  paper_id    BIGINT      NULL,
  plate_id    BIGINT      NULL,
  copies      INT         NOT NULL,
  due_date    DATE        NULL,
  job_state   VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_no (job_no),
  KEY idx_job_paper (paper_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 校色试印台账：一条账同时点着一张工单、一块印版、一台印刷机。
-- 只允许落在「待印」的工单上；一张工单最多只能有一条「通过」：
-- pass_job_id 在记「通过」时由服务层写成 job_id，记「不通过」时为 NULL，
-- 对它加唯一索引——NULL 不占名额（可以先多次不通过），通过却只能有一条。
-- 两个人同时落通过，数据库只放一条进去，后到的撞唯一键。
CREATE TABLE color_proof (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  job_id      BIGINT      NOT NULL,
  plate_id    BIGINT      NOT NULL,
  press_id    BIGINT      NOT NULL,
  result      VARCHAR(8)  NOT NULL,
  operator    VARCHAR(32) NULL,
  note        VARCHAR(255) NULL,
  proof_time  DATETIME    NOT NULL,
  pass_job_id BIGINT      NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_proof_pass_job (pass_job_id),
  KEY idx_proof_job (job_id),
  KEY idx_proof_plate (plate_id),
  KEY idx_proof_press (press_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO press (press_code, press_name, model_text, operator, press_state) VALUES
('P-01', '四色胶印机 A', 'SM102', '老陈', '运行'),
('P-02', '四色胶印机 B', 'SM74', '小刘', '运行'),
('P-03', '单色胶印机 C', 'GTO52', '小刘', '停机'),
('P-04', '数码印刷机 D', 'C8000', '老陈', '封存');

INSERT INTO plate (plate_code, plate_name, plate_size, press_id, plate_date, plate_state) VALUES
('PL-01', '画册封面版', '四开', 1, '2026-09-10', '在用'),
('PL-02', '画册内页版', '四开', 1, '2026-09-10', '在用'),
('PL-03', '说明书版', '八开', 2, '2026-09-12', '已磨损'),
('PL-04', '不干胶标签版', '八开', 3, '2026-09-14', '在用'),
('PL-05', '旧海报版', '对开', 2, '2026-08-20', '已作废');

INSERT INTO paper (paper_code, paper_name, gram_weight, stock, warn_line, paper_state) VALUES
('PP-01', '铜版纸', 157, 32000, 10000, '充足'),
('PP-02', '双胶纸', 80, 9000, 15000, '紧张'),
('PP-03', '哑粉纸', 128, 4000, 8000, '紧张'),
('PP-04', '牛皮纸', 120, 0, 5000, '缺货'),
('PP-05', '书写纸', 70, 50000, 12000, '充足');

INSERT INTO print_job (job_no, client_name, paper_id, plate_id, copies, due_date, job_state) VALUES
('PJ-01', '星海文具', 1, 1, 50000, '2026-09-20', '印刷中'),
('PJ-02', '乐学教育', 2, 2, 30000, '2026-09-21', '待印'),
('PJ-03', '云图文化', 5, 3, 80000, '2026-09-22', '待印'),
('PJ-04', '光明印务', 1, 4, 20000, '2026-09-18', '已完成'),
('PJ-05', '凯达实业', 4, 2, 15000, '2026-09-25', '待印');

-- PJ-02 已在 P-01 上用 PL-02 校过色并通过（眼下版仍装在该机器、机器仍在跑，算有效通行证）
INSERT INTO color_proof (job_id, plate_id, press_id, result, operator, note, proof_time) VALUES
(2, 2, 1, '通过', '老陈', '首件签样，色差在范围内', '2026-09-18 10:20:00');
