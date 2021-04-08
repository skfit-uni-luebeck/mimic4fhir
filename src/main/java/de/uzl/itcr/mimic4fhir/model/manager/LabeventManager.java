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
package de.uzl.itcr.mimic4fhir.model.manager;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.*;

import de.uzl.itcr.mimic4fhir.model.MLabevent;
import de.uzl.itcr.mimic4fhir.work.Config;

public class LabeventManager extends ModelManager<Observation, MLabevent> {

    public LabeventManager(){
        super();
    }

    @Override
    public Observation createResource(MLabevent mLabevent, Config config){
        Observation observation = new Observation();

        switch(config.getSpecification()){
            case KDS:
                observation.getMeta().addProfile(
                        "https://www.medizininformatik-initiative.de/fhir/core/modul-labor/StructureDefinition/ObservationLab");

                // Add identifier
                observation.addIdentifier().setUse(Identifier.IdentifierUse.USUAL)
                        .setType(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0203")
                                .setCode("OBI").setDisplay("Optometrist license number")))
                        .setSystem("http://www.imi-mimic.de/labevents").setValue(String.valueOf(mLabevent.getLabeventId()))
                        .setAssigner(new Reference(mLabevent.getPatId()));

                // Add categories to the observation
                CodeableConcept category = new CodeableConcept();
                // All observations are laboratory observations
                category.addCoding(new Coding().setSystem("http://hl7.org/fhir/observation-category").setCode("laboratory")
                        .setDisplay("Laboratory"));
                category.addCoding(
                        new Coding().setSystem("http://loinc.org").setCode("26436-6").setDisplay("Laboratory studies"));

                // Actual result
                if (mLabevent.hasNumVal()) {
                    Quantity value = new Quantity();
                    value.setValue(mLabevent.getNumValue());
                    value.setUnit(mLabevent.getUnit());

                    observation.setValue(value);
                } else {
                    // If the data is absent, add Data-Absent-Reason in the appropriate place
                    Base child;
                    try {
                        child = observation.addChild("dataAbsentReason");
                        child.castToCodeableConcept(child).addCoding()
                                .setSystem("http://terminology.hl7.org/CodeSystem/data-absent-reason").setCode("asked-unknown")
                                .setDisplay("Asked But Unknown");
                    } catch (FHIRException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case R4:
                //all laboratory
                observation.addCategory().addCoding().setSystem("http://hl7.org/fhir/observation-category").setCode("laboratory").setDisplay("Laboratory");

                //Actual result
                if(mLabevent.hasNumVal()) {
                    Quantity value = new Quantity();
                    value.setValue(mLabevent.getNumValue());
                    value.setUnit(mLabevent.getUnit());

                    observation.setValue(value);
                }
                else
                {
                    String value = mLabevent.getValue();
                    //Unit added with "(<unit>)"
                    if(mLabevent.getUnit() != null && mLabevent.getUnit().length() > 0) {
                        value += " (" + mLabevent.getUnit() + ")";
                    }
                    observation.setValue(new StringType(value));
                }
                break;
        }

        observation.setStatus(Observation.ObservationStatus.FINAL);

        CodeableConcept cc = new CodeableConcept();
        // Type of Observation
        if (mLabevent.getLoinc() != null) {
            cc.addCoding().setSystem("http://loinc.org").setCode(mLabevent.getLoinc());
            cc.setText(mLabevent.getMeasurementType());
        } else {
            // Representation as plain text if no loinc code available
            cc.setText(mLabevent.getMeasurementType());
        }
        observation.setCode(cc);

        // Pat-Reference
        observation.setSubject(new Reference(mLabevent.getPatId()));

        /*
         * Enc-Reference In the R4 specification the context field has been replaced by
         * the partOf field
         */
        observation.addPartOf(new Reference(mLabevent.getEncId()));

        // Record-Date
        observation.setEffective(new DateTimeType(mLabevent.getAcquisitionDate()));

        // Performer is not available

        // Interpretation (from "flag")
        if (mLabevent.isAbnormal()) {
            cc = new CodeableConcept();
            cc.addCoding().setSystem("http://hl7.org/fhir/v2/0078").setCode("A").setDisplay("Abnormal");
            observation.addInterpretation(cc);
        }

        // Add comment if available; this is not mandatory
        if (mLabevent.getComments() != null && mLabevent.getComments().length() > 0) {
            observation.addNote(new Annotation().setText(mLabevent.getComments()));
        }

        return observation;
    }

}
