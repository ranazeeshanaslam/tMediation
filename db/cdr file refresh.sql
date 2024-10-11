select * from tmr_tblfilenames where fn_filename = 'billing.log.2014-08-09-17'  

delete from sdr_tbltelesc5cdrs where fn_fileID = 17341
/
delete from sdr_tbltelesc5cdrs_start where fn_fileID = 17341
/
delete from tmr_tblfilenames  where fn_fileID = 17341
/
delete from  sdr_tblsubmissedcdrs  where fn_fileID = 17341
/
truncate table SDR_TBLMEDCDRDUPLICATECHECK
/

update TMR_TBLFILENAMES set FN_PROCESSINGDATE=sysdate, FS_FILESTATEID = 2, FN_TOTALRECORDS=2, 
 FN_PROCESSEDRECORDS=FN_PROCESSEDRECORDS+1, FN_DupRecords=0 ,FN_billableRecords=0 where FN_FILEID = 17337 and NE_ElementID= 26 and FN_ISSECONDARY=0


 
 select * from tmr_tblfilenames where fn_filename = '101120141800.15'  

delete from sdr_tbltelessswcdrs where fn_fileID = 40704
/
delete  /*+ INDEX(sdr_tbltelessswicdrs IX_SDR_TBLTELESSSWICDRS_DT)*/  from sdr_tbltelessswicdrs where fn_fileID = 40704
/
delete from tmr_tblfilenames  where fn_fileID = 40704
/
delete from  sdr_tblsubmissedcdrs  where fn_fileID = 40704
/
delete from SDR_TBLMEDCDRDUPLICATECHECK  where fn_fileID = 40704
/

select count(*) from sdr_tblicpcdrs where ICPR_RAWCDRID in ( select TSSW_RAWCDR_ID from sdr_tbltelessswcdrs where   fn_fileID = 40704)


update TMR_TBLFILENAMES set FN_PROCESSINGDATE=sysdate, FS_FILESTATEID = 2, FN_TOTALRECORDS=2, 
 FN_PROCESSEDRECORDS=FN_PROCESSEDRECORDS+1, FN_DupRecords=0 ,FN_billableRecords=0 where FN_FILEID = 17337 and NE_ElementID= 26 and FN_ISSECONDARY=0

