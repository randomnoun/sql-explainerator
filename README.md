# sql-explainerator

**sql-explainerator**  converts query execution plans ( the output from an 'EXPLAIN' statement ) into pretty diagrams

It attempts to recreate the type of diagrams you get from the 'visual explain' pane in [MySQL Workbench](https://www.mysql.com/products/workbench/), and looks like this:

![](https://raw.githubusercontent.com/randomnoun/sql-explainerator/master/src/site/readme/sakila-7g.png)

The images are in SVG form, although you can also generate HTML output which wraps the SVG.

## Why would anyone want to do that  ?

Because diagrams are easier to comprehend than the tabular or JSON 'explain' output you get in MySQL.

And becaue it's in java, so I might be able to hook it into a running app server to generate these things on the fly rather than having to start up MySQL Workbench and cut and paste SQL across in order to click the explain query button on that.

And, of course, because I'm a man, and apparently I live for explaining things.

## How do I use it ? 

Usage text:
```
C:\util\java> java -jar sql-explainerator-0.0.1-with-dependencies.jar --help
usage: SqlExplainerator [options]
 -h,--help                  This usage text
 -i,--infile <infile>       input file, or '-' for stdin; default = stdin
 -o,--outfile <outfile>     output file, or '-' for stdout; default = stdout
 -f,--format <format>       output format (svg or html); default = svg
 -j,--jdbc <jdbc>           JDBC connection string
 -u,--username <username>   JDBC username
 -p,--password <password>   JDBC password
 -d,--driver <driver>       JDBC driver class name; default = org.mariadb.jdbc.Driver
 -q,--sql <sql>             SQL to explain

This command will convert a MySQL JSON execution plan to diagram form.
The execution plan can be supplied via stdin or --infile (1), or can be retrieved from a MySQL
server (2).

(1): When supplying the execution plan via stdin or --infile, use the JSON generated via the
'EXPLAIN FORMAT=JSON (query)' statement, e.g.

  mysql --user=root --password=abc123 --silent --raw --skip-column-names \
    --execute "EXPLAIN FORMAT=JSON SELECT 1 FROM DUAL" sakila > plan.json

to generate the query plan JSON, then

  SqlExplainerator --infile plan.json --outfile plan.svg
or
  cat plan.json | SqlExplainerator > plan.svg

to generate the SVG diagram.

(2) When supplying the SQL against a database instance, you must supply the connection string,
username, password and sql, e.g.:

  SqlExplainerator --jdbc jdbc:mysql://localhost/sakila --username root --password abc123 \
    --sql "SELECT 1 fROM DUAL" --outfile plan.svg
```

And some example command-lines:

```
C:\util\java> java -jar sql-explainerator-0.0.1-SNAPSHOT-with-dependencies.jar --jdbc jdbc:mysql://localhost/sakila --username root --password abc123 --sql "SELECT 1 fROM DUAL" --outfile plan.svg
```

```
C:\util\java>"c:\Program Files\MySQL\MySQL Server 8.0\bin\mysql" --user=root --password=abc123 --silent --raw --skip-column-names --execute "EXPLAIN FORMAT=JSON SELECT 1 FROM DUAL" sakila > plan.json

C:\util\java>java -jar sql-explainerator-0.0.1-SNAPSHOT-with-dependencies.jar --infile plan.json --outfile plan.svg
```

## Where can I get it ? 

TBD

## What databases can I run this on ?

MySQL

## Doesn't this already exist ?

Well, MySQL Workbench exists, but that's not so easy to use from within a webapp.

But yes, you'd imagine this would already exist somewhere, wouldn't you.

## Futher reading

* MariaDB EXPLAIN is not MySQL EXPLAIN: https://mariadb.com/kb/en/explain-format-json-differences/
* More of that: https://mariadb.com/kb/en/differences-between-the-mysql-and-mariadb-query-optimizer/
* More MariaDB EXPLAIN things: https://mariadb.com/kb/en/explain-format-json/
* More MariaDB EXPLAIN things, in blog form: https://web.archive.org/web/20200218115814/http://s.petrunia.net/blog/?p=93
* A not very useful EXPLAIN visualiser project: https://github.com/Preetam/explain-analyzer
* Some EXPLAIN JSON tests from the mysql-server source: https://github.com/mysql/mysql-server/blob/8d8c986e5716e38cb776b627a8eee9e92241b4ce/mysql-test/r/window_std_var_optimized.result
* Some more of those: https://github.com/mysql/mysql-server/search?p=2&q=ordering_operation

## Licensing

sql-explainerator is licensed under the BSD 2-clause license.

