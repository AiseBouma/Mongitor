# Mongitor
Tool to monitor MongoDB clusters. The current version is still in beta. You are however encouraged to try it out and send me some feedback. I am interested in bugs as well as new features.

# Screenshots
![status screenshot](https://github.com/AiseBouma/Mongitor/blob/master/screenshots/status.png?raw=true)
*The Mongitor home page showing the status of all MongoDB servers.*
<br>
<br>
<br>
![overview screenshot](https://github.com/AiseBouma/Mongitor/blob/master/screenshots/overview.png?raw=true)
*Show a summary of all MongoDB servers.*
<br>
<br>
<br>
![replication screenshot](https://github.com/AiseBouma/Mongitor/blob/master/screenshots/replication.png?raw=true)
*Show the replication lag of a secondary. Run commands with the click of a button, including checking the oplog.*
<br>
<br>
<br>
![credentials screenshot](https://github.com/AiseBouma/Mongitor/blob/master/screenshots/credentials.png?raw=true)
*Set the credentials for MongoDB.*
<br>
<br>
<br>
![collection screenshot](https://github.com/AiseBouma/Mongitor/blob/master/screenshots/collection.png?raw=true)
*Show statistics for a collection, including data on the sharding*
<br>
<br>
<br>
# Running Mongitor
1. Install a recent java 8 jdk from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
1. In C:\Program Files\Java\jdk1.8.0_151\jre\lib\security\java.security uncomment the line #crypto.policy=unlimited
1. Download Mongitor.zip from the [downloads](https://github.com/AiseBouma/Mongitor/tree/master/downloads) directory and unzip.
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
To build from source:

There are two projects: Mongitor and MongitorBackend.

In Eclipse import the projects from GitHub via File - Import... - Git - Pojects from Git - Next >
Select Clone URI and click Next >
Use https://github.com/AiseBouma/Mongitor.git as URI and Next >
Select master and Next >
Select a directory and Next >
Import existing Eclipse projects and Next >
Finish

Mongitor is the website. Export the project to a war file.

MongitorBackend is a jettybased webserver. Get Mongitor.zip from the downloads directory and extract. Edit the file Mongitor.properties to match you setup.
Now create a run configuration with:
1. MongitorMain as main class
1. The full path of the file Mongitor.properties as argument
1. Runtime JRE point to your JDK directory (e.g. C:\Program Files\Java\jdk1.8.0_151)
1. In C:\Program Files\Java\jdk1.8.0_151\jre\lib\security\java.security uncomment the line #crypto.policy=unlimited
1. Run the MongitorBackend project

# Architecture



