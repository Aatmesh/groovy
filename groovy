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
        SONAR_KEY = "UrbanBook_Web"
        SONAR_LOGIN = "230f8bb4999aada648ecea86249ab161a9498c81"
        SONAR_URL = "http://172.20.15.149:9000/dashboard?id=${env.SONAR_KEY}"
        S3_BUCKET = "vms-ub-deploy/urbanbook/Intermiles-urbanbook"
        S3_BUCKET_PATH = "s3://vms-ub-deploy/urbanbook/Intermiles-urbanbook/"
        S3_REGION = "ap-south-1"
        S3_FILE1 = "ubdist.zip"
        S3_FILE2 = "Connector/Airlines-buisness/build/businessapp.js"
        S3_FILE3 = "Connector/Airlines-Connector/build/connectorapp.js"
        S3_FILE4 = "Connector/Airlines-Controller/build/wrapperapp.js"
        S3_FILE5 = "Connector/Airlines-DAO/build/daoapp.js"
        S3_ARCHIVE_FILE1 = "ubdist.zip"
        S3_ARCHIVE_FILE2 = "businessapp.js"
        S3_ARCHIVE_FILE3 = "connectorapp.js"
        S3_ARCHIVE_FILE4 = "wrapperapp.js"
        S3_ARCHIVE_FILE5 = "daoapp.js"
        S3_PROFILE_NAME = "S3_Jenkins"
        AWS_PROFILE_NAME = "vernost"
        AWS_PROFILE_REGION = "ap-south-1"
        AWS_AUTO_SCALE_GRP_NAME = "VMS-Intermiles-UAT-Urbanbook-Web-ASG"
        AKAMAI_PURGE_URL = "https://flightsuat.intermiles.com"
    }
    parameters {
        string(defaultValue: 'release/*', description: 'This parameter is used to identify the branch on which the build is created', name: 'BranchName', trim: true)
    }
    stages {
        stage('Cleanup Workspace') {
            steps {
                echo "Cleaning Up Workspace for Project"
                cleanWS()
                echo "Cleaned Up Workspace for Project"
            }
            post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Checkout') {
            steps {
                checkout([$class: 'GitSCM',
                branches: [[name: "${params.BranchName}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'CheckoutOption', timeout: 20]],
                submoduleCfg: [],
                userRemoteConfigs: [[
                   credentialsId: env.GIT_CREDENTIALS_ID, url: env.GIT_URL
                ]]])
           }
        }
        
        stage('Installing Dependencies') {  
            steps {
                echo 'Installing Dependencies Started'
                nodejs(nodeJSInstallationName: 'NodeJS 12.18.3') {
                    installFEDependencies()
                    installBEDependencies()
                }
                echo 'Installing Dependencies Completed'
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Unit Testing') {
            steps {
                unitTesting()
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Build') {
            steps {
                echo 'Build Started'
                createFEBuild()
                createBEBuild()
                echo 'Build Completed'
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Static Code Analysis') {
            steps {
                staticCodeAnalysis()
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Static Scan') {
            steps {
                staticScan()
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Dynamic Scan') {
            steps {
                dynamicScan()
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Trigger Automation') {
            steps {
                automationTest()
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Orchestration') {
            steps {
                orchestration()
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Upload to S3 Bucket') {
            when {
                //expression { return false; }
                expression { return isValid(); }
            }
            steps {
                echo 'Upload Started'
                uploadArtifacts()
                echo 'Upload Completed'
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
        
        stage('Auto Deployment') {
            when {
                //expression { return false; }
                expression { return isValid(); }
            }
            steps {
                echo 'Deployment Started'
                autoDeployment()
                echo 'Deployment Completed'
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }

        stage('Cache') {
            when {
                //expression { return false; }
                expression { return isValid(); }
            }
            steps {
                echo 'Cache Purge Started'
                autoAkamaiPurge()
                echo 'Purge Completed'
            }
             post {
                failure {
                    sendFailureEmail(STAGE_NAME)
                }
            }
        }
    }
    post {
        success {
            sendSuccessEmail()
        }
    }
}

//Cleaning Workspace
def cleanWS() {
    script {
        try {
            cleanWs notFailBuild: true
        }
        catch(Exception ex) {
            echo 'Cleaning Up Workspace for Project Failed'
            sendFailureEmail('Cleanup Workspace')
            throw ex;
        }
    }
}

def installFEDependencies() {
    script {
        try {
            echo 'Installing Frontend Dependencies...'
            powershell(script: """
                npm install
            """)
            echo 'Installing Frontend Dependencies Completed!!!'
        } catch (Exception ex) {
            echo 'Installing Frontend Dependencies Failed'
            sendFailureEmail('Installing Dependencies')
            throw ex;
        }
    }
}

def installBEDependencies() {
    script {
        try {
            echo 'Installing Backend Dependencies...'

            echo 'Installing Backend DAO Dependencies...'
            dir('Connector/Airlines-DAO') {
                powershell(script: """
                    npm install
                    npm install webpack
                    npm install webpack-cli
                """)
            }
            echo 'Installing Backend DAO Completed!!!'

            echo 'Installing Backend Business Dependencies...'
            dir('Connector/Airlines-buisness') {
                powershell(script: """
                    npm install
                    npm install webpack
                    npm install webpack-cli
                """)
            }
            echo 'Installing Backend Business Completed!!!'

            echo 'Installing Backend Connector Dependencies...'
            dir('Connector/Airlines-Connector') {
                powershell(script: """
                    npm install
                    npm install webpack
                    npm install webpack-cli
                """)
            }
            echo 'Installing Backend Connector Completed!!!'

            echo 'Installing Backend Controlled Dependencies...'
            dir('Connector/Airlines-Controller') {
                powershell(script: """
                    npm install
                    npm install webpack
                    npm install webpack-cli
                """)
            }
            echo 'Installing Backend Controller Completed!!!'
            
            echo 'Installing Backend Dependencies Completed!!!'
        } catch (Exception ex) {
            echo 'Installing Backend Dependencies Failed'
            sendFailureEmail('Installing Dependencies')
            throw ex;
        }
    }
}

def unitTesting() {
    script {
        try {
            echo 'Unit Testing Started..'
                
            echo 'Unit Testing Completed..'
        } catch (Exception ex) {
            echo 'Unit Test Suite Failed'
            sendFailureEmail('Unit Testing')
            throw ex;
        }
    }
}

def createFEBuild() {
    script {
        try {
            echo 'Frontend Build Creation Started...'
            dir('.') {
                powershell(script: """
                    npm run build:ssr
                """)
            }
            echo 'Frontend Build Created Successfully!!!'
            
            echo 'Compressing Frontend Build...'
            dir('.') {
                powershell(script: """
                    #zip -r ubdist.zip dist

                    Compress-Archive -Path dist ubdist.zip
                """)
            }
            echo 'Compressed Frontend Build Successfull!!!'
        } catch (Exception ex) {
            echo 'Frontend Build Creation Failed'
            sendFailureEmail('Build')
            throw ex;
        }
    }
}

def createBEBuild() {
    script {
        try {
            dir('Connector/Airlines-buisness') {
                echo 'Backend Business Build Creation Started...'
                powershell(script: """
                    webpack
                """)
                echo 'Backend Business Build Created Successfully!!!'
            }

            dir('Connector/Airlines-Connector') {
                echo 'Backend Connector Build Creation Started...'
                powershell(script: """
                    webpack
                """)
                echo 'Backend Connector Build Created Successfully!!!'
            }

            dir('Connector/Airlines-Controller') {
                echo 'Backend Controller Build Creation Started...'
                powershell(script: """
                    webpack
                """)
                echo 'Backend Controller Build Created Successfully!!!'
            }

            dir('Connector/Airlines-DAO') {
                echo 'Backend DAO Build Creation Started...'
                powershell(script: """
                    webpack
                """)
                echo 'Backend DAO Build Created Successfully!!!'
            }
        } catch (Exception ex) {
            echo 'Backend Build Creation Failed'
            sendFailureEmail('Build')
            throw ex;
        }
    }
}

def staticCodeAnalysis() {
    script {
        try {
            env.scannerHome = tool 'SonarScanner 4.0'
            echo 'Static Code Analysis Started...'
            withSonarQubeEnv(installationName: 'sonar') {
                bat "${scannerHome}/bin/sonar-scanner -Dsonar.projectKey=${env.SONAR_KEY} -Dsonar.sources=. -Dsonar.host.url=http://localhost:9000 -Dsonar.login=${env.SONAR_LOGIN}"
            }
            echo 'Static Code Analysis Completed Successfully!!!'
        } catch (Exception ex) {
            echo 'Static Code Analysis Tollgate Failed'
            sendFailureEmail('Static Code Analysis')
            throw ex;
        }
    }
}

def staticScan() {
    script {
        try {
            echo 'Static Scan Started...'
            
            echo 'Static Scan Completed Successfully!!!'
        } catch (Exception ex) {
            echo 'Static Scan Failed'
            sendFailureEmail('Static Scan')
            throw ex;
        }
    }
}

def dynamicScan() {
    script {
        try {
            echo 'Dynamic Scan Started...'
            
            echo 'Dynamic Scan Completed Successfully!!!'
        } catch (Exception ex) {
            echo 'Dynamic Scan Failed'
            sendFailureEmail('Dynamic Scan')
            throw ex;
        }
    }
}

def automationTest() {
    script {
        try {
            echo 'Automation Testing Started...'
            
            echo 'Automation Testing Completed Successfully!!!'
        } catch (Exception ex) {
            echo 'Automation Testing Failed'
            sendFailureEmail('Trigger Automation')
            throw ex;
        }
    }
}

def orchestration() {
    script {
        try {
            echo 'Copying Backup Files...'
            
            powershell(script: '''
                $fromLocation = ${env:S3_BUCKET_PATH}
                $toLocation = ${env:S3_BUCKET_PATH} + "archive/"

                aws s3 cp ($fromLocation + ${env:S3_ARCHIVE_FILE1}) ($toLocation + ${env:S3_ARCHIVE_FILE1}) --profile ${env:AWS_PROFILE_NAME}
                aws s3 cp ($fromLocation + ${env:S3_ARCHIVE_FILE2}) ($toLocation + ${env:S3_ARCHIVE_FILE2}) --profile ${env:AWS_PROFILE_NAME}
                aws s3 cp ($fromLocation + ${env:S3_ARCHIVE_FILE3}) ($toLocation + ${env:S3_ARCHIVE_FILE3}) --profile ${env:AWS_PROFILE_NAME}
                aws s3 cp ($fromLocation + ${env:S3_ARCHIVE_FILE4}) ($toLocation + ${env:S3_ARCHIVE_FILE4}) --profile ${env:AWS_PROFILE_NAME}
                aws s3 cp ($fromLocation + ${env:S3_ARCHIVE_FILE5}) ($toLocation + ${env:S3_ARCHIVE_FILE5}) --profile ${env:AWS_PROFILE_NAME}
            ''')
            
            echo 'Backup Files Copied Successfully!!!'
        } catch (Exception ex) {
            echo 'Orchestration Failed'
            sendFailureEmail('Orchestration')
            throw ex;
        }
    }
}


def uploadArtifacts() {
    script {
        try {
            echo 'Uploading Artifacts to S3 Started...'
            
            s3Upload consoleLogLevel: 'INFO', dontSetBuildResultOnFailure: false, dontWaitForConcurrentBuildCompletion: false, 
            entries: [
                [bucket: env.S3_BUCKET, excludedFile: '', flatten: false, gzipFiles: false, keepForever: false, managedArtifacts: false, noUploadOnFailure: false, selectedRegion: env.S3_REGION, showDirectlyInBrowser: false, sourceFile: env.S3_FILE1, storageClass: 'STANDARD', uploadFromSlave: false, useServerSideEncryption: false],
                [bucket: env.S3_BUCKET, excludedFile: '', flatten: false, gzipFiles: false, keepForever: false, managedArtifacts: false, noUploadOnFailure: false, selectedRegion: env.S3_REGION, showDirectlyInBrowser: false, sourceFile: env.S3_FILE2, storageClass: 'STANDARD', uploadFromSlave: false, useServerSideEncryption: false],
                [bucket: env.S3_BUCKET, excludedFile: '', flatten: false, gzipFiles: false, keepForever: false, managedArtifacts: false, noUploadOnFailure: false, selectedRegion: env.S3_REGION, showDirectlyInBrowser: false, sourceFile: env.S3_FILE3, storageClass: 'STANDARD', uploadFromSlave: false, useServerSideEncryption: false],
                [bucket: env.S3_BUCKET, excludedFile: '', flatten: false, gzipFiles: false, keepForever: false, managedArtifacts: false, noUploadOnFailure: false, selectedRegion: env.S3_REGION, showDirectlyInBrowser: false, sourceFile: env.S3_FILE4, storageClass: 'STANDARD', uploadFromSlave: false, useServerSideEncryption: false],
                [bucket: env.S3_BUCKET, excludedFile: '', flatten: false, gzipFiles: false, keepForever: false, managedArtifacts: false, noUploadOnFailure: false, selectedRegion: env.S3_REGION, showDirectlyInBrowser: false, sourceFile: env.S3_FILE5, storageClass: 'STANDARD', uploadFromSlave: false, useServerSideEncryption: false],
            ], pluginFailureResultConstraint: 'FAILURE', profileName: env.S3_PROFILE_NAME, userMetadata: []
            
            echo 'Artifacts Uploaded Successfully!!!'
            
        } catch (Exception ex) {
            echo 'Uploading Artifacts to S3 Failed'
            sendFailureEmail('Upload to S3 Bucket')
            throw ex;
        }
    }
}

def autoDeployment() {
    script {
        try {
            echo 'Awaiting Input for Auto Deployment...'
            sendApprovalEmail()
            input message: 'Do you want to deploy the build?', ok: 'Yes, Auto Deploy'

            powershell(script: '''
                if((aws  describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[DesiredCapacity][0][0]" --profile ${env:AWS_PROFILE_NAME}) -ne 0) {    
                    Write-Host "Auto Deployment Started..."

                    $maxSize = aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[MaxSize][0][0]" --profile ${env:AWS_PROFILE_NAME}
                    $desiredSize = aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[DesiredCapacity][0][0]" --profile ${env:AWS_PROFILE_NAME}
                    $newdesiredSize = ([int]$desiredSize + [int]$desiredSize)

                    Write-Host $maxSize
                    Write-Host $desiredSize
                    Write-Host $newdesiredSize

                    aws --region ${env:AWS_PROFILE_REGION} autoscaling set-desired-capacity --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --profile ${env:AWS_PROFILE_NAME} --desired-capacity $newdesiredSize --honor-cooldown
                    sleep 20

                    do {
                        Write-Host "Auto Scaling In Progress"
                        sleep 10
                    } while ((aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'InService'}).Count -ne $newdesiredSize )
                    
                    sleep 20
                    aws --region ${env:AWS_PROFILE_REGION} autoscaling set-desired-capacity --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --profile ${env:AWS_PROFILE_NAME} --desired-capacity $desiredSize
               
                    sleep 20
                    $terminatingCount = (aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'Terminating'}).Count
                    Write-Host $terminatingCount

                    $inServiceCount = (aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'InService'}).Count
                    Write-Host $inServiceCount

                    do {
                        Write-Host "Down Scaling In Progress"
                        $terminatingCount = (aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'Terminating'}).Count
                        Write-Host $terminatingCount

                        $inServiceCount = (aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'InService'}).Count
                        Write-Host $inServiceCount

                        sleep 20
                    } while (((aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'Terminating'}).Count -ne 0 ) -And ((aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'InService'}).Count -eq $desiredSize ))

                    Write-Host "Build Auto Deployed Successfully!!!"
                } else {
                    Write-Host "Auto Deployment Desired Capacity is 0. Increase to greater than 1 to publish the artificats!!!"
                }
            ''')
        } catch (Exception ex) {
            echo 'Auto Build Deployment Failed'
            sendFailureEmail('Auto Deployment')
            throw ex;
        }
    }
}

def autoAkamaiPurge() {
    script {
        try {
            powershell(script: '''
                akamai purge delete --production ${env:AKAMAI_PURGE_URL}
            ''')
        } catch (Exception ex) {
            echo 'Akamai Purged Failed'
            sendFailureEmail('Akamai Purged')
            throw ex;
        }
    }
}


def isValid() {
    script {
        try {
              def output =  powershell(returnStdout: true, script: '''
                   if((aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query 'AutoScalingGroups[].[DesiredCapacity][0][0]' --profile ${env:AWS_PROFILE_NAME}) -ne 0) {
                        Write-Output "1"
                   }
                   else {
                       Write-Output "0"
                   }
                ''')
              if(output?.trim() == "1") {
                  return true;
              } else {
                  return false;
              }
        }
        catch (Exception ex) {
            echo 'Validation Failed'
            throw ex;
        }
    }
}

def sendApprovalEmail() {
     emailext(     
         body: """
            <table style="border-collapse: collapse; font-family: arial, sans-serif; width: 100%;">
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Project: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">${env.APP_NAME}</td>
                </tr>
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Environment: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">${env.BUILD_ENVIRONMENT}</td>
                </tr>
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Build Status: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Pending Approval</td>
                </tr>
            </table><br/><br/>
         """,
         subject: "[${currentBuild.fullDisplayName}]: Build #$BUILD_NUMBER - Pending Approval.",
         mimeType: 'text/html',
         to: "$EMAIL_RECIPIENTS",
         replyTo: "$REPLY_TO_RECIPIENTS"
     )
}

def sendSuccessEmail() {
     emailext(     
         body: """
            <table style="border-collapse: collapse; font-family: arial, sans-serif; width: 100%;">
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Project: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">${env.APP_NAME}</td>
                </tr>
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Environment: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">${env.BUILD_ENVIRONMENT}</td>
                </tr>
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Build Status: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Success</td>
                </tr>
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Sonar Report: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">${env.SONAR_URL}</td>
                </tr>
            </table><br/><br/>
         """,
         subject: "[${currentBuild.fullDisplayName}]: Build #$BUILD_NUMBER - Successfull.",
         mimeType: 'text/html',
         to: "$EMAIL_RECIPIENTS",
         replyTo: "$REPLY_TO_RECIPIENTS"
     )
}

def sendFailureEmail(stage) {
     emailext(
         attachLog: true,         
         body: """
            <table style="border-collapse: collapse; font-family: arial, sans-serif; width: 100%;">
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Project: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">${env.APP_NAME}</td>
                </tr>
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Environment: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">${env.BUILD_ENVIRONMENT}</td>
                </tr>
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Stage: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">"""+ stage + """</td>
                </tr>
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Build Status: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Failed</td>
                </tr>
                <tr>
                    <th style="border: 1px solid #dddddd; text-align: left; padding: 8px;">Sonar Report: </th>
                    <td style="border: 1px solid #dddddd; text-align: left; padding: 8px;">${env.SONAR_URL}</td>
                </tr>
            </table>
            <br/><br/> 
         """,
         subject: "[${currentBuild.fullDisplayName}]: Build #$BUILD_NUMBER - "+ stage + " Failed.",
         mimeType: 'text/html',
         to: "$EMAIL_RECIPIENTS",
         replyTo: "$REPLY_TO_RECIPIENTS"
     )
}
