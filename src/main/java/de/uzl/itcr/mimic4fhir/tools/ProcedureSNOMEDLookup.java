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

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import de.uzl.itcr.mimic4fhir.work.Config;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ProcedureSNOMEDLookup {

	private final ConcurrentHashMap<String, List<String>> icd9SnomedLookupTable;
	private final ConcurrentHashMap<String, List<String>> icd10SnomedLookupTable;
	private Config config;

	public ProcedureSNOMEDLookup(Config config) {
		this.icd9SnomedLookupTable = new ConcurrentHashMap<>();
		this.icd10SnomedLookupTable = new ConcurrentHashMap<>();
		this.config = config;
	}

	public List<String> getSnomedForIcd9(String icd9code) {
		// Remove unnecessary blank space characters
		icd9code = icd9code.trim();
		// Insert '.' after second character
		icd9code = icd9code.substring(0, 2) + "." + icd9code.substring(2);
		if (this.icd9SnomedLookupTable.containsKey(icd9code)) {
			return this.icd9SnomedLookupTable.get(icd9code);
		} else {

			String url = config.getICD9ProcToSnomed()
					+ "/$translate?system=http://hl7.org/fhir/sid/icd-9-cm/procedure&code=" + icd9code
					+ "&source=http://hl7.org/fhir/sid/icd-9-cm/procedure&target=http://snomed.info/sct";
			List<String> snomedCodes = this.findSnomedCode(url);

			this.icd9SnomedLookupTable.put(icd9code, snomedCodes);
			return snomedCodes;
		}
	}

	public List<String> getSnomedForIcd10(String icd10code) {
		// Remove unnecessary blank space characters
		icd10code = icd10code.trim();
		if (this.icd10SnomedLookupTable.containsKey(icd10code)) {
			return this.icd10SnomedLookupTable.get(icd10code);
		} else {
			
			String url = config.getICD10PCStoSnomed()
					+ "/$translate?system=http://hl7.org/fhir/sid/icd-10-pcs&code=" + icd10code
					+ "&source=http://hl7.org/fhir/sid/icd-10-pcs&target=http://snomed.info/sct";
			List<String> snomedCodes = this.findSnomedCode(url);

			this.icd10SnomedLookupTable.put(icd10code, snomedCodes);
			return snomedCodes;
		}
	}

	private List<String> findSnomedCode(String url) {
		CloseableHttpClient httpclient = HttpClients.createDefault();

		HttpGet httpGet = new HttpGet(url);

		List<String> snomedCodes = new ArrayList<>();

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
				JsonArray jsonSnomedCodes = respObject.getJsonArray("parameter");
				for (int i = 1; i < jsonSnomedCodes.size(); i++) {
					String snomedCode = jsonSnomedCodes.getJsonObject(i).getJsonArray("part").getJsonObject(1)
							.getJsonObject("valueCoding").getString("code");
					snomedCodes.add(snomedCode);
				}
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
		return snomedCodes;
	}

}
