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
package de.uzl.itcr.mimic4fhir.work;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import de.uzl.itcr.mimic4fhir.model.*;
import de.uzl.itcr.mimic4fhir.model.manager.StationManager;

/**
 * Connection, access and querys to postgresDB
 *
 * @author Stefanie Ververs
 */
public class ConnectDB {
	private static Config configuration;
	private Connection connection = null;

	private PreparedStatement statementSelectOnePatientFromAdmissionsView;
	private PreparedStatement statementSelectAmountOfPatientIDs;
	private PreparedStatement statementSelectAmountOfPatientIDsRandom;
	private PreparedStatement statementCountPatients;
	private PreparedStatement statementGetDiagnoses;
	private PreparedStatement statementGetProcedures;
	private PreparedStatement statementGetChartEvents;
	private PreparedStatement statementGetLabEvents;
	private PreparedStatement statementGetPrescriptions;
	private PreparedStatement statementGetAmountOfPatientsLimit;
	private PreparedStatement statementGetAmountOfPatientsLimitRandom;
	private PreparedStatement statementGetAdmissions;
	private PreparedStatement statementGetTransfers;
	private PreparedStatement statementGetStations;
	private PreparedStatement statementGetDiagnosticReports;
	private PreparedStatement statementGetImagingStudies;
	private PreparedStatement statementGetIcdCodes;;

