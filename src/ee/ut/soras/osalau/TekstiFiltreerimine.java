package ee.ut.soras.osalau;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ee.ut.soras.osalau.OsalauSona.MARGEND;

/**
 *   Abimeetodid teksti filtreerimiseks ja tykeldamiseks.
 * 
 *   @author Siim Orasmaa
 */
public class TekstiFiltreerimine {

	/**
	 *   Jagab sisendteksti sonad lauseteks, vastavalt morfoloogilises analyysis
	 *  margitud lausepiiridele, ning tagastab lausete listi.
	 */
	public static List<List<OsalauSona>> eraldaLaused(List<OsalauSona> sonad){
		List<List<OsalauSona>> laused = new ArrayList<List<OsalauSona>>();
		int lauseAlgus = 0;
		for (int i = 0; i < sonad.size(); i++) {
			if (((sonad.get(i)).getMorfSona()).onLauseLopp()){
				laused.add( sonad.subList( lauseAlgus, i + 1 ) );
				lauseAlgus = i + 1;
			}
		}
		return laused;
	}
	
	/**
	 *   Jagab etteantud lausete j2rjendis iga lause omakorda juppideks - selliselt, et
	 *   laused on poolitatud kindlate osalausepiiride kohalt; Seega on tagastatavas
	 *   j2rjendis tekst poolitatud nii lausepiiride kui ka m2rgitud kindlate osalausepiiride
	 *   kohalt. Tagastab jagamise tulemust sisaldava jarjendi.
	 *  <br><br>
	 */
	public static List<List<OsalauSona>> jagaKindlatePiirideKohalt( List<List<OsalauSona>> laused ){
		List<List<OsalauSona>> uusTykeldus = new ArrayList<List<OsalauSona>>();
		for (List<OsalauSona> lause : laused) {
			List<OsalauSona> osalause = new ArrayList<OsalauSona>();
			for (int i = 0; i < lause.size(); i++) {
				OsalauSona sona = lause.get(i);
				if (sona.omabMargendit(MARGEND.KIILU_ALGUS)){
					// TODO: praegu pole p2ris kindel, kas poolitama peaks ka kiilu kohalt
					eemaldaOLPiiridAlgusestLopust( osalause );
					uusTykeldus.add( osalause );
					osalause = new ArrayList<OsalauSona>();
				}
				osalause.add( sona );
				if (sona.omabMargendit(MARGEND.KINDEL_PIIR) || 
						sona.omabMargendit(MARGEND.KIILU_LOPP)){
					// TODO: praegu pole p2ris kindel, kas poolitama peaks ka kiilu kohalt
					eemaldaOLPiiridAlgusestLopust( osalause );
					uusTykeldus.add( osalause );
					osalause = new ArrayList<OsalauSona>();
				}
			}
			if (osalause.size() > 0){
				eemaldaOLPiiridAlgusestLopust( osalause );
				uusTykeldus.add( osalause );
			}
		}
		return uusTykeldus;
	}
	
