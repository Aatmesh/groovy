pipeline {
    agent {
        node {
            label "master"
        }
    }

    tools {nodejs 'node'}
    environment {
        APP_NAME = 'Vetravel - Web'
        GIT_URL = 'https://vernost123@bitbucket.org/vernosteam/vtravel-frontend-hotel-app.git'
        GIT_CREDENTIALS_ID = "be882718-d211-4233-b8be-30c96afebad5"
        EMAIL_RECIPIENTS = "aatmesh.kedar@vernost.in"
        REPLY_TO_RECIPIENTS = "aatmesh.kedar@vernost.in"
        
    }
    parameters {
        string(defaultValue: 'master/*', description: 'This parameter is used to identify the branch on which the build is created', name: 'BranchName', trim: true)
    }
    stages {
        stage('Cleanup Workspace') {
            steps {
                echo "Cleaning Up Workspace for Project"
                autoDeployment3()
                echo "Cleaned Up Workspace for Project"
            }
        }
    }
}

def autoDeployment3() {
    script {
        try {
            sh '''
                maxSize=0;
                desiredSize=0;
                newdesiredSize=0;
                terminatingCount=0;
                inServiceCount=0;
                
                if [[ $(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[DesiredCapacity][0][0]") != 2 ]]; then
                    echo "Auto Deployment Started..."

                    maxSize=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[MaxSize][0][0]")
                    desiredSize=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[DesiredCapacity][0][0]")
                    newdesiredSize=$(($desiredSize + $desiredSize))

                    echo $maxSize
                    echo $desiredSize
                    echo $newdesiredSize
                    
                    aws --region us-east-1 autoscaling set-desired-capacity --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --desired-capacity $newdesiredSize --honor-cooldown
                    sleep 20
            
                    while (($(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[Instances][0][0]" | grep -c InService) != $newdesiredSize))
                        do
                        echo "Auto Scaling In Progress"
                        sleep 10
                    done
                    
                    echo "Down Scaling Start"
                    sleep 300
                    aws --region us-east-1 autoscaling set-desired-capacity --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --desired-capacity $desiredSize --honor-cooldown
                    
                    sleep 30
                    terminatingCount=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[Instances][0][0]" | grep -c Terminating)
                    echo $terminatingCount
                    
                    inServiceCount=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[Instances][0][0]" | grep -c InService)
                    echo $inServiceCount
                    
                    while(($(aws --region us-east-1  autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[Instances][0][0]" | grep -c Terminating) != 0) && ($(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[Instances][0][0]" | grep -c InService) == $desiredSize))
                        do
                        echo "Down Scaling In Progress"
                        terminatingCount=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[Instances][0][0]" | grep -c Terminating)
                        echo $terminatingCount

                        inServiceCount=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name environment echo "$AWS_AUTO_SCALE_GRP_NAME" --query "AutoScalingGroups[].[Instances][0][0]" | grep -c InService)
                        echo $inServiceCount

                        sleep 20
                    done
                    
                    echo 'Build Auto Deployed Successfully!!!'
                    
                else
                    echo "Auto Deployment Desired Capacity is 0. Increase to greater than 1 to publish the artificats!!!"
                fi

            '''
        }
        catch (Exception ex) {
            echo 'Auto Build Deployment Failed'
            throw ex;
        }
    }
}




