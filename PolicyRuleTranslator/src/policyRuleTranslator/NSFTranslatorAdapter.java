package policyRuleTranslator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.dom.DeferredElementImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import validatore.Validatore;

public class NSFTranslatorAdapter{							
	private String xsdLanguage, xmlRule, xmlAdapter, outputName;	//xsd con il linguaggio dell'NSF; xml con le regole da convertire a questo NSF; xml con l'elenco delle capability di questo nsf e quindi gl iadapter; nome del file dove generare l'output
	
	//gli elementi protected possono essere visti anche dalle classi che estendono iptablesAdapter
	private String temporaryRule;						//regola temporanea su cui viene riscritta ogni volta la regola (per ogni regola presente in xml)
	protected String temporaryCapaAndAttributes;		//stringa che contiene il nome della capability e una "lista" di nome valore per ogni campo 
	protected String temporaryCapa;						//stringa che contiene il nome della capability
	private List<String> temporaryListCapaOfRule;		//lista che contiene tutte le capability della regola
	private FileWriter fileWriter;						//elemento per permettere la stampa su file
	private PrintWriter printWriter;					//elemento per permettere la stampa su file
	private NodeList adapterNodes;						//elemento che contiene la lista dei nodi securityCapability che contengono le informazioni degli adapter per ogni capability
	protected Element myCapaAdapter;					//elemento che contiene l'adapter della capability preso da 
	
	
//la mia idea è che una NSF può avere una istanza di questa classe, e quando ha bisogno diconvertire chiama adatta() passandogli i relativi xsd, xml, fileDiOutput
	public static void main(String[] args) {

		String languagePathXSD = null;
		String capabilityInstance = null;
		String ruleInstanceXML = null;
		String outputFile = null;
		String starter = null;
		String finisher = null;
		String forced = null;
		boolean scr = false;
		boolean ecr = false;
		
		if(args.length < 3 || args.length > 9) {
			System.out.println("bad arguments error");
			return;
		}
		else {
			languagePathXSD = args[0];
			//System.out.println(languagePathXSD);
			capabilityInstance = args[1];
			//System.out.println(capabilityInstance);
			ruleInstanceXML = args[2];
			//System.out.println(ruleInstanceXML);
			for(int i = 3; i < args.length; i++) {
				if(args[i].contains("+s")) {
					starter = args[i].substring(2);
					//System.out.println(starter);
				}
				else if(args[i].contains("+e")) {
					finisher = args[i].substring(2);
					//System.out.println("fin = "+finisher);
				}
				else if(args[i].contentEquals("-f")) {
					forced = args[i];
					//System.out.println(forced);
				}
				else if(args[i].contentEquals("+crs")) {
					scr = true;
				}
				else if(args[i].contentEquals("+cre")) {
					ecr = true;
				}
				else if(outputFile == null) {
					outputFile = args[i];
					//System.out.println(outputFile);
				}
				else {
					System.out.println("bad arguments format");
					return;
				}
			}
		}
		NSFTranslatorAdapter it = new NSFTranslatorAdapter();
		it.translate(languagePathXSD, capabilityInstance, ruleInstanceXML, outputFile, starter, finisher, forced, scr, ecr);
	}
	
	public NSFTranslatorAdapter() {
		this.temporaryListCapaOfRule = new ArrayList<String>();
	}
	
	public boolean validate() {
		Validatore v = new Validatore(this.xsdLanguage,this.xmlRule);
		if(!v.validate()) {
			System.out.println("the instance is invalid");
			return false;
		}
		return true;
	}

	//funzione che riceve 5 parametri, 
	//1) xsd = linguaggio dell'nsd, file xsd al quale fa riferimento il file xml; 
	//2) xmlRule = file con le regole secondo il linguaggio specificato in xsd; 
	//3) xmlAdapter = file conl quale è stato generato il linguaggio, è il file creato a mano che contiene le capability interessate e i dettagli per l'adapter
	//4) outputName = file su cui fare l'output, nullabile
	//5) startString = parametro stringa che mi permette di stabilire se voglio una "parte" iniziale della stringa uguale per ogni rule, nullabile
	
	public void translate(String xsd, String xmlAdapter, String xmlRule, String outputName, String startString, String endString, String forced, boolean scr, boolean ecr) {
		this.xsdLanguage = xsd;
		this.xmlAdapter = xmlAdapter;
		this.xmlRule = xmlRule;
		this.adapterNodes = getNodelistOfElementFromDocumentByTagname(generateDocument(this.xmlAdapter),"securityCapability");
		String myStartString = startString;
		String myEndString = endString;
		boolean force = false;
		if(forced!=null) {
			force = true;
		}
		
		
		if(!validate()) {	//controllo che l'istanza delle regole nell' xml sia una istanza relativa all'xsd del linguaggio
			return;
		}
		
		if(outputName!=null) {		//controllo se esiste un nome di output o mettere quello di default
			this.outputName = outputName;
		}
		else {
			this.outputName = "policy.txt";
		}
		try {
			 new FileWriter(this.outputName); 							//creo un nuovo file se esistente
			 this.fileWriter = new FileWriter(this.outputName, true);	//Set true for append mode
			 this.printWriter = new PrintWriter(this.fileWriter);
		} catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
		} 
		
