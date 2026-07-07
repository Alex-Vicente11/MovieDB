pipeline {
    agent any

    environment {
        ANDROID_HOME = '/opt/android-sdk'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'git@github.com:Alex-Vicente11/MovieDB.git',
                    credentialsId: 'jenkins-moviedb-ssh'
            }
        }

        stage('Prepare SDK') {
            steps {
                sh 'echo "sdk.dir=$ANDROID_HOME" > local.properties'
                sh 'chmod +x gradlew'
            }
        }

        stage('Lint') {
            steps {
                sh './gradlew lint'
            }
        }

        stage('Unit Tests') {
            steps {
                sh './gradlew testDebugUnitTest'
            }
        }

        stage('Build APK') {
            steps {
                sh './gradlew assembleDebug'
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
            archiveArtifacts artifacts: '**/build/outputs/apk/debug/*.apk', fingerprint: true, allowEmptyArchive: true
        }
        success {
            echo 'Pipeline completado exitosamente'
        }
        failure {
            echo 'El pipeline falló, revisa el Console Output para más detalles.'
        }
    }
}