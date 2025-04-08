pipeline {
    tools {
        maven 'maven-latest'

    }

    environment {
      //  registry = 'haythem25/khlail-2'
        registryCredential = 'dockerhub-credentials'
        dockerImage = ''
        DOCKER_REPO = "haythem25/school-app"

    }

    agent any

    stages {
        stage('CHECKOUT GIT') {
            steps {
                checkout scmGit(branches: [[name: '*/**']], extensions: [], userRemoteConfigs: [[credentialsId: 'github-credentials', url: 'https://github.com/haythem-farjallah/school-app.git']])
            }
        }

        stage('MVN CLEAN') {
            steps {
                dir('backend'){
                    sh 'mvn clean'
                }
            }
        }

        stage('ARTIFACT CONSTRUCTION') {
            steps {
                echo 'ARTIFACT CONSTRUCTION...'
                dir('backend') {
                    sh 'mvn package -Dmaven.test.skip=true -P test-coverage'
                }
            }
        }

        stage('UNIT TESTS') {
            steps {
                echo 'Launching Unit Tests...'
                dir('backend') {
                    sh 'mvn test'
                }
            }
        }

        stage('MVN SONARQUBE') {
            steps {
                dir('backend') {
                    withCredentials([usernamePassword(credentialsId: 'sonarqube-credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'mvn sonar:sonar -Dsonar.login=$USERNAME -Dsonar.password=$PASSWORD -Dsonar.host.url=http://sonarqube:9000'
                    }
                }
            }
        }

        stage('BUILDING OUR IMAGES') {
            steps {
                script {

                    // Build the backend image
                    def backendImage = docker.build("${DOCKER_REPO}:backend-latest", "./backend")

                    // Build the front image
                    def frontImage = docker.build("${DOCKER_REPO}:front-latest", "./frontend")

                    // Save the image IDs for the next stage
                    env.BACKEND_IMAGE_ID = backendImage.id
                    env.FRONT_IMAGE_ID = frontImage.id
                }
            }
        }

        stage('PUSH OUR IMAGES') {
            steps {
                script {
                    docker.withRegistry('', registryCredential) {
                        // Push the backend image
                        docker.image("${DOCKER_REPO}:backend-latest").push()

                        // Push the front image
                        docker.image("${DOCKER_REPO}:front-latest").push()
                    }
                }
            }
        }
        stage('DEPLOYS APPS') {
            steps {
                script {
                    dir('docker') {
                        sh 'docker-compose -f docker-compose.yml up -d'
                    }
                }
            }
        }






    }

//    post {
//        always {
//            emailext(
//            subject: "${currentBuild.result}: Job ${JOB_NAME} [${BUILD_NUMBER}]",
//            body: 'The build is complete.',
//            to: 'khalilouchari12@gmail.com',
//            from: 'haythemfarjallah4@gmail.com'
//            )
//        }
//    }
}