	/**
	 * Create new DB-Connection with Config-Object
	 *
	 * @param configuration
	 */
	public ConnectDB(Config configuration) {

		this.configuration = configuration;
		// Do some stuff to do DB-Connection..

		try {
			Class.forName("org.postgresql.Driver");

		} catch (ClassNotFoundException e) {

			e.printStackTrace();
			return;
		}

		this.connection = null;

		// Schema-Construction, if necessary:
		String schema = "";
		if (ConnectDB.configuration.getSchemaPostgres() != null
				&& ConnectDB.configuration.getSchemaPostgres().length() > 0) {
			schema = "?currentSchema=" + ConnectDB.configuration.getSchemaPostgres();
		}

		try {
			connection = DriverManager.getConnection(
					"jdbc:postgresql://" + ConnectDB.configuration.getPostgresServer() + ":"
							+ ConnectDB.configuration.getPortPostgres() + "/"
							+ ConnectDB.configuration.getDbnamePostgres() + schema,
					ConnectDB.configuration.getUserPostgres(), ConnectDB.configuration.getPassPostgres());
			this.createViews();
			this.prepareStatements();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * create the views required for the downstream queries
	 *
	 * @throws SQLException if the statements fail to execute
	 */
	private void createViews() throws SQLException {
		this.connection.prepareStatement("DROP VIEW IF EXISTS public.patient_admission_view;\n"
				+ "CREATE VIEW public.patient_admission_view AS (\n"
				+ "  SELECT patients.subject_id patient_subjectid,\n" + "         gender,\n" + "         anchor_age,\n"
				+ "         anchor_year,\n" + "         anchor_year_group,\n" + "         dod,\n"
				+ "         admissions.subject_id admission_subjectid,\n"
				+ "         admissions.hadm_id admission_hadm_id,\n" + "         admittime,\n" + "         dischtime,\n"
				+ "         deathtime,\n" + "         admission_type,\n" + "         admission_location,\n"
				+ "         discharge_location,\n" + "         insurance,\n" + "         language,\n"
				+ "         marital_status,\n" + "         ethnicity,\n" + "         edregtime,\n"
				+ "         edouttime,\n" + "         hospital_expire_flag,\n"
				+ "         transfers.subject_id transfer_subjectid,\n"
				+ "         transfers.hadm_id transfer_hadmid,\n" + "         transfer_id,\n" + "         eventtype,\n"
				+ "         careunit,\n" + "         intime,\n" + "         outtime\n" + "  FROM mimic_core.patients\n"
				+ "           FULL JOIN mimic_core.admissions ON patients.subject_id = admissions.subject_id\n"
				+ "           FULL JOIN mimic_core.transfers ON admissions.hadm_id = transfers.hadm_id AND admissions.subject_id = transfers.subject_id\n"
				+ "  ORDER BY patients.subject_id, admission_hadm_id, transfer_id\n" + "      );").execute();
	}

	/**
	 * prepare statements for repeated execution against the current connection
	 *
	 * @throws SQLException if the statements could not be prepared
	 */
	private void prepareStatements() throws SQLException {
		this.statementSelectOnePatientFromAdmissionsView = this.connection
				.prepareStatement("SELECT * FROM patient_admission_view " + "WHERE patient_subjectid = ? "
						+ "ORDER BY patient_subjectid, admission_hadm_id, transfer_id;");
		this.statementSelectAmountOfPatientIDs = this.connection
				.prepareStatement("SELECT subject_id from mimic_core.patients ORDER BY subject_id LIMIT ?;");
		this.statementSelectAmountOfPatientIDsRandom = this.connection
				.prepareStatement("SELECT subject_id FROM mimic_core.patients ORDER BY random() LIMIT ?;");
		this.statementCountPatients = this.connection.prepareStatement("SELECT COUNT(*) FROM MIMIC_CORE.PATIENTS;");
		this.statementGetDiagnoses = this.connection.prepareStatement("SELECT * FROM mimic_hosp.diagnoses_icd d "
				+ "INNER JOIN mimic_hosp.d_icd_diagnoses i ON d.icd_code = i.icd_code "
				+ "WHERE d.subject_id = ? AND d.hadm_id = ? " + "ORDER BY d.seq_num DESC");
		this.statementGetProcedures = this.connection.prepareStatement("SELECT * FROM mimic_hosp.procedures_icd p "
				+ "INNER JOIN mimic_hosp.d_icd_procedures i ON p.icd_code = i.icd_code "
				+ "WHERE p.subject_id = ? AND p.hadm_id = ? " + "ORDER BY p.seq_num DESC");
		this.statementGetChartEvents = this.connection.prepareStatement(
				"SELECT C1.SUBJECT_ID, C1.HADM_ID, C1.CHARTTIME, C1.VALUE, C1.VALUENUM, C1.VALUEUOM, D.LABEL "
						+ "FROM (SELECT C.SUBJECT_ID, C.HADM_ID, C.CHARTTIME, C.VALUE, C.VALUENUM, C.VALUEUOM, C.ITEMID FROM MIMIC_ICU.CHARTEVENTS C) AS C1 "
						+ "INNER JOIN MIMIC_ICU.D_ITEMS D ON C1.ITEMID = D.ITEMID " + "WHERE C1.HADM_ID = ?");
		this.statementGetLabEvents = this.connection.prepareStatement(
				"SELECT L1.SUBJECT_ID, L1.HADM_ID, L1.CHARTTIME, L1.VALUE, L1.VALUENUM, L1.VALUEUOM, L1.FLAG, D.LABEL, D.FLUID, D.LOINC_CODE, L1.LABEVENT_ID, L1.COMMENTS "
						+ "FROM (SELECT L.SUBJECT_ID, L.HADM_ID, L.CHARTTIME, L.VALUE, L.VALUENUM, L.VALUEUOM, L.FLAG, L.ITEMID, L.LABEVENT_ID, L.COMMENTS "
						+ "FROM MIMIC_HOSP.LABEVENTS L) AS L1 INNER JOIN MIMIC_HOSP.D_LABITEMS D ON L1.ITEMID = D.ITEMID "
						+ "WHERE L1.SUBJECT_ID = ? AND L1.HADM_ID = ?");
		this.statementGetPrescriptions = this.connection
				.prepareStatement("SELECT * FROM MIMIC_HOSP.PRESCRIPTIONS WHERE SUBJECT_ID = ? AND HADM_ID = ?");
		this.statementGetAmountOfPatientsLimit = this.connection
				.prepareStatement("SELECT * FROM MIMIC_CORE.PATIENTS LIMIT ?");
		this.statementGetAmountOfPatientsLimitRandom = this.connection
				.prepareStatement("SELECT * FROM MIMIC_CORE.PATIENTS ORDER BY random() LIMIT ?");
		this.statementGetAdmissions = this.connection
				.prepareStatement("SELECT * FROM MIMIC_CORE.ADMISSIONS WHERE SUBJECT_ID = ?");
		this.statementGetTransfers = this.connection
				.prepareStatement("SELECT * FROM MIMIC_CORE.TRANSFERS WHERE SUBJECT_ID = ? AND HADM_ID = ?");
		this.statementGetStations = this.connection
				.prepareStatement("SELECT DISTINCT CAREUNIT \" + \"FROM MIMIC_CORE.TRANSFERS ORDER BY CAREUNIT");
		this.statementGetDiagnosticReports = this.connection
				.prepareStatement("SELECT * FROM CXR.RECORDS WHERE SUBJECT_ID = ?");
		this.statementGetImagingStudies = this.connection
				.prepareStatement("SELECT * FROM CXR.STUDIES WHERE SUBJECT_ID = ? AND STUDY_ID = ?");
		this.statementGetIcdCodes = this.connection
				.prepareStatement("SELECT icd_code, long_title FROM mimic_hosp.d_icd_procedures WHERE icd_version = ?");
	}

	public static Connection getConnection() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		Connection con = null;
		// Schema-Construction, if necessary:
		String schema = "";
		if (configuration.getSchemaPostgres() != null && configuration.getSchemaPostgres().length() > 0) {
			schema = "?currentSchema=" + configuration.getSchemaPostgres();
		}

		try {
			con = DriverManager.getConnection(
					"jdbc:postgresql://" + configuration.getPostgresServer() + ":" + configuration.getPortPostgres()
							+ "/" + configuration.getDbnamePostgres() + schema,
					configuration.getUserPostgres(), configuration.getPassPostgres());

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return con;
	}

	/**
	 * How many patients in MIMIC_CORE.Patients?
	 *
	 * @return number of patients
	 */
	public int getNumberOfPatients() {
		/*
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the PATIENTS table is in the
		 * CORE module
		 */
		int count = 0;
		try {
			ResultSet rs = statementCountPatients.executeQuery();
			while (rs.next()) {
				count = rs.getInt(1);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return count;
	}

	/**
	 * Get first patient in Mimic-Patients-Table
	 *
	 * @return filled MPatient-Object
	 */
	public MPatient getFirstPatient() {
		/*
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the PATIENTS table is in the
		 * CORE module
		 */
		String query = "SELECT * FROM MIMIC_CORE.PATIENTS ORDER BY ROW_ID LIMIT 1";
		return getOnePatientFromDb(query);
	}

	/**
	 * Get all patients up until a specified row number
	 *
	 * @param numOfPatients specified row number of the last patient to be retrieved
	 * @param random        true if the patients retrieved should be randomized
	 * @return Array of MPatient object obtained from the data base
	 */
	public MPatient[] getAmountOfPatients(int numOfPatients, boolean random) {
		MPatient[] patients;
		PreparedStatement statement = random ? statementGetAmountOfPatientsLimitRandom
				: statementGetAmountOfPatientsLimit;
		try {
			patients = new MPatient[numOfPatients];
			statement.setInt(1, numOfPatients);
			ResultSet rs = statement.executeQuery();
			ResultSetMetaData metaData = rs.getMetaData();
			while (rs.next()) {
				MPatient mPat = new MPatient();
				/*
				 * Date of Birth has been removed due to not being present in MIMICIV's
				 * MIMIC_CORE.PATIENTS table Some column indices have been changed according to
				 * changes in the tables structure
				 */
				// Note that explicit birth dates are no longer present in MIMICIV
				// SUBJECT_ID
				mPat.setPatientSubjectId(rs.getString(1));
				// GENDER
				mPat.setGender(rs.getString(2));
				// DOD
				mPat.setDeathDate(rs.getDate(6));
				// Admissions
				getPatientAdmissions(mPat);
				// DiagnosticReports
				getDiagnosticReports(mPat);
				patients[rs.getRow() - 1] = mPat;
			}
			return patients;
		} catch (SQLException exc) {
			exc.printStackTrace();
		}
		return null;
	}

	/**
	 * Get patient by subjectId
	 *
	 * @param subjectId subjectId of patient in patients-Table
	 * @return filled MPatient-Object
	 */
	@Deprecated
	public MPatient getPatientByRowId(int subjectId) {
		/*
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the PATIENTS table is in the
		 * CORE module ROW_ID changed to subject_id
		 */
		String query = "SELECT * FROM MIMIC_CORE.PATIENTS WHERE subject_id = " + subjectId;
		return getOnePatientFromDb(query);
	}

	@Deprecated
	private MPatient getOnePatientFromDb(String query) {

		PreparedStatement statement;
		try {
			statement = connection.prepareStatement(query);
			ResultSet rs = statement.executeQuery();

			if (rs.next()) {
				MPatient mPat = new MPatient();
				// SUBJECT_ID
				mPat.setPatientSubjectId(rs.getString(2));
				// DOB
				mPat.setBirthDate(rs.getDate(4));
				// GENDER
				mPat.setGender(rs.getString(3));
				// DOD
				mPat.setDeathDate(rs.getDate(5));

				// Admissions
				getPatientAdmissions(mPat);

				return mPat;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void getPatientAdmissions(MPatient pat) {
		/*
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the ADMISSIONS table is in the
		 * CORE module
		 */
		try {
			statementGetAdmissions.setInt(1, Integer.parseInt(pat.getPatientSubjectId()));
			ResultSet rs = statementGetAdmissions.executeQuery();
			while (rs.next()) {
				MAdmission mAdm = new MAdmission();
				// core.admission.hadm_id now column 2 instead of 3
				mAdm.setAdmissionId(rs.getString(2));

				// Times
				// core.admission.admittime now column 3 instead of 4
				mAdm.setAdmissionTime(rs.getDate(3));
				// core.admission.dischtime now column 4 instead of 5
				mAdm.setDischargeTime(rs.getDate(4));

				// Type
				// core.admission.admission_type now column 6 instead of 7
				mAdm.setAdmissionType(rs.getString(6));

				// DschLoc
				// discharge_location now column 8 instead of 9
				mAdm.setDischargeLocation(rs.getString(8));

				// marital_status now column 11 instead of 12
				mAdm.setMaritalStatus(rs.getString(11));
				// core.admission.dischtime now column 10 instead of 11
				mAdm.setLanguage(rs.getString(10));
				// No more religion column in table core.admissions
				// mAdm.setReligion(rs.getString(12));
				// admission_location now column 7 instead of 8
				mAdm.setAdmissionLocation(rs.getString(7));

				// Diagnoses
				getDiagnoses(pat.getPatientSubjectId(), mAdm);

				// Procedures
				getProcedures(pat.getPatientSubjectId(), mAdm);

				// Chartevents
				getChartEvents(mAdm, pat.getPatientSubjectId());

				// Labevents
				getLabEvents(mAdm, pat.getPatientSubjectId());

				/*
				 * Due to this method being disabled this part has to be disabled as well until
				 * MIMIC IV receives sufficient updates //Noteevents getNoteEvents(mAdm,
				 * pat.getPatientSubjectId());
				 */

				// Prescriptions
				getPrescriptions(mAdm, pat.getPatientSubjectId());

				// Transfers
				getTransfers(mAdm, pat.getPatientSubjectId());

				// Patient ID
				mAdm.setPatId(pat.getPatientSubjectId());

				pat.addAdmission(mAdm);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void getChartEvents(MAdmission admission, String patientSubjId) {
		/*
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the CHARTEVENTS and D_ITEMS
		 * table are in the MIMIC_ICU module Column cgid (care giver id) does no longer
		 * exist in mimic_icu.chartevents table
		 */

		try {
			statementGetChartEvents.setInt(1, Integer.parseInt(admission.getAdmissionId()));
			ResultSet rs = statementGetChartEvents.executeQuery();

			while (rs.next()) {

				// Value = null ausschließen -> kein Wert
				if (rs.getObject(5) != null) {

					MChartevent event = new MChartevent();

					// Rekord-Datum
					event.setRecordDate(rs.getDate(3));

					// Type (Item)
					event.setMeasurementType(rs.getString(7));

					// Value + ValueNum
					event.setValue(rs.getString(4));
					if (rs.getObject(5) != null) {
						event.setNumValue(rs.getDouble(5));
					}

					// Unit
					if (rs.getObject(6) != null) {
						event.setUnit(rs.getString(6));
					}

					event.setPatId(patientSubjId);

					event.setEncId(admission.getAdmissionId());

					admission.addEvent(event);

				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void getLabEvents(MAdmission admission, String patientSubjId) {
		/**
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the LABEVENTS and D_LABITEMS
		 * table are in the MIMIC_HOSP module
		 */
		try {
			statementGetLabEvents.setInt(1, Integer.parseInt(patientSubjId));
			statementGetLabEvents.setInt(2, Integer.parseInt(admission.getAdmissionId()));
			ResultSet rs = statementGetLabEvents.executeQuery();

			// Test
			while (rs.next()) {
				// Value = null ausschließen -> kein Wert
				if (rs.getObject(4) != null) {

					MLabevent event = new MLabevent();

					// Rekord-Datum
					event.setAcquisitionDate(rs.getDate(3));

					// Type (Item)
					event.setMeasurementType(rs.getString(8));

					// Fluid
					event.setFluid(rs.getString(9));

					// Loinc-Code
					if (rs.getObject(10) != null) {
						event.setLoinc(rs.getString(10));
					}

					// Value + ValueNum
					event.setValue(rs.getString(4));
					if (rs.getObject(5) != null) {
						event.setNumValue(rs.getDouble(5));
					}

					// Unit
					if (rs.getObject(6) != null) {
						event.setUnit(rs.getString(6));
					}

					// Flag
					// "delta" - might mean both, not considered
					if (rs.getObject(7) != null && rs.getString(7) == "abnormal") {
						event.setAbnormal(true);
					}

					// Labevent_id
					event.setLabeventId(rs.getInt(11));

					// Comments
					event.setComments(rs.getString(12));

					event.setPatId(patientSubjId);

					event.setEncId(admission.getAdmissionId());

					admission.addLabEvent(event);

				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This method is disabled due to its corresponding table not currently being
	 * available in the database
	 */
	/*
	 * private void getNoteEvents(MAdmission admission, String patientSubjId) {
	 * String query = "SELECT * " + "FROM NOTEEVENTS " + "WHERE SUBJECT_ID = " +
	 * patientSubjId + " AND HADM_ID= " + admission.getAdmissionId();
	 * PreparedStatement statement; try { statement =
	 * connection.prepareStatement(query); ResultSet rs = statement.executeQuery();
	 * while (rs.next()) {
	 *
	 * boolean isError = rs.getString(10) == "1";
	 *
	 * MNoteevent event = new MNoteevent();
	 *
	 * event.setHasError(isError);
	 *
	 * //Charttime (incl. date; 5) and Chartdate (4) - two columns..
	 * if(rs.getObject(5) != null) { event.setChartdate(rs.getDate(5)); } else{
	 * event.setChartdate(rs.getDate(4)); }
	 *
	 * //might be null event.setCaregiverId(rs.getInt(9));
	 *
	 * event.setCategory(rs.getString(7)); event.setDescription(rs.getString(8));
	 *
	 * event.setText(rs.getString(11));
	 *
	 * admission.addNoteEvent(event);
	 *
	 * }
	 *
	 * } catch (SQLException e) { // TODO Auto-generated catch block
	 * e.printStackTrace(); } }
	 */
	private void getDiagnoses(String patId, MAdmission adm) {
		/*
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the diagnosis_icd and
		 * d_icd_diagnosis tables are in the mimic_hosp module The icd9_code column of
		 * the diagnoses_icd as well as in the d_icd_diagnoses table have been renamed
		 * to 'icd_code'
		 */
		try {
			statementGetDiagnoses.setInt(1, Integer.parseInt(patId));
			statementGetDiagnoses.setInt(2, Integer.parseInt(adm.getAdmissionId()));
			ResultSet rs = statementGetDiagnoses.executeQuery();
			while (rs.next()) {
				MDiagnose mDiag = new MDiagnose();
				mDiag.setIcdCode(rs.getString(4));
				mDiag.setLongTitle(rs.getString(7));
				mDiag.setSeqNumber(rs.getInt(3));
				mDiag.setIcdVersion(rs.getString(5));
				mDiag.setPatId(patId);
				mDiag.setIcdCode(adm.getAdmissionId());

				adm.addDiagnose(mDiag);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void getProcedures(String patId, MAdmission adm) {
		/*
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the procedures_icd and
		 * d_icd_procedures tables are in the mimic_hosp module The icd9_code column of
		 * the procedures_icd as well as in the d_icd_procedures table have been renamed
		 * to 'icd_code'
		 */
		try {
			statementGetProcedures.setInt(1, Integer.parseInt(patId));
			statementGetProcedures.setInt(2, Integer.parseInt(adm.getAdmissionId()));
			ResultSet rs = statementGetProcedures.executeQuery();
			while (rs.next()) {
				MProcedure mProc = new MProcedure();
				mProc.setIcdCode(rs.getString(5));
				mProc.setLongTitle(rs.getString(9));
				mProc.setSeqNumber(rs.getInt(3));
				switch (rs.getString(6)) {
				case "9":
					mProc.setIcdVersion(MProcedure.IcdVersion.ICD9PROC);
					break;
				case "10":
					mProc.setIcdVersion(MProcedure.IcdVersion.ICD10PROC);
					break;
				}
				mProc.setPatId(patId);
				mProc.setEncId(adm.getAdmissionId());

				adm.addProcedure(mProc);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This method is disabled due to its corresponding table not currently being
	 * available in the database
	 */
	/**
	 * Get dictionary with all caregivers - Key: Id, Value: Caregiver-Object
	 *
	 * @return dictionary
	 */
	/*
	 * public HashMap<Integer,MCaregiver> getCaregivers() { String query =
	 * "SELECT * FROM caregivers"; HashMap<Integer,MCaregiver> caregivers = new
	 * HashMap<Integer,MCaregiver>();
	 *
	 * PreparedStatement statement; try { statement =
	 * connection.prepareStatement(query); ResultSet rs = statement.executeQuery();
	 * while (rs.next()) { MCaregiver cg = new MCaregiver();
	 * cg.setCaregiverId(rs.getInt(2)); cg.setLabel(rs.getString(3));
	 * cg.setDescription(rs.getString(4));
	 *
	 * caregivers.put(cg.getCaregiverId(),cg); } } catch (SQLException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); }
	 *
	 * return caregivers; }
	 */
	private void getPrescriptions(MAdmission admission, String patientSubjId) {
		/*
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the PRESCRIPTIONS table is in
		 * the HOSP module
		 */
		try {
			statementGetPrescriptions.setInt(1, Integer.parseInt(patientSubjId));
			statementGetPrescriptions.setInt(2, Integer.parseInt(admission.getAdmissionId()));
			ResultSet rs = statementGetPrescriptions.executeQuery();
			while (rs.next()) {
				MPrescription pres = new MPrescription();

				// Column id's of the two columns changed from 5 to 4 and 6 to 5 respectively
				pres.setStart(rs.getDate(4));
				pres.setEnd(rs.getDate(5));

				// 7 to 6
				pres.setDrugtype(rs.getString(6));
				// 8 to 7
				pres.setDrug(rs.getString(7));

				/*
				 * Both columns where removed from the prescriptions table
				 *
				 * pres.setDrugNamePoe(rs.getString(8));
				 * pres.setDrugNameGeneric(rs.getString(10));
				 */

				// This column is no longer present in the prescriptions table
				// pres.setFormularyDrugCd(rs.getString(11));

				// 12 to 8
				pres.setGsn(rs.getString(8));
				// 13 to 9
				pres.setNdc(rs.getString(9));
				// 14 to 10
				pres.setProdStrength(rs.getString(10));
				// Attribute represented in the KDS spec thus it is included

				// 15 to 11
				pres.setDoseValRx(rs.getString(12));
				// 16 to 12
				pres.setDoseUnitRx(rs.getString(13));
				// 17 to 13
				pres.setFormValDisp(rs.getString(14));
				// 18 to 14
				pres.setFormUnitDisp(rs.getString(15));
				// 19 to 15
				pres.setRoute(rs.getString(17));

				pres.setPatId(patientSubjId);

				pres.setEncId(admission.getAdmissionId());

				admission.addPrescription(pres);

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void getTransfers(MAdmission admission, String patientSubjId) {
		/*
		 * Schema name added to table name due to structural changes in MIMIC IV
		 * grouping the tables into modules In this case the TRANSFERS table is in the
		 * CORE module
		 */
		try {
			statementGetTransfers.setInt(1, Integer.parseInt(patientSubjId));
			statementGetTransfers.setInt(2, Integer.parseInt(admission.getAdmissionId()));
			ResultSet rs = statementGetTransfers.executeQuery();

			int index = 0;
			while (rs.next()) {
				index++;
				MTransfer t = new MTransfer();

				// Column id changed 5 to 3
				t.setTransferId(rs.getString(3) + "-" + index);

				// 6 to 4
				t.setEventType(rs.getString(4));

				// Column no longer exists in transfers table
				// t.setPrevUnit(rs.getString(7));
				// 8 to 5, Column renamed to 'careunit'
				t.setCareUnit(rs.getString(5));

				// 11 to 6
				t.setIntime(rs.getDate(6));
				// 12 to 7
				t.setOuttime(rs.getDate(7));

				admission.addTransfer(t);

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public StationManager getStations() {
		try {
			ResultSet rs = statementGetStations.executeQuery();
			List<String> stationNames = new ArrayList<>();

			while (rs.next()) {
				stationNames.add(rs.getString(1));
			}

			return new StationManager(stationNames);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	private void getDiagnosticReports(MPatient patient) {
		try {
			statementGetDiagnosticReports.setInt(1, Integer.parseInt(patient.getPatientSubjectId()));
			ResultSet rs = statementGetDiagnosticReports.executeQuery();
			List<MDiagnosticReport> diagnosticReports = new ArrayList<>();

			while (rs.next()) {
				MDiagnosticReport dReport = new MDiagnosticReport();

				dReport.setSubjectId(rs.getString(1));
				dReport.setStudyId(rs.getString(2));
				dReport.setPath(rs.getString(3));
				dReport.setReport(rs.getString(4));
				this.getImagingStudies(dReport, patient.getPatientSubjectId());

				diagnosticReports.add(dReport);
			}

			patient.setDiagnosticReports(diagnosticReports);
		} catch (SQLException exc) {
			exc.printStackTrace();
		}
	}

	private void getImagingStudies(MDiagnosticReport diagnosticReport, String patientSubjId) {
		try {
			statementGetImagingStudies.setInt(1, Integer.parseInt(patientSubjId));
			statementGetImagingStudies.setInt(2, Integer.parseInt(diagnosticReport.getStudyId()));
			ResultSet rs = statementGetImagingStudies.executeQuery();
			List<MImagingStudy> iStudies = new ArrayList<>();

			while (rs.next()) {
				MImagingStudy iStudy = new MImagingStudy();

				iStudy.setSubjectId(rs.getString(1));
				iStudy.setSubjectId(rs.getString(2));
				iStudy.setDicomId(rs.getString(3));
				iStudy.setPath(rs.getString(4));

				iStudies.add(iStudy);
			}

			diagnosticReport.setStudies(iStudies);
		} catch (SQLException exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Due to changes in the structure of the transfers table the requested
	 * attributes curr_wardid and curr_careunit have changed and thus make this
	 * method not usable for now curr_careunit = careunit? curr_wardid =
	 * transfer_id?
	 * <p>
	 * Get dictionary with all locations = wards, key = wardId, value: MWard-Object
	 *
	 * @return dictionary
	 */
	/*
	 * public HashMap<Integer, MWard> getLocations() { //Schema name added to table
	 * name due to structural changes in MIMIC IV grouping the tables into modules
	 * //In this case the transfers table is in the mimic_core module String query =
	 * "SELECT DISTINCT transfer_id, careunit FROM mimic_core.transfers";
	 * HashMap<Integer,MWard> wards = new HashMap<Integer,MWard>();
	 *
	 * PreparedStatement statement; try { statement =
	 * connection.prepareStatement(query); ResultSet rs = statement.executeQuery();
	 * while (rs.next()) { MWard ward = new MWard(); ward.setWardId(rs.getInt(1));
	 * ward.setCareUnit(rs.getString(2));
	 *
	 * wards.put(ward.getWardId(),ward); } } catch (SQLException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); }
	 *
	 * return wards; }
	 */
	public void printRowCounts(String... tableNames) {
		// not transformed to proper prepared statement because a) doesn't work for this
		// query, b) isn't used
		PreparedStatement statement;
		ResultSet rs;
		for (String tableName : tableNames) {
			try {
				statement = this.connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
				rs = statement.executeQuery();
				rs.next();
				System.out.println(rs.getInt(1));
			} catch (SQLException exc) {
				System.out.println(exc.getMessage());
				exc.printStackTrace();
			}
		}
	}

	/**
	 * This is just a method for the analysis of the data base. It retrieves all
	 * distinct value in a specified column of a specified table and prints them to
	 * the console.
	 *
	 * @param table  table from which data will be retrieved
	 * @param column column of the table containing the data
	 */
	public void printDistinctEntriesOfColumn(String table, String column) {
		// not transformed to proper prepared statement because a) doesn't work for this
		// query, b) isn't used
		PreparedStatement statement;
		ResultSet rs;
		try {
			statement = this.connection.prepareStatement("SELECT " + column + " FROM " + table + " GROUP BY " + column);
			rs = statement.executeQuery();
			int i = 1;
			while (rs.next()) {
				System.out.println(i + ":" + rs.getString(1) + ";");
				i++;
			}
		} catch (SQLException exc) {
			System.out.println(exc.getMessage());
			exc.printStackTrace();
		}
	}

	public void printDistinctEntriesOfJoin(String tableJoin, String... columns) {
		// not transformed to proper prepared statement because a) doesn't work for this
		// query, b) isn't used
		PreparedStatement statement;
		ResultSet rs;
		try {
			String queryColumns = "";
			for (String column : columns) {
				queryColumns += (column + ", ");
			}
			queryColumns = queryColumns.substring(0, queryColumns.length() - 2);
			statement = this.connection.prepareStatement(
					"SELECT " + queryColumns + " FROM (" + tableJoin + ") AS T GROUP BY " + queryColumns);
			rs = statement.executeQuery();
			int i = 1;
			while (rs.next()) {
				System.out.print(i + ": ");
				for (int j = 1; j <= columns.length; j++) {
					System.out.print(rs.getString(j) + " ");
				}
				System.out.println();
				i++;
			}
		} catch (SQLException exc) {
			System.out.println(exc.getMessage());
			exc.printStackTrace();
		}
	}

	public String[] getAmountOfPatientIds(int numberOfAllPatients, boolean random) {
		PreparedStatement statement = random ? statementSelectAmountOfPatientIDsRandom
				: statementSelectAmountOfPatientIDs;
		try {
			String[] patientIds = new String[numberOfAllPatients];
			statement.setInt(1, numberOfAllPatients);
			ResultSet resultSet = statement.executeQuery();
			int thisId = 0;
			while (resultSet.next()) {
				patientIds[thisId++] = resultSet.getString("subject_id");
			}
			return patientIds;
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return null;
	}

	synchronized public MPatient getPatientBySubjectIdSynchronized(String subjectId) {
		return this.getPatientBySubjectId(subjectId);
	}

	public MPatient getPatientBySubjectId(String subjectId) {
		try {
			this.statementSelectOnePatientFromAdmissionsView.setInt(1, Integer.parseInt(subjectId));
			ResultSet rs = statementSelectOnePatientFromAdmissionsView.executeQuery();
			MPatient mPatient = new MPatient();
			boolean hasGottenPatientData = false;
			String currentHadmId = null;
			String previousHadmId = null;
			int transferIndex = 0;
			MAdmission mAdm = null;
			while (rs.next()) {
				if (!hasGottenPatientData) {
					// SUBJECT_ID
					mPatient.setPatientSubjectId(rs.getString(1));
					// GENDER
					mPatient.setGender(rs.getString(2));
					// DOD
					mPatient.setDeathDate(rs.getDate(6));
					hasGottenPatientData = true;
				}
				currentHadmId = rs.getString("admission_hadm_id");

				if (currentHadmId == null) {
					System.out.println(String.format("Subject %s has no admission!", subjectId));
				} else {

					if (!currentHadmId.equals(previousHadmId)) {
						// this is a new admission
						if (previousHadmId != null) {
							// when we encounter a new HADM id, we need to push the previous admission
							// however, this can't be done during the processing of the first db row
							mPatient.addAdmission(mAdm);
						}
						mAdm = new MAdmission();
						transferIndex = 0;
						mAdm.setAdmissionId(rs.getString("admission_hadm_id"));
						mAdm.setAdmissionTime(rs.getDate("admittime"));
						mAdm.setDischargeTime(rs.getDate("dischtime"));
						mAdm.setAdmissionType(rs.getString("admission_type"));
						mAdm.setDischargeLocation(rs.getString("discharge_location"));
						mAdm.setMaritalStatus(rs.getString("marital_status"));
						mAdm.setLanguage(rs.getString("language"));
						mAdm.setAdmissionLocation(rs.getString("admission_location"));
						getDetailsForMAdmissionWithoutTransfers(mAdm, mPatient.getPatientSubjectId());
					}

					MTransfer mTransfer = new MTransfer();

					mTransfer.setTransferId(rs.getString("transfer_id") + "-" + ++transferIndex);
					mTransfer.setEventType(rs.getString("eventtype"));
					mTransfer.setCareUnit(rs.getString("careunit"));
					mTransfer.setIntime(rs.getDate("intime"));
					mTransfer.setOuttime(rs.getDate("outtime"));
					assert mAdm != null;
					mAdm.addTransfer(mTransfer);

					previousHadmId = currentHadmId;
				}
			}
			if (configuration.useCXR()) {
				getDiagnosticReports(mPatient);
			}
			return mPatient;
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
		return null;
	}

	public MAdmission getDetailsForMAdmissionWithoutTransfers(MAdmission mAdmission, String subjectId) {
		getDiagnoses(subjectId, mAdmission);
		getProcedures(subjectId, mAdmission);
		getChartEvents(mAdmission, subjectId);
		getLabEvents(mAdmission, subjectId);
		getPrescriptions(mAdmission, subjectId);
		return mAdmission;
	}

	public enum IcdVersion {
		ICD9(9), ICD10(10);

		private final int value;

		IcdVersion(int value) {
			this.value = value;
		}

		public int valueOf() {
			return this.value;
		}
	}

	/**
	 * This is just a method for the analysis of the data base. It retrieves all ICD
	 * codes of a specified version and either prints them to the console or into a
	 * file
	 *
	 * @param version     the icd version of the codes you want to retrieve
	 * @param printToFile if this value is true a file containing the retrieved
	 *                    codes will be created. Otherwise all codes get printed to
	 *                    the console
	 */
	public ArrayList<String> getICDCodes(IcdVersion version, boolean printToFile) {
		ArrayList<String> icdList = new ArrayList<>();
		try {
			statementGetIcdCodes.setInt(1, version.valueOf());
			ResultSet rs = statementGetIcdCodes.executeQuery();
			if (printToFile) {
				try {
					File icdFile = new File("output\\icdOutput.txt");
					if (icdFile.createNewFile()) {
						System.out.println("File created: " + icdFile.getName());
					} else {
						System.out.println("File already exists.");
					}
					FileWriter writer = new FileWriter(icdFile);
					while (rs.next()) {
						writer.write((new StringBuffer(rs.getString(1)).insert(3, '.').toString() + ";"
								+ rs.getString(2) + "\n"));
						icdList.add(rs.getString(1));
					}
					writer.close();
				} catch (IOException exc) {
					System.out.println(exc.getMessage());
					exc.printStackTrace();
				}
			} else {
				while (rs.next()) {
					System.out.println(rs.getString(1) + ";" + rs.getString(2));
					icdList.add(rs.getString(1));
				}
			}
			return icdList;
		} catch (SQLException exc) {
			System.out.println(exc.getMessage());
			exc.printStackTrace();
			return null;
		}
	}

	public void printDBStructure() {
		PreparedStatement statement;
		ResultSet rs;
		try {
			statement = this.connection
					.prepareStatement("SELECT s.nspname AS schema_table, " + "s.oid AS id_schema, u.usename AS ROLE "
							+ "FROM pg_catalog.pg_namespace s JOIN pg_catalog.pg_user u "
							+ "ON u.usesysid = s.nspowner ORDER BY schema_table;");
			rs = statement.executeQuery();
			System.out.println("==Schemas:");
			while (rs.next()) {
				System.out.println(rs.getString(2) + ": " + rs.getString(1));
			}
		} catch (SQLException exc) {
			System.out.println(exc.getMessage());
			exc.printStackTrace();
		}
	}

}
