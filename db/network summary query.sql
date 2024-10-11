select count(*) from SDR_TBLNetworkCDRSummary where NCS_TIME Between  to_date('2014-11-03 00:00:00', 'YYYY-MM-DD HH24:MI:SS')  
    AND  to_date('2014-11-03 23:59:59', 'YYYY-MM-DD HH24:MI:SS')
 

select count(*) from SDR_TBLNetworkCDRSummary where NCS_TIME Between  to_date('2014-11-03 00:00:00', 'YYYY-MM-DD HH24:MI:SS')  
    AND  to_date('2014-11-03 23:59:59', 'YYYY-MM-DD HH24:MI:SS')


 
 select sum(NoOfCalls) from (
  SELECT  to_char(NCS_TIME,'YYYY-MM-DD HH24') as NCS_TIME , sum(NCS_CCALLS) as NoOfCalls, sum(NCS_TCALLS) as NoOfTCalls, 
   sum(ceil(NCS_DURATION)) as CallDuration, sum(ceil(NCS_ROUNDEDDUR)) as RoundedDur  from SDR_TBLNetworkCDRSummary ncs   
    WHERE 1=1  AND ncs.NCS_TIME Between  to_date('2014-11-03 00:00:00', 'YYYY-MM-DD HH24:MI:SS')  
    AND  to_date('2014-11-03 23:59:59', 'YYYY-MM-DD HH24:MI:SS')     group by  to_char(NCS_TIME,'YYYY-MM-DD HH24')  order by  NCS_TIME
  )