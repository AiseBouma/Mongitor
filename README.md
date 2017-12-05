# Mongitor
Tool to monitor MongoDB clusters.

# Running Mongitor
1. Install a recent java 8 jdk from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
1. Download the files Mongitor.war, MongitorBackend.jar and Mongitor.properties from the [downloads](https://github.com/AiseBouma/Mongitor/tree/master/downloads) directory.
1. Edit the paths in Mongitor.properties, so that they match your setup.
1. Start Mongitor, by typing something like:
`"C:\Program Files\Java\jdk1.8.0_151\bin\java" -jar c:\local\MongitorBackend.jar  "c:\local\Mongitor.properties"`
1. Point your web browser to http://localhost:8080/Mongitor.
1. Set your Mongitor password, when asked for it.
1. Enter the details of your Mongo Router when asked for.
1. Optionally enter extra credentials for MongoDB hosts by clicking on Credentials in the menu on the left.
1. Click Start Monitoring in the menu on the left.
1. Read the User Manual via the link in the menu.

# Building Mongitor
To build form source:

There are two projects: Mongitor and MongitorBackend.

Mongitor is the website. Create a webapp project in Eclipse and add the files. Then export the project to a war file.

MongitorBackend is a jettybased webserver. More information will follow.
