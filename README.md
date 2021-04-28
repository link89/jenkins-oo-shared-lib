# jenkins-oo-shared-lib

An un-opinionated Jenkins shared library built with the object-oriented
principles in mind.

I know there are already popular Jenkins share libraries, like
[Piper](https://github.com/SAP/jenkins-library) for example, are powerful and 
popular. But for solutions like Piper, one of the common concern for a team 
to adopt them is that they are opinionated, especially to a team that have 
already had a bunch of pipeline scripts. In order to use those libraries, you 
need to not only have the knowledge of Jenkins, but also the knowledge of domain 
specific conventions and concepts defined by them, which would take you extra 
time before you can start to use it in your own task.

In my opinion, an ideal Jenkins libraries should avoid the restrictions mentioned 
above. That's why I design this share library to help me maintain my pipeline scripts.

I would suggest you to use it with 
[Jenkins-Job-Builder](https://jenkins-job-builder.readthedocs.io/en/latest/index.html)
if you want to get rid of the Jenkins' web UI to set up your pipeline.

## Goals

By creating this new Jenkins share library, my hope is that

- It should be un-opinionated, so that it can be easily adopted with little efforts.

- Any well-trained Java/Groovy programmers can start to use it without any extra 
  knowledge besides of Jenkins. That's why it is built with the popular 
  object-oriented paradigm.
  
- IDE friendly. IDE features like auto suggestions and refactor tools should 
  work correctly when you are working with this library.
  
## Installation

Follow the Jenkins' official 
[guideline](https://www.jenkins.io/doc/book/pipeline/shared-libraries/#global-shared-libraries) 
to add this library in global or folder level. Then you can start to use it in
your own pipeline script. 
  
## Get Started

### Migrate Your Scripts in 1 Minute

Given you already have a normal scripted pipeline that check out the source code 
from github, which may look like this:

```groovy
node {
  checkout([
          $class: 'GitSCM',
          branches: [[name: '*/main']],
          extensions: [[$class: 'CleanCheckout']],
          userRemoteConfigs: [
                  [url: 'git@github.com:link89/selenium-federation.git']
          ]
  ])
}
```

To migrate your script to this library, what you need is import this library to
your script, overwrite the `doRun` method by copy-paste your original script,
and add `jenkins.` to the steps functions provided by Jenkins.
```groovy

// Import this share library, you should add it to Jenkins before you start to use it.
@Library('jenkins-oo-shared-lib')
import com.github.link89.jenkins.BaseJob

// Start to use the method defined in BaseJob by deriving your new class from it
class SimpleJob extends BaseJob {
  void doRun() {
    // to access the native methods of Jenkins script via `jenkins`
    jenkins.node {
      jenkins.checkout([
              $class: 'GitSCM',
              branches: [[name: '*/main']],
              extensions: [[$class: 'CleanCheckout']],
              userRemoteConfigs: [
                      [url: 'git@github.com:link89/selenium-federation.git']
              ]
      ])
    }
  }
}

// You need to pass the script handler to the job object,
// so that it can access the native methods provided by Jenkins.
new SimpleJob(jenkins: this).run()
```

See, it doesn't take too much effort to migrate your script to this library.

### Using this Shared Library in Your Script

Since the checkout is a common operation, I have already provided a friendly 
method to do the same thing with less code. Now your can rewrite your checkout 
method to make your script more clean.

```groovy
@Library('jenkins-oo-shared-lib')
import com.github.link89.jenkins.BaseJob

class SimpleJobV2 extends BaseJob {
  void doRun() {
    jenkins.node {
      // Use the method define in `BaseJob` to simplify your script.
      gitSimpleCheckout([
              url: 'git@github.com:link89/jenkins-oo-shared-lib.git',
              branch: 'master',
      ])
    }
  }
}
new SimpleJobV2(jenkins: this).run()
```

Now you may want to parameterize the job so that other users can decide what  
project and branch to check out. Luckily, this shared library will load `yaml`
configuration for you automatically when it exists. What you need is add a 
multi-lines string field named `CONFIGS` to your job and fill it with the 
following default value

```yaml
git:
  url: git@github.com:link89/selenium-federation.git 
  branch: main
```

Now your script will look like this
```groovy
@Library('jenkins-oo-shared-lib')
import com.github.link89.jenkins.BaseJob

class SimpleJobV3 extends BaseJob {
  void doRun() {
    jenkins.node {
      // `c` is a magic method to help you read value from CONFIGS
      gitSimpleCheckout(c('git') )
    }
  }
}
new SimpleJobV3(jenkins: this).run()
```

### Implement Your Own Share Methods

Let's move forward. Suppose the project you are working on is a nodejs project,
that use `nvm` to manage the nodejs dependency. You may find there is a Jenkins
plugin named [nvm-wrapper](https://plugins.jenkins.io/nvm-wrapper/), but it 
didn't support to read the version from `.nvmrc` file. You know this will be 
useful for other nodejs projects. Then you can implement some helper method in
the `BaseJob` class this way

```groovy
class BaseJob {
  /* ... */
  /* ... */

  protected String getDefaultNodeJsVersion() { 'v12.22.1' }
    
  /**
   * A helper method to read NodeJs version from .nvmrc file
   * It is suppose to use with Jenkins nvm plugin
   * @return NodeJs version
   */
  String getNodeJsVersionFromNvmrc() {
    try {
      // the trim is essential or else the nvm plugin may fail!
      return jenkins.readFile('.nvmrc').trim()
    } catch (Exception e) {
      return defaultNodeJsVersion
    }
  }
}
```

And use it in your script:

```groovy
class SimpleJobV4 extends BaseJob {
  void doRun() {
    jenkins.node {
      gitSimpleCheckout(c('git') )
      jenkins.nvm(nodeJsVersionFromNvmrc) {
        jenkins.sh 'npm install'
        jenkins.sh 'npm publish'
      }
    }
  }
}
```