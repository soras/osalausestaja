package ee.ut.soras.osalau;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import ee.ut.soras.osalau.OsalauSona.MARGEND;
import ee.ut.soras.wrappers.impl.VabaMorfJSONReader;
import ee.ut.soras.wrappers.mudel.MorfAnSona;

/**
 *   Eesti keele osalausestaja Java implementatsioon. Programmi algoritm ja esialgne 
 *   implementatsioon (UNIX-i skriptide kujul) on loodud Heiki-Jaan Kaalepi poolt.   
 *   <br>
 *   Programmi toopohimetete lingvistilist poolt on kirjeldatud artiklites:
 *   <ul>
 *   <li> 
 *     Kaalep, Heiki-Jaan; Muischnek, Kadri (2012). Osalausete tuvastamine eestikeelses tekstis kui 
 *     iseseisev ülesanne. Helle Metslang, Margit Langemets, Maria-Maren Sepper (Toim.). Eesti 
 *     Rakenduslingvistika Ühingu aastaraamat (55 - 68). Tallinn: Eesti Rakenduslingvistika Ühing;
 *   </li>
 *   <li>
 *     Kaalep, Heiki-Jaan; Muischnek, Kadri (2012). Robust clause boundary identification for corpus 
 *     annotation. Nicoletta Calzolari, Khalid Choukri, Thierry Declerck, Mehmet Uğur Doğan, Bente 
 *     Maegaard, Joseph Mar (Toim.). Proceedings of the Eight International Conference on Language 
 *     Resources and Evaluation (LREC'12) (1632 - 1636). Istanbul, Türgi: ELRA;
 *   </li>
 *   </ul>
 *   
 *   @author Siim Orasmaa
 *   @author Heiki-Jaan Kaalep
 */
public class Osalausestaja {

	/**
	 *   Teostab tekstil osalausestamise. Tagastab m2rgenduste listi, kus iga sisendi s6na kohta on 
	 *  yks m2rgenduste objekt, mille alt saab k2tte nii s6na enda kui sellega seotud m2rgendused.
	 */
	public List<OsalauSona> osalausesta(List<MorfAnSona> sonad){
		// 0) Loome j2rjendi, kuhu hakatakse panema m2rgendusi
		List<OsalauSona> osalauSonad = new ArrayList<OsalauSona>( sonad.size() );
		for (int i = 0; i < sonad.size(); i++) {
			osalauSonad.add( new OsalauSona( sonad.get(i) ) );
		}
		// 0) Jagame teksti lauseteks vastavalt sisendis olevale lausepiiride m2rgendusele
		List<List<OsalauSona>> laused = TekstiFiltreerimine.eraldaLaused( osalauSonad );
		
		// 1) Margistame sulgudes oleva osa kui kindla kiilude sisu
		lisaSulgudeKindladPiirid( laused );
		
		// 2) Lisame potentsiaalselt osalause keskmeks olevad verbid;
		lisaKesksedVerbid( laused );
		
		// 3) Lisame oletuslikud piirid kirjavahemärkide ja ja/ning/ega/või alusel
		lisaOletuslikudPiirid1( laused );
		
		// 4) Märgendame otsese kõne/jutumärkidega soetud osalausepiirid
		lisaJutum2rkidePiirid( laused );
		
		// 5) Jagame teksti juppideks lausepiiride ja osalausepiiride kohalt
		List<List<OsalauSona>> lausedOsalaused = 
				TekstiFiltreerimine.jagaKindlatePiirideKohalt( laused );
		
		// 6) Oletuslike piiride eemaldamine #1 ning [:;] m2rkimine kindlate piiridena
		eemaldaOletuslikudPiirid1( lausedOsalaused );
		lausedOsalaused = TekstiFiltreerimine.jagaKindlatePiirideKohalt( laused );
		
		// 7) Kindlate piiride lisamine #1 - koma/m6ttekriipsu ja j2rgneva k6rvallauset alustava s6na piirid
		lisaKindladPiirid1( lausedOsalaused );
		lausedOsalaused = TekstiFiltreerimine.jagaKindlatePiirideKohalt( laused );
		
		// 8) Liitoeldiste korrektsioon
		liitOeldisteKorrektsioon( lausedOsalaused );
		// 9) Kui m6lemal pool oletuslikku piiri on osalause keskmeks sobiv verb, muudetakse piir kindlaks
		//    Lisaks yritatakse m6istatada juhte, kui ainult yhel pool oletuslikku piiri on keskmeks sobiv verb;
		eemaldaOletuslikudPiirid2( lausedOsalaused );
		lausedOsalaused = TekstiFiltreerimine.jagaKindlatePiirideKohalt( laused );
		
		// 10) Eemaldame oletuslikke osalausepiire, kui need paistavad olevat loetelu elementide vahel;
		eemaldaLoetelud(lausedOsalaused);
		
		// 11) Kordame punkti 9: Kui m6lemal pool oletuslikku piiri on osalause keskmeks sobiv verb, muudetakse 
		//     piir kindlaks;
		for (List<OsalauSona> osalause : lausedOsalaused) {
			new EelnevadJargnevadOeldised(osalause, new SonaMall( MARGEND.OLETATAV_PIIR ), true );
		}
		lausedOsalaused = TekstiFiltreerimine.jagaKindlatePiirideKohalt( laused );

		// 12) Teisendame osalauseid kiiludeks
		//     Eemaldame lause l6pust kindlad piirid (kui kogemata on kindlaid piire sattunud lausel6ppu)
		teisendaKiiludeks( laused, true );
		
		return osalauSonad;
	}


