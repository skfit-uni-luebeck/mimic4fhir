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
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;

import ca.uhn.fhir.model.primitive.IdDt;

/**
 * Represents one row in mimiciii.caregivers
 * @author Stefanie Ververs
 *
 */
public class MCaregiver {
	private int caregiverId;
	private String label;
	private String description;
	
	public int getCaregiverId() {
		return caregiverId;
	}
	public void setCaregiverId(int caregiverId) {
		this.caregiverId = caregiverId;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * Create FHIR-"Practitioner" from this caregivers' data
	 * @return FHIR-Practitioner
	 */
	public Practitioner getFhirRepresentation() {
		Practitioner p = new Practitioner();
		
		//Id
		p.addIdentifier().setSystem("http://www.imi-mimic.de/practitioner").setValue(Integer.toString(caregiverId));
		
		//Name
		p.addName().setFamily("Caregiver " + caregiverId);
		
		//Narrative
		p.getText().setStatus(NarrativeStatus.GENERATED);
		p.getText().setDivAsString("<div>Caregiver with Id " + caregiverId + "</div>");
	
		// temporary UUID
		p.setId(IdDt.newRandomUuid());
		
		return p;
	}
	
	/**
	 * Create FHIR-"PractitionerRole" from this caregivers' data
	 * @return FHIR-PractitionerRole
	 */
	public PractitionerRole getFhirRepresentationRole() {
		PractitionerRole role = new PractitionerRole();
		
		//Id
		role.addIdentifier().setSystem("http://www.imi-mimic.de/pracRole").setValue(Integer.toString(caregiverId));
				
		//code (~Role)
		CodeableConcept cc = new CodeableConcept();
		
		String descriptionToCheck = description;
		if(descriptionToCheck == null) {
			descriptionToCheck = label;
		}
		if(descriptionToCheck != null) {
			switch(descriptionToCheck) {
				
				case "RN": //Research Nurse
				case "Research Assistant":
					cc.addCoding().setSystem("http://hl7.org/fhir/practitioner-role").setCode("researcher").setDisplay("Researcher");
					break;
				case "Pharmacist":
					cc.addCoding().setSystem("http://hl7.org/fhir/practitioner-role").setCode("pharmacist").setDisplay("Pharmacist");
					break;
				case "Administrator":
					cc.addCoding().setSystem("http://hl7.org/fhir/practitioner-role").setCode("ict").setDisplay("ICT professional");
					break;
				case "IMD": //expected to be MDs
					cc.addCoding().setSystem("http://hl7.org/fhir/practitioner-role").setCode("doctor").setDisplay("Doctor");
					break;
				default:
					//check Label, because description not clear
					if(label != null) {
						switch(label) {
							case "Admin":
								cc.addCoding().setSystem("http://hl7.org/fhir/practitioner-role").setCode("ict").setDisplay("ICT professional");
								break;	
							case "RN":
								cc.addCoding().setSystem("http://hl7.org/fhir/practitioner-role").setCode("researcher").setDisplay("Researcher");
								break;
							case "Res": //Resident/Fellow/PA/NP
								cc.addCoding().setSystem("http://hl7.org/fhir/practitioner-role").setCode("nurse").setDisplay("Nurse");
								break;
							case "md":
							case "Md":
							case "MD":
							case "MD,PhD":
							case "Mds":
							case "MDs":
							case "MDS":
								cc.addCoding().setSystem("http://hl7.org/fhir/practitioner-role").setCode("doctor").setDisplay("Doctor");
								break;
							default:
								role = null; //if not one of the provided roles in Valueset -> no role
								break;
						}
					}else
					{
						role = null;
					}
				break;
			}
		}
		else {
			role = null;
		}
		
		if(role != null) {
			role.addCode(cc);
		}
		
		return role;
	}
	
}