		NodeList ruleList = getNodelistOfElementFromDocumentByTagname(generateDocument(this.xmlRule),"rule");	//prendo tutti i nodi che si chiamano rule
		
		for(int i = 0; i < ruleList.getLength(); i++) {															//ciclo sulle regole
			if(myStartString!=null) {																			//azzero la regola attuale e la inizializzo con la stringa passata o con una stringa vuota
				this.temporaryRule = myStartString;
			}
			else {
				this.temporaryRule = ""; 
			}
			if(scr) {
				this.temporaryRule = this.temporaryRule.concat("\n");
			}
			this.temporaryListCapaOfRule = new ArrayList<String>();
			Node rule = ruleList.item(i);
			if(!(rule instanceof DeferredElementImpl)) { 														//fattibile solo includendo la libreria relativa 
				continue;
			}
			Element ruleElement = (Element) rule;																//converto il nodo rule in un elemento per poterlo gestire meglio
			NodeList secCapas = ruleElement.getElementsByTagName("securityCapability");
			//System.out.println("ciclo sulle capa " + secCapas.getLength());
			for(int j = 0; j < secCapas.getLength(); j++) {														//ciclo sulle capability
				if(!(secCapas.item(j) instanceof DeferredElementImpl)) { 										//fattibile solo includendo la libreria relativa 
					continue;
				}
				Element capa = (Element) secCapas.item(j);														//converto il nodo della capability in un elemento per poterlo gestire meglio
				this.temporaryCapaAndAttributes = "";
				ricorriRegola(capa);																			//chiamo la funzione che genera la scrittura della regola nello standard deciso
				//System.out.println(this.temporaryCapa);
				String ret = clauseConverter();																	//traduco la clausola chiamando la funzione che si occupa di parlare con il LanguageAdapter
				if(ret!=null) {
					this.temporaryRule = this.temporaryRule+ret;												//se ho tradotto in qualcosa di utile allora lo aggiungo alla regola
					this.temporaryListCapaOfRule.add(this.temporaryCapa);
				}
			}
			
			if(checkRule()) {																					// controllo se la sintassi della frase

				if(ecr) {
					this.temporaryRule = this.temporaryRule.concat("\n");
				}
				if(myEndString!=null) {
					this.temporaryRule = this.temporaryRule+myEndString;
				}
				
				//System.out.println("la regola è giusta");
				this.printWriter.println(this.temporaryRule);													//stampa su file
			}
			else {
				if(!force) {
					System.out.println("a rule is not correct, the process is stopped.");
					return;
				}
			}
		}
		
		
		System.out.println("translated");
		this.printWriter.close();
	}
	
	
	//genera il documento in base al path, relativo al progetto o globale se si parte da C://
	private static Document generateDocument(String path) {
		DocumentBuilderFactory df;
		DocumentBuilder builder;
		df = DocumentBuilderFactory.newInstance();

		try {
			builder = df.newDocumentBuilder();
			return builder.parse(path);
		} catch (ParserConfigurationException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(0);
		} catch (SAXException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(0);
		} catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(0);
		}
		return null;
	}
	
	//genera la lista dei nodi da un documento rispetto ad un tagName
	private static NodeList getNodelistOfElementFromDocumentByTagname (Document d, String tagname) {
		return d.getElementsByTagName(tagname);
	}
	
	//funzione ricorsiva che esplora un elemento, quando arriva in fondo vuol dire che ha raggiunto gli attributi e li stampa in una stringa fissata, secondo il criterio della funzione stampa()
	private void ricorriRegola(Element e) {
		stampa(e);
		if(e.getChildNodes().getLength()==0) {
			return;
		}
		NodeList nl = e.getChildNodes();
		for(int i = 0; i < nl.getLength(); i++) {
			Node n = nl.item(i);
			if(!(n instanceof DeferredElementImpl)) { //fattibile solo includendo la libreria relativa 
				continue;
			}
			ricorriRegola((Element) n);
		}
	}

	//stampa nella stringa di riferimento l'elemento che viene passato. vengono stampati il tipo di capability che si sta considerando e gli attributi
	private void stampa(Element e) {
		if(e.getAttribute("xsi:type")!="") {
			//System.out.println(e.getAttribute("xsi:type"));
			this.temporaryCapaAndAttributes = this.temporaryCapaAndAttributes.concat(e.getAttribute("xsi:type")+" ");
		}
		else if(e.getChildNodes().getLength()==1) {																	
			//System.out.println("\t"+e.getNodeName());
			this.temporaryCapaAndAttributes =  this.temporaryCapaAndAttributes.concat(e.getNodeName()+" ");
			this.temporaryCapaAndAttributes =  this.temporaryCapaAndAttributes.concat(e.getTextContent()+" ");
		}
	}
		
	private String clauseConverter() {
		
		String pre, mid, body, post;
		
		String[] s = this.temporaryCapaAndAttributes.split(" ");					
		this.temporaryCapa = s[0];														//metto in temporaryCapa il nome della capability che si stà considerando in questo momento

		this.myCapaAdapter = findElementAdapterByCapa();													//cerco l'adapter della capability
		if(this.myCapaAdapter==null) {
			return "non ho l'adapter";
		}
		
		pre = getPre();
		if(pre==null) {
			return null;
		}
		//System.out.println(pre);
		mid = getMid();
		if(mid.equals("\\n")) {
			mid = System.lineSeparator();
		}
		if(mid==null) {
			return null;
		}
		//System.out.println(pre+mid);
		body = getBody();
		if(body==null) {
			return null;
		}
		//System.out.println(pre+mid+body);
		post = getPost();
		if(post.equals("\\n")) {
			post = System.lineSeparator();
		}
		if(post==null) {
			return null;
		}
		//System.out.println(pre+mid+body+post);
		return pre+mid+body+post;
	}
	
	private String getPre() {
		//definito dal command e quindi dalla possibilità di negare il comando

		//qui mi interessano le informazioni contenute in commandName ed in deniable
		String commandName = null;
		//System.out.println(this.temporaryCapaAndAttributes);
		String[] s = this.temporaryCapaAndAttributes.split(" ");
		

		NodeList commandNameList = this.myCapaAdapter.getElementsByTagName("commandName");						//cerco il commandName
		
		List<String> realCommandName = new ArrayList<String>();
		List<String> commandAttributeNameCondition = new ArrayList<String>(), commandAttributeValueCondition = new ArrayList<String>();
		
		
		for(int i = 0 ; i<commandNameList.getLength(); i++) {
			Element e1 = getElemenetIfDeferredElementImpl(commandNameList.item(i));
			if(e1==null)
				continue;
			realCommandName.add(getTextContextFromGetElementByTagName(e1,"realCommandName")); //chiamo la funzione che mi prende il contenuto di realCommandName e lo inserisco nella lista (sia che sia null sia che non lo sia)

			commandAttributeNameCondition.add(getTextContextFromGetElementByTagName(e1,"attributeName")); //chiamo la funzione che mi prende il contenuto di commandAttributeNameCondition e lo inserisco nella lista (sia che sia null sia che non lo sia)

			commandAttributeValueCondition.add(getTextContextFromGetElementByTagName(e1,"attributeValue")); //chiamo la funzione che mi prende il contenuto di commandAttributeValueCondition e lo inserisco nella lista (sia che sia null sia che non lo sia)
			
		}
		/*
		for(int i = 0; i < realCommandName.size();i++) {
			if(!realCommandName.isEmpty())
				System.out.println("nome nella lista " +realCommandName.get(i));
			if(!commandAttributeNameCondition.isEmpty())
				System.out.println("commandAttributeNameCondition nella lista " +commandAttributeNameCondition.get(i));
			if(!commandAttributeValueCondition.isEmpty())
				System.out.println("commandAttributeValueCondition nella lista " +commandAttributeValueCondition.get(i));
		}
		*/
		
		//controllo la clausola operazione, da quella deduco il comando ed in caso se negato. FINE. 
		//perchè anche nel caso di più commandName la lista dovrà restituirmi quel command name relativo a quella condition.
		
		for(int i = 0; i<realCommandName.size(); i++) {											//per ogni comando conosciuto per questa capability
			if(commandAttributeNameCondition.get(i)==null)														//controlla se esiste una condizione per quel comando
				continue;																						//se non esiste continua il ciclo
			if(this.temporaryCapaAndAttributes.contains(commandAttributeNameCondition.get(i))) {				//se esiste controlla se è contenuta nella stringa dei parametri passati riguardanti la clausola	
																												
				for (int j = 0; j< s.length-1; j++) {															//se è presente allora cerco nella stringa splittata qual'è il valore relativo 
																												//che sarà nella casella del vettore successiva a lui. (-1 perchè deve avere un valore, che è scritto dopo)
					if(s[j].contentEquals(commandAttributeNameCondition.get(i))) {								//se trovo lo stesso parametro
						if(s[j+1].contentEquals(commandAttributeValueCondition.get(i))){						//se è presente quel parametro (s[i+1]) controlla che sia lo stesso necessario a questo comando
							return realCommandName.get(i);														//ho trovato il commandName che cercavo
						}
					}
				}
			}
		}
		if(commandName==null) {
			for(int i = 0; i<realCommandName.size(); i++) {
				if(commandAttributeValueCondition.get(i)==null || commandAttributeValueCondition.get(i).contentEquals("EQUAL_TO")) {	//controlla se esiste un comando "di default" che può essere quello senza condizioni di utilizzo o quello che contiene come condizione "EQUAL_TO"
					return realCommandName.get(i);
				}
			}
		}
		return commandName;		
	}
	
	private String getMid() {
		//definito da internalClauseConcatenator
		String mid = getTextContextFromGetElementByTagName(this.myCapaAdapter,"internalClauseConcatenator");
		if(mid==null) {
			return " ";			//mid di default tra il pre e il body
		}
		return mid;
	}
	
	private String getBody() {
		//definito da body concatenator se ci sono più parametri
		//scorro tutti gli elementi di s, se trovo un elemento che può avere il concatenatore allora lo metto, 
		//altrimenti non metto niente e passo al concatenatore dopo, se nessun concatenatore rispetta quella s[i] allora ho concluso 
		//o non mi è stato passato il concatenatore tra quelle due pre e post
		String body = "";
		//System.out.println(this.temporaryCapaAndAttributes);
		String[] s = this.temporaryCapaAndAttributes.split(" ");  
		
		NodeList bodyConcatenator = this.myCapaAdapter.getElementsByTagName("bodyConcatenator");						//cerco il bodyConcatenator
		//System.out.println(this.temporaryCapa);
		List<String> realConcatenator = new ArrayList<String>(), preVariable = new ArrayList<String>(), postVariable = new ArrayList<String>();
		Map<String,String> attributeRegexMap = new HashMap<String,String>();
		Map<String,List<List<Integer>>> attributeFromToMap = new HashMap<String,List<List<Integer>>>();
		
		NodeList bodyValueRestricitonNodeList = this.myCapaAdapter.getElementsByTagName("bodyValueRestriciton");						//cerco il bodyValueRestriciton
		
		for(int i = 0; i< bodyValueRestricitonNodeList.getLength(); i++) {
			Element bodyValueType = getElemenetIfDeferredElementImpl(bodyValueRestricitonNodeList.item(i));
			if(bodyValueType==null)
				continue;
			String regexValue=null, attributeName = null;
			
			NodeList nl = bodyValueType.getElementsByTagName("attributeName");							//cerco il attributeName
			for(int j = 0; j < nl.getLength(); j++) {
				Element e2 = getElemenetIfDeferredElementImpl(nl.item(j));
				if(e2==null)
					continue;
				attributeName = e2.getTextContent();
			}
			
			nl = bodyValueType.getElementsByTagName("regexValue");										//cerco il regexValue
			for(int j = 0; j < nl.getLength(); j++) {
				Element e2 = getElemenetIfDeferredElementImpl(nl.item(j));
				if(e2==null)
					continue;
				regexValue = e2.getTextContent();
			}
			if (regexValue!=null) {
				//System.out.println("l'attributo: "+attributeName+" deve rispettare questa regex: "+regexValue);
				attributeRegexMap.put(attributeName, regexValue);						//inserisco nella mappa il nome dell'attributo e la sua regex, se esistono restrizioni 
			}
			

			List<List<Integer>> allFromToList = new ArrayList<List<Integer>>();
			nl = bodyValueType.getElementsByTagName("integerRange");									//cerco il integerRange
			for(int j = 0; j < nl.getLength(); j++) {
				List<Integer> fromToList = new ArrayList<Integer>();
				Element e2 = getElemenetIfDeferredElementImpl(nl.item(j));
				if(e2==null)
					continue;
				String from, to;
				NodeList fromNL = e2.getElementsByTagName("from");										//prendo il campo from
				from = fromNL.item(0).getTextContent();
				NodeList toNL = e2.getElementsByTagName("to");											//prendo il campo to
				to = toNL.item(0).getTextContent();
				
				if(Integer.valueOf(from)<=Integer.valueOf(to)) {
					fromToList.add(Integer.valueOf(from));
					fromToList.add(Integer.valueOf(to));
					//System.out.println(attributeName+ " " +from+"-"+to);
				}
				else if(Integer.valueOf(from)>Integer.valueOf(to)) {
					fromToList.add(Integer.valueOf(to));
					fromToList.add(Integer.valueOf(from));
					//System.out.println(attributeName+ " " +to+"-"+from);
				}
				
				allFromToList.add(fromToList);
			}
			if(allFromToList.size()>0) {
				attributeFromToMap.put(attributeName, allFromToList);									//aggiungo alla mappa il nome delle amia capability e la sua lista di liste di range

			}
			
		}
		
		
		for(int i = 0 ; i<bodyConcatenator.getLength(); i++) {
			Element e1 = getElemenetIfDeferredElementImpl(bodyConcatenator.item(i));
			if(e1==null)
				continue;
			realConcatenator.add(getTextContextFromGetElementByTagName(e1,"realConcatenator")); //chiamo la funzione che mi prende il contenuto di realConcatenator e lo inserisco nella lista (sia che sia null sia che non lo sia)

			preVariable.add(getTextContextFromGetElementByTagName(e1,"preVariable")); //chiamo la funzione che mi prende il contenuto di preVariable e lo inserisco nella lista (sia che sia null sia che non lo sia)

			postVariable.add(getTextContextFromGetElementByTagName(e1,"postVariable")); //chiamo la funzione che mi prende il contenuto di postVariable e lo inserisco nella lista (sia che sia null sia che non lo sia)
			
		}
		/*
		for(int i = 0; i < realConcatenator.size();i++) {
			if(!realConcatenator.isEmpty())
				System.out.println("realConcatenator nella lista " +realConcatenator.get(i));
			if(!preVariable.isEmpty())
				System.out.println("preVariable nella lista " +preVariable.get(i));
			if(!postVariable.isEmpty())
				System.out.println("postVariable nella lista " +postVariable.get(i));
		}
		*/
	
		List<String> clauseAttributesName = getAllClauseAttributesName();												//creo una lista che mi conterrà i possibili nomi dei campi
		//System.out.println(clauseAttributesName);

		//System.out.println(clauseAttributesName + " " + realConcatenator);
		
		
		//System.out.println("1) "+body);
		for (int i = 0; i < s.length-1; i++) {																			//scorro s che contiene tutti i parametri passati dall'xml dell'istanza della regola
			for (int j = 0; j < clauseAttributesName.size(); j++) {
				
				if(s[i].contentEquals(clauseAttributesName.get(j))) {													//se incontro uno degli attributi possibili
					String regex = getAttributeDefaultRegex(s[i]);															//cerco se l'attributo dovesse avere delle regex di default	
					//System.out.println("2) "+body);
					if(body=="") {

						//devo controllare se l'attributo di nome s[i] ha una regex come default value, in caso applicarla a s[i+1]
						//ho il nome dell'attributo in s[i]
						//ho il nome della capability in this.temporaryCapa
						//devo prendere il default value che contiene la regex	
													
						//System.out.println("la regex dell'attributo "+s[i]+" vale: "+regex);	
						if(regex!=null) {																							//controllo, se c'è, la regex di default
							if(!regexValidity(s[i+1],regex)) {
								//System.out.println("il parametro passato non rispetta la regex del parametro");
								return null;
							}
							
							else {
								//System.out.println("regex rispettata");
							}
							
						}
						if(attributeRegexMap.containsKey(s[i])){																	//controllo, se c'è, la regex di aggiuntiva
							regex=attributeRegexMap.get(s[i]);
							if(!regexValidity(s[i+1],regex)) {
								//System.out.println("il parametro passato "+ s[i+1]+" non rispetta la regex aggiuntiva del parametro " +regex);
								return null;
							}
							
							else {
								//System.out.println("regex aggiuntiva rispettata");
							}
							
						}
						
						
						if(attributeFromToMap.containsKey(s[i])) {														//controllo se ho delle restrizioni sui valori inseribili INTERI
							boolean foundRange = false;
							List<List<Integer>> integerRanges = attributeFromToMap.get(s[i]);
							for(int k = 0; k < integerRanges.size(); k++) {
								int from = integerRanges.get(k).get(0), to = integerRanges.get(k).get(1);
								//System.out.println(from+"-"+to);
								if(Integer.valueOf(s[i+1]) >= from &&  Integer.valueOf(s[i+1]) <= to) {					//se rispetto il range esco dal for e continuo senza problemi
									foundRange = true;
									break;																				
								}
							}
							if(!foundRange) {
								return null;
							}
						}
												
						body = body.concat(s[i+1]); 																	//è il primo pezzo che incontro lo metto subito dentro
						
					}
					else {
						for (int k = 0; k < realConcatenator.size(); k++) {
							
							//System.out.println("3) "+body);
							
							if(i>2 && s[i].contentEquals(postVariable.get(k)) && s[i-2].contentEquals(preVariable.get(k)) && !s[i-1].contentEquals(s[i+1])) {
								//devo controllare se l'attributo di nome s[i] ha una regex come default value, in caso applicarla a s[i+1]
								//System.out.println("la regex dell'attributo "+s[i]+" vale: "+regex);
								if(regex!=null) {																						//controllo se c'è la regex
									if(!regexValidity(s[i+1],regex)) {
										//System.out.println("il parametro passato non rispetta la regex del parametro");
										return null;
									}
									else {
										//System.out.println("regex rispettata");
									}
								}
								if(attributeRegexMap.containsKey(s[i])){																//controllo, se c'è, la regex di aggiuntiva
									regex=attributeRegexMap.get(s[i]);
									if(!regexValidity(s[i+1],regex)) {
										//System.out.println("il parametro passato non rispetta la regex aggiuntiva del parametro");
										return null;
									}
									
									else {
										//System.out.println("regex aggiuntiva rispettata");
									}
									
								}
								if(attributeFromToMap.containsKey(s[i])) {																	//controllo se ho delle restrizioni sui valori inseribili INTERI
									boolean foundRange = false;
									List<List<Integer>> integerRanges = attributeFromToMap.get(s[i]);
									for(int h = 0; h < integerRanges.size(); h++) {
										int from = integerRanges.get(h).get(0), to = integerRanges.get(h).get(1);
										//System.out.println(from+"-"+to);
										if(Integer.valueOf(s[i+1]) > from &&  Integer.valueOf(s[i+1]) < to) {					//se rispetto il range esco dal for e continuo senza problemi
											foundRange = true;
											break;																				
										}
									}
									if(!foundRange) {
										return null;
									}
								}
								
								body = body.concat(realConcatenator.get(k)+s[i+1]); 
							}
						}
					}
					break;	//è importantissimo questo break perchè in clauseAttributesName ci possono essere doppioni! quindi questo ferma il ciclo dopo aver trovato per la prima volta quel valore (caso in cui ci sono doppioni verrebbero fatte più volte iterazioni e azioni non dovute)
				}
			}
		}
		
		return body;
	}
	
	private String getPost() {
		//definito da clauseConcatenator
		String post = getTextContextFromGetElementByTagName(this.myCapaAdapter,"clauseConcatenator");
		if(post==null) {
			return " ";			//mid di default tra il pre e il body
		}
		return post;		
	}
	
	//cerca un elemento in base alla capability che si sta considerando in questo momento
	private Element findElementAdapterByCapa () {
		for(int i = 0; i<this.adapterNodes.getLength(); i++) {
			
			if(!(this.adapterNodes.item(i) instanceof DeferredElementImpl)) { 										//fattibile solo includendo la libreria relativa 
				continue;
			}
			Element e = (Element) this.adapterNodes.item(i);												//questo elemento contiene la security capability con il relativo adapter
			String capaname = e.getAttribute("xsi:type");
			String[] split = capaname.split(":");
			if(split.length==1) {
				capaname=split[0];
			}
			else {
				capaname = split[1];
			}
			if(this.temporaryCapa.contentEquals(capaname)) {										//questo è l'elemento relativo alla mia capability
				//System.out.println(capaname);		
				return e;
			}
		}
		return null;
	}
	
	// funzione che controlla se il nodo è un nodo di tipo elemento, se vero ne ritorna l'Elemento castato altrimenti torna null
	private Element getElemenetIfDeferredElementImpl(Node n) { 
		if(n instanceof DeferredElementImpl) { 		
			return (Element) n;								//fattibile solo includendo la libreria relativa 
		}
		return null;
	}
	
	//ritorna la stringa contenuta nell'elemento passato in base al tag passato
	private String getTextContextFromGetElementByTagName(Element e, String s) {

		NodeList nl = e.getElementsByTagName(s);
		for (int j = 0; j < nl.getLength();j++) {
			Element e2 = getElemenetIfDeferredElementImpl(nl.item(j));
			if(e2==null)
				continue;
			if(e2.getTextContent()==null)
				return null;
			else
				return e2.getTextContent();
		}
		return null;
	}
	
	//stampa nella stringa di riferimento l'elemento che viene passato. vengono stampati il tipo di capability che si sta considerando e gli attributi
	private List<String> getAllClauseAttributesName() {
		List<String> attributes = new ArrayList<String>();
		NodeList allCapas = getNodelistOfElementFromDocumentByTagname(generateDocument(this.xsdLanguage), "xs:complexType");
		for(int i = 0 ; i<allCapas.getLength(); i++) {
			Element e1 = getElemenetIfDeferredElementImpl(allCapas.item(i));
			if(e1==null)
				continue;
			if(e1.getAttribute("name").contentEquals(this.temporaryCapa)) {
				//ho trovato la capability interessata
				NodeList nl2 = e1.getElementsByTagName("xs:element");
				for(int j = 0; j<nl2.getLength(); j++) {
					Element e2 = getElemenetIfDeferredElementImpl(nl2.item(j));
					if(e2==null)
						continue;
					createListAttributes(allCapas,attributes,e2);
				}
			}
		}
		return attributes;
	}
	
		
	//crea in modo ricorsivo la lista degli attributi appartenenti all'elemento, se l'elemento è un tipo complesso allora lo cerca
	private void createListAttributes (NodeList elementNL, List<String> ls, Element e){
		
		int i;
		if(e.getAttribute("type")!="") {
			for (i = 0; i <elementNL.getLength(); i++) {
				Element e1 = getElemenetIfDeferredElementImpl(elementNL.item(i));
				if(e1==null)
					continue;
				if(e1.getAttribute("name").contentEquals(e.getAttribute("type"))){
					createListAttributes(elementNL,ls,e1);
					break;
				}
			}
			if(i==elementNL.getLength()) {
				//System.out.println(e.getAttribute("name"));
				ls.add(e.getAttribute("name"));
				return;
			}
		}
		
		NodeList nl = e.getElementsByTagName("xs:element");
		for(i = 0; i< nl.getLength(); i++) {
			Element e1 = getElemenetIfDeferredElementImpl(nl.item(i));
			if(e1==null)
				continue;
			createListAttributes(elementNL,ls,e1);
		}
		return;
	}
	
	//funzione che cerca in tutto il file del linguaggio l'attributo desiderato della capability che si sta considerando e ne ritorna, se esiste, la regex (NON è ricorsivo! va solo al primo livello del type
	private String getAttributeDefaultRegex(String attributeName) {
		NodeList allCapas = getNodelistOfElementFromDocumentByTagname(generateDocument(this.xsdLanguage), "xs:complexType");
		Element capa = getCapabilityElementFromNodeList(this.temporaryCapa,allCapas); 											//ho trovato la capability interessata
		if(capa==null)
			return null;
		//System.out.println("capa: "+capa.getAttribute("name"));
		NodeList nl = capa.getElementsByTagName("xs:element");
		for(int j = 0; j<nl.getLength(); j++) {
			Element e = getElemenetIfDeferredElementImpl(nl.item(j));
			if(e==null)
				continue;
			//System.out.println("e: "+e.getAttribute("name"));
			if(e.getAttribute("default")!="" && e.getAttribute("name").contentEquals(attributeName)) {							//se questo elemento contiene un defalut ed ha lo stesso nome richiesto torno il default
				//System.out.println("return e.getattribute: "+e.getAttribute("default"));
				return e.getAttribute("default");
			}
			
			Element e2 = getCapabilityElementFromNodeList(e.getAttribute("type"),allCapas);	
			if(e2==null)
				continue;
			//System.out.println("e2: "+e2.getAttribute("name"));
			NodeList nl2 = e2.getElementsByTagName("xs:element");
			for(int i = 0; i<nl2.getLength();i++) {
				Element e3 = getElemenetIfDeferredElementImpl(nl2.item(i));
				if(e3==null)
					continue;
				//System.out.println("e3: "+e3.getAttribute("name"));
				if(e3.getAttribute("default")!="" && e3.getAttribute("name").contentEquals(attributeName)) {							//se questo elemento contiene un deault ed ha il nome dell'attributo cercato
					//System.out.println("e3 default: "+e3.getAttribute("default"));
					return e3.getAttribute("default");
				}
			}
		}
		return null;
	}

	//cerca in una nodelist se esiste il tipo dell'elemento passato
	private Element getCapabilityElementFromNodeList(String capa, NodeList nl) {
		for(int i = 0 ; i<nl.getLength(); i++) {
			Element e1 = getElemenetIfDeferredElementImpl(nl.item(i));
			if(e1==null)
				continue;
			if(e1.getAttribute("name").contentEquals(capa)) {
				return e1;
			}
		}
		return null;
	}
	
	//funziona che verifica se le regole delle dipendenza sono soddisfatte
	private boolean checkRule() {
		for(int i = 0 ; i < this.temporaryListCapaOfRule.size(); i++) {											//per ogni capability che ho nella regola
//devono essere TUTTE rispettate le dipendenze delle capability
			boolean found = true;
			
			//System.out.println(this.temporaryListCapaOfRule.get(i));												
			this.temporaryCapa = this.temporaryListCapaOfRule.get(i);											//metto la capability in temporary capa perchè findElementAdapterByCapa lavora con quella capa
			this.myCapaAdapter = findElementAdapterByCapa();													//cerco l'adapter della capability

			//System.out.println("analizzo la capa: "+this.temporaryCapa);
			if(this.myCapaAdapter==null) {
				System.out.println("la capa: "+this.temporaryCapa+" non ha adapter");
				return false;
			}
			
			NodeList dependencyNodeList = this.myCapaAdapter.getElementsByTagName("dependency");						//cerco le dependency
			
			//List<String> respectedDependencyOfCapa = new ArrayList<String>();
			
			for(int j = 0 ; j<dependencyNodeList.getLength(); j++) {
//per ogni capability guardo se ALMENO UNA delle dipendenze in or è verificata
				List<String> presenceOfCapability = new ArrayList<String>(), presenceOfValue = new ArrayList<String>(), absenceOfCapability = new ArrayList<String>(), absenceOfValue = new ArrayList<String>();
				found = true;
				Element e1 = getElemenetIfDeferredElementImpl(dependencyNodeList.item(j));
				if(e1==null)
					continue;
				
				presenceOfCapability = getListOfTextValueOfElementByTagNameFromElement(e1,"presenceOfCapability");			// genero la lista di stringhe relative a quell'elemento
				presenceOfValue = getListOfTextValueOfElementByTagNameFromElement(e1,"presenceOfValue");					// genero la lista di stringhe relative a quell'elemento
				absenceOfCapability = getListOfTextValueOfElementByTagNameFromElement(e1,"absenceOfCapability");			// genero la lista di stringhe relative a quell'elemento
				absenceOfValue = getListOfTextValueOfElementByTagNameFromElement(e1,"absenceOfValue");						// genero la lista di stringhe relative a quell'elemento
			
				
				 /*
				for(int k = 0; k < presenceOfCapability.size(); k++) {
					if(!presenceOfCapability.isEmpty())
						System.out.println("presenceOfCapability nella lista " +presenceOfCapability.get(k));
				}
				for(int k = 0; k < presenceOfValue.size(); k++) {
					if(!presenceOfValue.isEmpty())
						System.out.println("presenceOfValue nella lista " +presenceOfValue.get(k));
				}
				for(int k = 0; k < absenceOfCapability.size(); k++) {
					if(!absenceOfCapability.isEmpty())
						System.out.println("absenceOfCapability nella lista " +absenceOfCapability.get(k));
				}
				for(int k = 0; k < absenceOfValue.size(); k++) {
					if(!absenceOfValue.isEmpty())
						System.out.println("absenceOfValue nella lista " +absenceOfValue.get(k));
				}
				*/
				
				for(int k=0; k<presenceOfCapability.size(); k++) {														//per ogni capability dalla quale dipendo
					if(presenceOfCapability.get(k)==null)
						continue;
					boolean found2 = false;
					for(int h = 0; h<this.temporaryListCapaOfRule.size(); h++) {										//guardo se in tutte le capability della mia regola c'è
						if(presenceOfCapability.get(k).contentEquals(this.temporaryListCapaOfRule.get(h))) {			//se la trovo
							found2 = true;																				//bene
						}
					}
					if(!found2) {																						//se finisco il giro senza trovarla found rimane a false e quindi la regola non è rispettata
						found = false;
					}
				}				
				for(int k=0; k<presenceOfValue.size(); k++) {															//per ogni valore dalla quale dipendo
					if(presenceOfValue.get(k)==null)
						continue;
					if(!this.temporaryRule.contains(presenceOfValue.get(k)))											//se non lo contengo
						found = false;																					//metto found a false;
				}
				for(int k=0; k<absenceOfCapability.size(); k++) {														//per ogni capability dalla quale dipendo
					if(absenceOfCapability.get(k)==null)
						continue;
					boolean found2 = false;
					for(int h = 0; h<this.temporaryListCapaOfRule.size(); h++) {										//guardo se in tutte le capability della mia regola c'è
						if(absenceOfCapability.get(h).contentEquals(this.temporaryListCapaOfRule.get(h))) {				//se la trovo
							found2 = true;																				//male
						}
					}
					if(found2) {																						//se finisco il giro senza trovarla found rimane a false e quindi la regola è rispettata
						found = false;
					}
				}		
				for(int k=0; k<absenceOfValue.size(); k++) {															//per ogni valore dalla quale dipendo
					if(absenceOfValue.get(k)==null)
						continue;
					if(this.temporaryRule.contains(absenceOfValue.get(k)))												//se lo contengo
						found = false;																					//metto found a falso
				}
				
				if(found) {																								//se ho trovato un true vuol dire che ho trovato tutta una dipendenza che funziona
					//System.out.println("ho trovato le dipendenze rispettate!!!");											
					break;
				}
			}
			//se ho una capa che non rispetta allora return false
			if(!found)
				return false;
		}
		return true;																									//se non trovo niente di vero allora ritorno falso.
	}
	
	//funzione che ritorna una lista contenente tutti i valori testuali contenuti negli elementi figlio dell'elemento passato
	private List<String> getListOfTextValueOfElementByTagNameFromElement (Element e, String tagName){
		List<String> ls = new ArrayList<String>();

		NodeList nl = e.getElementsByTagName(tagName);
		for (int k = 0; k < nl.getLength(); k++) {
			Element e2 = getElemenetIfDeferredElementImpl(nl.item(k));
			if(e2==null)
				continue;
			//System.out.println(tagName+" "+e2.getTextContent());
			ls.add(e2.getTextContent());
		}
		
		return ls;
	}
	
	//verifica la validità di value rispetto alla regex regex
	private boolean regexValidity(String value, String regex) {
		Pattern pattern = Pattern.compile(regex);
    	Matcher matcher = pattern.matcher(value);
		return matcher.matches();
	}
	

}
