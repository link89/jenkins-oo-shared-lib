package com.github.link89.jenkins

import com.cloudbees.groovy.cps.NonCPS
import org.yaml.snakeyaml.Yaml
import groovy.json.JsonBuilder

class BaseJob {
    static final Object EMPTY = new Object()

    /**
     * deep merge two map objects
     * @param lhs
     * @param rhs
     * @return
     */
    Map mergeMaps(PropertyValue.Map lhs, Map rhs) {
        rhs.each { k, v ->
            lhs[k] = (lhs[k] in Map ? mergeMaps(lhs[k], v) : v)
        }
        return lhs
    }

    /**
     *
     * @param data
     * @return
     */
    String dumpToJson(def data) {
        new JsonBuilder(data).toPrettyString()
    }

    /**
     * Properties
     */

    String getJenkinsBuildId() {
        jenkins.env.BUILD_ID
    }

    String getJenkinsBuildNumber() {
        jenkins.env.BUILD_NUMBER
    }

    String getJenkinsBuildUrl() {
        jenkins.env.BUILD_URL
    }

    String getJenkinsBuildTag() {
        jenkins.env.BUILD_TAG
    }

    String getJenkinsBuildDescription() {
        jenkins.currentBuild.getDescription()
    }

    void setJenkinsBuildDescription(String description) {
        jenkins.currentBuild.setDescription(description)
    }

    String getBuildResult() {
        jenkins.currentBuild.getResult() ?: internalResult
    }

    protected String yaml
    protected Map configs = [:]
    protected def jenkins
    protected String internalResult
    protected def exception
    protected long jobStartTime

    /**
     *  Default Values
     */

    @NonCPS
    protected String getDefaultConfigsParamName() { 'CONFIGS' }
    protected String getDefaultOverwriteParamName() {'OVERWRITE_CONFIGS'}
    protected int getDefaultStageTimeout() { 1800 }
    protected String getDefaultNodeJsVersion() { 'v12.22.1' }
    protected boolean getDryRun() { false }

    /**
     * The OVERWRITE_CONFIGS is useful when calling the job by another job,
     * It allows you to only set the values you want to modify
     * and use default for the other values.
     * @return
     */
    final Map getOverwriteConfigs() {
        yaml = jenkins.params[defaultOverwriteParamName]
        if(yaml) {
            Yaml parser = new Yaml()
            return parser.load(yaml)
        }
        return [:]
    }

    protected void loadConfigs() {
        yaml = jenkins.params[defaultConfigsParamName]
        if (yaml) {
            Yaml parser = new Yaml()
            configs = mergeMaps(parser.load(yaml), overwriteConfigs)
            logData(configs, 'configs')
        }
    }

    protected void init() {
        loadConfigs()
    }

    protected void onPostInit() {
        jenkins.echo "No onStart"
    }

    protected void onFailed() {
        jenkins.echo "No onFailed"
    }

    protected void onSuccess() {
        jenkins.echo "No onSuccess"
    }

    protected void onFinish() {
        jenkins.echo "No onFinish"
    }

    final void run() {
        jobStartTime = System.currentTimeMillis()
        jenkins.timestamps {
            try {
                init()
                onPostInit()
                doRun()
                internalResult = 'SUCCESS'
                onSuccess()
            } catch (Exception e) {
                internalResult = 'FAILED'
                exception = e
                onFailed()
                throw e
            } finally {
                onFinish()
            }
        }
    }

    /**
     * The subclass should overwrite this method to do the actual pipeline tasks
     */
    protected void doRun() {
        jenkins.error "overwrite me!"
    }

    /**
     * Read value from `this.configs`
     * null value is not allow unless set as defaultValue explicitly
     * @param path Data path, for example 'a.b.c'
     * @param defaultValue set default value
     * @return value
     */
    def c(String path, def defaultValue) {
        def value = this.configs
        for (String key in path.split('\\.')) {
            if (null == value) break
            value = value[key]
        }
        if(EMPTY.is(defaultValue)) {
            return notNull(value, "${path} is required")
        }
        return (null == value) ? defaultValue : value
    }

