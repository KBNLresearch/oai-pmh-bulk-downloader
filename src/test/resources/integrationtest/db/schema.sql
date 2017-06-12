CREATE TABLE DARE_PREPROCES (
  ID NUMBER(10) AUTO_INCREMENT,
  STATE NUMBER(10) DEFAULT 0 NOT NULL,
  KBOBJID VARCHAR2(255) NOT NULL,
  TS_CREATE TIMESTAMP(6) NOT NULL,
  TS_PROCESSED TIMESTAMP(6) DEFAULT NULL,
  FINGERPRINT VARCHAR2(512) DEFAULT NULL,
  REPOSITORY_ID NUMBER(10) NOT NULL,
  OAI_ID VARCHAR2(255) NOT NULL,
  OAI_DATESTAMP VARCHAR2(255) NOT NULL,
  PRIMARY KEY (ID)
);

CREATE TABLE ERROR_REPORTS (
  DARE_PREPROCES_ID NUMBER(10),
  MESSAGE varchar2(1024) DEFAULT NULL,
  URL varchar2(1024) DEFAULT NULL,
  STACKTRACE clob,
  STATUS_CODE number(10) DEFAULT NULL
);

CREATE TABLE repositories (
  id number(10) AUTO_INCREMENT,
  name VARCHAR2(255) DEFAULT NULL,
  url varchar2(255) DEFAULT NULL,
  metadataPrefix varchar2(255) DEFAULT NULL,
  oai_set varchar2(50) DEFAULT NULL,
  datestamp varchar2(50) DEFAULT NULL,
  schedule NUMBER(10) NOT NULL,
  enabled NUMBER(3) DEFAULT 0 NOT NULL,
  lastHarvest TIMESTAMP(6) DEFAULT NULL,
  PRIMARY KEY (id)
);
