<img src="https://github.com/user-attachments/assets/c57405ea-df1c-4dd4-b684-f49a9e1e1c67" alt="opencirc logo" width="80" style="vertical-align: down;">


  
**Construction Materials Passport Generator and Manager**

  This application allows users to generate and manage passports for construction materials through a REST API. It uses external dictionaries such as bsDD (https://search.bsdd.buildingsmart.org/) and lexicon (https://definehub.com/en/) to generate passports in the defined templates.

This README will guide you through the steps to clone the code, set up the application on your local machine, and use the API to generate passports.

**1. Prerequisites**
Before getting started, ensure you have the following software installed on your system:

- Git (https://git-scm.com/downloads)
- Docker (https://docs.docker.com/get-started/get-docker/)
- Docker Compose (https://docs.docker.com/compose/install/)


**2. Clone the Repository**

Open your terminal and clone the project:

    git clone https://github.com/opencirc/passport-manager.git


Navigate into the project directory:

    cd passport-manager

**3. Set Up and Run the Application**

The application can be started easily using Docker Compose.

1. Ensure Docker and Docker Compose are installed on your machine. 

2. In the project root directory, run:

```bash
docker-compose up --build -d
```
   
This command will:

- Build the Docker images 
- Start the necessary containers 
- Initialize the database
- Make the REST API available at http://localhost:8080

If you want to access the shell, run:

```bash
docker compose --profile shell run --rm shell
```

3. Once the containers are up and running, you should see logs in the terminal indicating that the application has started successfully.

**4. Accessing the API**

Swagger UI (Interactive API Documentation):

	http://localhost:8080/swagger-ui/index.html

Base API URL

	http://localhost:8080/

**5. Authentication**

To use the API, login using the test admin credentials:

 **With login credentials :**

```bash
{
  "email": "admin@test.com",
  "password": "Password123!"
}
```

Once logged in, you will receive a JWT access and refresh token which must be included in requests via cookies or headers.

```bash
access_token = <received JWT access token>
refresh_token = <received JWT refresh token>
```

**With Api Keys :**
Add the below keys in the request header in the `X-Api-Key` and `X-Api-Secret` fields.
```bash
X-Api-Key: fa76a9d9-caa7-41ba-b929-f468620fa023
X-Api-Secret: API_sRynttcLE0OfRM3kFi1RXFdP_34b0c0ed5ed2
```

You can pass them either through Request header.

**6. CLI Tool Commands**

This application also provides a CLI tool to manage users, API keys, templates, and seed the database.

**6.1 Register a New User:**

```bash
register-user --email <email> --password <password> --firstName <firstName> --lastName <lastName> --role <user/admin>
```

> --role : Defaults to user

> --password : Use at least 12 characters with a mix of uppercase, lowercase, digits, or symbols.

**6.2 Create API Key Command:**

```bash
create-api-key --userId <user id> --expiration-date (optional) <Expiration date in yyyy-MM-dd  --name  name of the token
```

**6.3 List API Keys:**

```bash
list-api-tokens --user-id <user id>
```

**6.4 Data Dictionary Command:**

```bash
fetch-template --dictionaryType <<bsdd/lexicon>> --type <<class/property>> --uri <<URI of the template>> --raw <<true/false>>
```

> --raw : default is false

>true: returns the template without added fields

>false: returns processed templates with added fields


**6.5 Seeder Commands:**

Seed initial data into the database:

```bash
seed --type USER                 # Adds only user data
seed --type PASSPORT_FROM_API    # Adds passport data fetched from external API templates
seed --type PASSPORT_FROM_JSON   # Adds passport data from pre-saved bsdd_templates.json
seed --type ALL                  # Adds both user and passport data using templates from bsdd_templates.json
seed                             # Defaults to ALL
```


**7.Common Issues and Troubleshooting**
	
**1. Port 8080 Already in Use.**

To Stop any process using port 8080:

```bash
sudo lsof -i :8080
sudo kill -9 <PID>
```

**2. Database Migration Fails**

Flyway migrations may fail if the database volume persists old data. Remove the volume and try again:

```bash
docker-compose down -v
docker-compose up --build
```

