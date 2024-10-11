alter table sdr_tbltelesc5cdrs_start drop column TC_SSWCALLTYPEID
/
alter table sdr_tbltelesc5cdrs drop column TC_SSWCALLTYPEID
/

alter table sdr_tbltelesc5cdrs add TC_CALLEDNUMBERODN varchar2(32)
/
alter table sdr_tbltelesc5cdrs add TC_CALLEDNUMBERTID varchar2(32)
/

alter table sdr_tbltelesc5cdrs_start add TC_CALLEDNUMBERODN varchar2(32)
/
alter table sdr_tbltelesc5cdrs_start add TC_CALLEDNUMBERTID varchar2(32)
/
alter table sdr_tbltelesc5cdrs_start add TC_FORWORDNUMBER varchar2(32)
/
