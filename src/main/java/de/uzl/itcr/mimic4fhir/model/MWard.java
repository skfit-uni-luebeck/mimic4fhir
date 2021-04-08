/***********************************************************************
Copyright 2018 Stefanie Ververs, University of Lübeck

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
/***********************************************************************/
package de.uzl.itcr.mimic4fhir.model;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Location;

import ca.uhn.fhir.model.primitive.IdDt;

/**
 * Represents one ward as location (from transfers)
 * @author Stefanie Ververs
 *
 */
public class MWard {
	private int wardId;
	private String careUnit;
	
	public String getCareUnit() {
		return careUnit;
	}
	public void setCareUnit(String careUnit) {
		if(careUnit == null) {
			this.careUnit = "NORMAL";
		}
		else {
			this.careUnit = careUnit;
		}
	}
	
	public int getWardId() {
		return wardId;
	}
	public void setWardId(int wardId) {
		this.wardId = wardId;
	}
	public String getWardName() {
		return "Ward " + wardId + " (" + careUnit + ")";
	}

	/**
	 * Create FHIR-"Location"
	 * @return FHIR-Location
	 */
	public Location getFhirLocation() {
		Location loc = new Location();
		
		loc.addIdentifier().setSystem("http://www.imi-mimic.de/wards").setValue(this.wardId + "_" + careUnit);
		
		loc.setName(getWardName());
		
		CodeableConcept cc = new CodeableConcept();
		switch(careUnit) {
			case "NORMAL":
			case "NWARD":	//Neonatal ward
				cc.addCoding().setCode("HU").setSystem("http://hl7.org/fhir/v3/RoleCode").setDisplay("Hospital unit");
				break;
			case "CCU": //Coronary care unit
				cc.addCoding().setCode("CCU").setSystem("http://hl7.org/fhir/v3/RoleCode").setDisplay("Coronary care unit");
				break;
			case "CSRU": 	//Cardiac surgery recovery unit
			case "MICU": 	//Medical intensive care unit
			case "SICU":	//Surgical intensive care unit
			case "TSICU":	//Trauma/surgical intensive care unit
				cc.addCoding().setCode("ICU").setSystem("http://hl7.org/fhir/v3/RoleCode").setDisplay("Intensive care unit");
				break;
			case "NICU": 	//Neonatal intensive care unit
				cc.addCoding().setCode("PEDNICU").setSystem("http://hl7.org/fhir/v3/RoleCode").setDisplay("Pediatric neonatal intensive care unit");
				break;

		}
		loc.addType(cc);
				
		loc.setId(IdDt.newRandomUuid());
		
		return loc;
	}
}
