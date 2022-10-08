select * from testAB
WHERE c=(SELECT c FROM justA LIMIT 1)
GROUP BY c 
HAVING c=(SELECT c FROM justA LIMIT 1)