# jenkins-oo-shared-lib

An un-opinionated Jenkins shared library built with the object-oriented
principles in mind.

I know there are already popular Jenkins share libraries, like
[Piper](https://github.com/SAP/jenkins-library) for example, are powerful and 
use by a lot of developers. But for solutions like Piper, one of the major
concern for a team to adopt them is that they are opinionated, especially
when the team have already had a bunch of pipeline scripts. In order to use 
those libraries, you need to not only have the knowledge of Jenkins, but also
knowledge of domain specific conventions and concepts defined by them, 
which would take you extra time to learn before you can start your own task.

In my opinion An idea Jenkins libraries should avoid those limitations mentioned 
above. That's why I design this share library to help me maintain my pipeline scripts.

I would suggest you to use it with 
[Jenkins-Job-Builder](https://jenkins-job-builder.readthedocs.io/en/latest/index.html)
if you want to get rid of the Jenkins' web UI to set up your pipeline.

## Goals

By creating this new Jenkins share library, my hope is that

- It must be un-opinionated, so that it can be easily adopted with little efforts.

- Any well-trained Java/Groovy programmers can start to use it without any extra 
  knowledge besides of Jenkins. That's why it is built with the popular 
  object-oriented paradigm.
  
- IDE friendly. IDE features like auto suggestions and refactor tools should 
  work correctly when you are working with this library.
  
## Installation

Follow the Jenkins official 
[guideline](https://www.jenkins.io/doc/book/pipeline/shared-libraries/#global-shared-libraries) 
to add this library in global or folder level. Then you can start to use it in
your own pipeline script. 
  
## Get Started

Given you already have a normal scripted pipeline that checkout the source code 
from github, which may look like this:

```groovy
node {
  checkout([
          $class: 'GitSCM',
          branches: [[name: '*/main']],
          extensions: [[$class: 'CleanCheckout']],
          userRemoteConfigs: [
                  [url: 'git@github.com:link89/jenkins-oo-shared-lib.git']
          ]
  ])
}
```

To use it with this library, what you need is import this library and write it 
this way
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
                      [url: 'git@github.com:link89/jenkins-oo-shared-lib.git']
              ]
      ])
    }
  }
}

// You need to pass the script handler to the job object,
// so that it can access the native methods provided by Jenkins.
new SimpleJob(jenkins: this).run()
```

Congratulations, you have already migrated your script to use this library by
just adding the `jenkins.` prefix to some Jenkins native methods.

Since the checkout is a common operation, I have already created a friendly 
method to do the same thing with less code.

```groovy
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

Now you may want to parameterize this job so that other users can decide the
project and branch to check out. Luckily, this shared library will load `yaml`
configuration for you automatically when it exists. What you need is add a 
multi-lines string field named `CONFIGS` to your job anf field with the 
following default value

```yaml
git:
  url: git@github.com:link89/jenkins-oo-shared-lib.git 
  branch: main
```

Now your your script will look like this
```groovy
class SimpleJobV3 extends BaseJob {
  void doRun() {
    jenkins.node {
      gitSimpleCheckout(c('git') )
    }
  }
}
new SimpleJobV3(jenkins: this).run()
```

See, you have already been good at using this library.
