-- similar to sakila-7g.sql
-- but there's materialised queries inside materialised queries, and a union

select distinct A.*, B.sales 
from (
    select sto.store_id, cit.city, cou.country
    from store sto
    left join address adr on sto.address_id = adr.address_id
    join city cit on adr.city_id = cit.city_id
    join country cou on cit.country_id = cou.country_id
) A
join (
    select cus.store_id, sum(pay.amount) sales
    from customer cus
    join payment pay on pay.customer_id = cus.customer_id
    where cus.first_name like 'a%'
    group by cus.store_id
    union
    select cus.store_id, sum(pay.amount) sales
    from (select customer.* from customer inner join 
         (select customer.customer_id, count(rental.rental_id)
          from customer inner join rental on customer.customer_id = rental.customer_id 
          group by customer.customer_id
          having count(rental.rental_id) > 1) rental_cus on customer.customer_id = rental_cus.customer_id) cus
    join payment pay on pay.customer_id = cus.customer_id
    group by cus.store_id
) B
on A.store_id = B.store_id
order by a.store_id;