    def c(String path) {
        c(path, EMPTY)
    }

    /**
     * Ensure value is not null
     * @param value
     * @param message error message
     * @return
     */
    def notNull(def value, String message) {
        if (null == value) {
            jenkins.error message
        }
        return value
    }

    /**
     * Print data in json format
     * @param data
     * @param description
     * @return
     */
    void logData(def data, String description) {
        if (description) jenkins.echo description
        jenkins.echo(dumpToJson(data))
    }
    void logData(def data) {
        logData(data, '')
    }

    /**
     * hello world
     */
    void helloWorld() {
        jenkins.echo 'Hello World!'
    }

    /**
     * enhanced stage
     * @param options Optional options
     *   name:
     *   timeout: timeout in seconds
     *   activity: timeout if inactive for specific time
     * @param closure
     */
    void stage(Map options, Closure closure) {
        String name = notNull options?.name, 'name is required'
        int timeout = options?.timeout ?: defaultStageTimeout
        boolean activity = false == options?.activity

        jenkins.timeout(time: timeout, activity: activity, unit: 'SECONDS') {
            try {
                jenkins.stage(name, closure)
                jenkins.echo "Stage ${name} is successed!"
            } catch (e) {
                jenkins.echo "Stage ${name} is failed!"
                jenkins.error e.dump()
            }
        }
    }

    /**
     * delegate `jenkins.env`
     * @return
     */
    def getEnv() {
        jenkins.env
    }

    /**
     * delegate `jenkins.node`
     * @param fn
     * @return
     */
    def node(String label, Closure fn) {
        jenkins.node(label) {
            fn()
        }
    }

    /**
     * clean up git workspace without removing git
     */
    void gitDistClean() {
        jenkins.sh "git checkout -f || true"
        jenkins.sh "git clean -xdff || true"
    }

    /**
     * checkout code from git repository
     * @param config
     *
     * Example:
     * url: git@github.com:link89/jenkins-oo-shared-lib.git  # required
     * branch: develop # required
     * credentialId: git-ssh-credential-id  # required
     * namespace: # optional, default is origin
     * mergeTo:  # optional, merge to target branch automatically
     *   url:  # optional, default is `url`
     *   branch:  # optional, default is `branch`
     *   credentialId:  # optional, default is `credentialId
     *   namespace:  # optional, default is `namespace`
     * timeout: 30  # optional, timeout in minute, default is 30
     */
    void gitSimpleCheckout(def config) {
        logData(config, 'checkout parameters')
        String sourceUrl = notNull config.url, 'url is required'
        String sourceBranch = notNull config.branch, 'branch is required'
        String sourceCredentialId = notNull config.credentialId, 'credentialId is required'
        String sourceNamespace = config.namespace ?: 'origin'

        String targetUrl = config?.mergeTo?.url ?: sourceUrl
        String targetBranch = config?.mergeTo?.branch ?: sourceBranch
        String targetCredentialId = config?.mergeTo?.credentialId ?: sourceCredentialId
        String targetNamespace = config?.mergeTo?.namespace?: sourceNamespace
        int timeout = config.timeout ?: 30

        def checkoutParams = [
                $class           : 'GitSCM',
                branches         : [[name: "${sourceNamespace}/${sourceBranch}"]],
                extensions       : [
                        [$class: 'PruneStaleBranch'],
                        [$class: 'CloneOption', timeout: timeout],
                ],
                userRemoteConfigs: [
                        [
                                credentialsId: sourceCredentialId,
                                name         : sourceNamespace,
                                url          : sourceUrl,
                        ]
                ]
        ]

        if (sourceBranch != targetBranch || sourceUrl != targetUrl) {
            checkoutParams.extensions.add( [
                    $class : 'PreBuildMerge',
                    options: [
                            fastForwardMode: 'FF',
                            mergeRemote    : targetNamespace,
                            mergeTarget    : targetBranch,
                    ]
            ] )
        }
        if (sourceNamespace != targetNamespace) {
            checkoutParams.userRemoteConfigs.add( [
                    credentialsId: targetCredentialId,
                    name         : targetNamespace,
                    url          : targetUrl,
            ] )
        }

        logData(checkoutParams, 'checkout params')
        jenkins.checkout checkoutParams
    }

