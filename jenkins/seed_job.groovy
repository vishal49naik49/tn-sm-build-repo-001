import javaposse.jobdsl.dsl.DslException
import jenkins.model.Jenkins
import hudson.model.AbstractProject

// Check if AWS credential parameter is passed or not
def awsCredentialId = getBinding().getVariables()['AWS_CREDENTIAL']
if (awsCredentialId == null) {
    throw new DslException('Please pass AWS credential parameter ' + 'AWS_CREDENTIAL' )
}

// Sagemaker Project specific details
def sagemakerProjectName = "tn-sm-demo-project-001"
def sagemakerProjectId = "p-pulse6dygmzq"
def sagemakerPipelineExecutionRole = "arn:aws:iam::772979422923:role/service-role/AmazonSageMakerServiceCatalogProductsUseRole"
def awsRegion = "us-east-1"
def artifactBucket = "sagemaker-project-p-pulse6dygmzq"

def pipelineName = "sagemaker-" + sagemakerProjectName + "-" + sagemakerProjectId + "-modelbuild"

// Get git details used in JOB DSL so that can be used for pipeline SCM also
def jobName = getBinding().getVariables()['JOB_NAME']
def gitUrl = getBinding().getVariables()['GIT_URL']
def gitBranch = getBinding().getVariables()['GIT_BRANCH']
def jenkins = Jenkins.getInstance()
def job = (AbstractProject)jenkins.getItem(jobName)
def remoteSCM = job.getScm()
def credentialsId = remoteSCM.getUserRemoteConfigs()[0].getCredentialsId()

pipelineJob(pipelineName) {
    description("Sagemaker Build and Training Pipeline")
    keepDependencies(false)
    parameters {
        stringParam("ARTIFACT_BUCKET", artifactBucket, "S3 bucket to store training artifact")
        stringParam("SAGEMAKER_PROJECT_NAME", sagemakerProjectName, "Sagemaker Project Name")
        stringParam("SAGEMAKER_PROJECT_ID", sagemakerProjectId, "Sagemaker Project Id")
        stringParam("AWS_REGION", awsRegion, "Region where project is created")
        stringParam("SAGEMAKER_PIPELINE_ROLE_ARN", sagemakerPipelineExecutionRole, "Role to be used by Sagemaker pipeline to execute.")
        credentialsParam("AWS_CREDENTIAL") {
            description("AWS credentials to use for creating entity")
            defaultValue(awsCredentialId)
            type("com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl")
            required(true)
        }
    }
    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(gitUrl)
                        credentials(credentialsId)
                    }
                    branch(gitBranch)
                }
            }
            scriptPath("jenkins/Jenkinsfile")
        }
    }
    disabled(false)
    triggers {
        scm("* * * * *")
    }
}