	/**
	 *  M2hismeetod ehk wrapper meetodi <tt>osalausesta</tt> lihtsamaks v2ljakutsumiseks PyVabamorfi 
	 *  v2ljundi peal; N6uab ainult s6ne kujul JSON sisendit ning v2ljastab tulemused samuti ainult 
	 *  JSON s6ne kujul, peites keerukamad andmestruktuurid;  
	 */
	public String osalausestaPyVabamorfJSON( String sisendJSON ) throws Exception {
		List<MorfAnSona> tekstiSonad = null;
		try {
			BufferedReader inputReader = new BufferedReader( new StringReader(sisendJSON) );
			tekstiSonad = VabaMorfJSONReader.parseJSONtext(inputReader);
			inputReader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		List<OsalauSona> margendatudSonad = this.osalausesta( tekstiSonad );		
		return ValjundiVormistaja.vormistaTulemusVabaMorfiJSONkujul( sisendJSON, 
				margendatudSonad, false);
	}


	/**
	 *   M2rgistab sulgudes oleva teksti kui kindlalt kiilu sisse kuuluva.
	 *   NB! M2rgendatakse vaid juhud, kus sulud on tasakaalus, st avavale
	 *   leidub kindlasti ka vastav sulgev sulg.
	 */
	private void lisaSulgudeKindladPiirid( List<List<OsalauSona>> laused ){
		SonaMall suludOnYmber = new SonaMall( Pattern.compile("^\\(.+\\)[.,;!?]*$") );
		// Tootleme sisendit lause-lause haaval
		for (List<OsalauSona> lause : laused) {
			//  Seal, kus sulgude paarsus klapib (avavale sulule leidub vastav sulgev sulg)
			//  m2rgendame osalausepiiri
			Stack<Integer> avavadSulud = new Stack<Integer>();
			for (int i = 0; i < lause.size(); i++) {
				String algSona = (lause.get( i )).getNormAlgSona();
				for (int j = 0; j < algSona.length(); j++) {
					if (algSona.charAt(j) == '('){
						avavadSulud.push( i );
					} else if (algSona.charAt(j) == ')' && !avavadSulud.isEmpty()) {
						Integer indeks = avavadSulud.pop();
						OsalauSona suluAlgusSona = lause.get( indeks );
						boolean voibKiilunaEraldada = true;
						if (indeks == i){
							try {
								//  Kui sulud algavad samas s6nas, milles need l6pevadki, kontrollime,
								//  et tegemist poleks juhtudega, kus sulge kasutatakse s6na sees, nt
								//       raamatukogu(de), (aja)kirjanduslikust, või(s)tlus  jms
								//  Lubame kiilude eraldamist vaid juhtudel, kui sulud paiknevad nii 
								//  s6na alguses kui ka l6pus, nt  (1998), (Riigikogu) 
								voibKiilunaEraldada = suludOnYmber.vastabMallileAND(suluAlgusSona);
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(-1);
							}
						}
						if (voibKiilunaEraldada){
							// Kuna leidus avavale sulule vastav sulgev sulg, siis
							// lisame m6lemale s6nale kiilum2rgendid
							suluAlgusSona.lisaMargend(OsalauSona.MARGEND.KIILU_ALGUS);
							(lause.get( i )).lisaMargend(OsalauSona.MARGEND.KIILU_LOPP);							
						}
					}
				}
			}
			//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(lause, true) );
		}
	}

	/**
	 *  M2rgistab osalausete keskmeks olevad verbid (enamasti on tegu just oeldisverbidega).
	 *  <br><br>
	 *  NB! Täpitähtede asemel kasutame vastavaid unicode'i symboleid, mille loetelu
	 *  leiab aadressilt: 
	 *  http://www.utf8-chartable.de/unicode-utf8-table.pl?number=512 
	 */
	private void lisaKesksedVerbid( List<List<OsalauSona>> laused ) {
		SonaMall onKomaLopus = new SonaMall( Pattern.compile("^.*,$") );
		// Tootleme sisendit lause-lause haaval
		for (List<OsalauSona> lause : laused) {
			for (int i = 0; i < lause.size(); i++) {
				// Hakkame kitsendama s6naga seotud morf tunnuste hulka
				MorfTunnusteHulk tunnused = new MorfTunnusteHulk( lause.get( i ) );
				MorfTunnusteHulk verbiTunnused = tunnused.filterByPOS("_V_");
				if (!verbiTunnused.isEmpty()){
					// Kui s6naga on seotud v2hemalt yks verbi-analyys, filtreerime edasi
					//
					// 1)  öeldiseks ei sobi ma, da, des, tama, v, tav vormid
					//     öeldiseks ei sobi ka üksikud ei, ära, nud, tud
					//     k6ik muud peaksid esialgu sobima
					if ((verbiTunnused.filterByFormNames("^\\s*(ma|mata|mas|mast|maks|da|des|tama|v|tav|tud|nud),?\\s*$")).isEmpty() && 
								(verbiTunnused.filterByLemma("ei")).isEmpty() && 
									(verbiTunnused.filterByLemma("\u00E4ra")).isEmpty()){
						(lause.get( i )).lisaMargend( MARGEND.OELDIS );						
					}
					// 2) 'ei' sobib öeldiseks konstruktsioonides:
					//       ei + tud, ei + nud, ei + käskiva kv verb
					if (!(verbiTunnused.filterByLemma("ei")).isEmpty()){
						if (i + 1 < lause.size()){
							MorfTunnusteHulk tunnused2 = 
									new MorfTunnusteHulk( (lause.get(i+1)).getMorfSona() );
							if ( !(tunnused2.filterByPOS("_V_")).filterByEnding( "^\\s*(nud|dud|tud)(ki)?\\s*$" ).isEmpty() ){
								// ei + tud, ei + nud
								(lause.get( i )).lisaMargend( MARGEND.OELDIS );
								(lause.get( i + 1 )).lisaMargend( MARGEND.OELDIS );
							}
							if ( !(tunnused2.filterByPOS("_V_")).filterByFormNames( "^\\s*(o),?\\s*$" ).isEmpty() ){
								// ei + kaskiv kv
								(lause.get( i )).lisaMargend( MARGEND.OELDIS );
								(lause.get( i + 1 )).lisaMargend( MARGEND.OELDIS );
							}
						}
					}
					// 3) 'ära' + käskiva kv verb sobib öeldiseks;
					if (!(verbiTunnused.filterByLemma("\u00E4ra")).isEmpty()){
						if (i + 1 < lause.size()){
							MorfTunnusteHulk tunnused2 = 
									new MorfTunnusteHulk( (lause.get(i+1)).getMorfSona() );
							if ( !(tunnused2.filterByPOS("_V_")).filterByFormNames( "^\\s*(o),?\\s*$" ).isEmpty() ){
								// ära + kaskiv kv
								(lause.get( i )).lisaMargend( MARGEND.OELDIS );
								(lause.get( i + 1 )).lisaMargend( MARGEND.OELDIS );
							}
						}
					}
					// 4) 'ole' + nud sobib öeldiseks;
					if (!(verbiTunnused.filterByLemma("ole")).isEmpty()){
						if (i + 1 < lause.size()){
							MorfTunnusteHulk tunnused2 = 
									new MorfTunnusteHulk( (lause.get(i+1)).getMorfSona() );
							if ( !(tunnused2.filterByPOS("_V_")).filterByEnding( "^\\s*(nud)(ki)?\\s*$" ).isEmpty() ){
								// ole + nud
								(lause.get( i )).lisaMargend( MARGEND.OELDIS );
								(lause.get( i + 1 )).lisaMargend( MARGEND.OELDIS );
							}
						}
					}
					if (!(verbiTunnused.filterByFormNames("^\\s*(des|mata|maks),?\\s*$")).isEmpty()){
						// 5) kui -des/-mata/-maks on lauses vahetult pärast koma (des/mata/maks lauselühend järelasendis), 
						// siis sobivad öeldisteks:
						if (i - 1 >= 0){
							try {
								if ( onKomaLopus.vastabMallileAND( lause.get(i-1) ) ){
									(lause.get( i )).lisaMargend( MARGEND.OELDIS );
								}
							} catch (Exception e) {
								e.printStackTrace();
								System.exit(-1);
							}
						}
						// 6) Komale järgnev mitte ja tollele järgnev -des -- des sobib öeldiseks:
						if (!(verbiTunnused.filterByFormNames("^\\s*(des),?\\s*$")).isEmpty()){
							if (i - 2 >= 0){
								try {
									if ( (lause.get(i-1)).getNormAlgSona() != null && 
											((lause.get(i-1)).getNormAlgSona()).equals("mitte") && 
												onKomaLopus.vastabMallileAND( lause.get(i-2) )){
										(lause.get( i )).lisaMargend( MARGEND.OELDIS );
									}
								} catch (Exception e) {
									e.printStackTrace();
									System.exit(-1);
								}
							}
						}
						// 7) Kui -des/-maks on vahetult lause alguses, siis sobivad öeldisteks
						// NB! See ei pruugi töötada mitmesõnaliste verbide puhul - tuleks korpusest järgi uurida
						// NB! Kui lausealgulisele -des/-maks verbile ei järgne koma, siis võib lausesse tekkida
						//     kaks öeldist. Tuleks uurida, kas see tekitab kusagil probleeme.
						if (!(verbiTunnused.filterByFormNames("^\\s*(des|maks),?\\s*$")).isEmpty()){
							// Kui s6na on juba lause alguses, loeme 8eldiseks:
							if (i == 0){
								(lause.get( i )).lisaMargend( MARGEND.OELDIS );
							} else {
								// Kui s6na pole lause alguses, loeme 8eldiseks, kui algab
								// suurtähega (kuna v6ib olla tegu lausestamise veaga ...)
								String algSona = (lause.get( i )).getNormAlgSona();
								// Kontrollime, et s6na algaks suurt2hega
								if (algSona != null && algSona.matches("^[ABCDEFGHIJKLMNOPRS\u0160\u017DTUV\u00D5\u00C4\u00D6\u00DC].*")){
									(lause.get( i )).lisaMargend( MARGEND.OELDIS );
								}                        		
							}
						}
					}
				}
			}
			//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(lause, false) );
		}
	}
	
	/**
	 *  K6net alustavad jutum2rgid;<br>
	 *  TODO: siin saaks neid veel t2psemalt organiseerida 
	 */
	private Pattern koneAlgusJutumark = Pattern.compile("^[\"\u00AB\u02EE\u030B\u201C\u201D\u201E].*$");
	
	/**
	 *  K6ne l6petavad jutum2rgid;<br>
	 *  TODO: siin saaks neid veel t2psemalt organiseerida 
	 */
	private Pattern koneLoppJutumark  = Pattern.compile("^.*[\"\u00BB\u02EE\u030B\u201C\u201D\u201E]$");
	
	/**
	 *  M2rgistab kõik võimalikud osalausepiiride asukohad: teatud kirjavahemärkide juures ja 
	 *  sidesõnade <i>ja, ning, ega, või</i> juures.<br><br>
	 *  
	 *  NB! Kasutame ka unicode'i symboleid, mille loetelu leiab ntx aadressilt: 
	 *  http://www.utf8-chartable.de/unicode-utf8-table.pl?number=512 
	 */
	private void lisaOletuslikudPiirid1( List<List<OsalauSona>> laused ){
		// Tootleme sisendit lause-lause haaval
		for (List<OsalauSona> lause : laused) {
			for (int i = 0; i < lause.size(); i++) {
				String algSona = (lause.get(i)).getNormAlgSona();
				if (algSona != null && (i != 0) && (i != lause.size()-1)){
					// 1) m2rgime lause keskel (väiksetähelised) ja/ning/ega/või oletatavateks piirideks
					if (algSona.matches("^(ja|ning|ega|v\u014Di|v\u00F5i)$")){
						(lause.get( i )).lisaMargend( MARGEND.OLETATAV_PIIR );
					}
					// 2) m2rgime lause keskel kirjavahem2rgid, mis t2histavad piire
					//    , ; : . ? ! --- kui need on s6na l6pus, siis on oletatav piir
					if (algSona.matches("^.*[,;:?!]$")){
						(lause.get( i )).lisaMargend( MARGEND.OLETATAV_PIIR );
					}
					if (algSona.matches("^[.]+$")){
						//
						//   2.1) Punkti lubame oletuslikuks piiriks vaid siis, kui see esinebki
						//        yksikuna v6i mitme j2rjestikkuse punktina. M6ne s6na v6i arvu 
						//        l6pus esineva punkti puhul pole kindel, kas see ikka l6petab
						//        osalause või hoopis m6ne s6na (lyhendi / arvu);
						//
						// 
						(lause.get( i )).lisaMargend( MARGEND.OLETATAV_PIIR );
					}
					//    " » “ ” --- kui need on s6na l6pus, siis on oletatav piir
					if ((koneLoppJutumark.matcher(algSona)).matches()){
						(lause.get( i )).lisaMargend( MARGEND.OLETATAV_PIIR );
					}
					//    " « “ ” --- kui need on s6na alguses, siis on oletatav piir
					if ((koneAlgusJutumark.matcher(algSona)).matches()){
						(lause.get( i )).lisaMargend( MARGEND.OLETATAV_PIIR );
					}
					//   kui kriips on yksinda, siis on oletatav piir
					if (algSona.matches("^\\s*(-{1,}|\u2212|\uFF0D|\u02D7|\uFE63|\u002D|\u2010|\u2011|\u2012|\u2013|\u2014|\u2015)\\s*$")){
						(lause.get( i )).lisaMargend( MARGEND.OLETATAV_PIIR );
					}
				} else if (algSona != null){
					// A) kui tegemist on oletatavasti s6naga (v2hemalt yks t2ht) ning l6pus on kirjavahem2rgid, 
					//    m2rgime maha oletatava piiri 
					//    , ; : . ? ! --- kui need on s6na l6pus, siis on oletatav piir
					if (algSona.matches("^.*\\p{Alpha}.*[,;:.?!]$")){
						(lause.get( i )).lisaMargend( MARGEND.OLETATAV_PIIR );
					}
				}
			}
			//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(lause, false) );
		}
	}
	
	/**
	 *   M2rgistab otsese k6ne alguse ja l6pu kui kindlad osalausepiirid; <br>
	 *   Kui jutumärkide paarsus on kindlaks tehtav ja osalause keskmeks sobiv 
	 *   verb asub nii jutumärkidest seespool kui ka väljas, siis on jutumärkide vahel
	 *   tõenäoliselt tsitaat (s.t kiil) ja see märgitaksegi sellisena; 
	 */
	private void lisaJutum2rkidePiirid( List<List<OsalauSona>> laused  ){
		// oeldise olemasolu
		SonaMall oeldis         = new SonaMall( MARGEND.OELDIS );
		// osalausepiiride (nii kindlate kui oletatavate) olemasolu
		SonaMall osalausepiir   = new SonaMall( MARGEND.OLETATAV_PIIR );
		osalausepiir.lisaVajalikMargend(MARGEND.KINDEL_PIIR);
		// k6ik jutum2rgid, nii alustavad kui lopetavad
		SonaMall koikJutuMargid = new SonaMall(koneAlgusJutumark); 
		koikJutuMargid.lisaTeksikujuKirjeldus(koneLoppJutumark);
		// k6ik jutum2rgid + v6imalikud ja kindlad osalausepiirid
		SonaMall jutuMarkJaOLP = new SonaMall(koneAlgusJutumark);
		jutuMarkJaOLP.lisaTeksikujuKirjeldus(koneLoppJutumark);
		jutuMarkJaOLP.lisaVajalikMargend(MARGEND.OLETATAV_PIIR);
		jutuMarkJaOLP.lisaVajalikMargend(MARGEND.KINDEL_PIIR);
		jutuMarkJaOLP.lisaVajalikMargend(MARGEND.KIILU_ALGUS);
		jutuMarkJaOLP.lisaVajalikMargend(MARGEND.KIILU_LOPP);
		try {
			// Tootleme sisendit lause-lause haaval
			for (List<OsalauSona> lause : laused) {
				//
				// M2rgistame otsese k6ne alguse ja l6pu kui kindlad osalausepiirid;
				//
				for (int i = 0; i < lause.size(); i++) {
					OsalauSona sona   = lause.get(i);
					String tekstiSona = sona.getNormAlgSona();
					// 1) Kui koolon on alustavate jutum2rkide ees, ning koolonile eelneb
					//    (ilma osalausepiirideta) oeldis, siis on alustavad jutum2rgid kindel 
					//    osalausepiir ...
					if (tekstiSona.matches("^.*:$") && sona.omabMargendit(MARGEND.OLETATAV_PIIR)){
						if (i+1 < lause.size()){
							String tekstiSona2 = (lause.get( i+1 )).getNormAlgSona();
							if ((koneAlgusJutumark.matcher(tekstiSona2)).matches()){
								// Kontrollime, kas eelneb (t6en2oliselt) k6nelemise (REPORTING) verb 
								// ning vahel pole keelatud m2rgendeid:
								int j = TekstiFiltreerimine.eelnebMargendigaSona(lause, i, oeldis, osalausepiir );
								if (j > -1){
									(lause.get( i+1 )).lisaMargend(MARGEND.KONE_ALGUS);
									(lause.get( i+1 )).lisaMargend(MARGEND.KINDEL_PIIR);
									(lause.get( i+1 )).eemaldaMargend(MARGEND.OLETATAV_PIIR);
									(lause.get( i )).eemaldaMargend(MARGEND.OLETATAV_PIIR);
								}
							}
						}
					}
					// 2) Kui lauselõpu/vahemärgile (!?.,) järgneb l6petav jutumärk ja seejärel järgneb
					// (ilma osalausepiirideta) oeldis, siis on l6petavad jutum2rgid kindel 
					// osalausepiir ...
					if (tekstiSona.matches("^.*[,;.?!]$") && sona.omabMargendit(MARGEND.OLETATAV_PIIR)){
						if (i+1 < lause.size()){
							String tekstiSona2 = (lause.get( i+1 )).getNormAlgSona();
							if ((koneLoppJutumark.matcher(tekstiSona2)).matches()){
								int j = TekstiFiltreerimine.jargnebMargendigaSona(lause, i+1, oeldis, osalausepiir );
								if (j > -1){
									(lause.get( i+1 )).lisaMargend(MARGEND.KONE_LOPP);
									(lause.get( i+1 )).lisaMargend(MARGEND.KINDEL_PIIR);
									(lause.get( i+1 )).eemaldaMargend(MARGEND.OLETATAV_PIIR);
									(lause.get( i )).eemaldaMargend(MARGEND.OLETATAV_PIIR);
								}
							}
						}
					} else if ((koneLoppJutumark.matcher(tekstiSona)).matches() && 
										tekstiSona.matches("^.*[,;.?!].{1,2}$") && 
												sona.omabMargendit(MARGEND.OLETATAV_PIIR)){
						int j = TekstiFiltreerimine.jargnebMargendigaSona(lause, i, oeldis, osalausepiir );
						if (j > -1){
							(lause.get( i )).lisaMargend(MARGEND.KONE_LOPP);
							(lause.get( i )).lisaMargend(MARGEND.KINDEL_PIIR);
							(lause.get( i )).eemaldaMargend(MARGEND.OLETATAV_PIIR);
						}
					}
				}
				// System.out.println( TekstiFiltreerimine.debugMargendustegaLause(lause, false) );
				//
				//  Tsitaat:  kui lauses on täpselt ühed jutumärgid (keskel) ning jutum2rkidest v2ljaspool 
				// ja seespool leidub oeldis, ning jutum2rgid pole varasemalt m2rgitud kindlaks piiriks, 
				// eralda jutum2rkide vahele j22v osa kiiluna;
				//
				// Leiame k6ik jutum2rgid, mis pole veel m2rgitud kindlaks piiriks 
				List<Integer> jutuMarkideIndeksid = new ArrayList<Integer>();
				for (int i = 0; i < lause.size(); i++) {
					OsalauSona sona = lause.get(i);
					if (koikJutuMargid.vastabMallileOR( sona ) && !(sona.omabMargendit(MARGEND.KINDEL_PIIR))) {
						jutuMarkideIndeksid.add( i );
					}
				}
				//
				// Kui on t2pselt kaks jutum2rki ning 6eldised paiknevad nii sees kui v2ljas, eraldame 
				// sisu kiiluna (v6imaliku tsitaadina) ...
				//
				if (jutuMarkideIndeksid.size() == 2){
					int alustavMark = jutuMarkideIndeksid.get( 0 );
					int lopetavMark = jutuMarkideIndeksid.get( 1 );
					int oeldisEnneMarke = 
							TekstiFiltreerimine.eelnebMargendigaSona(lause, alustavMark, oeldis, jutuMarkJaOLP);
					int oeldisParastMarke =
							TekstiFiltreerimine.jargnebMargendigaSona(lause, lopetavMark, oeldis, jutuMarkJaOLP);
					int oeldisMarkideSees = 
							TekstiFiltreerimine.jargnebMargendigaSona(lause, alustavMark, oeldis, koikJutuMargid);
					//
					//  Kontrollime, et jutum2rkidepaari sisse ei j22ks teisi jutum2rke (selliseid, mis viitaksid
					//  v6imalikule probleemile m2rkide paarsusega)
					//
					int jutum2rkM2rkideSees = TekstiFiltreerimine.jargnebMargendigaSonaOR(lause, alustavMark, koikJutuMargid, null);
					if (jutum2rkM2rkideSees > -1  &&  jutum2rkM2rkideSees < lopetavMark){
						continue;
					}
					if (oeldisMarkideSees > -1 && (oeldisEnneMarke > -1 || oeldisParastMarke > -1)){
						(lause.get( alustavMark )).eemaldaMargend(MARGEND.OLETATAV_PIIR);
						(lause.get( alustavMark )).eemaldaMargend(MARGEND.KINDEL_PIIR);
						(lause.get( alustavMark )).lisaMargend(MARGEND.KIILU_ALGUS);
						(lause.get( lopetavMark )).eemaldaMargend(MARGEND.OLETATAV_PIIR);
						(lause.get( lopetavMark )).eemaldaMargend(MARGEND.KINDEL_PIIR);
						(lause.get( lopetavMark )).lisaMargend(MARGEND.KIILU_LOPP);
					}
				}
				//
				//   Muud jutum2rgid pole piirid -- eemaldame need, et need ei hakkaks j2rgmiste reeglite t88d
				//  segama ...
				//
				for (int i = 0; i < lause.size(); i++) {
					OsalauSona sona = lause.get(i);
					if (koikJutuMargid.vastabMallileOR(sona) && sona.omabMargendit(MARGEND.OLETATAV_PIIR)){
						sona.eemaldaMargend(MARGEND.OLETATAV_PIIR);
						//   NB! HJK-l oli siin veel m2rkus, et lause algusesse/l6ppu tuleks " alles j2tta,
						//       et kindlad_lahku (jagaKindlatePiirideKohalt) saaks 6igesti t88tada, aga see 
						//       j2i arusaamatuks ...
					}
				}
				//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(lause, false) );
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 *   Oletuslike piiride eemaldamine #1:<br>
	 *   ** Eemalda osalausepiir _N_ : _N_ vahelt (nt "12 : 55")<br>
	 *   ** Eemalda osalausepiir kriipsuga l6ppeva s6na j2relt (nt "puu- ja köögivili")<br>
	 *   ** Kui mitu lausel6pum2rki on yksteise j2rel, j2ta alles viimase piir (nt "Mida ?! ? !")<br>
	 *   ** M2rgista semikoolon ; ja koolon : kui kindlad osalausepiirid;
	 */
	private void eemaldaOletuslikudPiirid1(List<List<OsalauSona>> tykeldus) {
		SonaMall koolonLopus      = new SonaMall( Pattern.compile("^.*:$") );
		SonaMall kriipsLopus      = new SonaMall( Pattern.compile("^[^-]+-$") );
		SonaMall ainultkirjaVM    = new SonaMall( Pattern.compile("^[.?!]{1,}$") );
		SonaMall kirjaVMLopus     = new SonaMall( Pattern.compile("^.*[.?!]{1,}$") );
		SonaMall koolonSemiKLopus = new SonaMall( Pattern.compile("^.*[;:]$") );
		SonaMall initsiaalid      = new SonaMall( Pattern.compile("^([A-Z\u0160\u017D\u00D5\u00C4\u00D6\u00DC-]+\\.)+:?$") );
		try {
			// Tootleme sisendit seni eraldatud osalausete haaval
			OsalauSona eelmineSona = null;
			for (List<OsalauSona> osalause : tykeldus) {
				for (int i = 0; i < osalause.size(); i++) {
					OsalauSona sona = osalause.get(i);
					//
					//  1. Eemalda osalausepiir _N_ : _N_ vahelt (nt "12 : 55", "31: 45.")
					//  
					if (koolonLopus.vastabMallileAND(sona) && i+1 < osalause.size()){
						MorfTunnusteHulk j2rgmSonaTunnused = new MorfTunnusteHulk( osalause.get(i+1) );
						if (!(j2rgmSonaTunnused.filterByPOS("_N_")).isEmpty() || 
								!(j2rgmSonaTunnused.filterByPOS("_O_")).isEmpty()){
							// J2rgmine s6na on arv/number -- kontrollime ka seda v6i eelmist s6na ...
							MorfTunnusteHulk eelSonaTunnused = null; 
							if ((sona.getNormAlgSona()).length() > 1){
								eelSonaTunnused = new MorfTunnusteHulk( sona );
							} else if (i - 1 > -1) {
								eelSonaTunnused = new MorfTunnusteHulk( osalause.get(i-1) );
							}
							if (eelSonaTunnused != null && 
									!(eelSonaTunnused.filterByPOS("_N_")).isEmpty() ){
								sona.eemaldaMargend( MARGEND.OLETATAV_PIIR );
							}
						}
					}
					//
					//  2. Eemalda osalausepiir kriipsuga l6ppeva s6nale j2rgnevalt s6nalt (nt "puu- ja köögivili")
					//  
					if (kriipsLopus.vastabMallileAND(sona) && i+1 < osalause.size()){
						if ((osalause.get(i+1)).omabMargendit(MARGEND.OLETATAV_PIIR)){
							(osalause.get(i+1)).eemaldaMargend(MARGEND.OLETATAV_PIIR);
						}
					}
					//
					//  3. Kui on mitu lauselõpumärki üksteise järel, jäta alles viimase (oletuslik) piir
					//
					if (ainultkirjaVM.vastabMallileAND(sona) && sona.omabMargendit(MARGEND.OLETATAV_PIIR) && i - 1 > -1){
						OsalauSona eelmSona = osalause.get(i-1);
						if (kirjaVMLopus.vastabMallileAND( eelmSona ) && eelmSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
							eelmSona.eemaldaMargend( MARGEND.OLETATAV_PIIR );
						}
					}
					//
					//  4. [;:] on kindel piir
					//
					if (koolonSemiKLopus.vastabMallileAND(sona) && sona.omabMargendit(MARGEND.OLETATAV_PIIR)){
						sona.eemaldaMargend( MARGEND.OLETATAV_PIIR );
						sona.lisaMargend( MARGEND.KINDEL_PIIR );
					}
					//
					//  5. Initsiaalid (E.K., I.P., H., E.-K.R. jms) koos j2rgneva kooloniga lause alguses on kindel 
					//     piir (k6ne v6i repliigi algus)
					//
					if (eelmineSona == null || (eelmineSona.getMorfSona()).onLauseLopp()){
						if (initsiaalid.vastabMallileAND(sona)){
							if (koolonSemiKLopus.vastabMallileAND(sona)){
								sona.eemaldaMargend(MARGEND.OLETATAV_PIIR);
								sona.lisaMargend(MARGEND.KINDEL_PIIR);
							} else if (i+1 < osalause.size() && koolonSemiKLopus.vastabMallileAND(osalause.get(i+1))){
								(osalause.get(i+1)).eemaldaMargend(MARGEND.OLETATAV_PIIR);
								(osalause.get(i+1)).lisaMargend(MARGEND.KINDEL_PIIR);
							}
						}
					}
					eelmineSona = sona;
				}
				//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(osalause, false) );
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 *   Kindlate piiride lisamine #1 - osalausepiirid k6rvallausetes:<br>
	 *   ** kui komale/sidekriipsule järgneb vahetult ja|ning|ega|või, märgi kindel piir <br>
	 *   ** kui komale/sidekriipsule järgneb vahetult aga|kuigi, millele omakorda järgneb öeldis, siis märgi kindel piir <br>
	 *   ** kui komale/sidekriipsule järgneb vahetult et|kui|millal|..., siis märgi kindel piir; <br>
	 *   ** kui komale/sidekriipsule järgneb vahetult mis|kes|... (lemmade põhjal), siis märgi kindel piir;<br>
	 *   ** kui komale/sidekriipsule järgneb vahetult suvaline s6na ja seej2rel et, siis märgi kindel piir;<br>
	 */
	private void lisaKindladPiirid1(List<List<OsalauSona>> tykeldus) {
		SonaMall komaKriipsLopus = new SonaMall( 
			Pattern.compile("^.*(,|-{1,}|\u2212|\uFF0D|\u02D7|\uFE63|\u002D|\u2010|\u2011|\u2012|\u2013|\u2014|\u2015)$") );
		SonaMall jaNingEgaVoi    = new SonaMall( Pattern.compile("^(ja|ning|ega|v\u014Di|v\u00F5i)$") );
		SonaMall agaKuigi        = new SonaMall( Pattern.compile("^(aga|kuigi)$") );
		SonaMall etKuiMillalJne  = new SonaMall( 
			Pattern.compile("^(et|kui|millal|kus|kuhu|kust|sest|kuid|nagu|ehkki|siis|kuni|otsekui|justkui|kuna|kuidas|kas|siis)$") );
		// oeldise olemasolu
		SonaMall oeldis       = new SonaMall( MARGEND.OELDIS );
		// osalausepiiride (nii kindlate kui oletatavate) olemasolu
		SonaMall osalausepiir = new SonaMall( MARGEND.OLETATAV_PIIR );
		osalausepiir.lisaVajalikMargend(MARGEND.KINDEL_PIIR);
		try {
			// Tootleme sisendit seni eraldatud osalausete kaupa
			for (List<OsalauSona> osalause : tykeldus) {
				//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(osalause, false) );
				for (int i = 0; i < osalause.size(); i++) {
					OsalauSona sona = osalause.get(i);
					if (komaKriipsLopus.vastabMallileAND(sona) && i+1 < osalause.size() && 
						(sona.omabMargendit(MARGEND.OLETATAV_PIIR) || sona.omabMargendit(MARGEND.KINDEL_PIIR))){
						//
						// 1. kui komale/sidekriipsule järgneb vahetult ja|ning|ega|või, märgi kindel piir 
						//
						if (jaNingEgaVoi.vastabMallileAND( osalause.get(i+1) ) && 
								(( osalause.get(i+1) ).omabMargendit(MARGEND.OLETATAV_PIIR) || 
								 ( osalause.get(i+1) ).omabMargendit(MARGEND.KINDEL_PIIR)) ){
							sona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
							(osalause.get(i+1)).eemaldaMargend(MARGEND.OLETATAV_PIIR);
							(osalause.get(i+1)).eemaldaMargend(MARGEND.KINDEL_PIIR);
						}
						//
						// 2. kui komale/sidekriipsule järgneb vahetult aga|kuigi, millele omakorda järgneb öeldis, siis märgi kindel piir
						//
						// HJK: kas 'kuid' ja 'ehkki' puhul peaks samuti kontrollima verbi olemasolu?
						if (agaKuigi.vastabMallileAND( osalause.get(i+1) ) && 
							  TekstiFiltreerimine.jargnebMargendigaSona(osalause, i+1, oeldis, osalausepiir ) > -1){
							sona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
						}
						//
						// 3. kui komale/sidekriipsule järgneb vahetult et|kui|millal|..., siis märgi kindel piir
						//    (ysnagi produktiivne reegel)
						//
						if (etKuiMillalJne.vastabMallileAND( osalause.get(i+1) )){
							sona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
						}
						//
						// 4. kui komale/sidekriipsule järgneb vahetult mis|kes|... (lemmade põhjal), siis märgi kindel piir;
						//
						MorfTunnusteHulk j2rgmSona = new MorfTunnusteHulk( osalause.get(i+1) );
						if (!j2rgmSona.filterByLemmaRegExp("^(mis|mis_*sugune|milline|kes|see)$").isEmpty()){
							//  NB! Siin v6ib tekkida duplikaate, kuna 'sest' omab ka s6na 'see' morfoloogilist 
							// analyysi (kui yhestamine on puudulik), seet6ttu lubame siin uue kindla piiri
							// lisamist vaid siis, kui kindlat piiri pole veel lisatud (eelmise reegli poolt);
							sona.asendaMargendRange(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);

						}
						//
						// 5. kui komale/sidekriipsule järgneb vahetult suvaline s6na ja seej2rel et, siis märgi kindel piir;
						//
						if (i+2 < osalause.size() && 
							(osalause.get(i+2).getNormAlgSona()) != null &&
								((osalause.get(i+2).getNormAlgSona())).equals("et")){
							//
							//    Tähelepanek #1: Kui komale/sidekriipsule j2rgnebki mingi teine kirjavahem2rk, ja seej2rel
							//              "et", nt lauses: 
							//               "Eeldan - aga see on minu enda hüpotees - , et ta oli lugenud kriitikat."
							//              siis tekitatakse selle ja eelnevate reeglite abil m6lema kirjavahem2rgi
							//              taha kindel_piir.
							//     Kuna näitelauses (ja võimalik ka, et muudes sarnastes kontekstides) viitab see 
							//    tegelikult sidekriipsudega eraldatud kiilu olemasolule, võib vaielda, kas tegemist
							//    on yldse probleemiga. 
							//     ( HJK implementatsioon lisab piiri millegipärast vaid kahe kirjavahem2rgi keskele )
							//
							//    Tähelepanek #2: Kui komale/sidekriipsule on juba vahetult j2rgneva s6na t6ttu
							//                    m2rgend kylge pandud, siis ylej2rgmise s6na t6ttu enam m2rgendit
							//                    panna pole vaja. Nt lauses:
							//                     "On loomulik probleem ka välja tuua , selleks et saaksime selle lahendada ."
							//                    (reegel paneb ", see" t6ttu m2rgendi kylge )
							//    Seet6ttu lubame siin samuti ainult ranget asendamist, et v2ltida yleliigsete piiride teket;
							// 
							sona.asendaMargendRange(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
						}
					}
				}
				//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(osalause, false) );
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 *  Liitoeldiste korrektsioon:<br>
	 *  ** kui oeldiseks margitud nud-kesksonale jargneb oletuslik osalausepiir ja seejarel 
	 *     veel yks nud-kesksona ilma oeldise margendita, margi viimane nud-kesksona ka 
	 *     oeldiseks;  nt "ta on tulnud ja nainud"; <br>
	 *  ** kui voimalik, rakenda seda reeglit mitu korda, nt "ta on tulnud , nainud , teinud ja voitnud";<br>
	 *  ** NB! Parandamata jaavad ikkagi juhud, kus esimene nud-kesksona on olema-verbist/eitusest lahutatud, nt
	 *     "ta on juba ammu tulnud ja nainud";
	 */
	private void liitOeldisteKorrektsioon(List<List<OsalauSona>> tykeldus) {
		SonaMall oeldis       = new SonaMall( MARGEND.OELDIS );
		SonaMall oletatavpiir = new SonaMall( MARGEND.OLETATAV_PIIR );
		SonaMall kindelpiir   = new SonaMall( MARGEND.KINDEL_PIIR );
		try {
			// Tootleme sisendit seni eraldatud osalausete kaupa
			for (List<OsalauSona> osalause : tykeldus) {
				for (int i = 0; i < osalause.size(); i++) {
					OsalauSona sona = osalause.get(i);
					if (oeldis.vastabMallileAND(sona)){
						MorfTunnusteHulk sonaTunnused = new MorfTunnusteHulk( sona );
						if ( !(sonaTunnused.filterByPOS("_V_")).filterByFormNames("^(neg)?\\s*nud,?\\s*$").isEmpty() ){
							int jargnevOletatavPiir = -1;
							// 1) Leiame, kas kesks6nale j2rgneb oletatav lausepiir
							if (oletatavpiir.vastabMallileAND(sona)){
								jargnevOletatavPiir = i;
							} else {
								int j = i+1;
								while (j < osalause.size()){
									if ( kindelpiir.vastabMallileAND(osalause.get(j)) ){
										break;
									} else if ( oletatavpiir.vastabMallileAND(osalause.get(j)) ){
										jargnevOletatavPiir = j;
										break;
									}
									j++;
								}
							}
							if (jargnevOletatavPiir > -1){
							// 2) Leiame, kas oletatavale piirile j2rgneb nud-kesks6na, mis pole 8eldis;
								int j = jargnevOletatavPiir+1;
								while (j < osalause.size()){
									OsalauSona j2rgmSona = osalause.get(j);
									MorfTunnusteHulk tunnused2 = new MorfTunnusteHulk( j2rgmSona );
									if ( !(tunnused2.filterByPOS("_V_")).filterByFormNames("^\\s*nud,?\\s*$").isEmpty() ){
										if (!oeldis.vastabMallileAND(j2rgmSona)){
											// Kui nud-kesks6na pole veel 8eldis, muudame selle 8eldiseks ...
											j2rgmSona.lisaMargend( MARGEND.OELDIS );
											break;											
										}
									}
									if ( kindelpiir.vastabMallileAND(j2rgmSona) || 
											oletatavpiir.vastabMallileAND(j2rgmSona)){
										break;
									}
									j++;
								}
								//
								//   Probleemid: 
								//    1) kui ei arvesta eitusega nud-öeldisi (_V_ neg nud), võib jääda piir 
								//       määramata, nt lauses:
								//         Kuid inimesed ei pööranud mulle tähelepanu : ma polnud esimene ega jäänud ka viimaseks .
								//    2) kui arvestada eitusega nud-öeldisi (_V_ neg nud), või tekkida üleliigne 
								//       piir järgneva omadusõnalise nud-i juurde:
								//         Aga nüüd polnud mul enam õline ja määrdunud raudteelase vatikuub seljas , vaid Nataša venna vana jope .
								//    Variant (2) oli ka HJK programmis, praegu jääme ühtluse huvides selle juurde;
								//
							}
						}
					}
				}
				//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(osalause, false) );
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 *   Klass, mille abil saab analyysida osalauset/lauset ning teha kindlaks iga kirjeldusele vastava 
	 *   osalausepiiri puhul, kas sellele (osalausepiires) eelneb v6i j2rgneb 8eldis. Yhtlasi salvestatakse tulemused 
	 *   objekti alla paisktabelitesse <code>eelnebOeldis</code> ja <code>jargnebOeldis</code>, et neid oleks v6imalik
	 *   hiljem uuesti kasutada.
	 *    
	 *   @author Siim Orasmaa
	 */
	private class EelnevadJargnevadOeldised {
		
		/**
		 *  Tabel, milles osalausepiirile (antud kui tokeni indeks osalauses) 
		 *  vastab sellele eelneva 8eldise indeks (v6i -1, kui 8eldist ei eelne);
		 */
		private HashMap<Integer, Integer> eelnebOeldis;
		/**
		 *  Tabel, milles osalausepiirile (antud kui tokeni indeks osalauses)
		 *  vastab sellele jargneva 8eldise indeks (v6i -1, kui 8eldist ei jargne);
		 */
		private HashMap<Integer, Integer> jargnebOeldis;
		
		/**
		 *   Teeb iga (kirjeldusele vastava) osalausepiiri juures kindlaks, kas sellele eelneb v6i j2rgneb 8eldis
		 *   ning salvestab tulemused hilisemaks kasutamiseks klassi all olevatesse paisktabelitesse
		 *   (<code>eelnebOeldis</code> ja <code>jargnebOeldis</code>);<br>
		 *   <br>
		 *   Osalausepiiri t2psem kirjeldus tuleb s6namalli kujul ette anda (muutujas <code>otsitavpiir</code>).<br>
		 *   <br>
		 *   Kui <code>muudaOletatavadKindlaks == true</code>, siis muudetakse k6ik (kirjeldusele vastanud) oletatavad 
		 *   piirid, mille m6lemal pool on 8eldis, kindlateks osalausepiirideks.
		 */
		public EelnevadJargnevadOeldised(List<OsalauSona> osalause, SonaMall otsitavpiir, boolean muudaOletatavadKindlaks){
			this.eelnebOeldis  = new HashMap<Integer, Integer>();
			this.jargnebOeldis = new HashMap<Integer, Integer>();
			leiaOtsitavatePiirideEelnevadJargnevadOeldised(osalause, otsitavpiir, muudaOletatavadKindlaks);
		}
		
		/**
		 *   Teeb iga (kirjeldusele vastava) osalausepiiri juures kindlaks, kas sellele eelneb v6i j2rgneb 8eldis
		 *   ning salvestab tulemused hilisemaks kasutamiseks klassi all olevatesse paisktabelitesse
		 *   (<code>eelnebOeldis</code> ja <code>jargnebOeldis</code>);<br>
		 *   <br>
		 *   Osalausepiiri t2psem kirjeldus tuleb s6namalli kujul ette anda (muutujas <code>otsitavpiir</code>).<br>
		 *   <br>
		 *   Kui <code>muudaOletatavadKindlaks == true</code>, siis muudetakse k6ik (kirjeldusele vastanud) oletatavad 
		 *   piirid, mille m6lemal pool on 8eldis, kindlateks osalausepiirideks.
		 */
		public void leiaOtsitavatePiirideEelnevadJargnevadOeldised(List<OsalauSona> osalause, SonaMall otsitavpiir, boolean muudaOletatavadKindlaks){
			SonaMall oeldis = new SonaMall( MARGEND.OELDIS );
			try {
				for (int i = 0; i < osalause.size(); i++) {
					OsalauSona sona = osalause.get(i);
					//
					//  0.  teeme iga oletatava piiri juures kindlaks, kas sellele eelneb v6i j2rgneb 8eldis   
					//      salvestame tulemused hilisemaks kasutamiseks paisktabelisse;
					//
					if (otsitavpiir.vastabMallileOR(sona)){
						int eelnevOeldis  = TekstiFiltreerimine.eelnebMargendigaSona(osalause,  i, oeldis, otsitavpiir);
						int jargnevOeldis = TekstiFiltreerimine.jargnebMargendigaSona(osalause, i, oeldis, otsitavpiir);
						if (jargnevOeldis == -1){
							//  Nyanss:
							// V6ib juhtuda, et j2rgnevat 8eldist ei leita, kuna 8eldise kyljes on ka oletatava piiri 
							// m2rk ning meetod jargnebMargendigaSona kontrollib enne keelutingimust kui n6utud tingimust, 
							// ning seega tagastab -1;
							// Veendumaks, et tegu polnud ylalkirjeldatud juhuga, kontrollime igaks juhuks uuesti 
							// j2rgnevaid s6nu, aga seekord ilma kitsendusteta:
							int jargnevOeldis2  = TekstiFiltreerimine.jargnebMargendigaSona(osalause, i, oeldis, null);
							int jargnevOletatav = TekstiFiltreerimine.jargnebMargendigaSonaOR(osalause, i, otsitavpiir, null);
							if (jargnevOeldis2 > -1 && jargnevOeldis2 == jargnevOletatav){
								// Kui ongi nii, et oeldise ja oletatava piiri token langevad kokku (st sisuliselt
								// on oeldise taga oletatav piir), loeme selle juhuks, kus ka jargneb oeldis
								jargnevOeldis = jargnevOeldis2;
							}
						}
						if (eelnevOeldis == -1){
							//  Nyanss:
							// kui ainult j2rgneb oeldis, kontrollime igaks juhuks, ega see s6na pole ise 8eldis -
							// kui on, siis saame ka m2rkida, et tegemist on n8 eelneva 8eldisega ...
							if (sona.omabMargendit(MARGEND.OELDIS)){
								eelnevOeldis = i; 
							}
						}
						(this.eelnebOeldis).put(i, eelnevOeldis);
						(this.jargnebOeldis).put(i, jargnevOeldis);
						if (muudaOletatavadKindlaks && sona.omabMargendit(MARGEND.OLETATAV_PIIR)){
							//
							//   Kui m6lemal pool oletuslikku piiri on keskmeks sobiv verb, muudame piiri kindlaks;
							//
							if (eelnevOeldis > -1 && jargnevOeldis > -1){
								sona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
		public int getEelnevaOeldiseIndeks(Integer indeksOsalauses){
			if (this.eelnebOeldis != null && (this.eelnebOeldis).containsKey(indeksOsalauses)){
				return (this.eelnebOeldis).get(indeksOsalauses);
			}
			return -1;
		}
		
		public int getJargnevaOeldiseIndeks(Integer indeksOsalauses){
			if (this.jargnebOeldis != null && (this.jargnebOeldis).containsKey(indeksOsalauses)){
				return (this.jargnebOeldis).get(indeksOsalauses);
			}
			return -1;
		}
	}
	
	/**
	 *   Oletuslike piiride eemaldamine #2:<br>
	 *   ** kui m6lemal pool oletuslikku osalausepiiri on liitlause keskmeks sobiv verb, m2rgendatakse piir kindlana;<br>
	 *   ** lisaks yritatakse m6istatada juhte, kus ainult yhel pool oletuslikku piiri on keskmeks sobiv verb ning teisel
	 *      pool verb puudub, Nt. kui "ja" ees on oeldis, aga järel pole, ning j2rgneva "ning" järel on öeldis, siis "ning" 
	 *      järel on kindel piir; nagu lauses "Saabusime laevade ja rongidega ning läksime lennukitega." <br>
	 */
	private void eemaldaOletuslikudPiirid2(List<List<OsalauSona>> tykeldus) {
		SonaMall oeldis       = new SonaMall( MARGEND.OELDIS );
		SonaMall oletatavpiir = new SonaMall( MARGEND.OLETATAV_PIIR );
		SonaMall jaMall       = new SonaMall( Pattern.compile("^ja$") );
		SonaMall ningMall     = new SonaMall( Pattern.compile("^ning$") );
		SonaMall komagaS6na   = new SonaMall( Pattern.compile("^.*,$") );
		SonaMall sisaldabT2hteNumbrit = new SonaMall( Pattern.compile("^.*\\p{Alnum}.*$") );
		SonaMall jaNingEgaV6iMall     = new SonaMall( Pattern.compile("^(ja|ning|ega|v\u014Di|v\u00F5i)$") );
		SonaMall oletatavJaKindel     = new SonaMall( MARGEND.OLETATAV_PIIR );
		oletatavJaKindel.lisaVajalikMargend( MARGEND.KINDEL_PIIR );
		try {
			// Tootleme sisendit seni eraldatud osalausete kaupa
			for (List<OsalauSona> osalause : tykeldus) {
				//
				//  1.  teeme iga oletatava piiri juures kindlaks, kas sellele eelneb v6i j2rgneb 8eldis   
				//      salvestame tulemused hilisemaks kasutamiseks paisktabelisse;
				//      kui m6lemal pool oletuslikku piiri on 8eldis, muudame piiri kindlaks;
				//
				EelnevadJargnevadOeldised eelnevadJargnevadOeldised = 
						new EelnevadJargnevadOeldised(osalause, new SonaMall( MARGEND.OLETATAV_PIIR ), true );
				for (int i = 0; i < osalause.size(); i++) {
					OsalauSona sona = osalause.get(i);
					if (oletatavpiir.vastabMallileAND(sona)){
						int eelnevOeldis  = eelnevadJargnevadOeldised.getEelnevaOeldiseIndeks(i);
						int jargnevOeldis = eelnevadJargnevadOeldised.getJargnevaOeldiseIndeks(i);
						//
						//  2. kui "ja" ees on oeldis, aga järel pole, ning j2rgneva "ning" järel on öeldis, siis "ning" 
						//     järel on kindel piir;
						//     Nt. "Saabusime laevade ja rongidega ning l2ksime lennukitega."
						//
						if (jaMall.vastabMallileAND(sona)){
							if (eelnevOeldis > -1 && jargnevOeldis == -1){
								int jargnevNing = TekstiFiltreerimine.jargnebMargendigaSona(osalause, i, ningMall, null);
								int jargnevOLP  = TekstiFiltreerimine.jargnebMargendigaSonaOR(osalause, i, oletatavJaKindel, null);
								if (jargnevNing > -1 && (jargnevOLP == -1 || jargnevOLP >= jargnevNing)){
									int ningEelnevOeldis  = eelnevadJargnevadOeldised.getEelnevaOeldiseIndeks(jargnevNing);
									int ningJargnevOeldis = eelnevadJargnevadOeldised.getJargnevaOeldiseIndeks(jargnevNing);
									if (ningEelnevOeldis == -1 && ningJargnevOeldis > -1){
										(osalause.get(jargnevNing)).asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
									}
								}
							}							
						}
					}
					if (oletatavpiir.vastabMallileAND(sona)){
						int eelnevOeldis  = eelnevadJargnevadOeldised.getEelnevaOeldiseIndeks(i);
						int jargnevOeldis = eelnevadJargnevadOeldised.getJargnevaOeldiseIndeks(i);
						//
						//  3. kui "ja/ning" j2rel on vahetult 8eldis, aga ees pole, m2rgi kindlaks osalausepiiriks;
						//
						if (jaMall.vastabMallileAND(sona) || ningMall.vastabMallileAND(sona)){
							if (eelnevOeldis == -1 && jargnevOeldis == i+1){
								//
								//   Kontrollime, et 'ja/ning'-ile ikkagi eelneks midagi, ning et see midagi
								//  oleks ikkagi suure t6en2osusega ka s6na, Nt ei tohiks kindlat piiri m2rkida
								//  lausetesse:
								//        Siis läheks ( ja läkski ) see tervikuna maksma miljoneid .
								//        ... ja teatas , et ole hea , Paabo astu kõrvale .
								//
								if (i-1 > -1 && sisaldabT2hteNumbrit.vastabMallileAND(osalause.get(i-1))){
									(osalause.get(i)).asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);									
								}
							}
						}						
					}
					if (oletatavpiir.vastabMallileAND(sona)){
						int eelnevOeldis  = eelnevadJargnevadOeldised.getEelnevaOeldiseIndeks(i);
						int jargnevOeldis = eelnevadJargnevadOeldised.getJargnevaOeldiseIndeks(i);
						//
						//  4. kui ebakindla piiri j2rel on ases6na ja 8eldis, ning kusagil kaugemal eespool
						//     on siiski 8eldis, m2rgi piir kindlaks;
						//
						if (eelnevOeldis == -1 && jargnevOeldis == i+2){
							// Kontrollime, et j2rgneb ases6na:
							MorfTunnusteHulk j2rgmSonaTunnused = new MorfTunnusteHulk( osalause.get(i+1) );
							if (!(j2rgmSonaTunnused.filterByPOS("_P_")).isEmpty()){
								//  Tegemist on 8eldisele eelneva ases6naga - nyyd leiame, kas kusagil kaugemal
								// eespool siiski on 8eldis:
								int eelnebOeldisKusagil = TekstiFiltreerimine.eelnebMargendigaSona(osalause, i, oeldis, null);
								if (eelnebOeldisKusagil > -1){
									(osalause.get(i)).asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);									
								}
							}
						}
					}
					if (oletatavpiir.vastabMallileAND(sona)){
						//
						//  5. kui 'ja|ning|ega|või' järel on mingi sõna ja selle järel ',' (mis pole kindel piir) siis
						//     on ilmselt on tegu sidesõnaga ja sel juhul 'ja|ning|ega|või' järel pole piir 
						//
						if (jaNingEgaV6iMall.vastabMallileAND(sona)){
							if (i+2 < osalause.size() && komagaS6na.vastabMallileAND( osalause.get(i+2) ) 
									&& !(osalause.get(i+2)).omabMargendit(MARGEND.KINDEL_PIIR)){
								(osalause.get(i)).eemaldaMargend( MARGEND.OLETATAV_PIIR );
							}
							// Juhuks, kui j2rgnev s6na on selline, et koma on kleepunud otse tema l6ppu
							if (i+1 < osalause.size() && komagaS6na.vastabMallileAND( osalause.get(i+1) ) 
									&& !(osalause.get(i+1)).omabMargendit(MARGEND.KINDEL_PIIR)){
								(osalause.get(i)).eemaldaMargend( MARGEND.OLETATAV_PIIR );
							}
						}
					}
					/// 
					///   kui lause või osalause algab 'kui' v6i 'et'-iga:
					///   TODO: v6imalik, et alljärgnevat loogikat saab veel refaktoreerida 
					///
					if (i == 0){
						MorfTunnusteHulk tunnused = new MorfTunnusteHulk( osalause.get(i) );
						int jargnebKomaga = TekstiFiltreerimine.jargnebMargendigaSona(osalause, i, komagaS6na, null);
						if (!(tunnused.filterByLemma("kui")).isEmpty()){
							if (jargnebKomaga > -1 && jargnebKomaga+1 < osalause.size()){
								//
								//   6. (osa)lausealgulisele "kui"-ile j2rgneb oletatava osalausepiiriga ',' millele vahetult 
								//      j2rgneb 8eldis (v6i verb), ning "kui" ja ',' vahel pole yhtegi teist osalausepiiri 
								//      m2rgendit, tuleb koma m2rkida kui kindel piir ...
								//
								if ( (osalause.get(jargnebKomaga)).omabMargendit( MARGEND.OLETATAV_PIIR ) ){
									MorfTunnusteHulk j2rgmSonaTunnused = new MorfTunnusteHulk( osalause.get(jargnebKomaga+1) );
									if (!(j2rgmSonaTunnused.filterByPOS("_V_")).isEmpty()){
										// Kontrollime, et 'kui' ja ',' vahele ei j22ks teisi lausepiirimargendeid
										if (TekstiFiltreerimine.eelnebMargendigaSonaOR(osalause, jargnebKomaga, oletatavJaKindel, null) == -1){
											(osalause.get(jargnebKomaga)).asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
											//
											//  Probleem: vaieldav, kas järgmises lauses tuleks "kui mitte ," järgi panna KINDEL_PIIR:  
											//    " Kui viiakse peavahti , on kõik läbi , kui mitte , on veel mingi ähmane lootus , " mõtlesin endamisi .
											//    (HJK programmis millegipärast ei panda, kuigi tegu võib olla ka programmiveaga)
											//
										}
									}
								}								
							}
							if (jargnebKomaga > -1 && jargnebKomaga+1 < osalause.size()){
								//
								//   7. (osa)lausealgulisele "kui"-ile j2rgneb oletatava osalausepiiriga ',' mille j2rel ja ees
								//      (mitte tingimata vahetult) on 8eldis (v6i verb), ning "kui" ja ',' vahel pole yhtegi teist 
								//      osalausepiiri m2rgendit, tuleb koma m2rkida kui kindel piir ...
								//
								if ( (osalause.get(jargnebKomaga)).omabMargendit( MARGEND.OLETATAV_PIIR ) ){
									// Kontrollime, et 'kui' ja ',' vahele ei j22ks teisi lausepiirimargendeid
									if (TekstiFiltreerimine.eelnebMargendigaSonaOR(osalause, jargnebKomaga, oletatavJaKindel, null) == -1){
										// Kontrollime, et 'kui' ja ',' vahel on verb v6i 8eldis
										boolean verbLeitud = false;
										for (int j = i+1; j <= jargnebKomaga; j++) {
											MorfTunnusteHulk sonaTunnused = new MorfTunnusteHulk( osalause.get(j) );
											if (!(sonaTunnused.filterByPOS("_V_")).isEmpty()){
												verbLeitud = true;
												break;
											}
										}
										if (verbLeitud){
											boolean teineVerbLeitud = false;
											// Kontrollime, et ','-le j2rgneb 8eldis
											for (int j = jargnebKomaga+1; j < osalause.size(); j++) {
												if (oletatavJaKindel.vastabMallileOR( osalause.get(j) )){
													break;
												}
												MorfTunnusteHulk sonaTunnused = new MorfTunnusteHulk( osalause.get(j) );
												if (!(sonaTunnused.filterByPOS("_V_")).isEmpty()){
													teineVerbLeitud = true;
													break;
												}
											}
											if (teineVerbLeitud){
												(osalause.get(jargnebKomaga)).asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
											}
										}
									}
								}
							}
						}
						if (!(tunnused.filterByLemma("et")).isEmpty()){
							if (jargnebKomaga > -1 && jargnebKomaga+1 < osalause.size() && 
									(osalause.get(jargnebKomaga)).omabMargendit( MARGEND.OLETATAV_PIIR ) ){
								//
								//  8. , 9. (osa)lausealgulisele "et"-ile j2rgneb oletatava osalausepiiriga ',' 
								//     mille j2rel on kindel 8eldis, aga ees v6ib olla nii 8eldis kui lihtsalt
								//     verb, ning "et" ja ',' vahel pole yhtegi teist osalausepiiri m2rgendit, 
								//     tuleb koma m2rkida kui kindel piir ...
								//
								//     Nt. 'Et arvata midagi, tuleb kõigepealt teada.'
								//
								if (TekstiFiltreerimine.jargnebMargendigaSona(osalause, jargnebKomaga, oeldis, oletatavJaKindel) > -1){
									// Kontrollime, et 'et' ja ',' vahel on verb v6i 8eldis
									boolean verbLeitud = false;
									for (int j = i+1; j <= jargnebKomaga; j++) {
										MorfTunnusteHulk sonaTunnused = new MorfTunnusteHulk( osalause.get(j) );
										if (!(sonaTunnused.filterByPOS("_V_")).isEmpty()){
											verbLeitud = true;
											break;
										}
									}
									if (verbLeitud){
										(osalause.get(jargnebKomaga)).asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
									}
								} else if ((osalause.get(jargnebKomaga-1)).omabMargendit(MARGEND.OELDIS)){
									// 
									//   10. ... ja kui 8eldis eelneb vahetult komale, ning j2rgneb verb, m2rgime koma kui
									//       osalausepiiri ...
									//
									boolean teineVerbLeitud = false;
									// Kontrollime, et ','-le j2rgneb 8eldis
									for (int j = jargnebKomaga+1; j < osalause.size(); j++) {
										if (oletatavJaKindel.vastabMallileOR( osalause.get(j) )){
											break;
										}
										MorfTunnusteHulk sonaTunnused = new MorfTunnusteHulk( osalause.get(j) );
										if (!(sonaTunnused.filterByPOS("_V_")).isEmpty()){
											teineVerbLeitud = true;
											break;
										}
									}
									if (teineVerbLeitud){
										(osalause.get(jargnebKomaga)).asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
									}
								}
							}
						}
					}
					if (oletatavpiir.vastabMallileAND(sona) && komagaS6na.vastabMallileAND(sona)){
						int eelnevOeldis  = eelnevadJargnevadOeldised.getEelnevaOeldiseIndeks(i);
						int jargnevOeldis = eelnevadJargnevadOeldised.getJargnevaOeldiseIndeks(i);
						if (eelnevOeldis == -1 && jargnevOeldis == i+1){
							//  
							//   11. kui komale ei eelne 8eldis, kyll aga on 8eldis kusagil kaugemal 
							//       eespool (osalause alguses) ning 8eldis j2rgneb ka vahetult,
							//       m2rgi komakoht kindlaks piiriks ...
							//
							//   Nt. Ja Karlsson, kes jooksis suure hooga, tõtanud sillast mööda, hüppaski vette.
							//
							if (TekstiFiltreerimine.eelnebMargendigaSona(osalause, i, oeldis, null) > -1){
								(osalause.get(i)).asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
							}
						}
					}
				}
				//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(osalause, false) );
			}				
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 *   Loetelude eemaldamine:<br>
	 *   ** Eemaldame oletuslikke osalausepiire, kui need paistavad olevat loetelu elementide vahel (
	 *   ehk m6lemal pool oletuslikku piiri on samas k22ndes/p88rdes s6na ); <br>
	 *   ** Kui morfoloogilised analyysid m6lemal pool osalausepiiri on mitmesed, v6rreldakse ainult
	 *   esimesi analyyse;
	 */
	private void eemaldaLoetelud(List<List<OsalauSona>> tykeldus) {
		SonaMall oletatavpiir = new SonaMall( MARGEND.OLETATAV_PIIR );
		try {
			for (List<OsalauSona> osalause : tykeldus) {
				for (int i = 0; i < osalause.size(); i++) {
					OsalauSona sona = osalause.get(i);
					if (oletatavpiir.vastabMallileAND(sona)){
						String sidend = "";
						if (sona.getNormAlgSona() != null ){
							sidend = sona.getNormAlgSona();
						}
						MorfTunnusteHulk esimeseSonaTunnused = null;
						MorfTunnusteHulk teiseSonaTunnused   = null;
						//
						//    Leiame k22nd/p88rds6nad m6lemalt poolt osalausepiiri 
						//
						MorfTunnusteHulk sonaTunnused = new MorfTunnusteHulk(sona);
						if ((sonaTunnused.filterByPOS("_Z_").isEmpty()) && (sonaTunnused.filterByPOS("_J_").isEmpty())){
							//  Kui piir jookseb p66rduva v6i k22nduva s6na kohalt (nt koma s6na l6pus)
							esimeseSonaTunnused = sonaTunnused;
						} else if (i - 1 > -1){
							//  Kui piir jookseb p66rduva v6i k22nduva s6na j2relt (nt sides6na s6na j2rel)
							esimeseSonaTunnused = new MorfTunnusteHulk( osalause.get(i-1) );
						}
						if (i + 1 < osalause.size()){
							teiseSonaTunnused = new MorfTunnusteHulk( osalause.get(i+1) );
						}
						// 
						//    Kui m6lemal pool piiri on yhesuguses k22ndes/pöördes s6na, on potentsiaalselt tegu 
						///  loeteluga ning v6ime oletatava osalausepiiri eemaldada;
						//
						if ( esimeseSonaTunnused != null && teiseSonaTunnused != null ){
							if (esimeseSonaTunnused.formNamesFromFirstAnalysesMatch( teiseSonaTunnused )){
								//
								//  kontrollime, et sidendiks ei oleks m6ttekriips (kui on, siis osalausepiiri ei kustuta)
								//
								if (!sidend.matches("^\\s*(-{1,}|\u2212|\uFF0D|\u02D7|\uFE63|\u002D|\u2010|\u2011|\u2012|\u2013|\u2014|\u2015)\\s*$")){
									sona.eemaldaMargend( MARGEND.OLETATAV_PIIR );
								}
							}
						}
					}
				}
				//System.out.println( ""+TekstiFiltreerimine.debugMargendustegaLause(osalause, false) );
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	/**
	 *   Osalausete teisendamine kiiludeks;
	 */
	private void teisendaKiiludeks(List<List<OsalauSona>> laused, boolean eemaldaLauseL6pustKindelPiir) {
		try{
			SonaMall koikpiirid = new SonaMall( MARGEND.KINDEL_PIIR );
			koikpiirid.lisaVajalikMargend( MARGEND.KIILU_ALGUS );
			koikpiirid.lisaVajalikMargend( MARGEND.KIILU_LOPP );
			koikpiirid.lisaVajalikMargend( MARGEND.OLETATAV_PIIR );
			SonaMall kindladpiirid = new SonaMall( MARGEND.KINDEL_PIIR );
			kindladpiirid.lisaVajalikMargend( MARGEND.KIILU_ALGUS );
			kindladpiirid.lisaVajalikMargend( MARGEND.KIILU_LOPP );
			SonaMall kindelpiir = new SonaMall( MARGEND.KINDEL_PIIR );
			SonaMall komagaS6na = new SonaMall( Pattern.compile("^.*,$") );
			SonaMall ainultKomaga = new SonaMall( Pattern.compile("^,+$") );
			SonaMall kusKustKuhuMillal = new SonaMall( Pattern.compile("^(kus|kust|kuhu|millal)$") );
			for (List<OsalauSona> lause : laused) {
				//
				//  Leiame lausesiseselt k6igile piiridele eelnevad ja j2rgnevad 8eldised
				//
				EelnevadJargnevadOeldised eelJargOeldised = new EelnevadJargnevadOeldised(lause, koikpiirid, false );
				for (int i = 0; i < lause.size(); i++) {
					OsalauSona sona = lause.get(i);
					if (kindelpiir.vastabMallileAND(sona)){
						int eelnevOeldis  = eelJargOeldised.getEelnevaOeldiseIndeks(i);
						int jargnevOeldis = eelJargOeldised.getJargnevaOeldiseIndeks(i);
						if (eelnevOeldis == -1 && jargnevOeldis > i){
							//
							//   # 1) kui algul on öeldiseta jupp, järgneb komade vahel mis/kes/et (vastavate lemmadega) 
						    //       öeldisega jupp, ning pärast koma tuleb samuti öeldisega jupp, siis märgi komade-
						    //       vaheline jupp kiiluks.
							//        Nt.  Mees, keda nägin, tundus tuttav.
							//
							MorfTunnusteHulk jargmSonaTunnused = new MorfTunnusteHulk( lause.get( i+1 ) );
							if (!(jargmSonaTunnused.filterByLemmaRegExp("^(mis|kes|et)$")).isEmpty()){
								// leiame selle osalause l6pu
								int j2rgmKindel = TekstiFiltreerimine.jargnebMargendigaSona(lause, i+1, kindelpiir, null);
								int j2rgmPiir = TekstiFiltreerimine.jargnebMargendigaSonaOR(lause, i+1, koikpiirid, null);
								if (j2rgmKindel > i+1 && j2rgmPiir == j2rgmKindel && komagaS6na.vastabMallileAND( lause.get(j2rgmKindel) )){
									int jargnevOeldis2 = eelJargOeldised.getJargnevaOeldiseIndeks(j2rgmKindel);
									//   Kui j2rgmine 8eldis j2rgneb vahetult j2rgmisele kindlale piirile, muudame 
									//  komadevahelise osa kiiluks ...
									if (jargnevOeldis2 == j2rgmKindel + 1){
										if (ainultKomaga.vastabMallileAND(sona)){
											// Kui koma oli esimesest s6nast eraldi, muudame komal kindla piiri kiilualguseks											
											sona.asendaMargend(MARGEND.KINDEL_PIIR, MARGEND.KIILU_ALGUS);
										} else {
											// Kui esimese s6na l6pus oli vahetult koma, tuleb kiilualgus p2rast s6na.
											sona.eemaldaMargend(MARGEND.KINDEL_PIIR);
											(lause.get(i+1)).lisaMargend(MARGEND.KIILU_ALGUS);
											//
											// NB! P2ris ilus see pole, kuna yhe osalause kylge j22b teise osalause
											// kirjavahem2rk, nt:
											//       "Mees, keda nägin, tundus tuttav." => "Mees, tundus tuttav"
											// aga paremat lahendust praegu ei ole, kuna tokeniseeringut muutma ei hakka;
											//
										}
										// Muudame j2rgmise kindla piiri m2rgendi kiilul6puks
										(lause.get(j2rgmKindel)).asendaMargend(MARGEND.KINDEL_PIIR, MARGEND.KIILU_LOPP);
										//
										//  NB! V6imalikud probleemsed kohad, kus see reegel ei t88ta:
										//  ** kui kiilu sees on veel teisi kiile v6i oletatavaid/kindlaid
										//     osalauseid, nt:
										//        See, keda (või mida) nägin, tundus tuttav.
										//        See, keda või mida nägin, tundus tuttav.
										//
										//  ** kui lause alguspoolest ei leita 8eldist yles, kuna see on oletatava
										//     piiri taga, siis eraldatakse kiiluna seal, kus ei tohiks, nt:
										//        Kas aluseks on laulupeod või tõdemus, et kui eestlasel midagi muud teha pole, hakkab ta laulma ? 
										//
									}
								}
							}
							//
							//   # 2) kui algul on öeldiseta jupp, järgneb komade vahel kus/kust/kuhu/millal (vastavate 
							//        sõnavormidega) öeldisega jupp, ning pärast koma tuleb samuti öeldisega jupp, siis 
							//        märgi komade-vaheline jupp kiiluks.
							//            Nt.  Lennujaam, kus õnnetus toimus, on praegu suletud.
							//
							if (kusKustKuhuMillal.vastabMallileAND(lause.get(i+1))){
								// leiame selle osalause l6pu
								int j2rgmKindel = TekstiFiltreerimine.jargnebMargendigaSona(lause, i+1, kindelpiir, null);
								int j2rgmPiir = TekstiFiltreerimine.jargnebMargendigaSonaOR(lause, i+1, koikpiirid, null);
								if (j2rgmKindel > i+1 && j2rgmPiir == j2rgmKindel && komagaS6na.vastabMallileAND( lause.get(j2rgmKindel) )){
									int jargnevOeldis2 = eelJargOeldised.getJargnevaOeldiseIndeks(j2rgmKindel);
									//   Kui j2rgmine 8eldis j2rgneb vahetult j2rgmisele kindlale piirile, muudame 
									//  komadevahelise osa kiiluks ...
									if (jargnevOeldis2 == j2rgmKindel + 1){
										if (ainultKomaga.vastabMallileAND(sona)){
											// Kui koma oli esimesest s6nast eraldi, muudame komal kindla piiri kiilualguseks
											sona.asendaMargend(MARGEND.KINDEL_PIIR, MARGEND.KIILU_ALGUS);
										} else {
											// Kui esimese s6na l6pus oli vahetult koma, tuleb kiilualgus p2rast s6na.
											sona.eemaldaMargend(MARGEND.KINDEL_PIIR);
											(lause.get(i+1)).lisaMargend(MARGEND.KIILU_ALGUS);
										}
										// Muudame j2rgmise kindla piiri m2rgendi kiilul6puks
										(lause.get(j2rgmKindel)).asendaMargend(MARGEND.KINDEL_PIIR, MARGEND.KIILU_LOPP);
									}
								}
							}
							//
							//  TODO: Tingimustel 1) ja 2) kontrollimisel suur osa sisust praktiliselt identne, 
							//        st saaks refaktoreerida; 
							//
						}
					}
					//
					//   Eemaldame lause viimase s6na l6pust kindla piiri.
					//   Seda l2heb t6en2oliselt tarvis siis, kui lausestamisel on mingi segadus tekkinud ja 
					//   on lausepiir t6mmatud kohtadesse, kus t6en2oliselt peaks olema ka osalausepiir, nt
					//   kooloni/jutum2rkide j2rele:
					//        Õhtul vahest saab : " 
					//        Käepigistus oli päris soe : "
					//        " Ma praen kõik margariiniga : "
					//
					if (i == lause.size()-1 && eemaldaLauseL6pustKindelPiir && kindelpiir.vastabMallileAND(sona)){
						sona.eemaldaMargend(MARGEND.KINDEL_PIIR);
					}
				}
				//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(lause, false) );
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
}
