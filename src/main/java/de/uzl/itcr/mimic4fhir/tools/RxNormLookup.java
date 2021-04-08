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
package de.uzl.itcr.mimic4fhir.tools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Provide RxNorm-Lookup for NDC (National Drug Code) and GSN (Generic Sequence Number)
 * Singleton: Use getInstance to get working object
 *
 * @author Stefanie Ververs
 */
public class RxNormLookup {

    private final ConcurrentHashMap<String, OptionalCacheResult<RxNormConcept>> rdxLookupNdc;
    private final ConcurrentHashMap<String, OptionalCacheResult<RxNormConcept>> rdxLookupGsn;
    private final ConcurrentHashMap<String, OptionalCacheResult<Ingredient>> ingredientLookupNdc;

    public RxNormLookup() {
        rdxLookupNdc = new ConcurrentHashMap<>();
        rdxLookupGsn = new ConcurrentHashMap<>();
        ingredientLookupNdc = new ConcurrentHashMap<>();
    }

    static class OptionalCacheResult<T> {

        private final boolean hasValue;
        private List<T> data = null;
        private CacheResultType resultType;

        public OptionalCacheResult(CacheResultType resultType) {
            this.hasValue = false;
            this.resultType = resultType;
        }

        public OptionalCacheResult(CacheResultType resultType, List<T> data) {
            this.data = data;
            this.resultType = resultType;
            this.hasValue = true;
        }

        public boolean isHasValue() {
            return hasValue;
        }

        public List<T> getData() {
            return data;
        }

        public CacheResultType getResultType() {
            return resultType;
        }

        @Override
        public String toString() {
            if (!hasValue)
                return "OptionalCacheResult{" +
                        "hasValue=" + false +
                        ", resultType=" + resultType +
                        '}';
            else if (data != null)
                return "OptionalCacheResult{" +
                        "hasValue=" + true +
                        ", data=" + data.size() +
                        ", resultType=" + resultType +
                        '}';
            return "OptionalCacheResult{" +
                    "hasValue=" + true +
                    ", data=MISSING!!!" +
                    ", resultType=" + resultType +
                    '}';
        }
    }

    enum CacheResultType {
        Ndc,
        Gsn,
        Ingredient
    }

    /**
     * Get RxNormConcepts for a NDC
     *
     * @param ndc National Drug Code
     * @return List of RxNorm-Concept
     */
    public List<RxNormConcept> getRxNormForNdc(String ndc) {
        if (rdxLookupNdc.containsKey(ndc)) {
            OptionalCacheResult<RxNormConcept> inCache = rdxLookupNdc.get(ndc);
            return inCache.hasValue ? inCache.getData() : null;
            //return rdxLookupNdc.get(ndc);
        } else {
            List<RxNormConcept> rdxNorm = findRxNormForNdc(ndc);
            if (rdxNorm.size() > 0) rdxLookupNdc.put(ndc, new OptionalCacheResult<>(CacheResultType.Ndc, rdxNorm));
            else {
                rdxLookupNdc.put(ndc, new OptionalCacheResult<RxNormConcept>(CacheResultType.Ndc));
                return null;
            }
            return rdxNorm;
            /*if (rdxNorm != null) {
                rdxLookupNdc.put(ndc, rdxNorm);
                return rdxNorm;
            }*/
        }
        //return null;
    }

    /**
     * Get RxNormConcepts for a GSN
     *
     * @param gsn Generic Sequence Number
     * @return List of RxNorm-Concept
     */
    public List<RxNormConcept> getRxNormForGsn(String gsn) {
        if (rdxLookupGsn.containsKey(gsn)) {
            OptionalCacheResult<RxNormConcept> inCache = rdxLookupGsn.get(gsn);
            return inCache.hasValue ? inCache.getData() : null;
            //return rdxLookupGsn.get(gsn);
        } else {
            List<RxNormConcept> rdxNorm = findRxNormForGsn(gsn);
            if (rdxNorm.size() > 0) rdxLookupGsn.put(gsn, new OptionalCacheResult<>(CacheResultType.Gsn, rdxNorm));
            else {
                rdxLookupGsn.put(gsn, new OptionalCacheResult<RxNormConcept>(CacheResultType.Gsn));
                return null;
            }
            return rdxNorm;
            /*if (rdxNorm != null) {
                rdxLookupGsn.put(gsn, rdxNorm);
                return rdxNorm;
            }*/
        }
        //return null;
    }

