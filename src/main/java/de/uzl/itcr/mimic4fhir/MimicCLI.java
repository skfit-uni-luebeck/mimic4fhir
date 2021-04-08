/*******************************************************************************
 * Copyright (C) 2021 S. Ververs, P. Behrend, J. Wiedekopf, H.Ulrich - University of Lübeck
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.uzl.itcr.mimic4fhir;

import org.springframework.util.StopWatch;

import de.uzl.itcr.mimic4fhir.model.manager.ModelVersion;
import de.uzl.itcr.mimic4fhir.work.Config;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParseResult;

@Command(name = "MIMIC CLI", version = "1.0.0", mixinStandardHelpOptions = true)
public class MimicCLI implements Runnable {

	@Option(names = { "-s", "--server" }, defaultValue = "141.83.20.95", description = "The PostgreSQL Server IP")
	String postgresServerIP;

	@Option(names = { "-p", "--port" }, defaultValue = "5432", description = "The PostgreSQL Port")
	String postgresPort;

	@Option(names = { "-d", "--database" }, defaultValue = "postgres", description = "The PostgreSQL Database")
	String postgresDatabase;

	@Option(names = { "-u", "--user" }, required = true, description = "The PostgreSQL User")
	String postgresUser;

	@Option(names = { "-pwd", "--password" }, required = true, description = "The PostgreSQL User Password")
	String postgresPassword;

	@Option(names = "-validate", defaultValue = "false", description = "Validates the Resources")
	boolean validateResources;

	@Option(names = "--file", defaultValue = "output/", description = "Output Path the Resources")
	String filePath;

	@Option(names = "--fhir", description = "FHIR Endpoint to submit the Resources")
	String fhirEndpoint;

	@Option(names = "--debug", defaultValue = "false", description = "Prints bundle into the console")
	boolean debug;

	@Option(names = "--patients", defaultValue = "1", description = "Number of Patients to transform")
	int patients;

	@Option(names = "--random", defaultValue = "false", description = "Randomly choose patients to convert")
	boolean random;

	@Option(names = "--thread", defaultValue = "false", description = "Enable Threading")
	boolean useThreading;

	@Option(names = "--kds", defaultValue = "false", description = "Enable to German MII KDS as the output")
	boolean useKDS;

	@Option(names = "--cxr", defaultValue = "false", description = "Enable the MIMIC IV CXR")
	boolean useCXR;

	@Option(names = "--9To10GM", defaultValue = "https://ontoserver.imi.uni-luebeck.de/fhir/ConceptMap/d9be1278-282b-4e80-8be5-226cb30a9eb5", description = "FHIR ConceptMap to translate ICD9 to ICD10GM")
	String ICD9toICD10GM;

	@Option(names = "--9ToSCT", defaultValue = "https://ontoserver.imi.uni-luebeck.de/fhir/ConceptMap/9f0b2a1f-8253-47fc-a8cf-118226823e22", description = "FHIR ConceptMap to translate ICD9 to SNOMED CT")
	String ICD9toSCT;

	@Option(names = "--9ProcToSCT", defaultValue = "https://ontoserver.imi.uni-luebeck.de/fhir/ConceptMap/01c83771-6524-46ef-aaa8-4f63e1d837ea", description = "FHIR ConceptMap to translate ICD9 Procedure to SNOMED CT")
	String ICD9ProcToSCT;

	@Option(names = "--10PCSToSCT", defaultValue = "https://ontoserver.imi.uni-luebeck.de/fhir/ConceptMap/03ea8e3a-7fc3-4fb3-8e30-21af497c2a63", description = "FHIR ConceptMap to translate ICD10 PCS to SNOMED CT")
	String ICD10PCStoSCT;

	public static void main(String[] args) {

		MimicCLI mimicCli = new MimicCLI();
		CommandLine cli = new CommandLine(mimicCli);
		ParseResult pR = null;
		try {
			pR = new CommandLine(mimicCli).parseArgs(args);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.exit(0);
		}

		if (pR.isUsageHelpRequested()) {
			System.out.println(cli.getHelp().optionList());
			System.exit(0);
		}

		// TODO fix
		if (pR.isVersionHelpRequested()) {
			System.out.println(cli.VERSION);
			System.exit(0);
		}

		mimicCli.run();
		int exitCode = 0;
		System.exit(exitCode);
	}

	@Override
	public void run() {
		// Add server and config data..
		Config configObj = new Config();

		configObj.setPassPostgres(postgresPassword);
		configObj.setPortPostgres(postgresPort);
		configObj.setUserPostgres(postgresUser);
		configObj.setPostgresServer(postgresServerIP);
		configObj.setDbnamePostgres(postgresDatabase);
		configObj.setSchemaPostgres("mimic_core,mimic_hops_mimic_icu,public");

		// Fhir
		configObj.setFhirServer("http://yourfhirserver.com/public/base/");
		configObj.setFhirxmlFilePath("output/");

		// Validation
		configObj.setValidateResources(validateResources);

		// ConceptMaps
		configObj.setICD9toICD10GM(ICD9toICD10GM);
		configObj.setICD9ToSnomed(ICD9toSCT);
		configObj.setICD9ProcToSnomed(ICD9ProcToSCT);
		configObj.setICD10PCStoSnomed(ICD10PCStoSCT);

		Mimic4Fhir app = new Mimic4Fhir();

		if (debug) {
			app.setOutputMode(OutputMode.PRINT_CONSOLE);
		} else {
			if (fhirEndpoint != null) {
				System.out.println("Using FHIR output to Server: " + fhirEndpoint);
				app.setOutputMode(OutputMode.PUSH_SERVER);
				configObj.setFhirServer(fhirEndpoint);
			} else {
				System.out.println("Using file output to path: " + filePath);
				app.setOutputMode(OutputMode.PRINT_FILE);
				configObj.setFhirxmlFilePath(filePath);
			}
		}

		if (useKDS) {
			configObj.setSpecification(ModelVersion.KDS);
		} else {
			configObj.setSpecification(ModelVersion.R4);
		}

		app.setConfig(configObj);

		if (random) {
			app.setTopPatients(patients, true);
		} else {
			app.setTopPatients(patients, false);
		}

		if (useThreading) {
			app.startWithThread();
		} else {
			StopWatch watch = new StopWatch();
			watch.start();
			app.start();
			watch.stop();
			System.out.print("Conversion complete in " + watch.getTotalTimeMillis() + " ms !");
		}

	}

}
