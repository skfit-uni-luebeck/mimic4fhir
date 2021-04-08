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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import de.uzl.itcr.mimic4fhir.work.Config;
import de.uzl.itcr.mimic4fhir.work.ConnectDB;

public class ICD9MapperLookup {

	private final ConcurrentHashMap<String, String> snomedLookupTable;
	private final ConcurrentHashMap<String, String> icd10gmLookupTable;
	private Config config;

	public ICD9MapperLookup(Config config) {
		this.snomedLookupTable = new ConcurrentHashMap<>();
		this.icd10gmLookupTable = new ConcurrentHashMap<>();
		this.config = config;
	}

	public String getICD10GMCode(String icd9code) {
		String formalIcd9code = StringManipulator.conformIcdString(icd9code);
		if (this.icd10gmLookupTable.containsKey(formalIcd9code)) {
			String cacheCode = icd10gmLookupTable.get(formalIcd9code);
			// System.out.println("ICD10 hit - " + formalIcd9code + " - " + cacheCode);
			return cacheCode.equals("NULL") ? null : cacheCode;
		} else {
			try {
				List<String> codes = findICD10GM(config.getICD9toICD10GM()
						+ "/$translate?system=http://hl7.org/fhir/sid/icd-9-cm&code=" + formalIcd9code
						+ "&source=http://hl7.org/fhir/sid/icd-9-cm&target=http://fhir.de/CodeSystem/dimdi/icd-10-gm");

				List<String> eightCodes = new ArrayList<>(), nineCodes = new ArrayList<>(),
						otherCodes = new ArrayList<>();
				for (String code : codes) {
					// this.snomedLookupTable.put(formalIcd9code, code);
					// TODO this doesn't make sense, does it? why does this put into SNOMED lookup
					// table?!

					// Extract ending of icd code; pattern: LNN.N or LNN.NN
					String ending = code.length() > 5 ? code.substring(5) : code.substring(4);
					switch (ending) {
					case ("8"):
						eightCodes.add(code);
						break;
					case ("9"):
						nineCodes.add(code);
						break;
					default:
						otherCodes.add(code);
						break;
					}
				}
				// Codes with '9' endings are least specific and thus the safest option followed
				// by '8' endings
				if (!nineCodes.isEmpty()) {
					this.icd10gmLookupTable.put(formalIcd9code, nineCodes.get(0));
					return nineCodes.get(0);
				} else if (!eightCodes.isEmpty()) {
					this.icd10gmLookupTable.put(formalIcd9code, eightCodes.get(0));
					return eightCodes.get(0);
				} else {
					this.icd10gmLookupTable.put(formalIcd9code, otherCodes.get(0));
					return otherCodes.get(0);
				}
			} catch (NoMatchError exc) {
				this.icd10gmLookupTable.put(formalIcd9code, "NULL");
				return null;
			}
		}
	}

