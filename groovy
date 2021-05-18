#!/usr/bin/env groovy

// Pipeline
pipeline {
    agent {
        node {
            label "master"
        }
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '20'))
        timestamps()
        skipStagesAfterUnstable()
        disableConcurrentBuilds()
    }
    tools {nodejs "NodeJS 12.18.3"}
    environment {
        APP_NAME = 'UB - Web'
        BUILD_ENVIRONMENT = 'UAT'
        GIT_URL = 'https://github.com/Aatmesh/groovy.git'
        GIT_CREDENTIALS_ID = "aatmesh"
        EMAIL_RECIPIENTS = "aatmesh.kedar@vernost.in"
        REPLY_TO_RECIPIENTS = "aatmesh.kedar@vernost.in"
    }
         stages {
                 stage('One') {
                 steps {
                     echo 'Hi, this is Zulaikha from edureka'
                 }
                 }
                 stage('Two') {
                 steps {
                    input('Do you want to proceed?')
                 }
                 }
                 stage('Three') {
                 when {
                       not {
                            branch "master"
                       }
                 }
                 steps {
                       echo "Hello"
                 }
                 }
                 stage('Four') {
                 parallel { 
                            stage('Unit Test') {
                           steps {
                                echo "Running the unit test..."
                           }
                           }
                            stage('Integration test') {
                              agent {
                                    docker {
                                            reuseNode true
                                            image 'ubuntu'
                                           }
                                    }
                              steps {
                                echo "Running the integration test..."
                              }
                           }
                           }
                           }
              }
}
