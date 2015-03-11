package ee.ut.soras.osalau;

import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import ee.ut.soras.osalau.OsalauSona.MARGEND;
import ee.ut.soras.wrappers.mudel.MorfAnRida;

/**
 *   Heuristikud puuduvate komade suhtes v&auml;hemtundlikuks osalausestamiseks. 
 *   Enamasti m&auml;&auml;ravad osalausepiirid k&otilde;iksugu koma-n&otilde;udvate-sides&otilde;nade (et, kui, millal jms) 
 *   j&auml;rgi kontekstidesse, kus m&otilde;lemale poole koma-n&otilde;udvat-sides&otilde;na j&auml;&auml;b osalause 
 *   keskmeks sobiv verb.
 *   
 *   @author Siim Orasmaa
 */
public class PuuduvateKomadeHeur {

	private static SonaMall agaKuigi     = new SonaMall( Pattern.compile("^(aga|kuigi)$") );
	private static SonaMall ent          = new SonaMall( Pattern.compile("^(ent)$", Pattern.CASE_INSENSITIVE) );
	private static SonaMall vaid         = new SonaMall( Pattern.compile("^(vaid)$", Pattern.CASE_INSENSITIVE) );
	//  Ysna sage ja tavaliselt osalausepiir: 'et'
	private static SonaMall et           = new SonaMall( Pattern.compile("^(et)$") );
	private static SonaMall etEelnev     = new SonaMall( Pattern.compile("^(olgugi|ainult|vaevalt|peaasi|ilma|mitte|arvesta(des|mata))$", 
																		Pattern.CASE_INSENSITIVE) );
	private static SonaMall vaata        = new SonaMall( Pattern.compile("^(vaata)$", Pattern.CASE_INSENSITIVE) );
	//  V2hemsagedased, aga v6iksid pigem olla osalausepiirid: sest, kuid, kuni, ehkki
	private static SonaMall sestKuidJne  = new SonaMall( Pattern.compile("^(sest|kuid|kuni|ehkki)$") );
	private static SonaMall alates       = new SonaMall( Pattern.compile("^(alates)$", Pattern.CASE_INSENSITIVE) );
	//  S6nad, mis v6ivad olla nii kysis6nad kui ka osalausepiirid: millal, kus, kuhu, kust, kuna, kuidas, kas
	private static SonaMall millalKusJne = new SonaMall( Pattern.compile("^(millal|kus|kuhu|kust|kuna|kuidas|kas|miks)$") );
	private static SonaMall kuidVaidJne  = new SonaMall( Pattern.compile("^(kuid|vaid|aga|ent|et)$") );
	//  S6na, mis v6ib olla nii m22rs6na kui ka sides6na:   siis
	private static SonaMall siis         = new SonaMall( Pattern.compile("^(siis)$", Pattern.CASE_INSENSITIVE) );
	private static SonaMall kui          = new SonaMall( Pattern.compile("^(kui)$", Pattern.CASE_INSENSITIVE) );
	private static SonaMall ehk          = new SonaMall( Pattern.compile("^(ehk)$", Pattern.CASE_INSENSITIVE) );
	//  S6na, mis v6ib olla nii v6rdluss6na kui ka sides6na:   nagu
	private static SonaMall nagu         = new SonaMall( Pattern.compile("^nagu$", Pattern.CASE_INSENSITIVE) );
	private static SonaMall naguEelnev   = new SonaMall( Pattern.compile("^(nii|selli(ne|st|sei?d|st?eks|st?ena))$", 
																		Pattern.CASE_INSENSITIVE) );
	//  S6na, mis v6ib olla nii v6rdluss6na kui ka sides6na:   kui
	private static SonaMall kuiEelnev1   = new SonaMall( Pattern.compile("^(juhu(l|ks)|juhtudel)$", Pattern.CASE_INSENSITIVE) );
	private static SonaMall kuiEelnev2   = new SonaMall( Pattern.compile("^(enne|aja(l|ks|st|ga|ni)|"+
																		"aeg(a|ade(l|ks|st|ga|ni))|"+
																		"(hetk|moment)|(hetke|momendi)(l|st|ni|ks|st))|"+
																		"(see|seda|selle(le|st|ni|ks))$", 
																		Pattern.CASE_INSENSITIVE) );
	//  S6nad:  kusjuures, seejuures, sealjuures, otsekui, justkui
	private static SonaMall kusjuuresJne  = new SonaMall( Pattern.compile("^(kus|see|seal)juures|(otse|just)kui$") );
	
	
	/**
	 *   Pyyame leida kindlaid osalausepiire puuduvate kirjavahem2rkide (eelk6ige puuduva koma) tingimustes,
	 *   arvestades ainult sidendeid/sides6nu, osalausete keskseid verbe ja veel m6ningaid kontekstitunnuseid;
	 *   Sisuliselt proovime tegeleda k6igi nende juhtudega, millega tegeletakse meetodis 
	 *   <code>Osalausestaja.lisaKindladPiirid1()</code>
	 *   :<br>
	 *   <ul>
	 *   <li>  'aga|kuigi|ent|vaid', millele järgneb ja eelneb öeldis;  </li>
	 *   <li>  'et' millele j2rgneb ja eelneb 8eldis;  </li>
	 *   <li>  'sest|kuid|ehkki|kuni' millele j2rgneb ja eelneb 8eldis;  </li>
	 *   <li>  'millal|kus|kuhu|kust|kuna|kuidas|kas|miks' millele j2rgneb ja eelneb 8eldis;  </li>
	 *   <li>  'siis' millele j2rgneb ja eelneb 8eldis;  </li>
	 *   <li>  'nii-nagu' millele j2rgneb ja eelneb 8eldis;  </li>
	 *   <li>  konstruktsioonid 'kui'-ga, kuis 8eldise eelnevust/j2rgnevust ei n6ua: juhul-kui;  </li>
	 *   <li>  konstruktsioonid 'kui'-ga, kus peaks eelnema/j2rgnema 8eldis: enne-kui, ajal-kui, hetkel-kui, seda-kui;  </li>
	 *   <li>  lemma 'mis|mis_*sugune|milline|kes', millele eelneb ja j2rgneb 8eldis;  </li>
	 *   <li>  lemma 'mis|mis_*sugune|milline|kes', millele ei eelne, aga j2rgneb 8eldis, nt: see-mis, selline-kes;  </li>
	 *   <li>  'otsekui|justkui|kusjuures|seejuures|sealjuures', millele eelneb ja j2rgneb 8eldis;  </li>
	 *   <li>  kaks k6rvutipaiknevat osalause keskset verbi;  </li>
	 *   </ul>
	 *   NB! Kaesolev lahendus kasutab reeglite rakendamisel ysna naiivset algoritmi: reeglite j2rjekorda sisuliselt ei
	 *   arvestata, j2rjekord kujuneb vastavalt sellele, kuidas satutakse (osa)lauses vasakult paremale liikudes reegli 
	 *   rakendamiseks sobivale kohale. 
	 *   Tegelikult peaks see protsess arvestama ka reeglite j2rjekorda, nt olema mitme-etapiline ning eristama oletatavaid 
	 *   ja kindlaid piire; praegu pyyame lihtsamalt l2bi ajada, tehes eelduse, et tuleb tegeleda vaid yksikute/v2heste 
	 *   komavigadega, mitte systemaatilise ilma-komadeta-kirjutamisega.
	 *   <br>
	 */
	public static void lisaKindladPiirid1IlmaKomata( List<List<OsalauSona>> tykeldus ) {
		SonaMall osalausepiir = new SonaMall( MARGEND.OLETATAV_PIIR );
		osalausepiir.lisaVajalikMargend(MARGEND.KINDEL_PIIR);
		try {
			// Tootleme sisendit osalause-osalause haaval
			for (List<OsalauSona> osalause : tykeldus) {
				for (int i = 0; i < osalause.size(); i++) {
					// Hakkame kitsendama s6naga seotud morf tunnuste hulka
					if ( agaKuigi.vastabMallileAND( osalause.get( i ) ) && i > 0 ){
						//
						// ... 1. aga|kuigi, millele järgneb ja eelneb öeldis:
						//
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, true);
						if (eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
							//
							//     1.1. Kui aga-le järgneb asesõna ja seejärel öeldis, nt:
							//              meie otsustasime aga meie ei vastuta .
							//              See meenutas mulle midagi aga ma ei tea ... 
							//              Aimamisi teadis ta seda aga ta ei tahtnudki kohaneda ajaga ...
							//
							if (i+2< osalause.size()){
								MorfTunnusteHulk j2rgmSonaTunnused1 = new MorfTunnusteHulk( osalause.get(i+1) );
								if ( !(j2rgmSonaTunnused1.filterByPOS("_P_")).isEmpty() && 
									(osalause.get(i+2)).omabMargendit(MARGEND.OELDIS) ){
									OsalauSona eelmineSona = osalause.get(i-1);
									if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
										eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
									} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
										eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
									}
								}
							}
							//
							//     *1.2. Kui aga-le:
							//          *) ei eelne öeldis vahetult (st ei saa olla nt yhend "vaata aga vaata");
							//          *) eelnev öeldis pole olema-verb (st pole "Karu polevat ta aga näinud tea mis ajast saati.");
							//          *) eelnev öeldis pole vad-verb ("Aastapäeva kajastavad telejaamad aga murravad pead , mida ...")
							//          Nt.
							//             Seda juhtumit ei maksa üle tähtsustada aga mingi roll sellel tulevikuks kindlasti on .
							//     
							//
							MorfTunnusteHulk eelnevaOeldiseMorf1 = new MorfTunnusteHulk( osalause.get(eelnebJargnebOeldis[0]) );
							if ((eelnebJargnebOeldis[0] < i-1) && 
								(eelnevaOeldiseMorf1.filterByLemma("ole")).isEmpty() &&
								(eelnevaOeldiseMorf1.filterByFormNames("vad,")).isEmpty()){
								//
								//  NB! See reegel on väga tundlik morf yhestamise ja lausestamise vigade suhtes, nt 
								//      eksitakse lausetes:
								//          "Vast viis minut hiljem / aga kutsuti stjuardessid etteotsa"
								//          "Karistust kergendavad asjaolud / aga etendavad olulist rolli"
								//          "Maal elavad inimesed / aga tajusid etendust pisut vaiksemana"
								//          "Prae hinda / aga salakaup ei alanda."
								//          "kuus meest said kirja / aga võrdse aja 6 : 16.48. “ Seitsmendat kohta ei julenud ..."
								//
								OsalauSona eelmineSona = osalause.get(i-1);
								if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
									eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
								} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
									eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
								}
							}
						}
					}
					if ( ent.vastabMallileAND( osalause.get( i )) && i > 0 ){
						//
						// ... 1.3.  'ent', millele järgneb ja eelneb öeldis:
						//
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, true);
						if (eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
							OsalauSona eelmineSona = osalause.get(i-1);
							if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
								eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
							} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
								eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
							}
						}
					}
					if ( vaid.vastabMallileAND(osalause.get(i)) && i > 0 ){
						//
						// ... 1.4.  'vaid', millele järgneb öeldis ja eelneb eitav öeldis:
						//        Vastupidiselt ootustele ma mitte ei tõusnud vaid vajusin .
						//        Need ei teinud ingellikku tilinat vaid kõrisesid nagu kanapugu .
						//        Isegi kassade juures ei tegelenud inimesed enam ostmisega vaid kuulasid , mida Andreas räägib .
						//        Tol hooajal ei triumfeerinud me ju üksnes OMil vaid võitsime kõik sügisest alates kevadeni välja .
						//        Ajakirjaniku professionaalsus pole mitte paber ülikoolist vaid koosneb vähemalt neljast suurest osast .
						//
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, true);
						if (eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
							// Teeme kindlaks, kas eelnev 8eldis on eitav
							boolean eelnebEitus = false;
							MorfTunnusteHulk eelnevaOeldiseMorf1 = new MorfTunnusteHulk( osalause.get(eelnebJargnebOeldis[0]) );
							eelnevaOeldiseMorf1 = eelnevaOeldiseMorf1.filterByPOS("_V_");
							if (!((eelnevaOeldiseMorf1.filterByLemmaRegExp("^ole$")).filterByFormNames("^neg.*$")).isEmpty()){
								// Kui eelneb 'pole'
								eelnebEitus = true;
							} else if (!(eelnevaOeldiseMorf1.filterByFormNames("^(neg\\so|o|nud|tud).*$")).isEmpty()){
								// Kui eelneb 'ei'-ga yhilduv verb (neid on muidugi rohkem, aga juba meid huvitavad vaid
								// sellised, mida kaesolev systeem kesksete verbidena tuvastab)
								if ( eelnebJargnebOeldis[0] - 1 > -1 ){
									MorfTunnusteHulk eelnevaOeldiseMorf2 = 
											new MorfTunnusteHulk( osalause.get(eelnebJargnebOeldis[0] - 1) );
									if (!eelnevaOeldiseMorf2.filterByLemmaRegExp("^ei$").isEmpty()){
										eelnebEitus = true;
									}
								}
							}
							if ( eelnebEitus ){
								OsalauSona eelmineSona = osalause.get(i-1);
								if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
									eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
								} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
									eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
								}								
							}
						}
					}
					if ( et.vastabMallileAND( osalause.get( i ) ) && i > 0 ){
						//
						//     2.1. 'et' millele j2rgneb ja eelneb 8eldis:
						//            Kobasin siis teed otsides edasi , lootes et kusagil mujal on see ehk lihtsam .
						//            Olgu , ütleme et jõudis .
						//            miks te kohe mõtlete et tegu on needusega ?
						//        Jätame välja "vaata et"-ühendid, kuna need võivad osalause lõhkuda, nt:
						//           ... siis ta vaata et teeb tembu ära .
						//
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, true);
						if (eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1 && 
							!vaata.vastabMallileAND(osalause.get(i-1))){
							OsalauSona eelmineSona = osalause.get(i-1);
							if (etEelnev.vastabMallileAND(eelmineSona) && i-2 > -1){
								eelmineSona = osalause.get(i-2);
							}
							if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
								eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
							} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
								eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
							}
							//
							//   Osad on natuke kaheldavad/vaieldavad - nagu pysiyhendid v6i nii:
							//           Ta sai Egemoni saapaga nii et tolmas .
							//           Eesmärki on mul nii et tapab !
							//           Ja ta rääkis Valentinale augu pähe , veenmisoskust on tal nii et tapab .
							//           ... valetas Sõnnar nii et suu suitses .
							//           ... pea kolksatas vastu konte nii et silmist lendas sädemeid .
							//   Veakohad:
							//           Muuseas ajas mind peaaegu / et muigama Mihkli teade :
							// 
						} else if (eelnebJargnebOeldis[0] == -1 && eelnebJargnebOeldis[1] > -1 && i-1 > -1){
							//
							//     2.2. 'et' millele j2rgneb 8eldis, aga ei eelne:
							//               Selleks et jumalanna leiaks tee meie südameisse , on vaja valgust .
							//               See et aus inimene ei pääse enam viinale ligi , see on rumal jutt .
							//               Ikka , hoolimata sellest et kolmas meistriliiga hooaeg algab .
							//
							boolean eelnebSobivSona = false;
							MorfTunnusteHulk eelnevaSonaMorf = new MorfTunnusteHulk( osalause.get(i-1) );
							if ( !(eelnevaSonaMorf.filterByLemmaRegExp("^(see|selline|(selle|see)_(pärast|tõttu)|võimalik)$")).isEmpty() ){ 
								eelnebSobivSona = true;
							}
							if ( eelnebSobivSona ){
								OsalauSona eelmineSona = osalause.get(i-1);
								if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
									eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
								} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
									eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
								}
							}
						}
					}
					if ( sestKuidJne.vastabMallileAND( osalause.get( i ) )  &&  i > 0 ){
						//
						//     3. 'sest|kuid|ehkki|kuni' millele j2rgneb ja eelneb 8eldis:
						//              Me lõhume korra ära ja naudime kaost kuni ta ahistama hakkab .
						//              Ta vaikis hetkeks kuid kogus end kiirelt .
						//              Materjali kannab liustik kaasas kuni see välja sulab .
						//        ('sest|kuid|ehkki' paistavad olevat suhteliselt kindlad/selged tunnused,
						//         samas kui 'kuni' on k6ige sagedasem ja on teatud kontekstides probleemne)
						//
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, true);
						if (eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
							boolean kuniProbleemne = false;
							if (((osalause.get(i)).getNormAlgSona()).equalsIgnoreCase("kuni")){
								//
								//   Ei loe igaks juhuks osalausepiiriks, kui:
								//   *) 'kuni'-le eelneb/järgneb on arv või number;
								//   *) 'kuni'-le eelneb sona 'alates';
								//
								MorfTunnusteHulk eelnevaSonaMorf1  = new MorfTunnusteHulk( osalause.get( i - 1 ) );
								MorfTunnusteHulk jargnevaSonaMorf1 = new MorfTunnusteHulk( osalause.get( i + 1 ) );
								if (!(jargnevaSonaMorf1.filterByPOS("_N_")).isEmpty() ||
								    !(jargnevaSonaMorf1.filterByPOS("_O_")).isEmpty() ||
								    !(eelnevaSonaMorf1.filterByPOS("_N_")).isEmpty()  ||
								    !(eelnevaSonaMorf1.filterByPOS("_O_")).isEmpty() ){
									kuniProbleemne = true;
								}
								if (!(eelnevaSonaMorf1.filterByLemmaRegExp(".*[0-9].*")).isEmpty() || 
								    !(jargnevaSonaMorf1.filterByLemmaRegExp(".*[0-9].*")).isEmpty()){
									// Nt  Parim t° on +2° kuni +4° C .
									//     4- kuni 16-megabaidist mälu kasutatakse ketta vahemäluna
									kuniProbleemne = true;
								}
								if (TekstiFiltreerimine.eelnebMargendigaSona(osalause, i, alates, osalausepiir) > -1){
									kuniProbleemne = true;
								}
							}
							if ( !kuniProbleemne ){
								OsalauSona eelmineSona = osalause.get(i-1);
								if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
									eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
								} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
									eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
								}
								//
								//   Kahtlased kohad:
								//      kas arvutiprogramm peab nõudma , et õpilane jätkaks vastamist / kuni ta jõuab õige vastuseni .
								//      punase ristiku saak suurenes kuni Ca/Mg suhe kitsenes 10 … 19 .
								//   Veakohad:
								//      Tegijatele on välja pandud kolme- / kuni viieaastased 1,3 miljoni euroni ulatuvad stipendiumid
								//      Mul hakkab suu vett jooksma , kui mõtlen missugune praad / sest tuleb .
								//
							}
						}
					}
					if ( millalKusJne.vastabMallileAND( osalause.get( i ) )  &&  i > 0 ){
						//
						//     4. 'millal|kus|kuhu|kust|kuna|kuidas|kas|miks' millele j2rgneb ja eelneb 8eldis:
						//           Igaüks võtab külmkapist ja sööb millal tahab ...
						//           Kardulad kõik külma käes väljas , ei tea kas kõlbavadki enam .
						//           polnud olulist erinevust kas materjal esitati graafilises või tekstilises formaadis .
						//           Teenus ja kvaliteet ei saa olla seostamatud kuna kvaliteet on viis kuidas teenust osutatakse .
						//
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, true);
						if (eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
							boolean kuidasProbleemne = false;
							boolean kustProbleemne   = false;
							if (((osalause.get(i)).getNormAlgSona()).equalsIgnoreCase("kuidas")){
								//
								//   Proovime j2tta v2lja 'kuidas' esinemist 'on-kuidas-on' yhendites, nt: 
								//      Nii ma siis mõtlesin , et olgu teistega kuidas on , mina hakkan võimlas käima .
								//
								MorfTunnusteHulk eelnevaOeldiseMorf1  = new MorfTunnusteHulk( osalause.get(eelnebJargnebOeldis[0]) );
								MorfTunnusteHulk jargnevaOeldiseMorf1 = new MorfTunnusteHulk( osalause.get(eelnebJargnebOeldis[1]) );
								if (!(eelnevaOeldiseMorf1.filterByPOS("_V_")).filterByLemma("ole").isEmpty() &&
									!(jargnevaOeldiseMorf1.filterByPOS("_V_")).filterByLemma("ole").isEmpty()){
									kuidasProbleemne = true;
								}
							}
							else if (((osalause.get(i)).getNormAlgSona()).equalsIgnoreCase("kust")){
								//
								//   Proovime j2tta v2lja 'kust' esinemist 'ei-tea-kust' yhendites, nt:
								//      Ei tea kust ilmub välja Tristan Tzara .
								//      Ei tea kust ilmaotsast ilmus Kivinõmme külasse noor ja ilus naine pojaga .
								//
								MorfTunnusteHulk eelnevaMorf1  = new MorfTunnusteHulk( osalause.get(i-1) );
								if (!(eelnevaMorf1.filterByPOS("_V_")).filterByLemma("tead").isEmpty()){
									kustProbleemne = true;
								}
							}
							if ( !kuidasProbleemne && !kustProbleemne ){
								OsalauSona eelmineSona = osalause.get(i-1);
								if (kuidVaidJne.vastabMallileAND(osalause.get(i-1)) && i-2 > -1){
									// 
									//   Kui s6nale juba eelneb vahetult m6ni sides6na (kuid, vaid, aga, et), siis tuleb 
									//   osalausepiir m2rkida kaks s6na tagasi ...
									//
									eelmineSona = osalause.get(i-2);
								}
								if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
									eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
								} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
									eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
								}
							}
						}
						//
						//    Kaheldav/vaieldav:
						//        Ütle / kus on saatanahing , mis nad kõik välja pole nuhkinud .
						//        Ütle / kus on tahumatu mats !
						//        Võta / kuidas tahad !
						//        et ma kuskilt jumal teab / kust tulnud olin
						//        Minupärast tule / millal tahad .
						//        Pane oma susla / kus tahad !
						//        õpilane eksis materjalis ära , teadmata / kuidas ta konkreetsele lehele sattus
						//        kaks käsku olla teineteisest / kas sõltuvad või mittesõltuvad .
						//    Vigane:
						//        Ka siin kehtib / kas kõik või mitte midagi .
						//
					}
					if ( siis.vastabMallileAND( osalause.get( i ) )  &&  i > 0 ){
						//
						//     5. 'siis' millele j2rgneb ja eelneb 8eldis:
						//           Suur muutus tuli siis kui mindi toiduainete otsimiselt üle nende tootmisele .
						//           Kui siht on utilitaarne õnn siis polegi valest pääsu .
						//           kui me nendesse ruumidesse kolisime siis arvasin et see on ajutine .
						//      Kuna 'siis' v6ib esineda ka m22rs6nana, pyyame siin vaadata pigem 'siis' esinemist 
						//     konstruktsioonides, nt 'ehk-siis', 'kui-siis';
						//
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, false);
						if ( eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
							int eelnKuiAsukoht = TekstiFiltreerimine.eelnebMargendigaSona(osalause, i, kui, null);
							int jargKuiAsukoht = TekstiFiltreerimine.jargnebMargendigaSona(osalause, i, kui, osalausepiir);
							//
							//  siis-kui variant, nt: 
							//    Klient tuleb panka siis kui ta saab aru , et sellest tulekust on talle kasu .
							//
							boolean siisKui = (jargKuiAsukoht == i + 1);
							//
							//  kui-siis variant, nt:
							//    kui sul on raske siis tule tagasi meie juurde
							//
							boolean kuiSiis = (eelnKuiAsukoht > -1);
							//
							//   ehk-siis variant, nt: 
							//    Asi lahenes õnnelikult , sest kliendid olid heasoovlikud ehk siis soovisid teistest rohkem edasi jõuda ;
							//
							boolean ehkSiis = (ehk.vastabMallileAND(osalause.get(i-1)));
							if ( kuiSiis || siisKui || ehkSiis ){
								OsalauSona eelmineSona = osalause.get(i-1);
								if ( ehkSiis ){
									//
									//   ehk-siis: nihutame piiri koha v6rra tahapoole;
									//
									eelmineSona = osalause.get(i-2);
								}
								else if ( siisKui ){
									//
									//   siis-kui: piir l2heb 'siis' j2relt,  Nt: 
									//  Klient tuleb panka siis kui ta saab aru , et sellest tulekust on talle kasu .
									//									
									eelmineSona = osalause.get(i);
								}
								if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
									eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
								} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
									eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
								}
							}
							//
							//   'siis', millele j2rgneb kaugemal 'kui', nt:
							//      Kas ma olen siis parem inimene kui ma riskin lennust maha jääda ? 
							//      Isamaatöö saab vaid siis olla edukas kui riik kindlustab vabatahtlikud ka vahenditega
							//   Nendel juhtudel l2heb osalausepiir 'kui' ette;
							//
							else if ( jargKuiAsukoht > -1 ){
								int[] eelnebJargnebOeldis2 = Osalausestaja.eelnebJargnebOeldis(osalause, jargKuiAsukoht, osalausepiir, false);
								if ( eelnebJargnebOeldis2[0] > -1 && eelnebJargnebOeldis2[1] > -1 ){
									OsalauSona eelmineSona = osalause.get(jargKuiAsukoht-1);
									if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
										eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
									} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
										eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
									}
								}
							}
							
						}
					}
					if ( nagu.vastabMallileAND( osalause.get( i ) )  &&  i > 0  &&  naguEelnev.vastabMallileAND(osalause.get(i-1)) ){
						//
						//     6. 'nii-nagu' millele j2rgneb ja eelneb 8eldis:
						//          Kõik oli nii nagu pidi olema .
						//          Ma ei mänginud nii nagu mängiksin tervena .
						//          Kohalikku aadlisse suhtub Nyenstede neis kirjeldustes nii nagu hetkeolukord nõudis .
						//
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, false);
						if ( eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
							OsalauSona eelmineSona = osalause.get(i-1);
							if (i-3 > -1 && (et.vastabMallileAND(osalause.get(i-2)) || sestKuidJne.vastabMallileAND(osalause.get(i-2)))){
								//  Kui nii-nagu'le eelneb vahetult et/sest/kuid jms, siis tuleks koma m2rkida nonde ette
								eelmineSona = osalause.get(i-3);
							}
							if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
								eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
							} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
								eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
							}
							//
							//    Probleemiks on see, et 'nii-nagu' puhul on komakasutus "k6ikuv", st koma v6ib 
							//    nii eelneda v2ljendile kui ka olla selle keskel, nt
							//          Mõni muugi asi oli nii , nagu kirjeldasid raamatud .
							//          Olge täiuslikud , nii nagu teie Isa taevas on täiuslik .
							//
							//    Kahtlased kohad:
							//          Kujuneb nii / nagu kujuneb .
							//          ... ehk ei lähe kõik nii / nagu unistad . 
							//          " Teeme ikka nii / nagu peab , " otsustasin mängujuht olla .
							//
						}
					}
					if ( kui.vastabMallileAND( osalause.get( i ) )  &&  i > 0  &&  kuiEelnev1.vastabMallileAND(osalause.get(i-1)) ){
						//
						//     7.1. konstruktsioonid 'kui'-ga, kuis 8eldise eelnevust/j2rgnevust ei n6ua:
						//          juhul-kui: 
						//                Võimatus on objektiivne juhul kui sooritus on võimatu igaühe jaoks .
						//                Jooksevkonto puudujääk on igati loomulik juhul kui kapitalikonto on ülejäägis .
						//
						if ( i-2 == -1  ||  (i-2 > -1 && !osalausepiir.vastabMallileOR(osalause.get(i-2)) )){
							OsalauSona eelmineSona = osalause.get(i-1);
							if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
								eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
							} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
								eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
							}
						}
						//
						//
					}
					if ( kui.vastabMallileAND( osalause.get( i ) )  &&  i > 0  &&  kuiEelnev2.vastabMallileAND(osalause.get(i-1)) ){
						//
						//     7.2. konstruktsioonid 'kui'-ga, kus peaks eelnema/j2rgnema 8eldis:
						//          enne-kui: 
						//                Ja teati veel enne kui rääkima hakati .
						//                Maailmarekordeid ei kinnitata enne kui on selgunud dopinguproovide tulemused . 
						//          ajal-kui:
						//                ABBA alustas ju umbes samal ajal kui meiegi tegime oma esimese ansambli .
						//         hetkel-kui:
						//                Jõudsin õue samal hetkel kui nägin meest kino ees autosse istumas .
						//           seda-kui:
						//                Liikumisvabadus on see kui on palju ruumi ja sa võid seismise kohta valida .
						//
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, false);
						if ( eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
							if ( i-2 > -1 && !osalausepiir.vastabMallileOR(osalause.get(i-2)) ){
								OsalauSona eelmineSona = osalause.get(i-1);
								if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
									eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
								} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
									eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);									
								}
							}
						}
						//
						//   Probleem: nendes yhendites on komakasutus sageli "k6ikuv", st koma v6ib eelneda 
						//             vahetult "kui"-le, aga v6ib ka eelneda konstruktsiooni esimesele s6nale
						//             ("enne/ajal/hetkel"-le), nt:
						//      ... sabistasid ja seletasid inimesed veel tükk aega enne / kui vaiksemaks jäid .
						//          ( =>  sabistasid ja seletasid inimesed veel tükk aega / enne kui vaiksemaks jäid . )
						//      ... Miks tõsteti pensioniiga samal ajal / kui üle 35aastased ei leia tööd .
						//          ( =>  Miks tõsteti pensioniiga / samal ajal kui üle 35aastased ei leia tööd . )
						//      ... Hetkel / kui Lee tundmatule objektile sattus , oli ilm täiesti pilvitu .
						//          ( => Hetkel kui Lee tundmatule objektile sattus , oli ilm täiesti pilvitu . )
						//
						//   NB! Siinjuures paistab, et kuigi "enne/ajal/hetkel"-konstruktsioonides on komakasutus
						//   k6ikuv, siis "seda-kui" konstruktsioonides on see ysna fikseeritud, st koma on enamikel 
						//   juhtudest "seda" j2rel ja "kui" ees, nt
						//        Liikumisvabadus on see / kui on palju ruumi ja sa võid seismise kohta valida .
						//        Nii hakkas olema pärast seda / kui hüdroelektrijaam tööle pandi ,
						//
						
						//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(osalause, false));
					}
					
					if ( i > 0 ){
						MorfTunnusteHulk sona = new MorfTunnusteHulk( osalause.get( i ) );
						if ( !(sona.filterByLemmaRegExp("^mis|mis_*sugune|milline|kes$")).isEmpty() ) {
							int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, false);
							if ( eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
								//
								//     8.1. Lemma 'mis|mis_*sugune|milline|kes', millele eelneb ja j2rgneb 8eldis, nt:
								//          Aga ei tea mis kärbes mind küll hammustas .
								//          Kas see ongi see sombi kellest isa rääkis ?
								//          Alles siis said pojad aru mida nende isa oli mõtelnud !
								//          Sümbol on märk millega asendatakse mõistet või matemaatilist tehet vms.
								//          Arusaamatu on ka see , mida üldse müüakse ehk mis on 66 protsendi aktsiate sisu .
								//          Aga ma tean millised on ta palganumbrid ja isegi pool sellest on meie jaoks õudusunenägu .
								// 
								boolean probleemneJutumark = false;
								if ((Osalausestaja.koneAlgusJutumark.matcher((osalause.get(i)).getNormAlgSona())).matches() || 
									(Osalausestaja.koneAlgusJutumark.matcher((osalause.get(i-1)).getNormAlgSona())).matches()){
									//
									//   Kui kysis6nale eelneb jutum2rk, v6ib tegu olla pealkirja vms; 
									//  Igaks juhuks osalausepiiri ei m2rgi sellisel juhul;
									//
									probleemneJutumark = true;
								}
								boolean probleemneTeabMis = false;
								boolean probleemneKesTeab = false;
								boolean probleemneVerbikordus = false;
								String algSona = (osalause.get(i)).getNormAlgSona();
								if ( algSona.matches("(?i)mis|kes") ){
									//
									//   Proovime j2tta v2lja 'Verb-mis|kes-Verb' yhendeid, nt: 
									//      Toetusin koolimaja seina vastu ja mõtlesin , et oli mis oli .
									//      Uuriti mis uuriti , aga midagi välja ei uuritud .
									//      Vaar tõreles mis tõreles , ent tüdruk jalgade siputamist ei jätnud .
									//      Ole kes oled , ma tänan sind .
									//      olgu see mees kes ta on .
									//      olgu ta kes ta on .
									//
									MorfTunnusteHulk eelnevaOeldiseMorf1  = new MorfTunnusteHulk( osalause.get(eelnebJargnebOeldis[0]) );
									MorfTunnusteHulk jargnevaOeldiseMorf1 = new MorfTunnusteHulk( osalause.get(eelnebJargnebOeldis[1]) );
									eelnevaOeldiseMorf1 = eelnevaOeldiseMorf1.filterByPOS("_V_");
									jargnevaOeldiseMorf1 = jargnevaOeldiseMorf1.filterByPOS("_V_");
									if (!eelnevaOeldiseMorf1.isEmpty() &&  
										!jargnevaOeldiseMorf1.isEmpty()) {
										List<MorfAnRida> tunnused1 = eelnevaOeldiseMorf1.getTunnused();
										List<MorfAnRida> tunnused2 = jargnevaOeldiseMorf1.getTunnused();
										HashSet<String> lemmad1 = new HashSet<String>();
										HashSet<String> lemmad2 = new HashSet<String>();
										for (MorfAnRida morfAnRida : tunnused1) {
											lemmad1.add( morfAnRida.getLemmaIlmaVahemarkideta() );
										}
										for (MorfAnRida morfAnRida : tunnused2) {
											lemmad2.add( morfAnRida.getLemmaIlmaVahemarkideta() );
										}
										lemmad1.retainAll(lemmad2);
										if ( !lemmad1.isEmpty() ){
											probleemneVerbikordus = true;
										}
									}
									//
									//  Proovime j2tta v2lja 'teab-mis' yhendid, nt:   
									//      Ega see teab mis vaatamisväärsus poleks ka olnud .
									//      Sest teab mis julgustav tulevikumärk see nüüd küll ei ole .
									//      Teab mis kuulus ta nüüd on .
									//
									if ( algSona.matches("(?i)mis")  &&  eelnebJargnebOeldis[0] > -1 && 
										 ((osalause.get(eelnebJargnebOeldis[0])).getNormAlgSona()).equalsIgnoreCase("teab") ) {
										probleemneTeabMis = true;
									}
									//
									//  Proovime j2tta v2lja 'kes-teab' yhendid, nt:   
									//      Ehkki erinevus ema ja tütre vahel polnud kes teab kui suur .
									//      Laadijad olid aga vahepeal kes teab kuhu kadunud .
									//      Lähemale minnes aga hajub kes teab kuhu .
									//
									if ( algSona.matches("(?i)kes")  &&  eelnebJargnebOeldis[1] > -1 && 
									   ((osalause.get(eelnebJargnebOeldis[1])).getNormAlgSona()).equalsIgnoreCase("teab") ) {
										probleemneKesTeab = true;
									}
								}
								boolean olemasolevPiir = false;
								if (osalausepiir.vastabMallileOR(osalause.get(i-1)) ||
									(i - 1 > 0  &&  osalausepiir.vastabMallileOR(osalause.get(i-2)))) {
									//
									//    Kui vahetult eelnev s6na on juba m2rgitud piiriks, v6i piiriks on m2rgitud
									//    yle-eelmine s6na, siis on uue piiri lisamine probleemne ...
									//
									olemasolevPiir = true;
								}
								if ( !olemasolevPiir && !probleemneJutumark && !probleemneVerbikordus &&
									 !probleemneTeabMis && !probleemneKesTeab ) {
									OsalauSona eelmineSona = osalause.get(i-1);
									if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
										eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
									} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
										eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);
									}
								}
								//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(osalause, false) );
								//
								//   Kahtlased kohad ('mis|missugune|milline|kes'):
								//         Nagu ma neile teab / mida oleksin lubanud .
								//         Toodab munarakke / mis mühiseb .
								//         Ta võis jälle ei tea / mida korda saata .
								//           'Mis-tahad/mida-tahad' konstruktsioonid:
								//              Aga tehke / mis tahate - minu arust oli küll aru saada .
								//              Räägi sina / mis tahad , ma tean sinust paremini , millised on mehed .
								//              Mõtle / mida tahad , mõtle või seda , et tahan su miljoneid jagada .
								//              Läti korvpallurid said / mis tahtsid .
								//         Vaata / missugune koer mul on ! "
								//         Jätan siinkohal kõrvale / millises linnas mulle endale elada meeldiks , aga ...
								//         Mis edasi saab ehk / millisesse teatrisse läheksite ?
								//         Kuid mingil määral peab ükskõik / millise roodu relvastus olema enam-vähem ühesugune .
								//           'Yksk6ik-milline' konstruktsioonid:
								//              - Kuid mingil määral peab ükskõik / millise roodu relvastus olema enam-vähem ühesugune .
								//              + veerandil firmadest on ükskõik / milline on pangast väljapoole suunatav informatsioon .
								//         Mul oli kauss , millesse ei tea / kes oli sisse pressinud Rasso von Geertzi monogrammi .
								//           'ehk-kes|mis|milline' konstruktsioonid:
								//              Pole päris selge , kes võitis valimised ehk / kes kellega käib . 
								//
							}
							else if (eelnebJargnebOeldis[0] == -1  &&  eelnebJargnebOeldis[1] > -1  &&  i - 1 > -1){
								//
								//     8.2. Lemma 'mis|mis_*sugune|milline|kes', millele ei eelne, aga j2rgneb 8eldis, nt:
								//            Kõik mis on , selle paneme välja .
								//            See mis toimus , polnud poliitika , vaid võimuvõitlus .
								//            Kõik mis muusikaga seotud on meelelahutus ja seda tuleb järjest juurde .
								//            Üks inimene kellele saad kõik hinge pealt ära rääkida , peab ikka olema .
								//            Suurem osa sellest mis meil on , läheb niikuinii enesekaitsejõudude loomiseks .
								//            Kõigil kel on informatsiooni tema vanemate kohta , palub politsei helistada 110 .
								//
								boolean olemasolevPiir = false;
								if (osalausepiir.vastabMallileOR(osalause.get(i-1)) ||
								    (i - 1 > 0  && osalausepiir.vastabMallileOR(osalause.get(i-2))) ) {
									//
									//    Kui vahetult eelnev s6na on juba m2rgitud piiriks, v6i piiriks on m2rgitud
									//    yle-eelmine s6na, siis on uue piiri lisamine probleemne ...
									//
									olemasolevPiir = true;
								}
								MorfTunnusteHulk sonaMorf        = new MorfTunnusteHulk( osalause.get(i) );
								MorfTunnusteHulk eelnevaSonaMorf = new MorfTunnusteHulk( osalause.get(i-1) );
								boolean eelnebSobivYldsona = false;
								//
								//    Kui vahetult eelneb sobiv ylds6na|pronoomen, siis on tegemist potentsiaalse 
								//   osalausepiiriga;
								//
								if (!(sonaMorf.filterByLemmaRegExp("^(mis)$").isEmpty()) && 
									!eelnevaSonaMorf.filterByLemmaRegExp("^(k(\u014D|\u00F5)ik|see|selline|miski)$").isEmpty() ){ 
									eelnebSobivYldsona = true;
								}
								if (!(sonaMorf.filterByLemmaRegExp("^(mis_*sugune|milline)$").isEmpty()) && 
									!eelnevaSonaMorf.filterByLemmaRegExp("^(see|selline)$").isEmpty() ){ 
									eelnebSobivYldsona = true;
								}
								if (!(sonaMorf.filterByLemmaRegExp("^(kes)$").isEmpty()) && 
									!eelnevaSonaMorf.filterByLemmaRegExp("^(inimene|k(\u014D|\u00F5)ik|see|selline|keegi)$").isEmpty() ){ 
									eelnebSobivYldsona = true;
								}
								if ( !olemasolevPiir && eelnebSobivYldsona ){
									OsalauSona eelmineSona = osalause.get(i-1);
									if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
										eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
									} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
										eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);
									}
								}
								//
								//    Probleem - poolikuks jäävad kiilud:
								//       Kas see / millega nood ametis olid võis osutuda järjekordseks mobiilitrikiks või mitte?
								//       Kõik / mis puutub julgeolekusse põhineb ühel teisel loogikal .
								//         (juhtub siis, kui yhtegi koma pole)
								//
							}
							//System.out.println( TekstiFiltreerimine.debugMargendustegaLause(osalause, false) );
						}
					}
					//
					//     9.  'otsekui|justkui|kusjuures|seejuures|sealjuures', millele eelneb ja j2rgneb 8eldis, nt:
					//             Tundsin end kõhedalt justkui oleks mind ennast salaja jälgitud .
					//             Nad võitsid erastamiskonkursi kusjuures aktsiad osteti laenuga .
					//             Ta ilme kõneleb täielikust rõõmutusest justkui poleks talle Gustavi-sugune tuttav sugugi sobiv .
					//         (NB! tundub, et 'kusjuures|seejuures|sealjuures' on pigem omased teaduslikule tekstile/stiilile,
					//          ning 'otsekui|justkui' on pigem omased ilukirjanduslikule stiilile)
					//
					if ( kusjuuresJne.vastabMallileAND(osalause.get(i)) && i > 0 ){
						int[] eelnebJargnebOeldis = eelnebJargnebOeldisKitsendustega(osalause, i, osalausepiir, false, false, false);
						if ( eelnebJargnebOeldis[0] > -1 && eelnebJargnebOeldis[1] > -1){
							// 
							//   V2listame juhud, kus keskne verb eelneb vahetult, kuiv6rd need v6ivad olla 
							//   probleemsed, nt:
							//           Et Tuglase suhtumine Vildesse on sealjuures endiselt karune sellest kõneleb ...
							//           R. Tscherwinka juhib seejuures tähelepanu sellele et Absprache juures tuleb ...
							//
							if (eelnebJargnebOeldis[0] != i-1){
								OsalauSona eelmineSona = osalause.get(i-1);
								if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
									eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
								} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
									eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);
								}
							}
							//
							//     (!!) Vale reeglite rakendamise järjekord põhjustab probleeme:
							//    Keskkonnaõigus lähtus keskkonna / justkui suurest puhastusvõimest millele lisandus ka tööstusliku arengu sage eelistamine .
							//
						}
					}
					//
					//    10. Kui kaks keskset verbi on k6rvuti, m2rgi vahele osalausepiir, nt:
					//             Neid armastatakse julgen ma väita .
					//             Aga kui teisele poole jõudsin vaatasin taha - ...
					//             Muidu Te erilise luksusega silma ei paista armastate ainult häid autosid .
					//
					if ( (osalause.get(i)).omabMargendit(MARGEND.OELDIS) && i+1 < osalause.size()){
						MorfTunnusteHulk tunnused = new MorfTunnusteHulk(osalause.get(i));
						MorfTunnusteHulk verbiTunnused = tunnused.filterByPOS("_V_");
						//  J2tame k6rvale keskseks verbiks olevate liiteituste abiverbid
						if ( (verbiTunnused.filterByLemmaRegExp("^ei$")).isEmpty() && 
							 (verbiTunnused.filterByLemmaRegExp("^\u00E4ra$")).isEmpty() ){
							if ( (osalause.get(i+1)).omabMargendit(MARGEND.OELDIS )){
								MorfTunnusteHulk verbTunnused2 = 
									new MorfTunnusteHulk( (osalause.get(i+1)).getMorfSona() );
								boolean probleemneLiitaeg = false;
								// J2tame k6rvale keskseks verbiks olevad liitaja abiverbina toimivad olema-verbid
								if (!((verbiTunnused.filterByLemmaRegExp("^ole$")).isEmpty())){
									if ( !(verbTunnused2.filterByPOS("_V_")).filterByEnding( "^\\s*(nud)(ki)?\\s*$" ).isEmpty() ){
										// ole + nud
										probleemneLiitaeg = true;
									}
								}
								boolean probleemneVatVorm = false;
								if ( !((verbTunnused2.filterByPOS("_V_")).filterByEnding("^vat$")).isEmpty() ){
									// J2tame k6rvale probleemsed -vat konstruktsioonid, nt:
									//   ... vajab / loovat mõtlemist ...
									//   ... Kohtunik paistab / olevat veendunud selles et ...
									//   ... kuhu pereliikmete sisenemine näis / olevat võimatu .
									//   ... Suured teemad näivad / avanevat kõige kergemini just sellistes situatsioonides ...
									probleemneVatVorm = true;
								}
								//  Kui m6ni 8eldis on analyysilt j22nud mitmeseks (v.a. nud-mitmesus), 
								//  siis igaks juhuks piiri m2rkima ei hakka ...
								boolean probleemneMitmene = false;
								if ( (tunnused.getTunnused()).size() > 1  &&  !Pattern.matches("^.+nud(ki)?$", (osalause.get(i)).getNormAlgSona()) ){
									probleemneMitmene = true;
								}
								if ( (verbTunnused2.getTunnused()).size() > 1  &&  !Pattern.matches("^.+nud(ki)?$", (osalause.get(i+1)).getNormAlgSona()) ){
									probleemneMitmene = true;
								}
								if ( !probleemneLiitaeg && !probleemneVatVorm && !probleemneMitmene ){
									OsalauSona eelmineSona = osalause.get(i);
									if (eelmineSona.omabMargendit(MARGEND.OLETATAV_PIIR)){
										eelmineSona.asendaMargend(MARGEND.OLETATAV_PIIR, MARGEND.KINDEL_PIIR);
									} else if (!eelmineSona.omabMargendit(MARGEND.KINDEL_PIIR)) {
										eelmineSona.lisaMargend(MARGEND.KINDEL_PIIR);
									}
								}
								//
								//     Probleemsed kohad:
								//       - kaks k6rvutiolevat keskset verbi ei pruugi siiski veel t2hendada osalausepiiri, nt:
								//           Ma lähen teen selle ära.
								//           (aga kirjakeeles ei tundu see eriti sage nähtus olevat)
								//       - tundlikkus morf yhestamise vigade suhtes, nt:
								//           Rahandusspetsialistidel on / väljendus mis aasta kroonist räägime .
								//           " Kõik loengud on lubatud õhtupoolikul korraldada " selgitas / Tea .
								//       - kiilud jäävad eraldamata, kuigi sageli viitavad k6rvutiolevad verbid just kiilu
								//         olemasolule, nt:
								//           Tegevus millega Jumal maailma alal hoiab / on sama millega ta maailma lõi .
								//           ... naistel kes temaga ühes töötasid / tuli alustuseks harjuda kasvõi sellega ...
								//
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	
	
	/**
	 *   Teostab sama loogika, mis meetod <code>Osalausestaja.eelnebJargnebOeldis()</code>, ning seej2rel rakendab leitud
	 *   8eldistele lisakitsendusi: <br>
	 *   <ul>
	 *   <li>kui <code>lubaVoiOeldisena==false</code> ja kui eelnev/j2rgnev 8eldis on 'v6i', siis seda ei arvestata (kirjutatakse 
	 *   vastava 8eldise selle kohale -1); </li>
	 *   <li>kui <code>lubaVadOeldisena==false</code> ja kui eelnev/j2rgnev 8eldis on mitmuse -vad verb (nt kasvavad), siis seda ei arvestata (kirjutatakse 
	 *   vastava 8eldise kohale -1); Erand: kui vad-verb on mitmene olema-vorm ('on'), siis eeldame, et t6en2oliselt on ikka 6ige. </li>
	 *   </ul>
	 *   Tagastab tulemuse samas formaadis, mis meetod <code>Osalausestaja.eelnebJargnebOeldis()</code>.<br>
	 */
	public static int [] eelnebJargnebOeldisKitsendustega(List<OsalauSona> osalause, int i, SonaMall piir, 
			 boolean lubaSonaEnnastEelnevaOeldisena, boolean lubaVoiOeldisena, boolean lubaVadOeldisena) throws Exception {
		int [] eelnevJargnevOeldis = Osalausestaja.eelnebJargnebOeldis( osalause, i, piir, lubaSonaEnnastEelnevaOeldisena );
		if (eelnevJargnevOeldis[0] > -1){
			OsalauSona sona = osalause.get(eelnevJargnevOeldis[0]);
			String normAlgSona = sona.getNormAlgSona();
			if (!lubaVoiOeldisena){
				if ( normAlgSona.matches("^(v\u014Di|v\u00F5i)$") ){
					eelnevJargnevOeldis[0] = -1;
				}
			}
			if (!lubaVadOeldisena){
				MorfTunnusteHulk tunnused = new MorfTunnusteHulk( sona );
				MorfTunnusteHulk vadTunnused = (tunnused.filterByPOS("_V_")).filterByFormNames("vad");
				if ( !vadTunnused.isEmpty() && !normAlgSona.matches("^on$") ){
					eelnevJargnevOeldis[0] = -1;
				}
			}
		}
		if (eelnevJargnevOeldis[1] > -1){
			OsalauSona sona = osalause.get(eelnevJargnevOeldis[1]);
			String normAlgSona = sona.getNormAlgSona();
			if (!lubaVoiOeldisena){
				if ( normAlgSona.matches("^(v\u014Di|v\u00F5i)$") ){
					eelnevJargnevOeldis[1] = -1;
				}
			}
			if (!lubaVadOeldisena){
				MorfTunnusteHulk tunnused = new MorfTunnusteHulk( sona );
				MorfTunnusteHulk vadTunnused = (tunnused.filterByPOS("_V_")).filterByFormNames("vad");
				if ( !vadTunnused.isEmpty() && !normAlgSona.matches("^on$") ){
					eelnevJargnevOeldis[1] = -1;
				}
			}
		}
		return eelnevJargnevOeldis;
	}


}
