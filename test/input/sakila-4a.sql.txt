select last_name, count(*) actor_count 
from actor 
group by last_name
order by actor_count desc, last_name;