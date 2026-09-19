-- H2（MySQL 兼容模式）专用建表，结构与主 schema.sql 一致
DROP TABLE IF EXISTS color_proof;
DROP TABLE IF EXISTS print_job;
DROP TABLE IF EXISTS paper;
DROP TABLE IF EXISTS plate;
DROP TABLE IF EXISTS press;

CREATE TABLE press (
  id BIGINT NOT NULL AUTO_INCREMENT,
  press_code VARCHAR(24) NOT NULL,
  press_name VARCHAR(64) NOT NULL,
  model_text VARCHAR(32),
  operator VARCHAR(32),
  press_state VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (press_code)
);

CREATE TABLE plate (
  id BIGINT NOT NULL AUTO_INCREMENT,
  plate_code VARCHAR(24) NOT NULL,
  plate_name VARCHAR(64) NOT NULL,
  plate_size VARCHAR(16),
  press_id BIGINT,
  plate_date DATE,
  plate_state VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (plate_code)
);
CREATE INDEX idx_plate_press ON plate (press_id);

CREATE TABLE paper (
  id BIGINT NOT NULL AUTO_INCREMENT,
  paper_code VARCHAR(24) NOT NULL,
  paper_name VARCHAR(64) NOT NULL,
  gram_weight INT,
  stock INT NOT NULL DEFAULT 0,
  warn_line INT,
  paper_state VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (paper_code)
);

CREATE TABLE print_job (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_no VARCHAR(24) NOT NULL,
  client_name VARCHAR(64) NOT NULL,
  paper_id BIGINT,
  plate_id BIGINT,
  copies INT NOT NULL,
  due_date DATE,
  job_state VARCHAR(16) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE (job_no)
);
CREATE INDEX idx_job_paper ON print_job (paper_id);

CREATE TABLE color_proof (
  id BIGINT NOT NULL AUTO_INCREMENT,
  job_id BIGINT NOT NULL,
  plate_id BIGINT NOT NULL,
  press_id BIGINT NOT NULL,
  result VARCHAR(8) NOT NULL,
  operator VARCHAR(32),
  note VARCHAR(255),
  proof_time TIMESTAMP NOT NULL,
  pass_job_id BIGINT,
  PRIMARY KEY (id),
  UNIQUE (pass_job_id)
);
CREATE INDEX idx_proof_job ON color_proof (job_id);
CREATE INDEX idx_proof_plate ON color_proof (plate_id);
CREATE INDEX idx_proof_press ON color_proof (press_id);

INSERT INTO press (id, press_code, press_name, model_text, operator, press_state) VALUES
(1, 'P-01', '一号机', 'SM102', '老陈', '运行'),
(2, 'P-02', '二号机', 'SM74', '小刘', '运行'),
(3, 'P-03', '三号机', 'GTO52', '小刘', '停机'),
(4, 'P-04', '四号机', 'C8000', '老陈', '封存');

INSERT INTO plate (id, plate_code, plate_name, plate_size, press_id, plate_date, plate_state) VALUES
(1, 'PL-01', '封面版', '四开', 1, DATE '2026-09-10', '在用'),
(2, 'PL-02', '内页版', '四开', 1, DATE '2026-09-10', '在用'),
(3, 'PL-03', '说明书版', '八开', 2, DATE '2026-09-12', '已磨损'),
(4, 'PL-04', '标签版', '八开', 3, DATE '2026-09-14', '在用'),
(5, 'PL-05', '旧海报版', '对开', 2, DATE '2026-08-20', '已作废');

INSERT INTO paper (id, paper_code, paper_name, gram_weight, stock, warn_line, paper_state) VALUES
(1, 'PP-01', '铜版纸', 157, 32000, 10000, '充足');

INSERT INTO print_job (id, job_no, client_name, paper_id, plate_id, copies, due_date, job_state) VALUES
(1, 'PJ-01', '星海文具', 1, 1, 5000, DATE '2026-09-20', '印刷中'),
(2, 'PJ-02', '乐学教育', 1, 2, 3000, DATE '2026-09-21', '待印'),
(3, 'PJ-03', '云图文化', 1, 3, 8000, DATE '2026-09-22', '待印'),
(4, 'PJ-04', '光明印务', 1, 4, 2000, DATE '2026-09-18', '已完成'),
(5, 'PJ-05', '凯达实业', 1, 2, 1500, DATE '2026-09-25', '待印');