	private List<String> findICD10GM(String url) {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		List<String> icd10gmCodes = new ArrayList<>();

		CloseableHttpResponse response = null;
		try {

			// GET
			response = httpclient.execute(httpGet);

			// Response -> JSON Object
			HttpEntity entity = response.getEntity();
			String jsonResponse = EntityUtils.toString(entity);
			InputStream is = new ByteArrayInputStream(jsonResponse.getBytes());

			JsonReader jsonReader = Json.createReader(is);
			JsonObject respObject = jsonReader.readObject();
			jsonReader.close();

			if (respObject.getJsonArray("parameter").getJsonObject(0).get("valueBoolean").toString().equals("true")) {
				JsonArray jsonIcd10GmCodes = respObject.getJsonArray("parameter");

				// If only one match has been found only one element is present
				if (jsonIcd10GmCodes.size() <= 2) {
					icd10gmCodes.add(jsonIcd10GmCodes.getJsonObject(1).getJsonArray("part").getJsonObject(1)
							.getJsonObject("valueCoding").get("code").toString().replaceAll("\"", ""));
				}
				// Get all matches if multiple are present, start a second element (index 1)
				// since the first element does
				// not contain a match but other information
				else {
					for (int index = 1; index < jsonIcd10GmCodes.size(); index++) {
						JsonObject jsonIcd10GmCode = jsonIcd10GmCodes.getJsonObject(index);
						icd10gmCodes.add(jsonIcd10GmCode.getJsonArray("part").getJsonObject(1)
								.getJsonObject("valueCoding").get("code").toString().replaceAll("\"", ""));
					}
				}
			} else {
				throw new NoMatchError("No match found for ICD9 code!");
			}

			EntityUtils.consume(entity);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return icd10gmCodes;
	}

	public String getSNOMEDCode(String icd9code) {
		String formalIcd9code = StringManipulator.conformIcdString(icd9code);
		if (this.snomedLookupTable.containsKey(formalIcd9code)) {
			String cacheCode = snomedLookupTable.get(formalIcd9code);
			// System.out.println("SNOMED hit - " + formalIcd9code + " - " + cacheCode);
			return cacheCode.equals("NULL") ? null : cacheCode;
		} else {
			try {
				String code = findSNOMED(config.getICD9ToSnomed()
						+ "/$translate?system=http://hl7.org/fhir/sid/icd-9-cm&code=" + formalIcd9code
						+ "&source=http://hl7.org/fhir/sid/icd-9-cm&target=http://snomed.info/sct");
				this.snomedLookupTable.put(formalIcd9code, code);
				return code;
			} catch (NoMatchError exc) {
				System.out.println("No match found for ICD9 code '" + formalIcd9code + "'!");
				this.snomedLookupTable.put(formalIcd9code, "NULL");
				return null;
			}
		}
	}

	private String findSNOMED(String url) throws NoMatchError {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet(url);
		String snomedCode = null;
		CloseableHttpResponse response = null;
		try {
			// GET
			response = httpclient.execute(httpGet);
			// Response -> JSON Object
			HttpEntity entity = response.getEntity();
			String jsonResponse = EntityUtils.toString(entity);
			InputStream is = new ByteArrayInputStream(jsonResponse.getBytes());

			JsonReader jsonReader = Json.createReader(is);
			JsonObject respObject = jsonReader.readObject();
			jsonReader.close();

			if (respObject.getJsonArray("parameter").getJsonObject(0).get("valueBoolean").toString().equals("true")) {
				JsonValue jsonSnomedCode = respObject.getJsonArray("parameter").getJsonObject(1).getJsonArray("part")
						.getJsonObject(1).getJsonObject("valueCoding").get("code");
				if (jsonSnomedCode != null) {
					snomedCode = jsonSnomedCode.toString().replaceAll("\"", "");
				}
			} else {
				throw new NoMatchError("No match found for ICD9 code!");
			}

			EntityUtils.consume(entity);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return snomedCode;
	}

	public void printMissingMappings(ConnectDB.IcdVersion version, Config fhirConfig) {
		ConnectDB db = new ConnectDB(fhirConfig);
		List<String> icdList = db.getICDCodes(version, false), missingList = new ArrayList<>();
		String urlStart = "", urlEnd = "";
		switch (version.valueOf()) {
		case 9:
			urlStart = "https://ontoserver.imi.uni-luebeck.de/fhir/ConceptMap/9f0b2a1f-8253-47fc-a8cf-118226823e22/$translate?system=http://hl7.org/fhir/sid/icd-9-cm&code=";
			urlEnd = "&source=http://hl7.org/fhir/sid/icd-9-cm&target=http://snomed.info/sct";
		case 10:
			urlStart = "https://ontoserver.imi.uni-luebeck.de/fhir/ConceptMap/d9be1278-282b-4e80-8be5-226cb30a9eb5/$translate?system=http://hl7.org/fhir/sid/icd-9-cm&code=";
			urlEnd = "&source=http://hl7.org/fhir/sid/icd-9-cm&target=http://fhir.de/CodeSystem/dimdi/icd-10-gm";
		}
		int codeNum = icdList.size();
		int progress = 0;
		for (String icdCode : icdList) {
			String formalIcdCode = StringManipulator.conformIcdString(icdCode);
			try {
				String url = urlStart + formalIcdCode + urlEnd;
				this.findICD10GM(url);
			} catch (NoMatchError error) {
				missingList.add(formalIcdCode);
			}
			System.out.println("Progress: " + ++progress + "/" + codeNum);
		}
		if (missingList.size() > 0) {
			try {
				File mappingFile = new File("output\\icdOutput" + version.toString() + ".txt");
				if (mappingFile.createNewFile()) {
					System.out.println("File with missing mappings created: " + mappingFile.getName());
				} else {
					System.out.println("File with missing mappings already exists.");
				}
				FileWriter writer = new FileWriter(mappingFile);
				for (String icdCode : missingList) {
					writer.write(icdCode + "\n");
				}
				writer.close();
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
	}
}
