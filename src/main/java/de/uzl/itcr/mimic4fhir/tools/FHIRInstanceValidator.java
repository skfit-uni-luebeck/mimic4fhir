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
package de.uzl.itcr.mimic4fhir.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.PrePopulatedValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;

public class FHIRInstanceValidator {

	private static final Logger logger = LoggerFactory.getLogger(FHIRInstanceValidator.class);

	private static FHIRInstanceValidator instance;
	private static final FhirContext ctx = FhirContext.forR4();
	private static FhirValidator validator = ctx.newValidator();

	private FHIRInstanceValidator() {
		ValidationSupportChain supportChain = new ValidationSupportChain();

		DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(ctx);
		CommonCodeSystemsTerminologyService codeService = new CommonCodeSystemsTerminologyService(ctx);
		InMemoryTerminologyServerValidationSupport validationSupport = new InMemoryTerminologyServerValidationSupport(
				ctx);

		// Add all support and service elements to the support chain
		supportChain.addValidationSupport(defaultSupport);
		supportChain.addValidationSupport(codeService);
		supportChain.addValidationSupport(validationSupport);

		// Get KDS profiles from project resources folder
		StructureDefinition kdsPatient = this.getStructureDefinition("kds/snapshots/PatientIn.json");
		StructureDefinition kdsVitalstatus = this.getStructureDefinition("kds/snapshots/Observation-Vitalstatus.json");
		StructureDefinition kdsFall = this.getStructureDefinition("kds/snapshots/KontaktGesundheitseinrichtung.json");
		StructureDefinition kdsDiagnose = this.getStructureDefinition("kds/snapshots/Diagnose.json");
		StructureDefinition kdsMiiReferenz = this
				.getStructureDefinition("kds/snapshots/MII-Reference.StructureDefinition.json");
		StructureDefinition kdsMedikation = this.getStructureDefinition("kds/snapshots/Medication-duplicate-3.json");
		StructureDefinition kdsMedAdmin = this
				.getStructureDefinition("kds/snapshots/MedicationAdministration-duplicate-3.json");
		StructureDefinition kdsObservationLab = this.getStructureDefinition("kds/snapshots/ObservationLab.json");
		StructureDefinition kdsProzedur = this.getStructureDefinition("kds/snapshots/Prozedur.json");
		// Get code systems
		CodeSystem organizationType = new CodeSystem().setUrl("http://hl7.org/fhir/organization-type");
		CodeSystem entlassungsGrund = this.getCodeSystem("kds/codesystems/Entlassungsgrund.json");
		CodeSystem encounterDEAdditions = this.getCodeSystem("kds/codesystems/EncounterClassAdditionsDE.json");
		// Get value sets
		ValueSet encounterDE = this.getValueSet("kds/valuesets/EncounterClassDE.json");
		ValueSet restrictedEncounterStatus = this.getValueSet("kds/valuesets/RestrictedEncounterStatus.json");
		ValueSet maritalStatus = this.getValueSet("kds/valuesets/ValueSet-marital-status.json");
		ValueSet locationRoleType = this.getValueSet("kds/valuesets/ValueSet-ServiceDeliveryLocationRoleType.json");
		// Get Extensions
		StructureDefinition wirkstoffTypExtension = this
				.getStructureDefinition("kds/extensions/ExtensionWirkstofftyp.json");

		PrePopulatedValidationSupport prePopulatedSupport = new PrePopulatedValidationSupport(ctx);
		// Custom structure definitions
		prePopulatedSupport.addStructureDefinition(kdsPatient);
		prePopulatedSupport.addStructureDefinition(kdsVitalstatus);
		prePopulatedSupport.addStructureDefinition(kdsFall);
		prePopulatedSupport.addStructureDefinition(kdsDiagnose);
		prePopulatedSupport.addStructureDefinition(kdsMiiReferenz);
		prePopulatedSupport.addStructureDefinition(kdsMedikation);
		prePopulatedSupport.addStructureDefinition(kdsMedAdmin);
		prePopulatedSupport.addStructureDefinition(kdsObservationLab);
		prePopulatedSupport.addStructureDefinition(kdsProzedur);
		// Custom code systems
		prePopulatedSupport.addCodeSystem(organizationType);
		prePopulatedSupport.addCodeSystem(entlassungsGrund);
		prePopulatedSupport.addCodeSystem(encounterDEAdditions);
		// Custom value sets
		prePopulatedSupport.addValueSet(encounterDE);
		prePopulatedSupport.addValueSet(restrictedEncounterStatus);
		prePopulatedSupport.addValueSet(maritalStatus);
		prePopulatedSupport.addValueSet(locationRoleType);
		// Custon extensions
		prePopulatedSupport.addStructureDefinition(wirkstoffTypExtension);

		// Add PrePropulatedValidationSupport
		supportChain.addValidationSupport(prePopulatedSupport);

		CachingValidationSupport cachingChain = new CachingValidationSupport(supportChain);
		FhirInstanceValidator validatorModule = new FhirInstanceValidator(cachingChain);
		validator.registerValidatorModule(validatorModule);
	}

	public static FHIRInstanceValidator getInstance() {
		if (instance == null)
			instance = new FHIRInstanceValidator();
		return instance;
	}

	public void validateAndPrint(IBaseResource resource) {
		ValidationResult result = validator.validateWithResult(resource);
		List<SingleValidationMessage> messages = result.getMessages();
		if (result.isSuccessful()) {
			System.out.println("Validation was successful!");
		} else {
			System.out.println("Validation failed!");
		}
		for (SingleValidationMessage message : messages) {
			if (message.getSeverity() != ResultSeverityEnum.ERROR) {
				System.out.println("== Validation Message:");
				System.out.println("---- Location: " + message.getLocationString());
				System.out.println("---- Severity: " + message.getSeverity());
				System.out.println("---- Message:  " + message.getMessage());
			}
		}
	}

	// Idea: https://github.com/hapifhir/hapi-fhir/issues/552
	private String getProfileText(String pathToProfile) {
		String profileText = null;
		ClassLoader classLoader = getClass().getClassLoader();
		try (InputStream inputStream = classLoader.getResourceAsStream(pathToProfile)) {
			profileText = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return profileText;
	}

	private StructureDefinition getStructureDefinition(String pathToProfile) {
		String profileText = this.getProfileText(pathToProfile);
		return FHIRInstanceValidator.ctx.newJsonParser().parseResource(StructureDefinition.class, profileText);
	}

	private org.hl7.fhir.r4.model.ValueSet getValueSet(String pathToProfile) {
		String profileText = this.getProfileText(pathToProfile);
		return FHIRInstanceValidator.ctx.newJsonParser().parseResource(org.hl7.fhir.r4.model.ValueSet.class, profileText);
	}

	private org.hl7.fhir.r4.model.CodeSystem getCodeSystem(String pathToProfile) {
		String profileText = this.getProfileText(pathToProfile);
		return FHIRInstanceValidator.ctx.newJsonParser().parseResource(org.hl7.fhir.r4.model.CodeSystem.class, profileText);
	}

}
