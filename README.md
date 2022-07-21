# explain-to-image

**explain-to-image**  converts query optimisation plans ( the output from an 'EXPLAIN' statement ) into pretty diagrams

It's based on the 'visual explain' pane in [MySQL Workbench](https://www.mysql.com/products/workbench/), and looks like this:

( image )

## Why would anyone want to do that  ?

Because diagrams are easier to comprehend than the tabular or JSON 'explain' output you get in MySQL.

And becaue it's in java, so I might be able to hook it into a running app server to generate these things on the fly rather than booting up a desktop app and copying SQL across.

## How do I use it ? 

Usage text:
```
C:\util\java> java -jar explain-to-image-0.0.1-with-dependencies.jar
Missing required option: j
usage: ExplainToImageCli [options]
 -h,--help                      This usage text
 -j,--jdbc <jdbc>               JDBC connection string
 -u,--username <username>       JDBC username
 -p,--password <password>       JDBC password
 -d,--driver <driver>           JDBC driver class name; default = com.mysql.jdbc.Driver
 -s,--sql <sql>               the SQL to explain
etc

```

And an example command line:

```
C:\util\java> java -jar explain-to-image-0.0.1-with-dependencies.jar --jdbc jdbc:mysql://localhost/datatype-dev --username root --password abc123 --sql SELECT 1 FROM DUAL
[... output here ...]
```

## Where can I get it ? 

TBD

## What databases can I run this on ?

TBD
## Doesn't this already exist ?

Well, MySQL Workbench exists, but that's not so easy to use from within a webapp.

But yes, you'd imagine this would already exist somewhere, wouldn't you.

TBD




## Licensing

historytable-cli is licensed under the BSD 2-clause license.

