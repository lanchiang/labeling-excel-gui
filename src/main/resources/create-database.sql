DROP DATABASE IF EXISTS line_function_db;

CREATE DATABASE line_function_db;

\c line_function_db

DROP TABLE IF EXISTS excel_file CASCADE;

CREATE TABLE IF NOT EXISTS excel_file
(
    id  serial  PRIMARY KEY,
    excel_file_name text NOT NULL,
    spreadsheet_number  int NOT NULL
);

DROP TABLE IF EXISTS spreadsheet CASCADE;

CREATE TABLE IF NOT EXISTS spreadsheet
(
    id                serial PRIMARY KEY,
    excel_file_id   int NOT NULL,
    spread_sheet_name text NOT NULL,
    CONSTRAINT excel_file_id_fk FOREIGN KEY (excel_file_id)
        REFERENCES excel_file (id) MATCH SIMPLE
        ON UPDATE CASCADE ON DELETE CASCADE
);

DROP TABLE IF EXISTS line_function CASCADE;

CREATE TABLE IF NOT EXISTS line_function
(
    id                serial PRIMARY KEY,
    spreadsheet_id      int     NOT NULL,
    start_line_number int     NOT NULL,
    end_line_number   int     NOT NULL,
    line_type         text NOT NULL,
    CONSTRAINT data_file_id_fk FOREIGN KEY (spreadsheet_id)
        REFERENCES spreadsheet (id) MATCH SIMPLE
        ON UPDATE CASCADE ON DELETE CASCADE
);

-- DROP TABLE IF EXISTS spreadsheet_similarity CASCADE;
--
-- CREATE TABLE IF NOT EXISTS spreadsheet_similarity
-- (
--     id  serial  PRIMARY KEY,
--     spreadsheet_id_1    int NOT NULL,
--     spreadsheet_id_2    int NOT NULL,
--     similarity  float   NOT NULL,
--     CONSTRAINT spreadsheet_id_1_fk FOREIGN KEY (spreadsheet_id_1)
--         REFERENCES spreadsheet (id) MATCH SIMPLE
--         ON UPDATE CASCADE ON DELETE CASCADE,
--     CONSTRAINT spreadsheet_id_2_fk FOREIGN KEY (spreadsheet_id_2)
--         REFERENCES spreadsheet (id) MATCH SIMPLE
--         ON UPDATE CASCADE ON DELETE CASCADE
-- );