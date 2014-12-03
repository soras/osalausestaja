package ee.ut.soras.wrappers;

import java.util.regex.Pattern;


/**
 *  Valik eeldefineeritud mustreid ja s&otilde;net&ouml;&ouml;tlusutiliite
 *  morfoloogiliselt analyysitud sisendi t&ouml;&ouml;tlemiseks.
 * 
 * @author Siim Orasmaa
 */
public class TextUtilsForMorph {

	/**
	 *   Muster punktatsiooni eemaldamiseks s6na algusest.
	 *  <p>
	 *  Lisaks <tt>\p{Punct}</tt> klassi punktatsioonile eemaldame ka: 
	 *    <i>guillemet</i>-tyypi jutumargi (kasutusel sagedasti Postimehes)
	 */	
	private final static Pattern removePunctationFromBeginning =
		Pattern.compile("^(\\p{Punct}|\u00AB)+");
		
	/**
	 *   Muster punktatsiooni eemaldamiseks s6na l6pust.
	 *  <p> 
	 *  Lisaks <tt>\p{Punct}</tt> klassi punktatsioonile eemaldame ka: 
	 *    <i>guillemet</i>-tyypi jutumargi (kasutusel sagedasti Postimehes)
	 */
	private final static Pattern removePunctationFromEnding =
		Pattern.compile("(\\p{Punct}|\u00BB)+$");
	
	public final static Pattern beginningOfQuote = Pattern.compile("^(\u00AB|\")+");
	public final static Pattern endOfQuote		 = Pattern.compile("(\u00BB|\")+$");

	/**
	 *  <i>Trim</i> - eemaldame koik sone alguses ja lopus olevad tyhikud, 
	 * tabulaatorid jm <i>whitespace</i> symbolid (kuni esimese mittetyhikuni 
	 * algusest ning alates viimasest mittetyhikust lopus). 
	 * 
	 * @param source
	 * @return
	 */	
	public static String trim(String sone){
		return (ltrim(sone)).replaceAll("\\s+$", "");
	}
	
	/**
	 *  <i>Left-trim</i> - eemaldame koik sone alguses olevad tyhikud, 
	 * tabulaatorid jm <i>whitespace</i> symbolid (kuni esimese mittetyhikuni). 
	 * 
	 * @param source
	 * @return
	 */
	public static String ltrim(String source) {
		return source.replaceAll("^\\s+", "");
	}
	
	/**
	 *  Eemaldame sone <code>source</code> algusest ja lopust punktatsiooni m&auml;rgid (java 
	 *  regulaaravaldise klassi <tt>\p{Punct}</tt> symbolid).
	 *  <p>
	 */
	public static String trimSurroundingPunctation(String source){
		return (removePunctationFromEnding.matcher(
			   (removePunctationFromBeginning.matcher(source)).replaceAll("") )).replaceAll("");
	}

	/**
	 *   <p>
	 *   Normaliseerib (taandab yhele kujule) teatud erisymbolid s6nes:
	 *   <li>kriipsud (nt -, --, —)</li>
	 *   <li>HTML olemid &amp;lt; &amp;gt; &amp;amp &amp;pos; &amp;quot;</li>
	 *   <li>õ erikujud (ô, ō, ǒ) - !praegu jääb välja, tarbetu</li>
	 *   </p>
	 *   <br>
	 *   Infot symbolite ja nendele vastavate koodide kohta:<br>
	 *   http://www.fileformat.info/info/unicode/<br>
	 *   http://www.w3schools.com/tags/ref_entities.asp
	 *   <br>
	 *   <br>
	 */
	public static String normalizeSpecialSymbols(String input){
		// 1. Asendame k6ik kriipsude erikujud yhe kindla kriipsuga
		input = input.replaceAll("-{2,}", "-");
		input = input.replaceAll("(\u2212|\uFF0D|\u02D7|\uFE63|\u002D)", "-");
		input = input.replaceAll("(\u2010|\u2011|\u2012|\u2013|\u2014|\u2015)", "-");
		// 2. Asendame HTML olemid nendele vastavate märkidega
		input = input.replaceAll("&(quot|#34);", "\"");
		input = input.replaceAll("&(apos|#39);", "'");
		input = input.replaceAll("&(amp|#38);", "&");
		input = input.replaceAll("&(lt|#60);", "<");
		input = input.replaceAll("&(gt|#62);", ">");
		// 3. TODO: vajadusel lisada siia ka HTML/XML vms tag'ide v2ljaviskamine
		return input;
	}

}
