package ee.ut.soras.wrappers.mudel;

import java.util.List;
import java.util.StringTokenizer;

/**
 *  Vastab morfoloogilise analüsaatori väljundis sõna morf analüüsi ühele reale.
 * 
 * @author Siim Orasmaa
 */
public class MorfAnRida {
	
	/**
	 *  Lemma ehk algvormi tüvi;
	 */
	private String lemma;
	/**
	 *  Sõna lõpp, kusjuures mitmuse tunnus on temaga liitunud;
	 */
	private String lopp;
	/**
	 *  Sõnaliigi lühend;
	 */
	private String sonaliik;
	/**
	 *  Noomeni või verbi vormi lühend(id);
	 */
	private String vormiNimetused;
	
	
	/**
	 *   Lemma, kus teatud symbolid/tähed on normaliseeritud (viidud ühtsele kujule).
	 */
	private String normaliseeritudLemma;
	/**
	 *   Normaliseeritud lemma, millest on eemaldatud vahem2rgid. 
	 */
	private String lemmaIlmaVahemarkideta;
	
	
	/**
	 * @param analyysiRida ESTMORF-i valjundist saadud analyysirida, millest ekstraheerime
	 * 					   vastavad analyysiandmed (lemma, lopp, sonaliik jms.)
	 */
	public MorfAnRida(String analyysiRida) {
		eraldaReastElemendid(analyysiRida);
		normaliseeriLemmaNingLeiaVahemarkidetaKuju();
	}
	
	/**
	 * @param keyValuePairs Vabamorfi JSON v2ljundi morfoloogilist analyysi kirjeldavate 
	 *                      atribuutide loend, kus paarisarvulise indeksiga kohal on atribuudinimi
	 *                      (clitic, ending, form, partofspeech, root) ja paarituarvulise indeksiga
	 *                      kohal on sellele vastav atribuudiv22rtus;
	 * @throws Exception kui sisendjärjend pole paarisarvulise suurusega (võti-väärtus paaridest koosnev);
	 */
	public MorfAnRida(List<String> keyValuePairs) throws Exception {
		if ((keyValuePairs.size() % 2) != 0){
			throw new Exception(" Unexpected size of input key-value pairs - should be even-size list: "+ keyValuePairs.toString());
		}
		for (int i = 0; i < keyValuePairs.size(); i+=2) {
			String attribName  = keyValuePairs.get(i);
			String attribValue = keyValuePairs.get(i+1);
			if (attribName.equalsIgnoreCase("root")){
				this.lemma = attribValue;
			}
			if (attribName.equalsIgnoreCase("ending")){
				this.lopp = attribValue;
			}
			if (attribName.equalsIgnoreCase("form")){
				this.vormiNimetused = attribValue;
			}
			if (attribName.equalsIgnoreCase("partofspeech")){
				this.sonaliik = "_"+attribValue+"_";
			}
			// TODO: on veel kliitiku märgend, aga neid praegu ei salvesta (puudub vajadus?)
		}
		normaliseeriLemmaNingLeiaVahemarkidetaKuju();
		
	}
	
	/**
	 *  Eraldame analyysireast elemendid: lemma, lopp, sonaliik, vorminimetus;
	 * @param analyysiRida
	 */
	private void eraldaReastElemendid(String analyysiRida){
		/******* <Sona tyvi> ja <Sona lopp> ************/
		int esimeneKaldkriips = leiaEsimeseKaldkriipsuIndex(analyysiRida);
		// kui leidub sonalopu tahis
		if (analyysiRida.lastIndexOf("+") > -1){
			// lemma on algusest kuni sonalopu tahiseni
			this.lemma = analyysiRida.substring(0, analyysiRida.lastIndexOf("+"));
			this.lopp = analyysiRida.substring(analyysiRida.lastIndexOf("+") + 1,
											   esimeneKaldkriips - 1);
		} else {
			// lemma on algusest kuni tyhikuni enne esimest alamsonet "//"
			this.lemma = analyysiRida.substring(0, esimeneKaldkriips - 1);
			this.lopp = "0";
		}
		int viimaneAlaJoon = analyysiRida.lastIndexOf("_");
		int viimaneKaldkriips = analyysiRida.lastIndexOf("/"); 
		/*********** <Sonaliik> ***************/
		this.sonaliik = analyysiRida.substring(viimaneAlaJoon - 2,
											   viimaneAlaJoon + 1);
		/********* <vormiNimetused> ***********/
		if ((viimaneKaldkriips - 2) - (viimaneAlaJoon + 2) > 0){
			this.vormiNimetused = analyysiRida.substring(viimaneAlaJoon + 2,
					 									 viimaneKaldkriips - 2);
		} else {
			this.vormiNimetused = "";
		}
	}

	
	/**
	 *  Morf analysaatori analyysirida on kujul:
	 *  <blockquote>
	 *  		tüvi+lõpp // [sõnaliik] [vormi nimetus] //
	 *  </blockquote>
	 *  Leiame analyysireas vasakult esimese l6iku "[sõnaliik] [vormi nimetus]" 
	 *  ymbritseva symboli '/' indeksi.
	 * 
	 * @param analyysiRida
	 * @return
	 */
	private static int leiaEsimeseKaldkriipsuIndex(String analyysiRida){
		int i = analyysiRida.length() - 1;
		int symboleidSeni = 0;
		for (; i > -1; i--) {
			if (analyysiRida.charAt(i)=='/'){
				symboleidSeni++;
			}
			if (symboleidSeni==4){
				break;
			}
		}
		return i;
	}
	
