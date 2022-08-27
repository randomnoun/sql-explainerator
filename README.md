# explain-to-image

**explain-to-image**  converts query optimisation plans ( the output from an 'EXPLAIN' statement ) into pretty diagrams

It's based on the 'visual explain' pane in [MySQL Workbench](https://www.mysql.com/products/workbench/), and looks like this:

![](https://raw.githubusercontent.com/randomnoun/sql-explain-to-image/master/src/site/readme/sakila-7g.png)

## Why would anyone want to do that  ?

Because diagrams are easier to comprehend than the tabular or JSON 'explain' output you get in MySQL.

And becaue it's in java, so I might be able to hook it into a running app server to generate these things on the fly rather than having to start up MySQL Workbench and cut and pasting SQL across in order to click the explain query button on that.

## How do I use it ? 

Usage text:
```
C:\util\java> java -jar explain-to-image-0.0.1-with-dependencies.jar --help
usage: SqlExplainToImageCli [options]
 -h,--help                  This usage text
 -i,--infile <infile>       input file, or '-' for stdin
 -o,--outfile <outfile>     output file, or '-' for stdout
 -f,--format <format>       output format
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

  SqlExplainToImageCli --infile plan.json --outfile plan.svg
or
  cat plan.json | SqlPlainToImageCli > plan.svg

to generate the SVG diagram.

(2) When supplying the SQL against a database instance, you must supply the connection string,
username, password and sql, e.g.:

  SqlExplainToImageCli --jdbc jdbc:mysql://localhost/sakila --username root --password abc123 \
    --sql "SELECT 1 fROM DUAL" --outfile plan.svg


```

And some example command-lines:

```
C:\util\java> java -jar explain-to-image-0.0.1-SNAPSHOT-with-dependencies.jar --jdbc jdbc:mysql://localhost/sakila --username root --password abc123 --sql "SELECT 1 fROM DUAL" --outfile plan.svg
```

## Where can I get it ? 

TBD

## What databases can I run this on ?

MySQL

## Doesn't this already exist ?

Well, MySQL Workbench exists, but that's not so easy to use from within a webapp.

But yes, you'd imagine this would already exist somewhere, wouldn't you.

## Licensing

sql-explain-to-image is licensed under the BSD 2-clause license.

