package ee.ut.soras.osalau;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ee.ut.soras.wrappers.TextUtilsForMorph;
import ee.ut.soras.wrappers.mudel.MorfAnSona;

/**
 *   Yks tekstis6na/token koos selle morfoloogilise m2rgendusega ning osalausete tuvastamisel
 *  vajaliku/leitud informatsiooniga (oeldise olemasolu, osalausepiiride oletatav ja kindel 
 *  olemsaolu jms).
 *  
 *  @author Siim Orasmaa
 */
public class OsalauSona {

	// ======================================================
	//    S6na algne kuju ja informatsioon
	//    ( sisendist )
	// ======================================================
	
	private MorfAnSona morfSona = null;
	private String normAlgSona = null;
	
	public OsalauSona(MorfAnSona morfSona) {
		this.morfSona = morfSona;
		this.normAlgSona = TextUtilsForMorph.normalizeSpecialSymbols( (this.morfSona).getAlgSona() );
	}

	public MorfAnSona getMorfSona() {
		return morfSona;
	}
	
	public String getNormAlgSona() {
		return normAlgSona;
	}
	
	// ======================================================
	//    Osalausestaja poolt lisatav/lisatud 
	//    informatsioon
	// ======================================================

	/** Osalausestaja poolt kasutatavad margendid. */
	public static enum MARGEND {
		 // S6na/tokeni j2rel on kindel osalausepiir 
		 KINDEL_PIIR,
		 // S6na/tokeni j2rel on oletatav osalausepiir		 
		 OLETATAV_PIIR,
		 // S6na/tokeni ees on kiilu algus
		 KIILU_ALGUS,
		 // S6na/tokeni j2rel on kiilu l6pp
		 KIILU_LOPP,
		 
		 // S6na/token on 8eldis (v6i osa mitmes6nalisest 8eldisest)
		 OELDIS,
		 
		 // S6na/tokeni ees otsese k6ne algus
		 KONE_ALGUS,
		 // S6na/tokeni j2rel otsese k6ne lopp
		 KONE_LOPP
	};

	private List<MARGEND> margendid = null;
	
	/**
	 *   Seob antud s6naga etteantud margendi;
	 */
	public void lisaMargend(MARGEND margend){
		if (this.margendid == null){
			this.margendid = new ArrayList<MARGEND>();
		}
		(this.margendid).add(margend);
	}
	
	/**
	 *   Eemaldab etteantud margendi k6ik esinemised antud
	 *  s6na m2rgendite j2rjendist;
	 */
	public void eemaldaMargend(MARGEND margend){
		if (this.margendid != null){
			Iterator<MARGEND> iterator = (this.margendid).iterator();
			while (iterator.hasNext()){
				MARGEND olemasolevMargend = iterator.next();
				if (olemasolevMargend.equals(margend)){
					iterator.remove();
				}
			}
		}
	}
	
	/**
	 *   Sisuliselt: lisab antud s6na m2rgendite hulka m2rgendi <code>uus</code> ning
	 *  kustutab hulgast k6ik m2rgendi <code>vana</code> esinemised;
	 */
	public void asendaMargend(MARGEND vana, MARGEND uus){
		this.lisaMargend(uus);
		this.eemaldaMargend(vana);
	}
	
	/**
	 *   Sisuliselt: lisab antud s6na m2rgendite hulka m2rgendi <code>uus</code> ning
	 *  kustutab hulgast k6ik m2rgendi <code>vana</code> esinemised;<br><br>
	 *  Range: enne eelmainitud operatsioonide l2biviimist kontrollitakse, et kustutatav
	 *  m2rgend ikka t6esti olemas oleks, ning et lisatav m2rgend puudu oleks - kui yks
	 *  neist tingimustest ei kehti, siis operatsioone ei teostata;
	 */
	public void asendaMargendRange(MARGEND vana, MARGEND uus){
		if (this.omabMargendit(vana) && !this.omabMargendit(uus)){
			this.lisaMargend(uus);
			this.eemaldaMargend(vana);
		}
	}

	/**
	 *   Tagastab true, kui s6naga on seotud etteantud margend;
	 */
	public boolean omabMargendit(MARGEND margend){
		return (this.margendid != null && (this.margendid).contains(margend));
	}

	/**
	 *  Tagastab kindlad osalausepiirim2rgendid - KINDEL_PIIR, KIILU_ALGUS, KIILU_LOPP - kui 
	 *  neid on s6naga soetud; 
	 */
	public List<MARGEND> getKindladOLPMargendid() {
		List<MARGEND> kindladMargendid = new ArrayList<OsalauSona.MARGEND>();
		if (this.margendid != null){
			for (MARGEND margend : this.margendid) {
				if (margend == MARGEND.KINDEL_PIIR){
					kindladMargendid.add(margend);
				} else if (margend == MARGEND.KIILU_ALGUS){ 
					kindladMargendid.add(margend);
				} else if (margend == MARGEND.KIILU_LOPP){ 
					kindladMargendid.add(margend);
				}
			}
		}
		return kindladMargendid;
	}
	
	public List<MARGEND> getMargendid() {
		return margendid;
	}
	
}