    public List<Ingredient> getIngredientsForNDC(String ndc) {
        if (ingredientLookupNdc.containsKey(ndc)) {
            OptionalCacheResult<Ingredient> inCache = ingredientLookupNdc.get(ndc);
            //System.out.println("INGR hit: " + ndc + " - " + inCache);
            return inCache.hasValue ? inCache.getData() : null;
            //return ingredientLookupNdc.get(ndc);
        } else {
            //System.out.println("INGR miss: " + ndc);
            List<Ingredient> ingredientList = findIngredientsForNDC(ndc);
            if (ingredientList != null && ingredientList.size() > 0)
                ingredientLookupNdc.put(ndc, new OptionalCacheResult<>(CacheResultType.Ingredient, ingredientList));
            else {
                ingredientLookupNdc.put(ndc, new OptionalCacheResult<Ingredient>(CacheResultType.Ingredient));
                return null;
            }
            return ingredientList;
            /*if (ingredientList != null) {
                ingredientLookupNdc.put(ndc, ingredientList);
                return ingredientList;
            }*/
        }
        //return null;
    }

    private List<RxNormConcept> findRxNormForGsn(String gsn) {
        String url = "https://rxnav.nlm.nih.gov/REST/rxcui.json?idtype=GCN_SEQNO&id=" + gsn;
        return findRxNorm(url);
    }

    private List<RxNormConcept> findRxNormForNdc(String ndc) {
        String url = "https://rxnav.nlm.nih.gov/REST/rxcui.json?idtype=NDC&id=" + ndc;
        return findRxNorm(url);
    }

    private List<Ingredient> findIngredientsForNDC(String ndc) {
        List<RxNormConcept> rxNormConcepts = this.findRxNormForNdc(ndc);
        if (rxNormConcepts != null) {
            List<Ingredient> ingredientsList = new ArrayList<>();
            for (RxNormConcept rxConcept : rxNormConcepts) {
                String rxCui = rxConcept.getCui();
                String ingredientUrl = "https://rxnav.nlm.nih.gov/REST/rxcui/" + rxCui
                        + "/allrelated.json";
                List<Ingredient> ingredientsReturn = this.findIngredients(ingredientUrl);
                if (ingredientsReturn != null) {
                    ingredientsList.addAll(ingredientsReturn);
                }
            }
            if (ingredientsList != null) {
                return ingredientsList;
            }
        }
        return null;
    }

    private List<RxNormConcept> findRxNorm(String url) {
        //use of RxNorm REST API https://rxnav.nlm.nih.gov/REST

        CloseableHttpClient httpclient = HttpClients.createDefault();

        List<RxNormConcept> rxNormList = new ArrayList<RxNormConcept>();

        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response = null;
        try {
            //GET
            response = httpclient.execute(httpGet);

            //Response -> JSON Object
            HttpEntity entity = response.getEntity();
            String jsonResponse = EntityUtils.toString(entity);
            InputStream is = new ByteArrayInputStream(jsonResponse.getBytes());

            JsonReader jsonReader = Json.createReader(is);
            JsonObject respObject = jsonReader.readObject();
            jsonReader.close();

            JsonArray ids = respObject.getJsonObject("idGroup").getJsonArray("rxnormId");
            if (ids != null && !ids.isEmpty()) {
                for (JsonString rxNorm : ids.getValuesAs(JsonString.class)) {
                    RxNormConcept rc = new RxNormConcept();
                    rc.setCui(rxNorm.getString());
                    //get Name: Separate Call
                    rc.setName(getNameForCui(rc.getCui()));
                    rxNormList.add(rc);
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
                }
            }
        }
        return rxNormList;

    }

