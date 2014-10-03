package ee.ut.soras.wrappers.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ee.ut.soras.wrappers.TextUtilsForMorph;
import ee.ut.soras.wrappers.mudel.MorfAnSona;

/**
 *  T3-OLP formaadis (t3mesta morf analyys + yhestamine + lausepiirid + osalausepiirid) sisendist morfoloogilise analyysi 
 *  v2ljalugemine ning paigutamine MorfAnSona andmeobjektidesse; 
 * 
 * @author Siim Orasmaa
 */
public class T3OLPReader {
	
	/**
	 *  Eraldab T3-OLP formaadile vastavast sisendvoost morf analyysi tulemused ning paigutame andmemudelisse (MorfAnSona-de
	 *  jarjendisse). 
	 *  Eeldab, et morf analyys on kujul:
	 *  <pre>
	 *    &lt;s&gt;
	 *    Mees    mees+0 //_S_ sg n, //    mesi+s //_S_ sg in, //
	 *    peeti    peet+0 //_S_ adt, sg p, //    pida+ti //_V_ ti, //
	 *    ...
	 *    &lt;kindel_piir/&gt;
	 *    ...
	 *    &lt;/s&gt;
	 *  </pre>
	 * 
	 * @param input sisendvoog, milles on v&auml;liselt protsessilt morf analyysi tulemused
	 * @return nimestik eraldatud morf analyysi tulemustest
	 * @throws IOException kui sisendvoost lugemisel peaks ilmnema mingi t&otilde;rge
	 */
	public static List<MorfAnSona> parseT3OLPtext( BufferedReader input ) throws Exception {
		List<MorfAnSona> tulemused = new ArrayList<MorfAnSona>();
		String rida;
		String        sona       = null;
		StringBuilder ignorePart = null;
		MorfAnSona eelmineSona   = null;
		boolean inSideSentence   = false; // lause sees
		boolean inSideCleft      = false; // kiillause sees
		int tokenPosition        = 1; // token'i positsioon sisendis
		while ((rida = input.readLine()) != null){
			// ---------------------------------------------------------------------------------------
			//  Jätame vahele ignoreeritava osa
			// ---------------------------------------------------------------------------------------
			if (rida.length() > 0 && rida.equals("<ignoreeri>")){
				ignorePart = new StringBuilder();
				tokenPosition++;
				while ((rida = input.readLine()) != null){
					tokenPosition++;
					if (rida.length() > 0 && rida.equals("</ignoreeri>")){
						break;
					}
					ignorePart.append( rida );
				}
				continue;
			}
			if (rida.length() > 0){
				// ---------------------------------------------------------------------------------------
				//  Lause algus ja l6pp
				// ---------------------------------------------------------------------------------------
				if (rida.equals("<s>")){
					inSideSentence = true;
				} else if (rida.equals("</s>")){
					inSideSentence = false;
					// Lausel6pp
					if (eelmineSona != null){
						eelmineSona.setOnLauseLopp(true);
					}
				} else if (rida.equals("<kiil>")){
					inSideCleft = true;
				} else if (rida.equals("</kiil>")){
					inSideCleft = false;					
				} else if (rida.equals("<kindel_piir/>")){
					// Osalause piir
					if (eelmineSona != null){
						eelmineSona.setOlpOnKindelPiir(true);
					}
				} else if (inSideSentence){
					// ---------------------------------------------------------------------------------------
					//  Eeldame, et morf analyys on kujul:
					//
					//     Mees    mees+0 //_S_ sg n, //    mesi+s //_S_ sg in, //
					//     peeti    peet+0 //_S_ adt, sg p, //    pida+ti //_V_ ti, //
					//
					// ---------------------------------------------------------------------------------------
					// Analyyside vahel on eraldajaks t2pselt 4 tyhikut
					String [] parts = rida.split("\\s{4}");
					if (parts.length < 2){
						throw new IOException(" Unexpected t3-olp format: '"+rida+"'");
					} else {
						sona = parts[0];
						MorfAnSona jooksevSona = new MorfAnSona( sona );
						jooksevSona.setTokenPosition(tokenPosition);
						for (int i = 1; i < parts.length; i++) {
							jooksevSona.lisaAnalyysiRida( TextUtilsForMorph.ltrim( parts[i] ) );
						}
						tulemused.add(jooksevSona);
						eelmineSona = tulemused.get(tulemused.size()-1);
						sona = null;
					}
					//if (logi != null) { logi.println(rida); }
				}
				tokenPosition++;
			}
	    }
		return tulemused;
	}

}
