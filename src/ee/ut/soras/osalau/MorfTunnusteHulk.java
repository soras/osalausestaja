package ee.ut.soras.osalau;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import ee.ut.soras.wrappers.mudel.MorfAnRida;
import ee.ut.soras.wrappers.mudel.MorfAnSona;


/**
 *   Morfoloogiliste analyyside hulk, mis vastab yhe s6na morfoloogilistele analyysidele
 *  ning mida on v6imalik erinevate tunnuste j2rgi (nt s6naliigi, lemma jms) j2rgi
 *  kitsendada ja filtreerida;
 *   
 *  @author Siim Orasmaa
 */
public class MorfTunnusteHulk {

	List<MorfAnRida> tunnused = null;

	/**
	 *   Eelkompileeritud tunnuste otsimise regulaaravaldised: selleks, et avaldisi
	 *   ei peaks iga s6na sobitamisel alati uuesti kompileerima ...
	 */
	static private HashMap<String, Pattern> preCompiledPatterns = 
				new HashMap<String, Pattern>();
			
	/**
	 *   Konstrueerib uue, tyhja morfoloogiliste tunnuste hulga;
	 */
	public MorfTunnusteHulk() {
		this.tunnused = new ArrayList<MorfAnRida>(5);
	}
	
	/**
	 * Konstrueerib etteantud <code>MorfAnSona</code>'le vastava morfoloogiliste tunnuste hulga;
	 */
	public MorfTunnusteHulk(MorfAnSona sona) {
		this.tunnused = new ArrayList<MorfAnRida>(5);
		if (sona.kasLeidusAnalyys()){
			(this.tunnused).addAll( sona.getAnalyysiTulemused() );
		}
	}

	/**
	 * Konstrueerib etteantud <code>OsalauSona</code>'le vastava morfoloogiliste tunnuste hulga;
	 */
	public MorfTunnusteHulk(OsalauSona sona) {
		this.tunnused = new ArrayList<MorfAnRida>(5);
		if ((sona.getMorfSona()).kasLeidusAnalyys()){
			(this.tunnused).addAll( (sona.getMorfSona()).getAnalyysiTulemused() );
		}
	}
	
	/**
	 *  Filtreerib seda tunnuste hulka pos-tag'i j2rgi ja tagastab
	 *  uue tunnuste hulga, kus on ainult antud pos tag'ile vastavad 
	 *  morfoloogilised tunnused; 
	 */
	public MorfTunnusteHulk filterByPOS(String pos){
		MorfTunnusteHulk uusHulk = new MorfTunnusteHulk();
		for (MorfAnRida analyys : this.tunnused) {
			if ( analyys.getSonaliik() != null && (analyys.getSonaliik()).equalsIgnoreCase(pos) ){
				(uusHulk.tunnused).add(analyys);
			}
		}
		return uusHulk;
	}
	
	/**
	 *  Filtreerib seda tunnuste hulka lemma j2rgi ja tagastab
	 *  uue tunnuste hulga, kus on ainult lemmat sisaldavad 
	 *  morfoloogilised analyysid; 
	 */
	public MorfTunnusteHulk filterByLemma(String lemma){
		MorfTunnusteHulk uusHulk = new MorfTunnusteHulk();
		for (MorfAnRida analyys : this.tunnused) {
			if ( analyys.getLemma() != null && (analyys.getLemma()).equals(lemma) ){
				(uusHulk.tunnused).add(analyys);
			}
		}
		return uusHulk;
	}
	
	/**
	 *  Filtreerib seda tunnuste hulka lemmasid kirjeldava regulaaravaldise 
	 *  j2rgi ja tagastab uue tunnuste hulga, kus on ainult kirjeldatud
	 *  lemmasid sisaldavad morfoloogilised analyysid; 
	 */
	public MorfTunnusteHulk filterByLemmaRegExp(String lemmaRegExpStr){
		MorfTunnusteHulk uusHulk = new MorfTunnusteHulk();
		Pattern lemmaRegExp = getPreCompiledPattern(lemmaRegExpStr);
		for (MorfAnRida analyys : this.tunnused) {
			if ( analyys.getLemma() != null && (lemmaRegExp.matcher(analyys.getLemma())).matches() ){
				(uusHulk.tunnused).add(analyys);
			}
		}
		return uusHulk;
	}
	
