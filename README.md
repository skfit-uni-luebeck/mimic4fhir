# mimic4fhir 
## Introduction
mimic4fhir converts data from MIMIC IV database (PostgreSQL) to HL7 FHIR R4 resources. To get access to MIMIC-IV , you have to complete a privacy course, see [here](https://mimic-iv.mit.edu/docs/access/) for more information. In addition to the MIMIC data, four FHIR ConceptMaps are needed for the translation of the codes. You can find a program to generate them [here](https://github.com/itcr-uni-luebeck/AthenaConceptMaps).

## How to use
Bundles are created per patient admission/encounter. If the number of resources in a bundle exceeds 15000, a new bundle will be created. This limits the bundle size to ~20MB and are transcaction bundles with [conditional creates](https://www.hl7.org/fhir/http.html#ccreate).
Resource bundles can be 
- printed to console
- saved as xml file
- pushed to a fhir server

by setting the "outputMode": 
```sh
app.setOutputMode(OutputMode.PRINT_FILE);
```
The parameter "topPatients" allows to limit the number of loaded patients; 0 means all patients. Transforming always starts with Patient 1.
```sh
app.setTopPatients(10);
```
A [RabbitMQ server](https://www.rabbitmq.com/) is required to run on localhost. 
Please note: Performance is highly dependent on the following and might be quite low:
- database partitioning and indexing for table chartevents (by HADM_ID)
    - There is a script [here](link) which adds additional indeces to the PostgresSQL DB
- server performance (if pushed to a server)

We recommend starting with a low number of patients and with saving to xml files to check the database performance.   
 
### Example main method:
```java
// Add server and config data..
		Config configObj = new Config();

		// Postgres
		configObj.setPassPostgres("Pa33word!");
		configObj.setPortPostgres("5432");
		configObj.setUserPostgres("user");
		configObj.setPostgresServer("192.168.0.1");
		configObj.setDbnamePostgres("postgres");

		// Fhir
		configObj.setFhirServer("http://server.com/fhir/");
		configObj.setFhirxmlFilePath("output/");

		// Validation
		// Set to true, if you want to validate with the InstanceValidator
		configObj.setValidateResources(false);

		// ConceptMaps URI needed for the conversion
		configObj.setICD9toICD10GM("https://server.com/fhir/ConceptMap/d9be1278-282b-4e80-8be5-226cb30a9eb5");
		configObj.setICD9ToSnomed("https://server.com/fhir/ConceptMap/9f0b2a1f-8253-47fc-a8cf-118226823e22");
		configObj.setICD9ProcToSnomed("https://server.com/fhir/ConceptMap/01c83771-6524-46ef-aaa8-4f63e1d837ea");
		configObj.setICD10PCStoSnomed("https://server.com/fhir/ConceptMap/03ea8e3a-7fc3-4fb3-8e30-21af497c2a63");
		
		// Use CXR 
		// If you have access to the CXR, the conversion will added DiagnosticReport and ImagingStudying
		//for the corresponding patients
		configObj.setUseCXR(false);
		
		//Specification 
		// Choose plain R4 or the German MII KDS output
		configObj.setSpecification(ModelVersion.KDS);

		Mimic4Fhir app = new Mimic4Fhir();
		app.setConfig(configObj);
		app.setOutputMode(OutputMode.PRINT_FILE);
		// 25 Patients chosen random (boolean flag) from the MIMIC IV
		app.setTopPatients(25, true);
		// You can run the conversion single-threaded
		//app.start();
		// Or with 10 Threads
		app.startWithThread();
```

In addition, there is a command line interface to accelerate the creation for automated testing.

```sh

java -jar target/mimic4fhir-1.0.0-jar-with-dependencies.jar -help
  -d, --database=<postgresDatabase>
                             The PostgreSQL Database
      --debug                Prints bundle into the console
      --fhir=<fhirEndpoint>  FHIR Endpoint to submit the Resources
      --file=<filePath>      Output Path the Resources
  -h, --help                 Show this help message and exit.
      --kds                  Enable to German MII KDS as the output
  -p, --port=<postgresPort>  The PostgreSQL Port
      --patients=<patients>  Number of Patients to transform
      -pwd, --password=<postgresPassword>
                             The PostgreSQL User Password
      --random               Randomly choose patients to convert
  -s, --server=<postgresServerIP>
                             The PostgreSQL Server IP
      --thread               Enable Threading
  -u, --user=<postgresUser>  The PostgreSQL User
  -V, --version              Print version information and exit.
      -validate              Validates the Resources
      --10PCSToSCT=<ICD10PCStoSCT>
                             FHIR ConceptMap to translate ICD10 PCS to SNOMED CT
      --9ProcToSCT=<ICD9ProcToSCT>
                             FHIR ConceptMap to translate ICD9 Procedure to
                               SNOMED CT
      --9To10GM=<ICD9toICD10GM>
                             FHIR ConceptMap to translate ICD9 to ICD10GM
      --9ToSCT=<ICD9toSCT>   FHIR ConceptMap to translate ICD9 to SNOMED CT
      --cxr                  Enable the MIMIC IV CXR
```

Example:

```sh
java -jar target/mimic4fhir-1.0.0-jar-with-dependencies.jar --server=192.186.0.1 --user=user --password=Pa33word! --patients=20 --thread --file=output/
```

## License
This source code is licensed under GNU Affero General Public License v3 (AGPL-3.0).