def autoDeployment2() {
    script {
        try {
            sh '''
                maxSize=0;
                desiredSize=0;
                newdesiredSize=0;
                terminatingCount=0;
                inServiceCount=0;
                
                if [[ $(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[DesiredCapacity][0][0]") != 2 ]]; then
                    echo "Auto Deployment Started..."

                    maxSize=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[MaxSize][0][0]")
                    desiredSize=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[DesiredCapacity][0][0]")
                    newdesiredSize=$(($desiredSize + $desiredSize))

                    echo $maxSize
                    echo $desiredSize
                    echo $newdesiredSize
                    
                    aws --region us-east-1 autoscaling set-desired-capacity --auto-scaling-group-name uaeexchange-asg-test --desired-capacity $newdesiredSize --honor-cooldown
                    sleep 20
            
                    while (($(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[Instances][0][0]" | grep -c InService) != $newdesiredSize))
                        do
                        echo "Auto Scaling In Progress"
                        sleep 10
                    done
                    
                    echo "Down Scaling Start"
                    sleep 300
                    aws --region us-east-1 autoscaling set-desired-capacity --auto-scaling-group-name uaeexchange-asg-test --desired-capacity $desiredSize --honor-cooldown
                    
                    sleep 30
                    terminatingCount=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[Instances][0][0]" | grep -c Terminating)
                    echo $terminatingCount
                    
                    inServiceCount=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[Instances][0][0]" | grep -c InService)
                    echo $inServiceCount
                    
                    while(($(aws --region us-east-1  autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[Instances][0][0]" | grep -c Terminating) != 0) && ($(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[Instances][0][0]" | grep -c InService) == $desiredSize))
                        do
                        echo "Down Scaling In Progress"
                        terminatingCount=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[Instances][0][0]" | grep -c Terminating)
                        echo $terminatingCount

                        inServiceCount=$(aws --region us-east-1 autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query "AutoScalingGroups[].[Instances][0][0]" | grep -c InService)
                        echo $inServiceCount

                        sleep 20
                    done
                    
                    echo 'Build Auto Deployed Successfully!!!'
                    
                else
                    echo "Auto Deployment Desired Capacity is 0. Increase to greater than 1 to publish the artificats!!!"
                fi

            '''
        }
        catch (Exception ex) {
            echo 'Auto Build Deployment Failed'
            throw ex;
        }
    }
}

def autoDeployment1() {
    script {
        try {
            echo 'Awaiting Input for Auto Deployment...'
           sh '''
           maxSize=0;
           desiredSize=0;
         
                if [[ $(aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test  --query 'AutoScalingGroups[].[DesiredCapacity][0][0]') != 1 ]]; then
echo "bucket exists"
 echo "Auto Deployment Started..."

                    maxSize=aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[MaxSize][0][0]" --profile ${env:AWS_PROFILE_NAME}
                    desiredSize=aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[DesiredCapacity][0][0]" --profile ${env:AWS_PROFILE_NAME}

                    echo $maxSize
                    echo $desiredSize
                    echo "Build Auto Deployed Successfully!!!"
else
echo "bucket does not exist or permission is not there to view it."
fi 
                   
                } else {
                    Write-Host "Auto Deployment Desired Capacity is 0. Increase to greater than 1 to publish the artificats!!!"
                }
            )
            '''

            
        } catch (Exception ex) {
            echo 'Auto Build Deployment Failed'
            sendFailureEmail('Auto Deployment')
            throw ex;
        }
    }
}


def autoDeployment() {
    script {
        try {
            echo 'Awaiting Input for Auto Deployment...'
            input message: 'Do you want to deploy the build?', ok: 'Yes, Auto Deploy'

            sh(script:'''
                if((aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name uaeexchange-asg-test --query 'AutoScalingGroups[].[Instances][0][0][0]' | grep -c InService !=0) {    
                    Write-Host "Auto Deployment Started..."

                    $maxSize = aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[MaxSize][0][0]" --profile ${env:AWS_PROFILE_NAME}
                    $desiredSize = aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[DesiredCapacity][0][0]" --profile ${env:AWS_PROFILE_NAME}

                    Write-Host $maxSize
                    Write-Host $desiredSize

                    aws --region ${env:AWS_PROFILE_REGION} autoscaling set-desired-capacity --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --profile ${env:AWS_PROFILE_NAME} --desired-capacity $maxSize --honor-cooldown
                    sleep 20

                    do {
                        Write-Host "Auto Scaling In Progress"
                        sleep 10
                    } while ((aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'InService'}).Count -ne $maxSize )
                    
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
                    } while (((aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'Terminating'}).Count !=0 ) -And ((aws autoscaling describe-auto-scaling-groups --auto-scaling-group-name ${env:AWS_AUTO_SCALE_GRP_NAME} --query "AutoScalingGroups[].[Instances][0][0]" --profile ${env:AWS_PROFILE_NAME} | ConvertFrom-Json | where {$_.LifecycleState -eq 'InService'}).Count -eq $desiredSize ))

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
