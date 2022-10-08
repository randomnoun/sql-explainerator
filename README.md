# sql-explainerator

**sql-explainerator**  converts query execution plans ( the output from an 'EXPLAIN' statement ) into pretty diagrams

It attempts to recreate the type of diagrams you get from the 'visual explain' pane in [MySQL Workbench](https://www.mysql.com/products/workbench/), and looks like this:

![](https://raw.githubusercontent.com/randomnoun/sql-explainerator/master/src/site/resources/img/sakila-7g.png)

The images are in SVG form, although you can also generate HTML output which wraps the SVG.

## Why would anyone want to do that  ?

Because diagrams are easier to comprehend than the tabular or JSON 'explain' output you get in MySQL.

And becaue it's in java, so I might be able to hook it into a running app server to generate these things on the fly rather than having to start up MySQL Workbench and cut and paste SQL across in order to click the explain query button on that.

And, of course, because I'm a man, and apparently I live for explaining things.

## Can I see some pictures ?

Sure.

Here's some examples of the sort of output it generates. 

Both the SVG and HTML contain javascript to display the tooltips, but you can produce output without the javascript, if that's your bag.

The Workbench output is for comparison, and was created by taking some screenshots of Workbench 8.0.30 running against MySQL Server 8.0.27, both on Windows. The :poop: symbol represents particularly shithouse output. 

| Name | Output<br/>HTML | Output<br/>SVG | Input<br/>SQL | Input<br/>JSON | Workbench<br/>Comparison | Blurb |
|--|:--:|:--:|:--:|:--:|:--:|--|
| sakila-1 | [HTML](https://randomnoun.github.io/sql-explainerator/test/output/sakila-1-javascript.html) |  [SVG](https://randomnoun.github.io/sql-explainerator/test/output/sakila-1-javascript.svg) | [SQL](https://randomnoun.github.io/sql-explainerator/test/input/sakila-1.sql) | [JSON](https://randomnoun.github.io/sql-explainerator/test/input/sakila-1.json) | PNG | simple SELECT |


## How do I use it ? 

Usage text:
```
C:\util\java> java -jar sql-explainerator-0.0.1-with-dependencies.jar --help
usage: SqlExplaineratorCli [options]
 -h,--help                  This usage text
 -i,--infile <infile>       input file, or '-' for stdin; default = stdin
 -o,--outfile <outfile>     output file, or '-' for stdout; default = stdout
 -l,--layout <layout>       layout format (workbench or explainerator); default = explainerator
 -f,--format <format>       output format (svg or html); default = svg
 -t,--tooltip <tooltip>     tooltip type (none, title, attribute, javascript); default = title
 -j,--jdbc <jdbc>           JDBC connection string
 -u,--username <username>   JDBC username
 -p,--password <password>   JDBC password
 -d,--driver <driver>       JDBC driver class name; default = org.mariadb.jdbc.Driver
 -q,--sql <sql>             SQL to explain
 -c,--css <css>             alternate css file
 -s,--script <script>       alternate javascript file

This command will convert a MySQL JSON execution plan into an SVG diagram.
There are two layout methods: 'workbench' which will try to mimic the diagrams generated from MySQL
Workbench, or 'explainerator', which adds support for inserts, 'having' clauses, and window functions.

The execution plan can be supplied via stdin or --infile (Example 1), or can be retrieved from a
MySQL server (Example 2).

Example 1: To generate the query plan JSON, execute an 'EXPLAIN FORMAT=JSON' statement:

  mysql --user=root --password=abc123 --silent --raw --skip-column-names \
    --execute "EXPLAIN FORMAT=JSON SELECT 1 FROM DUAL" sakila > plan.json

then to generate the SVG diagram, supply this JSON as input to SqlExplaineratorCli:

  SqlExplaineratorCli --infile plan.json --outfile plan.svg
or
  cat plan.json | SqlPlainToImageCli > plan.svg


Example 2: To generate the diagram from an SQL statement, you will need to also supply a JDBC
connection string and any credentials required to connect, e.g.:

  SqlExplaineratorCli --jdbc jdbc:mysql://localhost/sakila --username root --password abc123 \
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

Hold your horses, it'll be in maven central soon.

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