	public String getLemma() {
		return lemma;
	}
	
	/**
	 *   Leiab antud lemma kaks täpsustavat kuju:
	 *   <ol>
	 *    <li>normaliseeritudLemma - kus on tähe õ erinevad variandid taandatud kõik
	 *        ühele variandile; - !praegu jääb välja, tarbetu</li>
	 *    <li>lemmaIlmaVahemarkideta - kus on normaliseeritudLemma'st
	 *        eemaldatud järgmised mitte-täht-sümbolid:
	 *       <ul>
	 *       <li> tyve loppu eraldavad margid '_'</li>
	 *       <li> loppu eraldavad margid '+'</li>
	 *       <li> sufiksit eraldav mark '='</li>
	 *       </ul>
	 *    </li>
	 *   </ol>
	 */
	public void normaliseeriLemmaNingLeiaVahemarkidetaKuju(){
		if (this.lemma != null){
			this.normaliseeritudLemma = new String(this.lemma);
			// 1) Normaliseerime: viime õ-d ühtsele kujule - praegu jääb välja
			//this.normaliseeritudLemma = (this.normaliseeritudLemma).
			//	replaceAll("(\u014D|\u00F4|\u01D2)", "\u00F5"); // väike õ  
			//this.normaliseeritudLemma = (this.normaliseeritudLemma).
			//	replaceAll("(\u00D4|\u014C|\u01D1)", "\u00D5"); // suur Õ
			
			// 2) Leiame kuju, kus on lemma ilma vahemärkideta
			StringTokenizer st = new StringTokenizer (this.normaliseeritudLemma, "=+_");
			StringBuilder sb = new StringBuilder();
			while (st.hasMoreElements()) {
				sb.append( (String) st.nextElement() );
			}
			this.lemmaIlmaVahemarkideta = sb.toString();			
		}
	}
	
	/**
	 *  Tagastab lemma, milles on l&auml;bi viidud normaliseerimine ning on 
	 *  valja jaetud teatud morf analyysile omased märgid. Vt täpsemalt 
	 *  meetodist {@link #normaliseeriLemmaNingLeiaVahemarkidetaKuju()};
	 */
	public String getLemmaIlmaVahemarkideta(){
		return this.lemmaIlmaVahemarkideta;
	}
	
	/**
	 *   M&auml;pping sonaliigi tahisest selle kirjeldusse. Ainult
	 *  katsetamise ja uurimise eesmargil. 
	 *  <p>
	 *  Vt ka http://www.filosoft.ee/html_morf_et/morfoutinfo.html
	 */
	private static final String[][] SONALIIK = {
		{"_A_",
			"omadussõna - algvõrre, nii käänduvad kui käändumatud"},
		{"_C_",
			"omadussõna - keskvõrre (adjektiiv - komparatiiv)"},
		{"_D_",
			"määrsõna (adverb), nt kõrvuti"},
		{"_G_",
			"genitiivatribuut (käändumatu omadussõna)"},
		{"_H_",
			"pärisnimi"},
		{"_I_",
			"hüüdsõna (interjektsioon), nt tere"},
		{"_J_",
			"sidesõna (konjunktsioon)"},
		{"_K_",
			"kaassõna (pre/postpositsioon), nt kaudu"},
		{"_N_",
			"põhiarvsõna (kardinaalnumeraal), nt kaks"},
		{"_O_",
			"järgarvsõna (ordinaalnumeraal), nt teine"},
		{"_P_",
			"asesõna (pronoomen), nt see"},
		{"_S_",
			"nimisõna (substantiiv), nt asi"},
		{"_U_",
			"omadussõna - ülivõrre (adjektiiv - superlatiiv), nt pikim"},
		{"_V_",
			"tegusõna (verb), nt lugema"},
		{"_X_",
			"verbi juurde kuuluv sõna, eraldi sõnaliigi tähistus puudub, nt plehku"},
		{"_Y_",
			"lühend, nt USA"},
		{"_Z_",
			"lausemärk"}
	};
	
	public String getSonaLiikPikalt(){
		String liikPikalt = "";
		for (String[] sonaliik : SONALIIK) {
			if (sonaliik[0].equals(this.sonaliik)){
				liikPikalt = sonaliik[1];
			}
		}
 		return liikPikalt;
	}

	public String getLopp() {
		return lopp;
	}

	public String getSonaliik() {
		return sonaliik;
	}
	
	/**
	 *  Kas tegemist on teguso~naga vo~i mitte?
	 */
	public boolean isVerb(){
		return (this.sonaliik).equals("_V_");
	}

	public String getVormiNimetused() {
		return vormiNimetused;
	}

