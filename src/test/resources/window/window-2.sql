SELECT k, 
  STD(i), 
  SUM(j), 
  STD(k) OVER (ROWS UNBOUNDED PRECEDING) std_wf 
FROM threeCol 
GROUP BY k