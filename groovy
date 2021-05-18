pipeline {
         agent any
         stages{
                 stage("Git Checkout") {
                 steps {
                    git credentialsId: 'github', url: 'https://github.com/Aatmesh/groovy.git'
                    }
                    }
                 }
                 }
