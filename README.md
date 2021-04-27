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

