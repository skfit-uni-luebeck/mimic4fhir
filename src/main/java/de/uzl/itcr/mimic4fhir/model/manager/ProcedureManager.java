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

import ca.uhn.fhir.model.primitive.IdDt;
import de.uzl.itcr.mimic4fhir.model.MProcedure;
import de.uzl.itcr.mimic4fhir.tools.ProcedureSNOMEDLookup;
import de.uzl.itcr.mimic4fhir.tools.RemoteInformationLookup;
import de.uzl.itcr.mimic4fhir.work.Config;

import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.List;

public class ProcedureManager extends ModelManager<Procedure, MProcedure> {

    public ProcedureManager(){
        super();
    }

    @Override
    public Procedure createResource(MProcedure mProcedure, Config config){
        Procedure proc = new Procedure();

        //Patient
        proc.setSubject(new Reference(mProcedure.getPatId()));

        //Encounter
        proc.setEncounter(new Reference(mProcedure.getEncId()));

        //References the structure definition for the procedure resource of the KDS resources
        proc.getMeta().addProfile("https://www.medizininformatik-initiative.de/fhir/core/modul-prozedur/StructureDefinition/Procedure");

        //Identifier
        proc.addIdentifier().setSystem("http://www.imi-mimic.de/procs").setValue(mProcedure.getEncId() + "_" + mProcedure.getSeqNumber());

        //State
        proc.setStatus(Procedure.ProcedureStatus.COMPLETED);

        switch (config.getSpecification()){
            case KDS:
                //Procedure itself (Code + Text)
                List<String> snomedCodes = new ArrayList<>();
                CodeableConcept code = new CodeableConcept();

                ProcedureSNOMEDLookup procedureSNOMEDLookup = RemoteInformationLookup.getInstance(config).procedureSNOMEDLookup;

                switch(mProcedure.getIcdVersion()) {
                    case ICD9PROC:
                        snomedCodes = procedureSNOMEDLookup.getSnomedForIcd9(mProcedure.getIcdCode());
                        break;
                    case ICD10PROC:
                        snomedCodes = procedureSNOMEDLookup.getSnomedForIcd10(mProcedure.getIcdCode());
                        break;
                }

                //If no appropriate code was found, denote this in the record
                if(snomedCodes.size() == 0){
                    Extension absentReasonExt = new Extension();
                    absentReasonExt.setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason");
                    absentReasonExt.setValue(new CodeType().setValue("asked-unknown"));
                    code.addExtension(absentReasonExt);
                    try {
                        code.setText("No SNOMEDCT code could be found for ICD" + mProcedure.getIcdVersion().valueOf() + " code '" + mProcedure.getIcdCode() + "'");
                    } catch (Exception e) {
                        System.out.println(mProcedure.getIcdCode() + ":" + mProcedure.getIcdVersion());
                    }

                }
                else{
                    //Add all found SNOMEDCT codes
                    for(String snomedCode : snomedCodes){
                        code.addCoding().setSystem("http://snomed.info/sct").setCode(snomedCode);
                    }
                    code.setText(mProcedure.getLongTitle());
                }

                proc.setCode(code);

                //Unfortunately MIMICIV does not contain any information on the time of the procedure
                Extension absentReasonExt = new Extension();
                absentReasonExt.setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason");
                absentReasonExt.setValue(new CodeType().setValue("unknown"));
                DateTimeType performed = new DateTimeType();
                performed.addExtension(absentReasonExt);
                proc.setPerformed(performed);
                break;

            case R4:
                //Procedure itself (Code + Text)
                CodeableConcept procedureCode = new CodeableConcept();
                procedureCode.addCoding().setSystem("http://hl7.org/fhir/sid/icd-9-cm").setCode(mProcedure.getIcdCode())
                        .setDisplay(mProcedure.getLongTitle());

                proc.setCode(procedureCode);
                break;
        }

        // Give the procedure a temporary UUID so that other resources in
        // the transaction can refer to it
        proc.setId(IdDt.newRandomUuid());

        return proc;
    }

}
