package ee.ut.soras.osalau;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import ee.ut.soras.wrappers.impl.T3OLPReader;
import ee.ut.soras.wrappers.impl.VabaMorfJSONReader;
import ee.ut.soras.wrappers.mudel.MorfAnSona;

/**
 *    Osalausestaja peaklass. Loeb sisendi, teostab eelt88tluse, kutsub v2lja 
 *   osalausestaja ja kirjutab osalausestamise tulemuse v2ljundisse.  
 *  
 *  @author Siim Orasmaa
 */
public class Main {
	
	private static void kuvaAbiInfo(){
		System.out.println();
		System.out.println("  Osalausete tuvastaja");
		System.out.println();
		System.out.println(" Sisendi formaadi maaramine:");
		System.out.println("  -format t3mesta -- Filosofti t3mesta valjund + lausepiirid;");
		System.out.println("          json    -- vabamorfi JSON valjund (vaikimisi);");
		System.out.println(" Valjund on samas formaadis, mis sisend - lisatud on vaid osalause-");
		System.out.println(" piiride margendid.");
		System.out.println();
		System.out.println(" Sisendi ja valjundi allika maaramine: ");
		System.out.println("  -pyvabamorf          -- yhe rea kaupa JSON sisendi t88tlus: loeb standard-");
		System.out.println("                          sisendist rea, analyysib seda ja kirjutab tulemuse");
		System.out.println("                          standardv2ljundisse.");
		System.out.println("  -in  stdin           -- standardsisendist lugemine (vaikimisi); ");
		System.out.println("       file <fileName> -- sisend loetakse failist <fileName>;");
		System.out.println("  -out stdout          -- standardvaljundisse kirjutamine (vaikimisi);");
		System.out.println("       file <fileName> -- valjund kirjutatakse faili <fileName>;");
		System.out.println();
		System.out.println("  NB! Eeldatakse, et sisend on alati UTF-8 kodeeringus, valjundisse ");
		System.out.println(" kirjutatav sisu on samuti alati UTF-8 kodeeringus. ");
		System.out.println();
		System.out.println("  -unesc_DBS     -- asendab sisendis kahekordsed \\ m2rgid yhekordsetega;");
		System.out.println("  -pretty_print  -- trykib JSON v2ljundi ilusti joondatult;");
		System.out.println("  -ins_comma_mis -- v2iksem tundlikkus komavigade suhtes;");
		System.out.println();
	}

