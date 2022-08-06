-- from https://github.com/joelsotelods/sakila-db-queries
-- running on the BSD-licensed sakila database downloaded from https://dev.mysql.com/doc/index-other.html
-- by knoxg on 2022-08-07
 
select A.*, B.sales 
from (
    select sto.store_id, cit.city, cou.country
    from store sto
    left join address adr
    on sto.address_id = adr.address_id
    join city cit
    on adr.city_id = cit.city_id
    join country cou
    on cit.country_id = cou.country_id
) A
join (
    select cus.store_id, sum(pay.amount) sales
    from customer cus
    join payment pay
    on pay.customer_id = cus.customer_id
    group by cus.store_id
) B
on A.store_id = B.store_id
order by a.store_id;