    private List<Ingredient> findIngredients(String url) {
        //use of RxNorm REST API https://rxnav.nlm.nih.gov/REST

        //First: Get ingredient rxCUIs for a rxNorm concept
        CloseableHttpClient httpclient = HttpClients.createDefault();

        List<Ingredient> ingredients = new ArrayList<Ingredient>();

        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response = null;

        try {
            //GET: ingredient names and rxcui's for later HTTP requests
            response = httpclient.execute(httpGet);

            //Response -> JSON Object
            HttpEntity entity = response.getEntity();
            String jsonResponse = EntityUtils.toString(entity);
            InputStream is = new ByteArrayInputStream(jsonResponse.getBytes());

            JsonReader jsonReader = Json.createReader(is);
            JsonObject respObject = jsonReader.readObject();
            jsonReader.close();

            JsonArray relatedJson = respObject.getJsonObject("allRelatedGroup").getJsonArray("conceptGroup");
            if (relatedJson != null && !relatedJson.isEmpty()) {
                //Multiple ingredients might be present; found in the IN TTY section of the JSON response
                JsonArray mIngredientsArray = relatedJson.getJsonObject(4).getJsonArray("conceptProperties");
                if (mIngredientsArray != null && !mIngredientsArray.isEmpty()) {
                    for (JsonObject jsonIngredient : mIngredientsArray.getValuesAs(JsonObject.class)) {
                        Ingredient ingredient = new Ingredient(jsonIngredient.getString("name"), jsonIngredient.getString("rxcui"));
                        ingredients.add(ingredient);
                    }
                }
            }

            //GET: ATC and SNOMED codes for each ingredient
            for (Ingredient ingredient : ingredients) {
                String atcUrl = "https://rxnav.nlm.nih.gov/REST/rxcui/" + ingredient.getRxCui() + "/property.json?propName=ATC";
                List<String> atcCodes = this.getCodes(atcUrl);
                if (atcCodes != null && !atcCodes.isEmpty()) {
                    ingredient.addAtcCodes(atcCodes);
                }

                String snomedUrl = "https://rxnav.nlm.nih.gov/REST/rxcui/" + ingredient.getRxCui() + "/property.json?propName=SNOMEDCT";
                List<String> snomedCodes = this.getCodes(snomedUrl);
                if (snomedCodes != null && !snomedCodes.isEmpty()) {
                    ingredient.addSnomedCodes(snomedCodes);
                }

                String uniiUrl = "https://rxnav.nlm.nih.gov/REST/rxcui/" + ingredient.getRxCui() + "/property.json?propName=UNII_CODE";
                List<String> uniiCodes = this.getCodes(uniiUrl);
                if (uniiCodes != null && !uniiCodes.isEmpty()) {
                    ingredient.addUniiCodes(uniiCodes);
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
        return ingredients;
    }

    /**
     * Returns a list of codes depending on the url provided to the method. Its intended to be used with URLs like:
     * https://rxnav.nlm.nih.gov/REST/rxcui/{rxcui}/property.json?propName={codesystem}
     * It is used primarily for retrieving ATC and SNOMEDCT codes for a given ingredient
     *
     * @param codesUrl URL of the RxNorm API for retrieving the codes
     * @return list containing the retrieved codes as Strings
     */
    private List<String> getCodes(String codesUrl) {
        //use of RxNorm REST API https://rxnav.nlm.nih.gov/REST

        //Get codes for a given rxCUI based on the URL
        CloseableHttpClient httpclient = HttpClients.createDefault();

        List<String> codeList = new ArrayList<String>();

        HttpGet httpGet = new HttpGet(codesUrl);

        CloseableHttpResponse response = null;

        try {
            //GET: codes from the rxNorm API
            response = httpclient.execute(httpGet);

            HttpEntity entity = response.getEntity();
            String jsonResponse = EntityUtils.toString(entity);
            InputStream is = new ByteArrayInputStream(jsonResponse.getBytes());

            JsonReader jsonReader = Json.createReader(is);
            JsonObject respObject = jsonReader.readObject();
            jsonReader.close();

            //Get JSON array containing all the retrieved codes from the JSON response
            if (!respObject.isNull("propConceptGroup")) {
                JsonArray jsonCodeList = respObject.getJsonObject("propConceptGroup").getJsonArray("propConcept");
                if (jsonCodeList != null && !jsonCodeList.isEmpty()) {
                    for (JsonObject codeObject : jsonCodeList.getValuesAs(JsonObject.class)) {
                        codeList.add(codeObject.getString("propValue"));
                    }
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
                }
            }
        }
        return codeList;
    }

    private String getNameForCui(String cui) {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        String name = null;
        String url = "https://rxnav.nlm.nih.gov/REST/rxcui/" + cui + "/property.json?propName=RxNorm%20Name";
        HttpGet httpGet = new HttpGet(url);

        CloseableHttpResponse response = null;
        try {
            //GET
            response = httpclient.execute(httpGet);

            //Response -> JSON Object
            HttpEntity entity = response.getEntity();
            String jsonResponse = EntityUtils.toString(entity);
            InputStream is = new ByteArrayInputStream(jsonResponse.getBytes());

            JsonReader jsonReader = Json.createReader(is);
            JsonObject respObject = jsonReader.readObject();
            jsonReader.close();

            name = respObject.getJsonObject("propConceptGroup").getJsonArray("propConcept").get(0).asJsonObject().getString("propValue");

            EntityUtils.consume(entity);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (Exception ex) {
                }
            }
        }
        return name;
    }
}
