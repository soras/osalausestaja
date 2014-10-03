package ee.ut.soras.osalau;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;

/**
 *  Osalausestamise tulemuste vormistamine sobivale kujule (Vabamorfi JSON, T3-OLP).
 *  
 *  @author Siim Orasmaa
 */
public class ValjundiVormistaja {
	
	/**
	 *   Tagastab v&auml;ljundi, kus tulemused on v2ljastatud T3-OLP kujul:
	 *   <ul>
	 *     <li> kui <code>skipIgnorePart == true</code>, siis on eemaldatud ignore-margendid koos sisuga.
	 *     <li> alles on lausepiiride jms margendid
	 *     <li> iga token on eraldi real
	 *   </ul>
	 */
	public static String vormistaTulemusT3OLPkujul(String sisendT3OLP, List<OsalauSona> margendusegaSonad, boolean skipIgnorePart) throws Exception {
		// V2ga umbkaudne hinnang suurusele
		StringBuilder valjund = new StringBuilder(margendusegaSonad.size() * 20);
		// Loome mappingu sisendteksti ja margenduste vahele
		HashMap<Integer, OsalauSona> tokenPosToMargendusMap = new HashMap<Integer, OsalauSona>();
		for (int i = 0; i < margendusegaSonad.size(); i++) {
			OsalauSona margendus = margendusegaSonad.get(i);
			if ((margendus.getMorfSona()).getTokenPosition() != -1){
				int key = (margendus.getMorfSona()).getTokenPosition();
				if (!tokenPosToMargendusMap.containsKey(key)){
					tokenPosToMargendusMap.put(key, margendus);
				} else {
					throw new Exception(" Duplicate labels for position "+String.valueOf(key)+" "+(margendus.getMorfSona()).getAlgSona()+": "+margendus+" "+(tokenPosToMargendusMap.get(i)));
				}
			} else {
				throw new Exception(" Unable to map MorfAnSona to tokenposition for "+(margendus.getMorfSona()).getAlgSona());
			}
		}
		// Kammime sisenditeksti l2bi, j2tame vahele ignore osad ning pistame sobivasse kohta vahele
		// ajav2ljendid
		String rida       = null;
		int tokenPosition = 1;
		BufferedReader input = new BufferedReader( new StringReader(sisendT3OLP) );
		while ((rida = input.readLine()) != null){
			// ---------------------------------------------------------------------------------------
			//  Jätame vahele ignoreeritava osa
			// ---------------------------------------------------------------------------------------
			if (rida.length() > 0 && rida.equals("<ignoreeri>")){
				if (!skipIgnorePart){
					valjund.append(rida);
					valjund.append("\n");
				}
				tokenPosition++;
				while ((rida = input.readLine()) != null){
					if (!skipIgnorePart){
						valjund.append(rida);
						valjund.append("\n");
					}
					tokenPosition++;
					if (rida.length() > 0 && rida.equals("</ignoreeri>")){
						break;
					}
				}
				continue;
			}
			// ---------------------------------------------------------------------------------------
			//  Sisu
			// ---------------------------------------------------------------------------------------
			if (rida.length() > 0){
				StringBuilder sonaStr = new StringBuilder(rida);
				sonaStr.append("\n");
				
				if (tokenPosToMargendusMap.containsKey(tokenPosition)){
					// TODO: Siin nyyd trykime v2lja s6nadega seotud m2rgenduse
					OsalauSona margend = tokenPosToMargendusMap.get( tokenPosition );
					if ( !(margend.getKindladOLPMargendid()).isEmpty() ){
						for (OsalauSona.MARGEND seotudMargend : margend.getKindladOLPMargendid()) {
							if (seotudMargend == OsalauSona.MARGEND.KINDEL_PIIR){
								sonaStr.append("<kindel_piir/>");
								sonaStr.append("\n");
							} else if (seotudMargend == OsalauSona.MARGEND.KIILU_ALGUS){
								// Kiilu alguse lisame enne s6na ...
								sonaStr.insert(0, "<kiil>" + "\n");
							} else if (seotudMargend == OsalauSona.MARGEND.KIILU_LOPP){
								sonaStr.append("</kiil>");
								sonaStr.append("\n");
							}
						}
					}
				}
				valjund.append(sonaStr);
				tokenPosition++;
			}
		}
		return valjund.toString();
	}
	
	
	/**
	 *   Tagastab v&auml;ljundi, kus JSON kujul vabamorfi v&auml;ljundile on lisatud osalausepiiride m&auml;rgendused.
	 */
	public static String vormistaTulemusVabaMorfiJSONkujul(String sisendJSON, List<OsalauSona> margendusegaSonad, boolean prettyPrint) throws Exception {
		// JSON-i generaatori loomine
		StringWriter sw = new StringWriter();
		Map<String, Object> properties = new HashMap<String, Object>(1);
		if (prettyPrint){
			properties.put(JsonGenerator.PRETTY_PRINTING, prettyPrint);
		}
		JsonGeneratorFactory jgf = Json.createGeneratorFactory(properties);
		JsonGenerator jsonGenerator = jgf.createGenerator( sw );
		// vana JSON-i sisu parser
		BufferedReader inputReader = new BufferedReader( new StringReader(sisendJSON) );
		JsonParser parser = Json.createParser( inputReader );
		
		// JSON-i v6tmete rada ( k6ige pealmine on vaadeldav v6ti... )
		Stack<String> jsonKeyPath = new Stack<String>(); 
		// Viimane event
		JsonParser.Event lastEvent = null;
		// Viimasele event'ile vastav v22rtus (kui on)
		String lastString = "";
		
		// jooksva s6na tekstikuju
		String tokenText    = null;
		
		// Loome mappingu sisendteksti ja margenduste vahele
		HashMap<Integer, OsalauSona> tokenPosToMargendusMap = new HashMap<Integer, OsalauSona>();
		for (int i = 0; i < margendusegaSonad.size(); i++) {
			OsalauSona margendus = margendusegaSonad.get(i);
			if ((margendus.getMorfSona()).getTokenPosition() != -1){
				int key = (margendus.getMorfSona()).getTokenPosition();
				if (!tokenPosToMargendusMap.containsKey(key)){
					tokenPosToMargendusMap.put(key, margendus);
				} else {
					throw new Exception(" Duplicate labels for position "+String.valueOf(key)+" "+(margendus.getMorfSona()).getAlgSona()+": "+margendus+" "+(tokenPosToMargendusMap.get(i)));
				}
			} else {
				throw new Exception(" Unable to map MorfAnSona to tokenposition for "+(margendus.getMorfSona()).getAlgSona());
			}
		}
		int currentTokenPosition = 1;
		while (parser.hasNext()) {
				JsonParser.Event event = parser.next();
				switch(event) {
					case START_ARRAY:
						if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
							jsonKeyPath.push( lastString );
							jsonGenerator.writeStartArray( lastString );
						} else {
							jsonGenerator.writeStartArray();
						}
						lastEvent = event;
						lastString = "";
						break;
					case END_ARRAY:
						// ------------------------------------------------------------------
						//   Sõna-järjendi lõpp: märgime, et viimane sõna lõpetas lause
						// ------------------------------------------------------------------
						if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals("words")){
						
						}
						lastEvent = event;
						lastString = "";
						jsonKeyPath.pop();
						jsonGenerator.writeEnd();
						break;
					case START_OBJECT:
						if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
							jsonGenerator.writeStartObject( lastString );
						} else {
							jsonGenerator.writeStartObject();
						}
						// ------------------------------------------------------------------
						//   Sõna-objekti algus
						// ------------------------------------------------------------------
						if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals("words")){
						}
						// ------------------------------------------------------------------
						//   Analüüs-objekti algus
						// ------------------------------------------------------------------
						if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals("analysis")){
						}
						lastEvent = event;
						lastString = "";
						break;
					case END_OBJECT:
						// ------------------------------------------------------------------
						//   Sõna-objekti lõpp
						// ------------------------------------------------------------------
						if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals( "words")){
							if (tokenText == null){
								throw new Exception(" Unable to find text form for a word ...");  
							}
							Integer positionKey = Integer.valueOf(currentTokenPosition);
							if (tokenPosToMargendusMap.containsKey( positionKey )){
								// Trykime v2lja s6naga seotud osalausepiiride m2rgenduse
								OsalauSona margendus = tokenPosToMargendusMap.get( positionKey );
								if ( !(margendus.getKindladOLPMargendid()).isEmpty() ){
									jsonGenerator.writeStartArray("clauseAnnotation");
									for (OsalauSona.MARGEND margend : margendus.getKindladOLPMargendid()) {
										jsonGenerator.write( margend.toString() );				
									}
									jsonGenerator.writeEnd();
								}
							} else {
								throw new Exception(" Unable to find MorfAnSona associated with the word "+tokenText+" at position "+currentTokenPosition+"...");
							}
							currentTokenPosition++;
							tokenText = null;
						}
						// ------------------------------------------------------------------
						//   Analüüsi-objekti lõpp
						// ------------------------------------------------------------------
						if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals( "analysis")){
						}
						lastEvent = event;
						lastString = "";
						jsonGenerator.writeEnd();
						break;
					case VALUE_FALSE:
						if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
							jsonGenerator.write(lastString, false);
						} else {
							jsonGenerator.write(false);
						}
						lastEvent = event;
						lastString = "";
						break;
					case VALUE_NULL:
						if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
							jsonGenerator.writeNull(lastString);
						} else {
							jsonGenerator.writeNull();
						}
						lastEvent = event;
						lastString = "";
						break;
					case VALUE_TRUE:
						if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
							jsonGenerator.write(lastString, true);
						} else {
							jsonGenerator.write(true);
						}
						lastEvent = event;
						lastString = "";
						break;
					case KEY_NAME:
						lastEvent  = event;
						lastString = parser.getString();
						break;
					case VALUE_STRING:
						if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
							jsonGenerator.write(lastString, parser.getString());
						} else {
							jsonGenerator.write(parser.getString());
						}
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
					case VALUE_NUMBER:
						if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
							if (parser.isIntegralNumber()){
								jsonGenerator.write(lastString, parser.getLong());
							} else {
								jsonGenerator.write(lastString, parser.getBigDecimal());
							}
						} else {
							if (parser.isIntegralNumber()){
								jsonGenerator.write(parser.getLong());
							} else {
								jsonGenerator.write(parser.getBigDecimal());
							}
						}
						lastEvent  = event;
						lastString = "";
						break;
				}
		}
		jsonGenerator.close();
		inputReader.close();
		return sw.toString();
	}

}
