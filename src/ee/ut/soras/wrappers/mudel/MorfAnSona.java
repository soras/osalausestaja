package ee.ut.soras.wrappers.mudel;

import java.util.ArrayList;
import java.util.List;

/**
 *   Morfoloogilise analüsaatori väljundist saadud ühe sõnaühiku (token'i) analüüsi tulemused.
 *   
 *   @author Siim Orasmaa
 */
public class MorfAnSona {
	
	/** Kas morf analysaator suutis yldse sona analyysida voi mitte */
	private boolean leidusAnalyys;	
	/** Sona algsel/analyysieelsel kujul */
	private String algSona;
	/** Morf analyysi tulemused: analyysiread */
	private List<MorfAnRida> analyysiTulemused;
	
	/** Kas antud s6na puhul on tegemist lauset l6petava s6naga. */
	private boolean onLauseLopp = false;

	//==============================================================================
	//   	T3-OLP sisendi spetsiifiline
	//==============================================================================
	
	/**
	 *  Kas s6nale j2rgneb kindel osalausepiir (vaid t3olp sisendi korral)?
	 */
	private boolean olpOnKindelPiir = false;
	
	//==============================================================================
	//   	J o o n d u s   e s i a l g s e   t e k s t i g a
	//==============================================================================
	
	/**
	 *  Mitmendana token sisendist loeti? (st token'i järjekorranumber tekstis)
	 *  Loendamine algab 1-st;
	 */
	private int tokenPosition = -1;

	
	
	public MorfAnSona(String algSona) {
	    this.algSona = algSona;
	    this.leidusAnalyys = false;
	    this.analyysiTulemused = new ArrayList<MorfAnRida>();
	}
	
	public void lisaAnalyysiRida(String analyysiRida){
		//  Lisame ainult siis, kui ka tegelikult on tegemist 
		// analyysireaga . .  . 
		if (!analyysiRida.equals("####")){
			MorfAnRida rida = new MorfAnRida(analyysiRida);
			analyysiTulemused.add(rida);
			this.leidusAnalyys = true;
		}
	}
	
	public void lisaAnalyysiRida(MorfAnRida analyysiRida){
		if (analyysiRida != null){
			analyysiTulemused.add(analyysiRida);
			this.leidusAnalyys = true;
		}
	}
	
	public List<MorfAnRida> getAnalyysiTulemused(){
		return analyysiTulemused;
	}
	
	public String getAlgSona() {
		return algSona;
	}
	
	public boolean kasLeidusAnalyys(){
		return leidusAnalyys;
	}

	/**
	 *    Kas tegemist on verbiga? Tagastab <tt>true</tt>, kui leidub v2hemalt 
	 *   yks vastava s6naliigiga analyys;
	 */
	public boolean onVerb(){
		if (!analyysiTulemused.isEmpty()){
			for (MorfAnRida morfAnRida : analyysiTulemused) {
				if (morfAnRida.getSonaliik().equals("_V_")){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 *    Kas tegemist on algv6rdelise omaduss6naga? Tagastab <tt>true</tt>, kui
	 *   leidub v2hemalt yks vastava s6naliigiga analyys;
	 */
	public boolean onAdjektiivPos(){
		if (!analyysiTulemused.isEmpty()){
			for (MorfAnRida morfAnRida : analyysiTulemused) {
				if (morfAnRida.getSonaliik().equals("_A_")){
					return true;
				}
			}
		}
		return false;
	}	

	public boolean onLauseLopp(){
		return onLauseLopp;
	}
	
	public void setOnLauseLopp(boolean onLauseLopp) {
		this.onLauseLopp = onLauseLopp;
	}
	
	/**
	 * Kas selle s6na j2rel on kindel osalausepiir? (kasutusel olp sisendi korral)
	 */
	public boolean onOlpKindelPiir() {
		return olpOnKindelPiir;
	}

	/**
	 * Kas selle s6na j2rel on kindel osalausepiir? (kasutusel olp sisendi korral)
	 */
	public void setOlpOnKindelPiir(boolean olpOnKindelPiir) {
		this.olpOnKindelPiir = olpOnKindelPiir;
	}

	public String toString(){
		StringBuilder str = new StringBuilder();
		str.append(this.algSona);
		if (this.leidusAnalyys){
			for (MorfAnRida rida : this.analyysiTulemused) {
				str.append(" | ");
				str.append(rida.toString());
			}
		}
		return str.toString();
	}

	public int getTokenPosition() {
		return tokenPosition;
	}

	public void setTokenPosition(int tokenPosition) {
		this.tokenPosition = tokenPosition;
	}
	
}
