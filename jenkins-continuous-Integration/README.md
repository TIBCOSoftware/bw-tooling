# Jenkins Continuous Integration with TIBCO BusinessWorks

# What is Jenkins Pipeline?
Jenkins is a software that allows continuous integration. Jenkins Pipeline is a suite of plugins which supports implementing and integrating continuous delivery pipelines into Jenkins.

# What is Continuous Integration?
Continuous Integration is a development practice that requires developers to integrate code into a shared repository at regular intervals. This concept was meant to remove the problem of finding later occurrence of issues in the build lifecycle. Continuous integration requires the developers to have frequent builds. The common practice is that whenever a code commit occurs, a build should be triggered.

# Using the bwdesign Utility
The bwdesign utility provides a command line interface for creating, validating, importing or exporting resources stored in a workspace.

# Using this Jenkinsfile for Continuous Integration with TIBCO BusinessWorks
> 1. If you want to run TIBCO Business Studio in "headless" mode on Linux for command line build and deploy, it is necessary to have the Xvfb service running as some of the underlying eclipse components require an X Window System running on *NX based operating systems.
> 2. To use the bwdesign utility, open a terminal and navigate to BW_HOME\bin. Imports git projects into the current workspace.
> 3. To use the bwdesign utility, open a terminal and navigate to BW_HOME\bin. Exports BW artifacts from the specified projects in the workspace to a folder. The artifacts can be EAR files.