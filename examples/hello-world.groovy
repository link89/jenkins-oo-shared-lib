@Library('jenkins-oo-shared-lib')
import com.github.link89.jenkins.BaseJob

class HelloWorldJob extends BaseJob {
    void doRun() {
        helloWorld()
    }
}

new HelloWorldJob(jenkins: this).run()
