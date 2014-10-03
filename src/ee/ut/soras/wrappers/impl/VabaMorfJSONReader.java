package ee.ut.soras.wrappers.impl;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.json.Json;
import javax.json.stream.JsonParser;

import ee.ut.soras.wrappers.mudel.MorfAnRida;
import ee.ut.soras.wrappers.mudel.MorfAnSona;

/**
 *    JSON kujul Vabamorfi v2ljundi sisselugemine ja paigutamine MorfAnSona andmestruktuur.
 *   Kasutab JSR 353: Java API-it JSON-i parsimiseks ( https://jsonp.java.net/ ).
 *   NB! Vajab uuemat Java versiooni (vähemalt JRE7) ...  
 *   
 *   @author Siim Orasmaa
 */
public class VabaMorfJSONReader {
	
	/**
	 * 
	 *   JSONi formaadis vabamorfi v2ljundist morfoloogiliste analyyside v2ljalugemine. Tagastab
	 *  morfoloogilist analüüsi sisaldavate s6na-objektide j2rjendi. 
	 *  Eeldame, et morf analyysi JSON kuju on j2rgmine (N2ide):
	 *  <pre>
	 *  {
	 *    "paragraphs": [ {
	 *          "sentences": [ {
	 *                  "words": [
	 *                             {
	 *                                "analysis": [ { "clitic": "", "ending": "0", "form": "?", "partofspeech": "O", "root": "2000." } ],
	 *                                "text": "2000."
	 *                             },
	 *                             ...
	 *                           ]
	 *                         }
	 *                       ]
	 *                    }
	 *                  ]
	 * }
	 * </pre>
	 *                             
	 * @param input JSON formaadis vabamorfi v2ljund
	 * @return tekstis6nad koos morfoloogilise analyysiga (MorfAnSona objektide järjend)
	 * @throws Exception kui JSONi parsimisel midagi viltu l2heb
	 */
	public static List<MorfAnSona> parseJSONtext(BufferedReader input) throws Exception {
		List<MorfAnSona> tulemused = new ArrayList<MorfAnSona>();
		
		// JSON-i v6tmete rada ( k6ige pealmine on vaadeldav v6ti... )
		Stack<String> jsonKeyPath = new Stack<String>();
		// Viimane event
		JsonParser.Event lastEvent = null;
		// Viimasele event'ile vastav v22rtus (kui on)
		String lastString = "";
		
		List<String> keyValuePairs = new ArrayList<String>();
		// jooksva s6na morf analyysi read
		List<MorfAnRida> analyysiRead = new ArrayList<MorfAnRida>();
		// jooksva s6na tekstikuju
		String tokenText = null;
		
		JsonParser parser = Json.createParser( input );
		while (parser.hasNext()) {
			JsonParser.Event event = parser.next();
			switch(event) {
				case START_ARRAY:
					if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
						jsonKeyPath.push( lastString );
					}
					//
					//System.out.println(printPath(jsonKeyPath)+" || "+event.toString());
					//
					lastEvent = event;
					lastString = "";
					break;
				case END_ARRAY:
					//
					//System.out.println(printPath(jsonKeyPath)+" || "+event.toString());
					//
					// ------------------------------------------------------------------
					//   Sõna-järjendi lõpp: märgime, et viimane sõna lõpetas lause
					// ------------------------------------------------------------------
					if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals("words")){
						if (!tulemused.isEmpty()){
							// Märgime, et viimase sõna järel lõpeb lause
							(tulemused.get(tulemused.size()-1)).setOnLauseLopp(true);
						}
					}
					lastEvent = event;
					lastString = "";
					jsonKeyPath.pop();
					break;
				case START_OBJECT:
					//
					//System.out.println(printPath(jsonKeyPath)+" || "+event.toString());
					//
					// ------------------------------------------------------------------
					//   Analüüs-objekti algus: tühjendame keyValuePairs massiivi
					// ------------------------------------------------------------------
					if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals("analysis")){
						keyValuePairs.clear();
					}
					lastEvent = event;
					lastString = "";
					break;
				case END_OBJECT:
					//
					//System.out.println(printPath(jsonKeyPath)+" || "+event.toString());
					//
					// ------------------------------------------------------------------
					//   Sõna-objekti lõpp: moodustame kokkukogutud andmetest uue sõna
					// ------------------------------------------------------------------
					if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals( "words")){
						if (tokenText == null){
							throw new Exception(" Unable to find text form for a word ...");  
						}
						MorfAnSona sona = new MorfAnSona(tokenText);
						for (MorfAnRida morfAnRida : analyysiRead) {
							sona.lisaAnalyysiRida( morfAnRida );
						}
						tulemused.add(sona);
						sona.setTokenPosition( tulemused.size() );
						tokenText = null;
						analyysiRead.clear();
					}
					// ------------------------------------------------------------------
					//   Analüüsi-objekti lõpp: moodustame uue analüüsirea
					// ------------------------------------------------------------------
					if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals( "analysis")){
						MorfAnRida analyysirida = new MorfAnRida(keyValuePairs);
						analyysiRead.add(analyysirida);
						keyValuePairs.clear();
					}
					lastEvent = event;
					lastString = "";
					break;
				case VALUE_FALSE:
				case VALUE_NULL:
				case VALUE_TRUE:
					//
					//System.out.println(printPath(jsonKeyPath)+" || "+event.toString());
					//
					lastEvent = event;
					lastString = "";
					break;
				case KEY_NAME:
					//
					//System.out.println(printPath(jsonKeyPath)+" || "+event.toString() + " " +
					//                 parser.getString() + " - ");
					lastEvent  = event;
					lastString = parser.getString();
					break;
				case VALUE_STRING:
				case VALUE_NUMBER:
					//
					//System.out.println(printPath(jsonKeyPath)+" || "+event.toString() + " " +
					//                   parser.getString());
					keyValuePairs.add(lastString);
					keyValuePairs.add(parser.getString());
					// ---------------------------------------------------
					//   Jätame meelde sõna tekstilise kuju ...
					// ---------------------------------------------------
					if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals( "words")){
						if (lastString != null && lastString.equals("text")){
							tokenText = parser.getString();
						}
					}
					lastEvent  = event;
					lastString = parser.getString();
					break;
			}
		}
		return tulemused;
	}
	
	/**
	 *  Debug: Väljastab magasini sisu sõnena;
	 */
	static String debugPrintPath(Stack<String> path){
		String s = "";
		for (int i = 0; i < path.size(); i++) {
			s += " "+path.get(i);
		}
		return s;
	}

}
