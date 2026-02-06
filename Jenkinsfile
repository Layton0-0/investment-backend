// CI: build, test, Docker image build, registry push.
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh './gradlew bootJar'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test'
            }
        }
        stage('Docker Build') {
            steps {
                script {
                    def tag = env.GIT_COMMIT?.take(7) ?: 'latest'
                    sh "docker build -t investment-backend:${tag} ."
                }
            }
        }
    }
}
