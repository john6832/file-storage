# File Storage Project

This project handles the upload, versioning and download of binary and multipart files.
These operations can be executed using a Rest Client consuming this awesome Restful web service.

Files are actually being stored on the file system. This approach provides the flexibility on a near future
to migrate our files to a Cloud Provider like Amazon S3 or Nexus Repository Manager.

On the database we are storing only the reference, or path, to the physical location of the archives.
On the database we are also storing the versioning records of every file along with their last modification date.

### Fast setup project

First clone this repository on a save location

If you are in a hurry, you can execute the following script

`./setup.sh`

That script will build the project, run unit tests, generate code coverage reports, create docker image, create and start docker compose services, and print their logs.

In case the rest service doesn't start, maybe that's because services deployment synchronization (sorry, not enough time to set supervisor scripts).
To fix this problem please run the docker compose up command once again:

`docker-compose --file docker/docker-compose.yml up -d`

### Slow setup project

If you feel inspired, you can also do all those steps manually following the next steps.

For building the project

`./gradlew build`

To execute the unit tests

`./gradlew test`

To generate coverage report

`./gradlew test jacocoTestReport`

For a better visibility of the code coverage report, open the following file on the browser:

`build/reports/jacoco/test/html/index.html`

For running the project you will have first to create the docker image locally, you can do that with the following command:

`./gradlew build docker`

This will create an image called 'metadata.io/file-storage:1.0'

Once the image is created you can run the project executing the command:

`docker-compose --file docker/docker-compose.yml up -d`



#### Setting up variables

Right inside the docker/docker-compose.yml file there are some environment variables you can use to tune the application

MYSQL_ROOT_PASSWORD - The password of the mysql root user 

MYSQL_ROOT_HOST - This is for allowing all users to access the database from any container

SPRING_DATASOURCE_URL - This is the url that should point to the database server

SPRING_DATASOURCE_USERNAME - Username for connectint to the database

SPRING_DATASOURCE_PASSWORD - Password for connecting to the database

FILE_DATABASE_PATH - Path on server where the files will be physically stored



### Endpoints and payloads

For simplicity of testing and understandability, a REST API Documentation plugin has been added to the project.

You can find the list of endpoints and examples of payloads using the following link:

`http://localhost:8080/swagger-ui.html`

You can even try those right away using the embedded "Try out" feature. A rich REST client that will save you time 
giving you hints about properties, useful notes, required parameters, types of requests, etc.

In case that doesn't work for you, or you are using other tools, like the mighty Postman, 
here you have a brief explanation about every endpoint:

#### Upload endpoint 1

`/files/upload` - POST

This is the first of 2 upload endpoints, this one receives a multipart file as request param.

In Postman, you select POST, "Body" tab, "form-data" option, and add a new row with key "file"
and attach the document you want to upload. Hit "Send" and you will receive a nice response with useful details:

Response: name, version, download URL, content type and size.

#### Upload endpoint 2

`/files/upload/{fileName}` - POST (replace variables with real values)

This is the second of 2 upload endpoints, this one receives a file name as a path variable and a binary body content.

In Postman, you select POST, "Body" tab, "binary" option, and attach 
the document you want to upload. Hit "Send" and you will receive a nice response with useful details:

Response: name, version, download URL, content type and size.

This file system supports versioning, that means, that if you submit the same file again, it wont be 
overwritten, but a new version will be created and both files will be preserved.

 
#### List uploaded files endpoint

`/files/` - GET

This endpoint will display a list of unique files that you have uploaded to the system, along
with their most recent version number, last modification date and an always cool download link for fast file
retrieval.

In Postman, you select GET, and hit "Send".

#### Download file endpoint

`/files/download/{fileName}?version={versionNumber}` - GET (replace variables with real values)

This endpoint will download a file that you have uploaded before. You can also specify which version you want to download.
The system will try to download the latest version in case the version parameter is not passed.

#### Update file endpoint 1

`/files/update/{fileName}?version={versionNumber}` - POST (replace variables with real values)

It could be the case that you want to update an existent version of the file. With this endpoint you 
can do that.

Note: You can create a NEW version of the file using the "Upload file endpoints".

As with the upload endpoints, this is the first of 2 endpoints for updating files. 
This one uses a multipart file as request param and required "fileName" and "versionNumber" variables on the path.

In Postman, you select POST, "Body" tab, "form-data" option, and attach 
the document you want to upload.
Hit "Send" and you will receive a nice response with useful details:

Response: name, version, updated download URL, updated content type and updated size.


#### Update file endpoint 2

`/files/update/{fileName}?version={versionNumber}` - POST (replace variables with real values)

It could be the case that you want to update an existent version of the file. With this endpoint you 
can do that.

Note: You can create a NEW version of the file using the "Upload file endpoints".

As with the upload endpoints, this is the second of 2 endpoints for updating files. 
This one uses binary body content and required "fileName" and "versionNumber" variables on the path.

In Postman, you select POST, "Body" tab, "binary" option, and add a new row with key "file" and attach 
the document you want to upload.
Hit "Send" and you will receive a nice response with useful details:

Response: name, version, updated download URL, updated content type and updated size.

#### Delete file endpoint

`/files/` - DELETE

If you need to delete a file, you can do that with this endpoint.

In Postman, you select DELETE, "Params" tab, add a row with required key "fileName" 
and an optional row with key "version". 

Note: If "version" parameter is not provided, the system will delete the file and ALL its versions.



Hope you have enjoyed my solution to Metadata.IO file storage system.

Thanks
