	public static void main(String[] args) {
		// ==================================================
		//   Parse command line arguments
		// ==================================================
		String format     = "json";
		String inputType  = "stdin";
		String inputFile  = null;
		String outputType = "stdout";
		String outputFile = null;
		boolean unescapeDoubleBackSlashes  = false;
		boolean prettyPrintJson            = false;
		boolean pyVabamorfProcessing       = false;
		boolean insensitiveToMissingCommas = false;
		if (args.length > 0){
			for (int i = 0; i < args.length; i++) {
				// Kuvame abiinfo
				if (args[i].matches("(?i)(-){1,2}(h|help|abi|appi)")){
					kuvaAbiInfo();
					System.exit(0);
				}
				if (args[i].matches("(?i)(-){1,2}pyvabamorf")){
					pyVabamorfProcessing = true;
				}
				// Sisendi/v2ljundi formaat
				if (args[i].matches("-format")  &&  i+1<args.length  &&  args[i+1].matches("(t3mesta|json)")){
					format = args[i+1];
				}
				// Sisendi allikas
				if (args[i].matches("-in")  &&  i+1<args.length){
					if (args[i+1].matches("(stdin|file)")){
						inputType = args[i+1];
						if (inputType.equalsIgnoreCase("file")){
							if (i+2<args.length){
								inputFile = args[i+2];
							} else {
								kuvaAbiInfo();
								System.exit(0);								
							}
						}						
					} else {
						kuvaAbiInfo();
						System.exit(0);						
					}
				} else if (args[i].matches("-infile")  &&  i+1<args.length){
					inputType = "file";
					inputFile = args[i+1];
				}
				// V2ljundi allikas
				if (args[i].matches("-out")  &&  i+1<args.length){
					if (args[i+1].matches("(stdout|file)")){
						outputType = args[i+1];
						if (outputType.equalsIgnoreCase("file")){
							if (i+2<args.length){
								outputFile = args[i+2];
							} else {
								kuvaAbiInfo();
								System.exit(0);								
							}
						}						
					} else {
						kuvaAbiInfo();
						System.exit(0);						
					}
				} else if (args[i].matches("-outfile")  &&  i+1<args.length){
					outputType = "file";
					outputFile = args[i+1];
				}
				if (args[i].matches("(?i)(-){1,2}(ins_comma_mis)")){
					insensitiveToMissingCommas = true;
				}
				// Kaigaste \\ muutmine kujule \
				if (args[i].matches("(?i)(-){1,2}(unesc_DBS)")){
					unescapeDoubleBackSlashes = true;
				}
				// JSON v2ljundi pretty print
				if (args[i].matches("(?i)(-){1,2}(pretty_?print)")){
					prettyPrintJson = true;
				}
			}
		}
		// ==================================================
		//   *) Pyvabamorfi JSON rezhiim
		// ==================================================	
		if (pyVabamorfProcessing){
			Osalausestaja osalausestaja = new Osalausestaja();
			Pattern emptyString = Pattern.compile("^\\s*$");
			try {
				// ---- V6tame sisendi standardsisendist
				Scanner sc = new Scanner(System.in, "UTF-8");
				PrintStream ps = new PrintStream(System.out, false, "UTF-8");
				while ( sc.hasNextLine() ) {
					String line = sc.nextLine();
					if ((emptyString.matcher(line)).matches()){
						ps.println( line );
						ps.flush();
						//System.out.println(line);
						//System.out.flush();
					} else {
						String result = osalausestaja.osalausestaPyVabamorfJSON(line, insensitiveToMissingCommas);
						ps.println( result );
						ps.flush();
						//System.out.println(result);
						//System.out.flush();
					}
				}
				sc.close();
				ps.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
			System.exit(0);
		}
		// ==================================================
		//   *) Sisend
		// ==================================================
		String sisendSone = null;	
		if (inputType.equalsIgnoreCase("stdin")){
			try {
				sisendSone = readInputFromStdIn();
			} catch (Exception e) {
				System.err.println("Viga: Standardsisendist lugemine ebaonnestus!");
				e.printStackTrace();
				System.exit(-1);
			}
		} else if (inputType.equalsIgnoreCase("file") && inputFile != null){
			// ---- V6tame sisendi failist
			try {
				sisendSone = readInputFromFile(inputFile);
			} catch (Exception e) {
				System.err.println("Viga: Failist lugemine ebaonnestus!");
				e.printStackTrace();
				System.exit(-1);
			} 
		}
		// ==================================================
		//   *) Osalausepiiride leidmine & m2rgendus
		// ==================================================
		List<MorfAnSona> tekstiSonad      = null;
		List<OsalauSona> margendatudSonad = null;
		try {
			if (unescapeDoubleBackSlashes){
				sisendSone = sisendSone.replace("\\\\", "\\");
			}
			BufferedReader inputReader = new BufferedReader( new StringReader(sisendSone) );
			if (format.equals("json") ){
				tekstiSonad = VabaMorfJSONReader.parseJSONtext(inputReader);
			} else if (format.equals("t3mesta") ){
				tekstiSonad = T3OLPReader.parseT3OLPtext(inputReader);
			}
			inputReader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		if (tekstiSonad != null){
			Osalausestaja ol = new Osalausestaja();
			margendatudSonad = ol.osalausesta( tekstiSonad, insensitiveToMissingCommas ); 
		}

		// ==================================================
		//   *) V2ljund
		// ==================================================
		String valjundSone = sisendSone; 
		try {
			if (margendatudSonad != null){
				if (format.equals("json") ){
					valjundSone = 
						ValjundiVormistaja.vormistaTulemusVabaMorfiJSONkujul(
								sisendSone, margendatudSonad, prettyPrintJson);
				} else if (format.equals("t3mesta") ){
					valjundSone = 
						ValjundiVormistaja.vormistaTulemusT3OLPkujul(
								sisendSone, margendatudSonad, false);
				}
			}
			if (outputType.equalsIgnoreCase("stdout")){
				PrintStream ps = new PrintStream(System.out, false, "UTF-8");
				ps.print( valjundSone );
				ps.flush();
				ps.close();
				//System.out.print( valjundSone ); 
			} else if (outputType.equalsIgnoreCase("file") && outputFile != null){
				printIntoFile(valjundSone, "UTF-8", outputFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Loeb sisendi UTF-8 kodeeringus standardsisendist
	 */
	private static String readInputFromStdIn() throws IOException {
		StringBuffer puhver = new StringBuffer();
		BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(System.in, "UTF-8"));
		int character;
		while ((character = stdInput.read()) != -1) {
			puhver.append( Character.toChars(character) );
		}
		stdInput.close();
		return puhver.toString();
	}

	/**
	 * Loeb sisendi UTF-8 kodeeringus failist
	 */
	private static String readInputFromFile(String inputFile) throws Exception {
		StringBuilder sb = new StringBuilder();			
		try {
			FileInputStream fstream = new FileInputStream( inputFile );
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader sisend = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String rida = null;
			while ((rida = sisend.readLine()) != null){
				if (rida.length() > 0){
					sb.append(rida + "\n");
				}
			}			    
		} catch (Exception e) {
			throw e;
		}
		return sb.toString();
	}

	/**
	 *  Kirjutab etteantud sisu content uude faili nimega outputFileName kasutades kodeeringut encoding.
	 */
	public static void printIntoFile(String content, String encoding, String outputFileName) throws Exception {
		// Stream to write file
		FileOutputStream fout = null;
		// Open an output stream
		fout = new FileOutputStream (outputFileName);
		OutputStreamWriter out = 
			new OutputStreamWriter(new BufferedOutputStream(fout), encoding);
		// Pring content
		out.write(content);
		out.flush();
		out.close();
		// Close our output stream
		fout.close();
	}

}
