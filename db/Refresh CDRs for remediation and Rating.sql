update sm_tblaccounts set sm_tblaccounts.AC_CURRENTBALANCE = sm_tblaccounts.AC_CURRENTBALANCE + ( select nvl(CallCharges, 0) from 
    ( select Ac_AccountNo, sum(SCR_AMOUNTCHARGED) as CallCharges from sdr_tblsubcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )  )
            group by ac_accountno
    ) calls where sm_tblaccounts.ac_accountno = calls.ac_accountNo
  )where ac_accountno in (select Ac_AccountNo from sdr_tblsubcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )  )
            group by ac_accountno)
/     
commit
/
update sm_tblsubscribers set sm_tblsubscribers.SUB_USEDLIMIT = sm_tblsubscribers.SUB_USEDLIMIT - ( select nvl(CallCharges, 0) from 
    ( select SUB_SUBSCRIBERID, sum(SCR_AMOUNTCHARGED) as CallCharges from sdr_tblsubcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )  )
            group by SUB_SUBSCRIBERID
    ) calls where sm_tblsubscribers.SUB_SUBSCRIBERID = calls.SUB_SUBSCRIBERID
  )where SUB_SUBSCRIBERID in (select SUB_SUBSCRIBERID from sdr_tblsubcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )  )
            group by SUB_SUBSCRIBERID)             
/
commit
/
update sm_tblsubsservices set sm_tblsubsservices.SS_SERVICEBALANCE = sm_tblsubsservices.SS_SERVICEBALANCE + ( select nvl(CallCharges, 0) from 
    ( select SS_SUBSRVID, sum(SCR_AMOUNTCHARGED) as CallCharges from sdr_tblsubcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )  )
            group by SS_SUBSRVID
    ) calls where sm_tblsubsservices.SS_SUBSRVID = calls.SS_SUBSRVID
  )where SS_SUBSRVID in (select SS_SUBSRVID from sdr_tblsubcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )  )
            group by SS_SUBSRVID)                     
/
commit
/            
update sm_tblsubsservices set sm_tblsubsservices.SS_USEDLIMIT = sm_tblsubsservices.SS_USEDLIMIT - ( select nvl(CallCharges, 0) from 
    ( select SS_SUBSRVID, sum(SCR_AMOUNTCHARGED) as CallCharges from sdr_tblsubcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )  )
            group by SS_SUBSRVID
    ) calls where sm_tblsubsservices.SS_SUBSRVID = calls.SS_SUBSRVID
  )where SS_SUBSRVID in (select SS_SUBSRVID from sdr_tblsubcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )  )
            group by SS_SUBSRVID) 
/
commit
/
delete from sdr_tblsubcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames 
                where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' ) 
        )
            
/
commit
/
delete from sdr_tblsubzerodurcdrs where CR_VOICERECORDID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames 
                where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' ) 
        )
/
commit
/                        
delete from sdr_tblsubmissedcdrs where SMCR_RAWCDRID in 
        ( select TC_CDRID from sdr_tbltelesc5cdrs where fn_fileID in 
            (select fn_fileid from tmr_tblfilenames 
                where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' ) 
        )
/
commit
/
delete from sdr_tbltelesc5cdrs where fn_fileID in 
    (select fn_fileid from tmr_tblfilenames 
        where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )       
/
commit
/
delete from sdr_tbltelesc5cdrs_start where fn_fileID in 
    (select fn_fileid from tmr_tblfilenames 
        where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )
                
/
commit
/
delete from sdr_TBLMEDCDRDUPLICATECHECK where fn_fileID in 
    (select fn_fileid from tmr_tblfilenames 
        where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' )
/
commit
/

delete from tmr_tblfilenames 
        where fn_filename like 'billing.log.2014-08-11-%' or fn_filename like 'billing.log.2014-08-12-%' 
        
/
commit
/                    