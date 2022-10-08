SELECT i, 
  STD(i) OVER aWindow std,
  VARIANCE(i) OVER aWindow variance,
  CUME_DIST() OVER aWindow dist,
  LEAD(i, 2) OVER aWindow lead2,
  NTH_VALUE(i, 3) OVER aWindow nthVal, k 
FROM threeCol
WINDOW aWindow AS (PARTITION BY k ORDER BY i)