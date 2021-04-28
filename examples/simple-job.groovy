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

// You need to pass the script handler to the concrete job object,
// so that it can access the native methods provided by Jenkins.
new SimpleJob(jenkins: this).run()



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


class SimpleJobV3 extends BaseJob {
    void doRun() {
        jenkins.node {
            // Use the method define in `BaseJob` to simplify your script.
            gitSimpleCheckout(c('git') )
        }
    }
}

new SimpleJobV3(jenkins: this).run()
