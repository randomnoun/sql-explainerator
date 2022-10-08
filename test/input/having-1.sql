select * from testAB
GROUP BY c 
HAVING c=(SELECT c FROM justA LIMIT 1)