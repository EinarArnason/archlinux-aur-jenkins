
def call(String url, String destination, String repoName, String buildCommand) {
    pipeline {
        agent any

        stages {
            stage('build') {
                steps {
                    git url
                    script {
                        def pkg = sh(returnStdout: true, script: "PKGDEST=${destination} makepkg --packagelist").trim()
                        def exitCode = sh(returnStatus: true, script: "if [ -f ${pkg} ]; then exit 13; else PKGDEST=${destination} ${buildCommand}; fi")
                        if (exitCode == 13) {
                            currentBuild.result = 'ABORTED'
                            error('Already built')
                        } else if (exitCode != 0) {
                            currentBuild.result = 'FAILURE'
                            error('Failed')
                        }
                    }
                }
            }
            
            stage('Deploy') {
                steps {
                    script {
                        def pkg = sh(returnStdout: true, script: "PKGDEST=${destination} makepkg --packagelist").trim()
                        def exitCode = sh(returnStatus: true, script: "repo-add ${destination}/${repoName}.db.tar.gz ${pkg}")
                        if (exitCode != 0) {
                            currentBuild.result = 'FAILURE'
                            error('Failed')
                        }
                    }
                }
            }
        }
    }
}