	/**
	 *   M&auml;pping noomenikategooria lyhendist selle kirjeldusse. Ainult
	 *  katsetamise ja uurimise eesmargil. 
	 *  <p>
	 *  Vt ka http://www.filosoft.ee/html_morf_et/morfoutinfo.html
	 */
	private static final String[][] NOMKAT_VORMINIMETUS = {
		{"ab",	 "abessiiv - ilmayltev"},
		{"abl",	 "ablatiiv - alaltytlev"},
		{"ad",	 "adessiiv - alalytlev"},
		{"adt",	 "aditiiv - suunduv (lyhike sisseytlev)"},
		{"all",	 "allatiiv - alaleytlev"},
		{"el",	 "elatiiv - seestytlev"},
		{"es",	 "essiiv - olev"},
		{"g",	 "genitiiv - omastav"},
		{"ill",	 "illatiiv - sisseytlev"},
		{"in",	 "inessiiv - seesytlev"},
		{"kom",	 "komitatiiv - kaasaytlev"},
		{"n",	 "nominatiiv - nimetav"},
		{"p",	 "partitiiv - osastav"},
		{"pl",	 "pluural - mitmus"},
		{"sg",	 "singulaar - ainusus"},
		{"ter",	 "terminatiiv - rajav"},
		{"tr",	 "translatiiv - saav"}
	};
	
	
	/**
	 *  Tagastab vorminimetuse pikal/kirjeldaval kujul. Kui nimetusi on mitu,
	 * asetatakse iga nimetus eraldi reale ning rea ette konkateneeritakse sone
	 * <code>taane</code>. NB! Tagastatava sone lopus on alati ka reavahetus.
	 * 
	 * @param taane sone, mis kleebitakse iga uue rea algusesse valjundis
	 */
	public String getVormiNimetusedPikalt(String taane){
		StringBuilder vormiNim = new StringBuilder();
		if (this.vormiNimetused.length() > 0){
			// jagame vorminimetuste sone komade kohalt tykkideks
			StringTokenizer tokenizer = new StringTokenizer(this.vormiNimetused, ",");
			while (tokenizer.hasMoreElements()) {
				String sone = (String) tokenizer.nextElement();
				if (isVerb()){
					vormiNim.append(taane);
					vormiNim.append(sone);
					vormiNim.append("\n");
				} else {
					StringTokenizer tokenizer2 = new StringTokenizer(sone, " ");
					while (tokenizer2.hasMoreElements()) {
						String sone2 = (String) tokenizer2.nextElement();
						for (String [] nomKatVormiNim : NOMKAT_VORMINIMETUS) {
							if (nomKatVormiNim[0].equals(sone2)){
								vormiNim.append(taane);
								vormiNim.append(nomKatVormiNim[1]);
								vormiNim.append(" (");
								vormiNim.append(nomKatVormiNim[0]);
								vormiNim.append(")");
								vormiNim.append("\n");
							}
						}
					}
				}
			}
		}
		return vormiNim.toString();
	};
	

	/**
	 *   Leiab, kas antud analyysirida sisaldab parameetrina antud morfoloogilist tunnust (sonaliik voi
	 *  k&auml;&auml;ne/p&ouml;&ouml;re jms).
	 *  <p>
	 *  NB! Kui antud analyys sisaldab mitut vormitunnust, tagastatakse t&otilde;ene kui vahemalt yks neist
	 *  sobitub.
	 *  <p>
	 *  NB! Kui vormitunnusele eelneb m&auml;rk ^, on tegemist eitusega, st kontrollitakse hoopis vormitunnuse
	 *  mitte-esinemist ning tagastatakse true, kui seda ei esinenud.
	 *  
	 * @param morfTunnus yksik morfoloogiline tunnus, sellisel kujul, nagu see on toodud morf analysaatori
	 *                   v&auml;ljundis;
	 */
	public boolean leiaKasMorfTunnusEsineb(String morfTunnus){
		if (morfTunnus.startsWith("^")){
			return !leiaKasMorfTunnusEsineb( morfTunnus.replaceAll("\\^","") );
		}
		// Leiame, kas tunnus kirjeldab sonaliiki voi on tegemist vormitunnusega
		if (morfTunnus.startsWith("_")){
			if (this.sonaliik != null){
				return ((this.sonaliik).equals(morfTunnus));
			} else {
				return false;
			}
		} else {
			if (this.vormiNimetused != null){
				StringTokenizer tokenizer = new StringTokenizer(this.vormiNimetused, ",");
				while (tokenizer.hasMoreElements()) {
					String vormiTunnusedKoond = (String) tokenizer.nextElement();
					String vormiTunnused [] = vormiTunnusedKoond.split("(\\s+)");
					for (String tunnus : vormiTunnused) {
						if (tunnus.equals(morfTunnus)){
							return true;
						}
					}
				}	
			}
		}
		return false;
	}
	
	public String toString(){
		StringBuilder str = new StringBuilder();
		str.append(this.lemma);
		str.append("+");
		str.append(this.lopp);
		str.append(" ");
		str.append(this.sonaliik);
		str.append(" ");
		str.append(this.vormiNimetused);
		return str.toString();
	}
	
}