    String getGitHead(){
        jenkins.sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    }

    /**
     * retrieve git parameters set by Gitlab Trigger from environment variable
     * @return
     */
    def getGitParamsFromGitlabTrigger(String credentialId) {
        // It seems like a bug that checkout didn't respect namespaces when the job is triggered by gitlab
        // In order to workaround this issue, here we use the origin and origin1 as namespace instead of
        // env.gitlabSourceNamespace and env.gitlabTargetBranch

        if(!env.gitlabSourceBranch) return null
        [
                url: env.gitlabSourceRepoSshURL,
                branch: env.gitlabSourceBranch,
                namespace: 'origin',
                credentialId: credentialId,
                mergeTo: [
                        url: env.gitlabTargetRepoSshURL,
                        branch: env.gitlabTargetBranch,
                        namespace: 'origin1',
                ]
        ]
    }


    /**
     * a simple parser that turn input "what so ever --key1=value1 --key2=value2 --switch"
     * into map [ 'key1': 'value1', 'key2': 'value2', 'switch=true']
     * @param input string to be parsed
     * @param defaults default values for each option
     */
    def parseSimpleArgs(String input, Map<String, Object> defaults) {
        String [] tokens = input.split('\\s+--')  // tokenize
        tokens.eachWithIndex { token, i ->
            if (i) {
                String[] args = token.split('=')
                if (args.length == 1) {  // option without value
                    defaults[token] = true
                } else {  // option with value
                    defaults[args[0]] = args[1]
                }
            }
        }
        return defaults
    }
    def parseSimpleArgs(String input) {
        parseSimpleArgs(input, [:])
    }

    /**
     * Using rsync command to publish artifacts to remote directory
     * example: `rsyncPublish('build/', 'ssh://root@www.example.com/var/html')
     * use with `jenkins.sshagent` if necessary
     * @param sourcePath artifacts to publish
     * @param remoteUri remote ssh host, for example ssh://root@www.example.com:2222/var/html
     */
    void rsyncPublish(String sourcePath, String remoteUri) {
        URI remote = new URI(remoteUri)
        String remoteTarget = "${remote.userInfo}@${remote.host}:${remote.path}".toString()
        String cmd = "rsync -azvh --delete --progress -e 'ssh -o StrictHostKeyChecking=no -p ${remote.port > 0 ? remote.port : 22}' ${sourcePath} ${remoteTarget}"
        if (dryRun) {
            jenkins.echo cmd
        } else {
            jenkins.sh cmd
        }
    }

    /**
     * Sanitize input string so that it can be use in the domain name by replacing invalid chars with -
     * @param s input string
     * @return
     */
    String sanitizeDomainName(String input) {
        input.replaceAll("[^A-Za-z0-9 ]", "-").toLowerCase()
    }

    /**
     * create .gz from files that are compressible and greater than 150 bytes,
     * which can be used by Nginx's static gzip module.
     * Use with `jenkins.dir` if you want to create .gz file in specific directory
     */
    void gzipWebArtifactsForNginx() {
        jenkins.sh """find . -type f -size +150c \\( -name "*.wasm" -o -name "*.css" -o -name "*.html" -o -name "*.js" -o -name "*.json" -o -name "*.map" -o -name "*.svg"  -o -name "*.xml" \\) | xargs -I{} bash -c 'gzip -5 < {} > {}.gz'"""
    }

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
