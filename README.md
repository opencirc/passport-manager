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

First, clone the repository to your local machine using Git. Open a terminal or command prompt and run the following command:

    git clone https://github.com/opencirc/passport-manager.git


Navigate into the project directory:

    cd passport-manager

**3. Set Up the Application**

Using Docker Compose

1. Ensure Docker and Docker Compose are installed on your machine. 

2. From the project directory, run the following command to start the application:

```bash
docker-compose up --build
```
   
This command will:

- Build the Docker images 
- Start the necessary containers 
- Make the REST API available at http://localhost:8080

3. Once the containers are up and running, you should see logs in the terminal indicating that the application has started successfully.

**4. Testing the API**

Once the application is running, you can start testing the REST API endpoints. Here's how to interact with the application:

The REST API is accessible with swagger at the following base URL:

   **http://localhost:8080/swagger-ui/index.html**


