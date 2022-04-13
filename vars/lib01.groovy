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

            stage('Cleanup') {
                deleteDir()
            }

        }

    }

}