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
import de.uzl.itcr.mimic4fhir.model.MDiagnose;
import de.uzl.itcr.mimic4fhir.tools.ICD9MapperLookup;
import de.uzl.itcr.mimic4fhir.tools.RemoteInformationLookup;
import de.uzl.itcr.mimic4fhir.tools.StringManipulator;
import de.uzl.itcr.mimic4fhir.work.Config;

import org.hl7.fhir.r4.model.*;

public class DiagnoseManager extends ModelManager<Condition, MDiagnose> {

    public DiagnoseManager(){
        super();
    }

    @Override
    public Condition createResource(MDiagnose mDiagnose, Config config){
        Condition cond = new Condition();

        if(config.getSpecification() == ModelVersion.KDS){
            cond.getMeta().addProfile("https://www.medizininformatik-initiative.de/fhir/core/modul-diagnose/StructureDefinition/Diagnose");

            //Since there is no data on the date on which the diagnosis was recorded the data is absent
            Extension absentReasonExt = new Extension();
            absentReasonExt.setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason");
            absentReasonExt.setValue(new CodeType().setValue("unknown"));
            cond.getRecordedDateElement().addExtension(absentReasonExt);
        }

        //Patient
        cond.setSubject(new Reference(mDiagnose.getPatId()));

        //Identifier
        cond.addIdentifier().setSystem("http://www.imi-mimic.de/diags").setValue(mDiagnose.getEncId() + "_" + mDiagnose.getSeqNumber());

        //Context -> Encounter
        //cond.setContext(new Reference(encId));

        //Diagnose itself (Code + Text)
        CodeableConcept diagnoseCode = new CodeableConcept();
        if(config.getSpecification() == ModelVersion.KDS){
            ICD9MapperLookup codeLookup = RemoteInformationLookup.getInstance(config).icd9MapperLookup;
            String icdCode = mDiagnose.getIcdCode().replaceAll("\\s", ""), returnCode = "";
            switch(mDiagnose.getIcdVersion()){
                case "9":
                    returnCode = codeLookup.getSNOMEDCode(icdCode);
                    diagnoseCode.addCoding().setSystem("http://snomed.info/sct").setCode(returnCode);
                    returnCode = codeLookup.getICD10GMCode(icdCode);
                    if(returnCode != null){
                        diagnoseCode.addCoding().setSystem("http://fhir.de/CodeSystem/dimdi/icd-10-gm").setCode(returnCode)
                                .setDisplay(mDiagnose.getLongTitle()).setVersion("2020");
                    }
                    break;
                default:
                    //TODO: ICD10 code conversion
                    //Currently ICD 10 Gm codes are treated as ICD 10 GM codes!
                    String icd10Code = StringManipulator.conformIcdString(mDiagnose.getIcdCode());
                    diagnoseCode.addCoding().setSystem("http://fhir.de/CodeSystem/dimdi/icd-10-gm").setCode(icd10Code)
                            .setDisplay(mDiagnose.getLongTitle()).setVersion("2020");
            }
        }
        else{
            diagnoseCode.addCoding().setSystem("http://hl7.org/fhir/sid/icd-9-cm").setCode(mDiagnose.getIcdCode())
                    .setDisplay(mDiagnose.getLongTitle());
        }

        cond.setCode(diagnoseCode);

        // Give the condition a temporary UUID so that other resources in
        // the transaction can refer to it
        cond.setId(IdDt.newRandomUuid());

        return cond;
    }

}
