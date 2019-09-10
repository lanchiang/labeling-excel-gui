DROP DATABASE IF EXISTS line_function_db;

CREATE DATABASE line_function_db;
\c line_function_db

DROP TABLE IF EXISTS data_file CASCADE;

CREATE TABLE IF NOT EXISTS data_file
(
         id                serial PRIMARY KEY,
         excel_file_name   text NOT NULL,
         spread_sheet_name text NOT NULL
);

DROP TABLE IF EXISTS line_function CASCADE;

CREATE TABLE IF NOT EXISTS line_function
     (
         id                serial PRIMARY KEY,
         data_file_id      int     NOT NULL,
         start_line_number int     NOT NULL,
         end_line_number   int     NOT NULL,
         line_type         char(1) NOT NULL,
         CONSTRAINT data_file_id_fk FOREIGN KEY (data_file_id)
             REFERENCES data_file (id) MATCH SIMPLE
             ON UPDATE CASCADE ON DELETE CASCADE
     );