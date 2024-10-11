Filesystem           1M-blocks      Used Available Use% Mounted on
/dev/cciss/c0d0p8         9917      3797      5609  41% /
/dev/cciss/c0d0p9         7933       272      7252   4% /home
/dev/cciss/c0d0p7        19840      3960     14856  22% /usr
/dev/cciss/c0d0p6        19840       800     18017   5% /var
/dev/cciss/c0d0p5        23807       173     22406   1% /tmp
/dev/cciss/c0d0p2        99192     87046      7027  93% /oracle
/dev/cciss/c0d0p1          494        17       452   4% /boot
tmpfs                    24576         0     24576   0% /dev/shm
/dev/mapper/mpath0p1    137799    108888     21913  84% /u01
/dev/mapper/mpath1p1    137799    108838     21962  84% /u02
/dev/mapper/mpath2p1    137799    108837     21963  84% /u03
/dev/mapper/mpath3p1    137799    103762     27038  80% /u04
/dev/mapper/mpath4p1    137799     93463     37337  72% /u05
/dev/mapper/mpath5p1    319216    196021    106980  65% /u06
/dev/mapper/mpath6p1    319216    174446    128555  58% /u07
/dev/mapper/mpath7p1    319216    189821    113180  63% /u08
/dev/mapper/mpath8p1    319216    215446     87555  72% /u09
/dev/mapper/mpath9p1    413090     50448    341659  13% /u10

Filesystem            Size  Used Avail Use% Mounted on
/dev/cciss/c0d0p8     9.7G  3.8G  5.5G  41% /
/dev/cciss/c0d0p9     7.8G  272M  7.1G   4% /home
/dev/cciss/c0d0p7      20G  3.9G   15G  22% /usr
/dev/cciss/c0d0p6      20G  800M   18G   5% /var
/dev/cciss/c0d0p5      24G  173M   22G   1% /tmp
/dev/cciss/c0d0p2      97G   86G  6.9G  93% /oracle
/dev/cciss/c0d0p1     494M   17M  452M   4% /boot
tmpfs                  24G     0   24G   0% /dev/shm
/dev/mapper/mpath0p1  135G  107G   22G  84% /u01
/dev/mapper/mpath1p1  135G  107G   22G  84% /u02
/dev/mapper/mpath2p1  135G  107G   22G  84% /u03
/dev/mapper/mpath3p1  135G  102G   27G  80% /u04
/dev/mapper/mpath4p1  135G   92G   37G  72% /u05
/dev/mapper/mpath5p1  312G  192G  105G  65% /u06
/dev/mapper/mpath6p1  312G  171G  126G  58% /u07
/dev/mapper/mpath7p1  312G  186G  111G  63% /u08
/dev/mapper/mpath8p1  312G  211G   86G  72% /u09
/dev/mapper/mpath9p1  404G   50G  334G  13% /u10



 insert into SDR_TBLMEDCDRDUPLICATECHECK (MCDC_CDRID, MCDC_EVENTTIME,  ne_elementid, FN_FileID)  values ('1560818385_124415330@175.207.97.42000:00:00', to_date('2014-11-22
 16:51:59' ,'YYYY-MM-DD HH24:MI:SS'), 23, 1240995)
 
 
 select min(MCDC_EVENTTIME) from SDR_TBLMEDCDRDUPLICATECHECK 
 
 7/29/2014 5:53:15 AM
 
 select * from tmr_tblfilenames where fn_filename='T201411221652'
 
select min(fn_fileid) from tmr_tblfilenames where fn_processingdate >= to_date('2014-11-21','yyyy-mm-dd') 

select min(fn_fileid) from tmr_tblfilenames where  fn_processingdate >= to_date('2014-11-21','yyyy-mm-dd') 
 and   FN_TOTALRECORDS - FN_PROCESSEDRECORDS > 6 and fn_fileid > 1238970

1230176


select * from tmr_tblfilenames where  fn_fileid between 1239529 and 1241006 
order by fn_fileid 

fn_filename='T201411210907'  -- fn_fileid >= 1239529

1238131     billing.log.2014-11-20-06  
1238558     billing.log.2014-11-20-15
1238648     billing.log.2014-11-20-17

1239529     T201411210900   ---------------------
1241005     T201411221715
1241006     MGC_FTPMGC1-UNIT1_1411222230.17.338678

update tmr_tblfilenames set fs_filestateid = 0 where fn_fileid in (1238131, 1238558, 1238648) or fn_fileid between 1239529 and 1241006


--select count(*) from sdr_tblicpmissedcdrs where ICPMC_RAWCDRID in (select NSSW_RAWCDR_ID from SDR_TBLNEXTONESSWCDRS where  fn_fileid= 1239545)
