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

public class Helper {
	
	/**
	 * Source: https://stackoverflow.com/questions/9710185/how-to-deal-with-invalid-characters-in-a-ws-output-when-using-cxf
	 * From xml spec valid chars:
	 * #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF] 
	 * any Unicode character, excluding the surrogate blocks, FFFE, and FFFF.
	 * @param text The String to clean
	 * @param replacement The string to be substituted for each match
	 * @return The resulting String
	 */
	public static String CleanInvalidXmlChars(String text, String replacement) {
	    String re = "[^\\x09\\x0A\\x0D\\x20-\\xD7FF\\xE000-\\xFFFD\\x10000-x10FFFF]";
		String newText = text.replaceAll(re, replacement);
		return newText;
	}
}
