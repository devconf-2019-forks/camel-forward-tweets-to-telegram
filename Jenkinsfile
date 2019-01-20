mvn = "mvn -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -s settings.xml"
cicdProject = "tweet-forwarder-cicd"
productionProject = "tweet-forwarder-prod"
pipeline {
    agent {
        label 'maven'
    }
    stages {
        stage('Build') {
            steps {
                script {
                    /* Maven build */
                    sh ("${mvn} clean package -Popenshift")
                }
            }
        }
        stage('Production') {
            steps {
                script {
                    openshift.withCluster() {
                        openshift.withProject(productionProject) {

                            openshift.tag("${cicdProject}/camel-forward-tweets-to-telegram:2.22.2", "${productionProject}/camel-forward-tweets-to-telegram:production")

                            def dc = openshift.selector("dc/camel-forward-tweets-to-telegram")
                            if (!dc.exists()) {
                                /* The deploymentconfig does not exist yet - the pipeline is probably run for the first time */

                                /* Create the secret using the following command
                                   oc create secret generic telegram-camel-demo-bot \
                                     "--from-literal=telegramAuthorizationToken=<your-telegram-token>" \
                                     -n tweet-forwarder-prod
                                 */
                                def telegramAuthorizationToken = openshift.selector("secret/telegram-camel-demo-bot").object().data.telegramAuthorizationToken
                                telegramAuthorizationToken = sh(script: "echo ${telegramAuthorizationToken} | base64 --decode", returnStdout: true)
                                def serviceApp = openshift.newApp(
                                    "--name=camel-forward-tweets-to-telegram",
                                    "-e", "telegramAuthorizationToken=${telegramAuthorizationToken}",
                                    "-i", "${productionProject}/camel-forward-tweets-to-telegram:production"
                                )
                                serviceApp.narrow('svc').expose()
                            } else {
                                /* nothing to do here: the deploymentconfig exists
                                 * and a new deployment was already triggered by tagging the image above */
                            }

                            /* Use Spring Boot Actuator's /health endpoint as a readiness and liveness probe */
                            openshift.set('probe', "dc/camel-forward-tweets-to-telegram", '--readiness',
                                '--get-url=http://:8080/actuator/health', '--initial-delay-seconds=5', '--period-seconds=2')
                            openshift.set('probe', "dc/camel-forward-tweets-to-telegram", '--liveness',
                                '--get-url=http://:8080/actuator/health', '--initial-delay-seconds=15', '--period-seconds=2')

                            /* The deployment triggered by new-app or push to image stream needs some time to finish
                             * oc rollout status --watch makes this script wait till the deployment is ready */
                            dc.rollout().status('--watch')

                            def route = openshift.selector('route/camel-forward-tweets-to-telegram')
                            def serviceUrl = 'http://' + route.object().spec.host
                            echo 'Camel app deployed: ' + serviceUrl
                        }
                    }
                }
            }
        }
    }
}
