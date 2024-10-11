-- Drop the existing table if it exists
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE NC_TBLEXCHANGES CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN -- Ignore error if table does not exist
         RAISE;
      END IF;
END;
/

-- Create the table with an Identity column for EX_EXCHANGEID
CREATE TABLE NC_TBLEXCHANGES
(
  EX_EXCHANGEID      NUMBER(6) GENERATED ALWAYS AS IDENTITY, -- Use Identity for auto-increment
  EX_EXCHANGENAME    VARCHAR2(100 BYTE)         DEFAULT '0'                 NOT NULL,
  ET_EXCHANGETYPEID  NUMBER(3)                  DEFAULT 0                   NOT NULL,
  CL_LOCATIONID      NUMBER(10)                 DEFAULT 0                   NOT NULL,
  SU_SYSUSERID       NUMBER(5)                  DEFAULT 0                   NOT NULL,
  SU_SYSUSERIP       VARCHAR2(32 BYTE)          DEFAULT '0'                 NOT NULL,
  SU_INSERTDATE      DATE                       DEFAULT sysdate             NOT NULL,
  SU_SYSUSERIDM      NUMBER(5)                  DEFAULT 0                   NOT NULL,
  SU_SYSUSERIPM      VARCHAR2(32 BYTE)          DEFAULT '0'                 NOT NULL,
  SU_MODIFYDATE      DATE                       DEFAULT sysdate             NOT NULL
)
TABLESPACE SYSTEM
PCTUSED    40
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            NEXT             1M
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            FREELISTS        1
            FREELIST GROUPS  1
            BUFFER_POOL      DEFAULT
           )
LOGGING 
NOCOMPRESS 
NOCACHE;

-- Add a primary key constraint on EX_EXCHANGEID
ALTER TABLE NC_TBLEXCHANGES ADD (
  PRIMARY KEY
  (EX_EXCHANGEID)
  USING INDEX
    TABLESPACE SYSTEM
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          64K
                NEXT             1M
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
                FREELISTS        1
                FREELIST GROUPS  1
                BUFFER_POOL      DEFAULT
               )
  ENABLE VALIDATE);

-- Since Identity column handles auto-increment, no need for sequence and trigger
-- Drop the old sequence if it exists
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_NC_TBLEXCHANGES';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -2289 THEN -- Ignore error if sequence does not exist
         RAISE;
      END IF;
END;
/

-- Remove the trigger as it's no longer needed with the Identity column
BEGIN
   EXECUTE IMMEDIATE 'DROP TRIGGER TRG_NC_TBLEXCHANGES';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -4080 THEN -- Ignore error if trigger does not exist
         RAISE;
      END IF;
END;
/




-- Drop the existing table if it exists
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE PAR_IC_TBLEXCHANGES CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN -- Ignore error if table does not exist
         RAISE;
      END IF;
END;
/

-- Create the table with an Identity column for EX_EXCHANGEID
CREATE TABLE PAR_IC_TBLEXCHANGES
(
  EX_EXCHANGEID    NUMBER(6) GENERATED ALWAYS AS IDENTITY, -- Use Identity for auto-increment
  EX_EXCHANGENAME  VARCHAR2(100 BYTE),
  NL_LOCATIONID    NUMBER(5)
)
TABLESPACE USERS
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          128K
            NEXT             1M
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
LOGGING 
NOCOMPRESS 
NOCACHE;

-- Add a primary key constraint on EX_EXCHANGEID
ALTER TABLE PAR_IC_TBLEXCHANGES ADD (
  PRIMARY KEY
  (EX_EXCHANGEID)
  USING INDEX
    TABLESPACE USERS
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          128K
                NEXT             1M
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
                BUFFER_POOL      DEFAULT
               )
  ENABLE VALIDATE);

-- Create an index on NL_LOCATIONID
CREATE INDEX EXCHANGE_LOCATIONID ON PAR_IC_TBLEXCHANGES
(NL_LOCATIONID)
LOGGING
TABLESPACE USERS
PCTFREE    10
INITRANS   2
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            NEXT             1M
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           );

-- Note: No sequence or trigger is needed due to the Identity column.
-- Drop the existing table if it exists
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE PAR_IC_TBLEXCHANGESPREFIX CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN -- Ignore error if table does not exist
         RAISE;
      END IF;
END;
/

-- Create the table with the necessary columns
CREATE TABLE PAR_IC_TBLEXCHANGESPREFIX
(
  EX_EXCHANGEID      NUMBER(6),                 -- Foreign key to EX_EXCHANGEID
  EX_EXCHANGEPREFIX  VARCHAR2(64 BYTE),         -- Prefix for exchange
  DPT_PREFIXTYPEID   NUMBER(3)                   -- Type of the prefix
)
TABLESPACE USERS
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          512K
            NEXT             1M
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
LOGGING 
NOCOMPRESS 
NOCACHE;

-- Add a primary key constraint on the combination of EX_EXCHANGEID and EX_EXCHANGEPREFIX
ALTER TABLE PAR_IC_TBLEXCHANGESPREFIX ADD (
  PRIMARY KEY
  (EX_EXCHANGEID, EX_EXCHANGEPREFIX)
  USING INDEX
    TABLESPACE USERS
    PCTFREE    10
    INITRANS   2
    MAXTRANS   255
    STORAGE    (
                INITIAL          576K
                NEXT             1M
                MINEXTENTS       1
                MAXEXTENTS       UNLIMITED
                PCTINCREASE      0
                BUFFER_POOL      DEFAULT
               )
  ENABLE VALIDATE);

-- Note: No sequences or triggers are needed in this table definition.