	/**
	 *  Eemaldab etteantud osalause (v6i lause) algusest ja l6pust oletuslikud piirid
	 *  (maksimaalselt kolm j2rjestikkust piiri algusest ja l6pust);
	 *  <br><br>
	 *  NB! V6imalik, et tegemist on ysna tehnilise heuristikuga, mis leiab k6ige rohkem
	 *  rakendust just koondkorpuses, kus punktuatsioon on s6nadest lahku t6stetud ning 
	 *  seet6ttu tuleb eemaldada yleliigsed m2rgid k6rvutipaikneva punktuatsiooni
	 *  kohalt.
	 */
	public static void eemaldaOLPiiridAlgusestLopust(List<OsalauSona> osalause){
		if (osalause.isEmpty()){
			return;
		}
		SonaMall olpiir = new SonaMall( MARGEND.OLETATAV_PIIR );
		SonaMall komaSonaL6pus = new SonaMall( Pattern.compile("^.*\\p{Alpha}.*,$") );
		try {
			int subListSize = Math.min( osalause.size(), 3 );
			List<OsalauSona> prefixList = osalause.subList(0, subListSize);
			List<OsalauSona> suffixList = osalause.subList(osalause.size()-subListSize, osalause.size());
			for (OsalauSona sona : prefixList) {
				if (olpiir.vastabMallileAND(sona)){
					//   Kontrollime, et tegemist poleks s6naga, mille l6ppu on kleepunud koma - kui on, 
					//  siis j2tame piiri siiski alles ...
					if (!komaSonaL6pus.vastabMallileAND(sona)){
						sona.eemaldaMargend(MARGEND.OLETATAV_PIIR);						
					} else {
						break;
					}
				} else {
					break;
				}
			}
			for (int i = suffixList.size()-1; i > -1; i--) {
				if (olpiir.vastabMallileAND( suffixList.get(i) )){
					(suffixList.get(i)).eemaldaMargend(MARGEND.OLETATAV_PIIR);
				} else {
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 *  Kontrollib, kas antud lauses eelneb positsioonile <code>i</code> sonamallile  
	 *  <code>otsitav</code> vastav sona - selliselt, et otsitava sona ja positsiooni 
	 *  <code>i</code> vahele ei j22 yhtegi s6na, mis vastaksid sonamallile
	 *   <code>keelatud</code>. 
	 *   <br>
	 *   Kui sonamallis <code>keelatud</code> on mitu tunnust, ei tohi vahel olev s6na 
	 *   vastata yhelegi neist; kui sonamallis <code>otsitav</code> on mitu tunnust,
	 *   peab otsitav s6na vastama k6igile (AND-tingimus);
	 *  <br><br>
	 *  Kui leidub tingimustele vastav s6na, tagastab selle indeksi lauses, vastasel juhul 
	 *  (otsitavat s6na ei leitud v6i vahel oli keelatud mallile vastav s6na) tagastab -1;    
	 */
	public static int eelnebMargendigaSona(List<OsalauSona> lause, int i, SonaMall otsitav, SonaMall keelatud) throws Exception {
		int j = i - 1;
		while (j >= 0){
			OsalauSona eelnevSona = lause.get( j );
			// Kontrollime, et s6na poleks sellisel kujul, nagu me ei taha
			if (keelatud != null && keelatud.vastabMallileOR(eelnevSona)){
				return -1;
			}
			// Kui s6na on sellisel kujul, nagu me tahame, tagastame s6na indeksi
			if (otsitav.vastabMallileAND(eelnevSona)){
				return j;
			}
			j--;
		}
		return -1;
	}
	
	/**
	 *  Kontrollib, kas antud lauses eelneb positsioonile <code>i</code> sonamallile  
	 *  <code>otsitav</code> vastav sona - selliselt, et otsitava sona ja positsiooni 
	 *  <code>i</code> vahele ei j22 yhtegi s6na, mis vastaksid sonamallile
	 *   <code>keelatud</code>. 
	 *   <br>
	 *   Kui sonamallis <code>keelatud</code> on mitu tunnust, ei tohi vahel olev s6na 
	 *   vastata yhelegi neist; kui sonamallis <code>otsitav</code> on mitu tunnust,
	 *   peab otsitav s6na vastama v2hemalt yhele (OR-tingimus);
	 *  <br><br>
	 *  Kui leidub tingimustele vastav s6na, tagastab selle indeksi lauses, vastasel juhul 
	 *  (otsitavat s6na ei leitud v6i vahel oli keelatud mallile vastav s6na) tagastab -1;    
	 */
	public static int eelnebMargendigaSonaOR(List<OsalauSona> lause, int i, SonaMall otsitav, SonaMall keelatud) throws Exception {
		int j = i - 1;
		while (j >= 0){
			OsalauSona eelnevSona = lause.get( j );
			// Kontrollime, et s6na poleks sellisel kujul, nagu me ei taha
			if (keelatud != null && keelatud.vastabMallileOR(eelnevSona)){
				return -1;
			}
			// Kui s6na on sellisel kujul, nagu me tahame, tagastame s6na indeksi
			if (otsitav.vastabMallileOR(eelnevSona)){
				return j;
			}
			j--;
		}
		return -1;
	}
	
	/**
	 *  Kontrollib, kas antud lauses jargneb positsioonile <code>i</code> sonamallile  
	 *  <code>otsitav</code> vastav sona - selliselt, et otsitava sona ja positsiooni 
	 *  <code>i</code> vahele ei j22 yhtegi s6na, mis vastaksid sonamallile
	 *   <code>keelatud</code>.
	 *   <br>
	 *   Kui sonamallis <code>keelatud</code> on mitu tunnust, ei tohi vahel olev s6na 
	 *   vastata yhelegi neist; kui sonamallis <code>otsitav</code> on mitu tunnust,
	 *   peab otsitav s6na vastama k6igile (AND-tingimus);
	 *  <br><br>
	 *  Kui leidub tingimustele vastav s6na, tagastab selle indeksi lauses, vastasel juhul 
	 *  (otsitavat s6na ei leitud v6i vahel oli keelatud mallile vastav s6na) tagastab -1;    
	 */
	public static int jargnebMargendigaSona(List<OsalauSona> lause, int i, SonaMall otsitav, SonaMall keelatud) throws Exception {
		int j = i + 1;
		while (j < lause.size()){
			OsalauSona jargnevSona = lause.get( j );
			// Kontrollime, et s6na poleks sellisel kujul, nagu me ei taha
			if (keelatud != null && keelatud.vastabMallileOR(jargnevSona)){
				return -1;
			}
			// Kui s6na on sellisel kujul, nagu me tahame, tagastame s6na indeksi
			if (otsitav.vastabMallileAND(jargnevSona)){
				return j;
			}
			j++;
		}
		return -1;
	}

	/**
	 *  Kontrollib, kas antud lauses jargneb positsioonile <code>i</code> sonamallile  
	 *  <code>otsitav</code> vastav sona - selliselt, et otsitava sona ja positsiooni 
	 *  <code>i</code> vahele ei j22 yhtegi s6na, mis vastaksid sonamallile
	 *   <code>keelatud</code>.
	 *   <br>
	 *   Kui sonamallis <code>keelatud</code> on mitu tunnust, ei tohi vahel olev s6na 
	 *   vastata yhelegi neist; kui sonamallis <code>otsitav</code> on mitu tunnust,
	 *   peab otsitav s6na vastama v2hemalt yhele (OR-tingimus);
	 *  <br><br>
	 *  Kui leidub tingimustele vastav s6na, tagastab selle indeksi lauses, vastasel juhul 
	 *  (otsitavat s6na ei leitud v6i vahel oli keelatud mallile vastav s6na) tagastab -1;    
	 */
	public static int jargnebMargendigaSonaOR(List<OsalauSona> lause, int i, SonaMall otsitav, SonaMall keelatud) throws Exception {
		int j = i + 1;
		while (j < lause.size()){
			OsalauSona jargnevSona = lause.get( j );
			// Kontrollime, et s6na poleks sellisel kujul, nagu me ei taha
			if (keelatud != null && keelatud.vastabMallileOR(jargnevSona)){
				return -1;
			}
			// Kui s6na on sellisel kujul, nagu me tahame, tagastame s6na indeksi
			if (otsitav.vastabMallileOR(jargnevSona)){
				return j;
			}
			j++;
		}
		return -1;
	}

	/**
	 *   Debug meetod osalausestaja m2rgenduse kuvamiseks.  
	 */
	public static String debugMargendustegaLause(List<OsalauSona> sonad, boolean kuvaAinultKindlad){
		StringBuilder sb = new StringBuilder();
		for (OsalauSona osalauSona : sonad) {
			sb.append( (osalauSona.getMorfSona()).getAlgSona() );
			if (kuvaAinultKindlad){
				if (!(osalauSona.getKindladOLPMargendid()).isEmpty()){
					for (OsalauSona.MARGEND m : osalauSona.getKindladOLPMargendid()) {
						sb.append( m );					
					}
				}			
			} else {
				if ((osalauSona.getMargendid()) != null){
					for (OsalauSona.MARGEND m : osalauSona.getMargendid()) {
						sb.append( m );
					}
				}
			}
			sb.append( " " );
		}
		return sb.toString();
	}
	
}