	/**
	 *  Filtreerib seda tunnuste hulka vorminimetuste regulaaravaldise j2rgi ja tagastab
	 *  uue tunnuste hulga, kus on ainult regulaaravaldisele vastavad morfoloogilised 
	 *  analyysid; 
	 */
	public MorfTunnusteHulk filterByFormNames(String formNamesRegExpStr){
		MorfTunnusteHulk uusHulk = new MorfTunnusteHulk();
		Pattern formNamesRegExp = getPreCompiledPattern(formNamesRegExpStr);
		for (MorfAnRida analyys : this.tunnused) {
			if ( analyys.getVormiNimetused() != null && (formNamesRegExp.matcher(analyys.getVormiNimetused())).matches() ){
				(uusHulk.tunnused).add(analyys);
			}
		}
		return uusHulk;
	}
	
	/**
	 *  Filtreerib seda tunnuste hulka k22nde/p88rdel6ppude regulaaravaldise j2rgi ja tagastab
	 *  uue tunnuste hulga, kus on ainult regulaaravaldisele vastavad morfoloogilised 
	 *  analyysid; 
	 */
	public MorfTunnusteHulk filterByEnding(String endingRegExpStr){
		MorfTunnusteHulk uusHulk = new MorfTunnusteHulk();
		Pattern endingRegExp = getPreCompiledPattern(endingRegExpStr);
		for (MorfAnRida analyys : this.tunnused) {
			if ( analyys.getLopp() != null && (endingRegExp.matcher(analyys.getLopp())).matches() ){
				(uusHulk.tunnused).add(analyys);
			}
		}
		return uusHulk;
	}

	/**
	 *  Uuendab eelkompileeritud regulaaravaldiste hulka - kui antud avaldist ei leidu
	 *  hulgas, kompileerib selle ja lisab hulka - ning tagastab hulgast juba olemasoleva /
	 *  kompileeritud avaldise.
	 */
	private Pattern getPreCompiledPattern(String patternString){
		if (!preCompiledPatterns.containsKey(patternString)){
			Pattern newPattern = Pattern.compile(patternString);
			preCompiledPatterns.put(patternString, newPattern);
		}
		return preCompiledPatterns.get(patternString);
	}
	
	/**
	 *  Kas selles tunnuste hulgas on veel tunnuseid j2rel?
	 */
	public boolean isEmpty(){
		return (this.tunnused == null || (this.tunnused).isEmpty());
	}
	
	/**
	 *    V6rdleb seda tunnuste hulka etteantud tunnuste hulgaga ning leiab, kas (morfoloogilised) 
	 *   vorminimetused m6lema tunnuste hulga esimese analyysi kyljes yhtivad. Kui tunnuste
	 *   hulgad on v6rreldavad ja vastavad vorminimetused yhtivad, tagastab <tt>true</tt>, k6igil muudel
	 *   juhtudel tagastab <tt>false</tt>.
	 */
	public boolean formNamesFromFirstAnalysesMatch(MorfTunnusteHulk otherMorphFeatures){
		if (!this.isEmpty() && !otherMorphFeatures.isEmpty()){
			MorfAnRida morfAnRida1 = (this.tunnused).get(0);
			MorfAnRida morfAnRida2 = (otherMorphFeatures.tunnused).get(0);
			String formNames1 = morfAnRida1.getVormiNimetused();
			String formNames2 = morfAnRida2.getVormiNimetused();
			return (formNames1 != null && formNames2 != null && formNames1.equals(formNames2));
		}
		return false;
	}
	
}
