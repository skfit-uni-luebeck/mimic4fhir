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
import de.uzl.itcr.mimic4fhir.model.MAdmission;
import de.uzl.itcr.mimic4fhir.model.MPatient;
import de.uzl.itcr.mimic4fhir.work.Config;

import org.hl7.fhir.r4.model.*;

public class PatientManager extends ModelManager<Patient, MPatient> {

    public PatientManager(){
        super();
    }

    @Override
    public Patient createResource(MPatient mPatient, Config config) {
        Patient pMimic = new Patient();

        switch(config.getSpecification()){
            case R4:
                //ID:
                pMimic.addIdentifier().setSystem("http://www.imi-mimic.de/patients").setValue(mPatient.getPatientSubjectId());

                //Name : Patient_ID
                pMimic.addName().setUse(HumanName.NameUse.OFFICIAL).setFamily("Patient_" + mPatient.getPatientSubjectId());
                break;
            case KDS:
                // References the structure definition for the patient resource of the KDS
                // resources
                pMimic.getMeta().addProfile(
                        "https://www.medizininformatik-initiative.de/fhir/core/modul-person/StructureDefinition/Patient");

                // ID:
                pMimic.addIdentifier().setSystem("http://www.imi-mimic.de/patients").setValue(mPatient.getPatientSubjectId())
                        .setUse(Identifier.IdentifierUse.USUAL).setType(new CodeableConcept().addCoding(
                        new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0203").setCode("MR")));

                // Name : Patient_ID
                Extension nachname = new Extension().setUrl("http://hl7.org/fhir/StructureDefinition/humanname-own-name")
                        .setValue(new StringType("Patient_" + mPatient.getPatientSubjectId()));
                HumanName name = new HumanName().setUse(HumanName.NameUse.OFFICIAL).setFamily("Patient_" + mPatient.getPatientSubjectId())
                        .addGiven(mPatient.getPatientSubjectId());
                name.getFamilyElement().addExtension(nachname);
                pMimic.addName(name);
                break;
        }

        // Date of Birth
        pMimic.setBirthDate(mPatient.getBirthDate());

        /*
         * Adding reason for birth date not being present since in the MIMICIV data base
         * birth dates are shifted
         */
        Extension absentReasonExt = new Extension();
        absentReasonExt.setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason");
        // Due to the shift birth dates are regarded as 'masked'
        absentReasonExt.setValue(new CodeType().setValue("masked"));
        pMimic.getBirthDateElement().addExtension(absentReasonExt);

        // Date of Death
        if (mPatient.getDeathDate() != null) {
            pMimic.setDeceased(new DateTimeType(mPatient.getDeathDate()));
        } else {
            pMimic.setDeceased(new BooleanType(false));
        }

        // Gender
        // Added administrative gender "other" as a possibility
        switch (mPatient.getGender()) {
            case "M":
                pMimic.setGender(Enumerations.AdministrativeGender.MALE);
                break;
            case "F":
                pMimic.setGender(Enumerations.AdministrativeGender.FEMALE);
                break;
            /*
             * It is not known to me whether or not such a gender is actually used in
             * MIMIC-IV or whether or not it is actually represented as "O" in the data base
             */
            case "O":
                if(config.getSpecification() == ModelVersion.KDS){
                    pMimic.setGender(Enumerations.AdministrativeGender.OTHER);
                    Extension genderExt = new Extension();
                    genderExt.setUrl("http://fhir.de/StructureDefinition/gender-amtlich-de");
                    genderExt.setValue(new Coding().setCode("X").setSystem("http://fhir.de/CodeSystem/gender-amtlich-de")
                            .setDisplay("unbestimmt"));
                    pMimic.getGenderElement().addExtension(genderExt);
                }
                break;
            default:
                pMimic.setGender(Enumerations.AdministrativeGender.UNKNOWN);
        }

        /*
         * Address: Example address since the data sets actually don't contain addresses
         * but are required in the KDS specification
         */
        if(config.getSpecification() == ModelVersion.KDS){
            Address address = new Address();
            address.setType(Address.AddressType.POSTAL);
            StringType line = address.addLineElement();
            line.setValueAsString("Musterstrasse 1");
            Extension street = new Extension(), houseNumber = new Extension();
            ;
            street.setUrl("http://hl7.org/fhir/StructureDefinition/iso21090-ADXP-streetName");
            street.setValue(new StringType("Musterstrasse"));
            houseNumber.setUrl("http://hl7.org/fhir/StructureDefinition/iso21090-ADXP-houseNumber");
            houseNumber.setValue(new StringType("1"));
            line.addExtension(street).addExtension(houseNumber);
            address.setCity("Berlin");
            address.setPostalCode("12043");
            address.setCountry("DE");
            pMimic.addAddress(address);
        }

        if (mPatient.getAdmissions().size() > 0) {
            // from first admission
            MAdmission firstAdm = mPatient.getAdmissions().get(0);

            // Marital Status -
            CodeableConcept cc = new CodeableConcept();

            if (firstAdm.getMaritalStatus() != null) {
                switch (firstAdm.getMaritalStatus()) {
                    case "MARRIED":
                        cc.addCoding().setCode("M").setSystem("http://hl7.org/fhir/ValueSet/marital-status")
                                .setDisplay("Married");
                        break;
                    case "SINGLE":
                        cc.addCoding().setCode("S").setSystem("http://hl7.org/fhir/ValueSet/marital-status")
                                .setDisplay("Never Married");
                        break;
                    case "WIDOWED":
                        cc.addCoding().setCode("W").setSystem("http://hl7.org/fhir/ValueSet/marital-status")
                                .setDisplay("Widowed");
                        break;
                    case "DIVORCED":
                        cc.addCoding().setCode("D").setSystem("http://hl7.org/fhir/ValueSet/marital-status")
                                .setDisplay("Divorced");
                        break;
                    case "SEPARATED":
                        cc.addCoding().setCode("L").setSystem("http://hl7.org/fhir/ValueSet/marital-status")
                                .setDisplay("Legally Separated");
                        break;
                    default:
                        cc.addCoding().setCode("UNK").setSystem("http://hl7.org/fhir/ValueSet/marital-status")
                                .setDisplay("Unknown");
                }
                pMimic.setMaritalStatus(cc);
            }

            // Language
            if (firstAdm.getLanguage() != null) {
                boolean addLanguage = true;
                CodeableConcept lc = new CodeableConcept();
                // Languages sometimes guessed - no dictionary or something in mimic..
                switch (firstAdm.getLanguage()) {
                    case "*DUT":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("nl")
                                .setDisplay("Dutch");
                        break;
                    case "URDU":
                    case "*URD":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ur").setDisplay("Urdu");
                        break;
                    case "*NEP":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ne")
                                .setDisplay("Nepali");
                        break;
                    case "TAGA":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("tl")
                                .setDisplay("Tagalog");
                        break;
                    case "*TOY":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("toy")
                                .setDisplay("Topoiyo");
                        break;
                    case "*RUS":
                    case "RUSS":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ru")
                                .setDisplay("Russian");
                        break;
                    case "ENGL":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("en")
                                .setDisplay("English");
                        break;
                    case "*ARM":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("hy")
                                .setDisplay("Armenian");
                        break;
                    case "CANT":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("yue")
                                .setDisplay("Cantonese");
                        break;
                    case "LAOT":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("tyl")
                                .setDisplay("Thu Lao");
                        break;
                    case "*MOR":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("mor")
                                .setDisplay("Moro");
                        break;
                    case "*FUL":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ff")
                                .setDisplay("Fulah");
                        break;
                    case "*ROM":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ro")
                                .setDisplay("Romanian");
                        break;
                    case "*TOI":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("toi")
                                .setDisplay("Tonga");
                        break;
                    case "BENG":
                    case "*BEN":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("bn")
                                .setDisplay("Bengali");
                        break;
                    case "**TO":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("to")
                                .setDisplay("Tonga");
                        break;
                    case "PERS":
                    case "*PER":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("fa")
                                .setDisplay("Persian");
                        break;
                    case "*TEL":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("te")
                                .setDisplay("Telugu");
                        break;
                    case "*YID":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ji")
                                .setDisplay("Yiddish");
                        break;
                    case "*CDI":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("cdi")
                                .setDisplay("Chodri");
                        break;
                    case "JAPA":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("jp")
                                .setDisplay("Japanese");
                        break;
                    case "ALBA":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("sq")
                                .setDisplay("Albanian");
                        break;
                    case "ARAB":
                    case "*ARA":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ar")
                                .setDisplay("Arabic");
                        break;
                    case "ITAL":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("it")
                                .setDisplay("Italian");
                        break;
                    case "*TAM":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("taq")
                                .setDisplay("Tamasheq");
                        break;
                    case "*SPA":
                    case "SPAN":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("es")
                                .setDisplay("Spanish");
                        break;
                    case "*BOS":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("bs")
                                .setDisplay("Bosnian");
                        break;
                    case "*AMH":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("am")
                                .setDisplay("Amharic");
                        break;
                    case "SOMA":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("so")
                                .setDisplay("Somali");
                        break;
                    case "CAPE":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("cap")
                                .setDisplay("Chipaya");
                        break;
                    case "*PUN":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("pa")
                                .setDisplay("Punjabi");
                        break;
                    case "POLI":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("pl")
                                .setDisplay("Polish");
                        break;
                    case "*CHI":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("zh")
                                .setDisplay("Chinese");
                        break;
                    case "*BUR":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("my")
                                .setDisplay("Burmese");
                        break;
                    case "*CAN":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("can")
                                .setDisplay("Chambri");
                        break;
                    case "*YOR":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("yox")
                                .setDisplay("Yoron");
                        break;
                    case "*KHM":
                    case "CAMB":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("km")
                                .setDisplay("Central Khmer");
                        break;
                    case "AMER":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("en")
                                .setDisplay("English");
                        break;
                    case "*LIT":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("lt")
                                .setDisplay("Lithuanian");
                        break;
                    case "*IBO":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ibn")
                                .setDisplay("Ibino");
                        break;
                    case "KORE":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ko")
                                .setDisplay("Korean");
                        break;
                    case "*FIL":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("fil")
                                .setDisplay("Filipino");
                        break;
                    case "THAI":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("th").setDisplay("Thai");
                        break;
                    case "**SH":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("sh")
                                .setDisplay("Serbo-Croatian");
                        break;
                    case "FREN":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("fr")
                                .setDisplay("French");
                        break;
                    case "*FAR":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("far")
                                .setDisplay("Fataleka");
                        break;
                    case "*CRE":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("crp")
                                .setDisplay("Creoles and pidgins");
                        break;
                    case "HIND":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("hi")
                                .setDisplay("Hindi");
                        break;
                    case "*HUN":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("hu")
                                .setDisplay("Hungarian");
                        break;
                    case "ETHI":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("eth")
                                .setDisplay("Ethiopian Sign Language");
                        break;
                    case "VIET":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("vi")
                                .setDisplay("Vietnamese");
                        break;
                    case "*MAN":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("man")
                                .setDisplay("Mandingo");
                        break;
                    case "GERM":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("de")
                                .setDisplay("German");
                        break;
                    case "*PHI":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("phi")
                                .setDisplay("Philippine languages");
                        break;
                    case "TURK":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("tr")
                                .setDisplay("Turkish");
                        break;
                    case "*DEA":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("mjl")
                                .setDisplay("Mandeali");
                        break;
                    case "PTUN":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ptu")
                                .setDisplay("Bambam");
                        break;
                    case "GREE":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("el")
                                .setDisplay("Modern Greek");
                        break;
                    case "MAND":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("cmn")
                                .setDisplay("Mandarin Chinese");
                        break;
                    case "HAIT":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("ht")
                                .setDisplay("Haitian");
                        break;
                    case "SERB":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("sr")
                                .setDisplay("Serbian");
                        break;
                    case "*BUL":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("bg")
                                .setDisplay("Bulgarian");
                        break;
                    case "*LEB":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("leb")
                                .setDisplay("Lala-Bisa");
                        break;
                    case "*GUJ":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("gu")
                                .setDisplay("Gujarati");
                        break;
                    case "PORT":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("pt")
                                .setDisplay("Portugese");
                        break;
                    case "* BE":
                        lc.addCoding().setSystem("http://hl7.org/fhir/ValueSet/languages").setCode("be")
                                .setDisplay("Belarusian");
                        break;
                    default:
                        addLanguage = false;
                }
                // Only add a language (and communication) if there is a fitting code for it in
                // the coding system
                if (addLanguage) {
                    pMimic.addCommunication().setLanguage(lc);
                }
            }
        }

        // Give the patient a temporary UUID so that other resources in
        // the transaction can refer to it
        pMimic.setId(IdDt.newRandomUuid());

        return pMimic;
    }
}
