def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    throttleCategory = 'example'

    throttle([throttleCategory]) {

        node('master') {

            stage('Checkout SCM') {
                checkout scm
                echo "Branch name is ${env.BRANCH_NAME}\nTag name is ${env.TAG_NAME}"
            }

            if (env.TAG_NAME ==~ /\d+\.\d+\.\d+-release/) {
                release_number = env.TAG_NAME.split('-')[0]
                println ("Release Number = " + release_number)

                DockerRepositoryAddress='docker.io'
                stage('Docker Build') {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh """
                        docker login ${DockerRepositoryAddress} -u $DOCKER_USER -p $DOCKER_PASSWORD
                        docker build -t ${DOCKER_USER}/${pipelineParams.projectName}:${release_number} ./app/
                        docker push     ${DOCKER_USER}/${pipelineParams.projectName}:${release_number}
                        """
                    }
                }

                jenkinsAgentDockerfilePath = "${env.WORKSPACE}" + "@libs/" + "${pipelineParams.ext_lib_name}"
                jenkinsAgentDockerfileName = "${jenkinsAgentDockerfilePath}" + "/run-agent.dockerfile"
                jenkinsAgentBuildName = 'run-agent:latest'
                jenkinsAgentBuildArgs = ''
                jenkinsAgentRunArgs = " -u 0:0 -v ${jenkinsAgentDockerfilePath}:/mnt"

                def RunAgent = docker.build("${jenkinsAgentBuildName}", "${jenkinsAgentBuildArgs} -f ${jenkinsAgentDockerfileName} .")
                Nomad_Address = '192.168.0.112'

                stage('Deploy to Nomad') {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
                        RunAgent.inside("${jenkinsAgentRunArgs}") {
                            sh """
                            cd /mnt && \
                            ansible-playbook deploy.yml \
                            -e nomad_address=${Nomad_Address} \
                            -e service_name=${pipelineParams.projectName} \
                            -e service_image=${DOCKER_USER}/${pipelineParams.projectName}:${release_number}
                            """
                        }
                    }
                }
            }

            stage('Cleanup') {
                deleteDir()
            }

        }

    }

}