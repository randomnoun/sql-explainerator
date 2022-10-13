Note to self:

To regenerate the mvn site on github, use this from within cygwin:

```
/c/java/prog/apache-maven-3.8.4/bin/mvn -B --settings 'c:/users/knoxg/.m2/settings-randomnoun.xml' site site:stage scm-publish:publish-scm
```

And to start the release, run:

```
/c/java/prog/apache-maven-3.8.4/bin/mvn -B --settings 'c:/users/knoxg/.m2/settings-randomnoun.xml' -DpreparationGoals=clean "-Darguments=-DscmBranch=main" -DscmBranch=main release:clean release:prepare
```

The site:site-deploy task has been disabled in the pom.xml, because site-deploy can't deploy to git yet.

Sources: 

* https://blog.ediri.io/how-to-create-a-github-gh-pages-branch-in-an-existing-repository
* https://www.lorenzobettini.it/2020/01/publishing-a-maven-site-to-github-pages/


