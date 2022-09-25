-- has group, distinct and order blocks in the execution plan, on the same join
select distinct customer.customer_id, customer.first_name
from customer
straight_join payment on customer.customer_id = payment.customer_id
group by customer.last_name
order by sum(payment.amount);