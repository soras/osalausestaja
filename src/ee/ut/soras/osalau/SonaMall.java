package ee.ut.soras.osalau;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ee.ut.soras.osalau.OsalauSona.MARGEND;

/**
 *  Yhte <code>OsalauSona</code> kirjeldav s6namall. V6imaldab mitmesuguste tunnuste 
 *  ( nt sona tekstikuju kirjeldavate regulaaravaldiste, m2rgendite olemasolu )
 *  abil s6nu kirjeldada ning kontrollida, kas mingi etteantud s6na vastab mallile;
 *  <br>
 *  Saab kontrollida nii seda, kas rahuldatud on k6ik tunnused korraga (tunnuste 
 *  konjunktsioon) kui ka seda, kas rahuldatud on v2hemalt yks tunnustest (tunnuste 
 *  disjunktsioon); 
 * 
 * @author Siim Orasmaa
 */
public class SonaMall {

	/**
	 *  Margendid, mis peavad s6na kyljes olema, et see vastaks mallile;
	 */
	private List<MARGEND> vajalikudMargendid = null;
	
	/**
	 *  Regulaaravaldised, millele peab s6na tekstikuju (algsona) vastama, 
	 *  et sona vastaks mallile;
	 */
	private List<Pattern> tekstikujuKirjeldused = null;
	
	/**
	 *   Initsialiseerib tyhja s6namalli;
	 */
	public SonaMall(){
	}
	
	/**
	 *   Initsialiseerib sonamalli, mis kontrollib etteantud m2rgendi
	 *  olemasolu;
	 */
	public SonaMall(MARGEND vajalikMargend) {
		this.lisaVajalikMargend(vajalikMargend);
	}
	
	/**
	 *   Initsialiseerib sonamalli, mis kontrollib tekstikuju sobivust 
	 *  etteantud regulaaravaldisele;
	 */
	public SonaMall(Pattern tekstikujuKirjeldus){
		this.lisaTeksikujuKirjeldus(tekstikujuKirjeldus);
	}
	
	/**
	 *  Leiab, kas etteantud s6na vastab k6ikidele selles mallis kirjeldatud 
	 *  tunnustele (nt omab n6utud m2rgendeid); Ehk sisuliselt kontrollitakse,
	 *  kas kehtib: <i>kitsendus1 AND kitsendus2 AND ... AND kitsendusN</i><br>
	 *  
	 *  Tagastab true, kui kehtib;<br><br>
	 *  Viskab erindi, kui vastavuse kontrollimisel tekkis probleeme (nt
	 *  sona objekti kyljes puudus s6na tekstikuju s6ne);
	 */
	public boolean vastabMallileAND(OsalauSona sona) throws Exception {
		if (this.vajalikudMargendid != null){
			// Kontrollime, kas s6na omab k6iki n6utud m2rgendeid
			for (MARGEND margend : this.vajalikudMargendid) {
				if (!sona.omabMargendit(margend)){
					return false;
				}
			}
		}
		if (this.tekstikujuKirjeldused != null){
			String tekstikuju = sona.getNormAlgSona();
			// Kontrollime, kas s6na vastab k6igile n6utud regulaaravaldistele
			if (tekstikuju != null){
				for (Pattern regexp : this.tekstikujuKirjeldused) {
					if (!(regexp.matcher(tekstikuju)).matches()){
						return false;
					}
				}				
			} else {
				throw new Exception(" Text form not found for word "+String.valueOf(sona));
			}
		}
		return true;
	}

	/**
	 *  Leiab, kas etteantud s6na vastab v2hemalt yhele selles mallis kirjeldatud 
	 *  tunnusele (nt omab yht n6utud m2rgenditest); Ehk sisuliselt kontrollitakse,
	 *  kas kehtib: <i>kitsendus1 OR kitsendus2 OR ... OR kitsendusN</i><br>
	 *  
	 *  Tagastab true, kui kehtib;<br><br>
	 *  Viskab erindi, kui vastavuse kontrollimisel tekkis probleeme (nt
	 *  sona objekti kyljes puudus s6na tekstikuju s6ne);
	 */
	public boolean vastabMallileOR(OsalauSona sona) throws Exception {
		if (this.vajalikudMargendid != null){
			// Kontrollime, kas s6na omab v2hemalt yhte n6utud m2rgendit
			for (MARGEND margend : this.vajalikudMargendid) {
				if (sona.omabMargendit(margend)){
					return true;
				}
			}
		}
		if (this.tekstikujuKirjeldused != null){
			String tekstikuju = sona.getNormAlgSona();
			// Kontrollime, kas s6na vastab v2hemalt yhele n6utud regulaaravaldistele
			if (tekstikuju != null){
				for (Pattern regexp : this.tekstikujuKirjeldused) {
					if ((regexp.matcher(tekstikuju)).matches()){
						return true;
					}
				}				
			} else {
				throw new Exception(" Text form not found for word "+String.valueOf(sona));
			}
		}
		return false;
	}

	/**
	 *  Lisab uue n6utud m2rgendi kontrollitavate m2rgendite hulka;
	 */
	public void lisaVajalikMargend(MARGEND margend){
		if (this.vajalikudMargendid == null){
			this.vajalikudMargendid = new ArrayList<OsalauSona.MARGEND>(2);
		}
		(this.vajalikudMargendid).add(margend);
	}
	
	/**
	 *  Lisab uue tekstikuju kirjeldava regulaaravaldise kontrollivate regulaaravaldise hulka;
	 */
	public void lisaTeksikujuKirjeldus(Pattern regexp){
		if (this.tekstikujuKirjeldused == null){
			this.tekstikujuKirjeldused = new ArrayList<Pattern>(2);
		}
		(this.tekstikujuKirjeldused).add(regexp);
	}
	
}
