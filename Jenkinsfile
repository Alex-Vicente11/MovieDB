pipeline {
    agent any

    environment {
        ANDROID_HOME = '/opt/android-sdk'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare SDK') {
            steps {
                withCredentials([string(credentialsId: 'tmdb-token', variable: 'TMDB_TOKEN')]) {
                    sh '''
                echo "sdk.dir=$ANDROID_HOME" > local.properties
                echo "TMDB_TOKEN=$TMDB_TOKEN" >> local.properties
            '''
                }
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
            when {
                anyOf {
                    branch 'main'
                    changeRequest()
                }
            }
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
            echo 'Pipeline completado exitosamente!!'
        }
        failure {
            echo 'El pipeline falló, revisa el Console Output para más detalles.'
        }
    }
}