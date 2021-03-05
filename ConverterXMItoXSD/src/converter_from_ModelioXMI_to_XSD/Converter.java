package converter_from_ModelioXMI_to_XSD;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.dom.DeferredElementImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


//si può aggiungere la possibilità di generare le associazioni e le classi di associazioni, che per ora non vengono considerate
//genero un elemento root di una classe chiamata NSF

public class Converter {

	public static void main(String[] args) {
		
		String pathXMI = null;
		String otuputPath = null;
		
		if (args.length == 2 ){
			pathXMI = args[0];
			otuputPath = args[1];
			
		}
		else if (args.length == 1) {
			pathXMI = args[0];
			otuputPath = "converted.xsd";
		}
		else {
			System.out.println("bad arguments error");
			return;
		}

		//System.out.println(pathXMI);
		//System.out.println(otuputPath);
		
		Document d = null;
		DocumentBuilderFactory df;
		DocumentBuilder builder;
		df = DocumentBuilderFactory.newInstance();
		XSDgenerator gen = new XSDgenerator();
		
		try {
			builder = df.newDocumentBuilder();
			d = builder.parse(pathXMI);
			
			//Element documentElement = d.getDocumentElement(); 											//contiene l'elemento xmi:XMI
			//NodeList model = documentElement.getElementsByTagName("uml:Model"); 							//contiene l'elemento uml:Model, è di classe DeepNodeListImpl
			//Element elementoNodiInModel = (Element) model.item(0);										//dato che model contiene un elemento unico prendo quello e lo converto ad element
			//NodeList packagedElement = elementoNodiInModel.getElementsByTagName("packagedElement");		//prendo gli elementi che si chiamano "packagedElement" contenuti nel modello
			NodeList packagedElement = d.getElementsByTagName("packagedElement");							//si può sostituire coi quattro comandi sopra.
			//System.out.println(packagedElement.getLength());		
			for(int i = 0; i < packagedElement.getLength(); i++) {
				if(packagedElement.item(i) instanceof DeferredElementImpl) {
					Element element = (Element) packagedElement.item(i);
					if (element.getAttribute("xmi:type").equalsIgnoreCase("uml:Class")) {				//controllo se il packagedElement rappresenta una classe
						
						Element complexType = gen.newElement("complexType");							//ne creo l'elemento
						gen.addAttribute(complexType, "name", element.getAttribute("name"));			//assegno il nome all'elemento
						gen.addNewElement(complexType);													//lo aggiungo al root
						
						NodeList list = element.getChildNodes();										//prendo tutti i nodi figlio dell'elemento
						
						Element general = null;
						Element complexContent = null;
						Element extension = null;
						Element sequence = null;
						Element internalElement = null;
						
						for(int j = 0; j < list.getLength(); j++) {										//scorro tutti i figli per trovare le caratteristiche che interessano
							if(!(list.item(j) instanceof DeferredElementImpl)) { 						//fattibile solo includendo la libreria relativa 
								continue;
							}
							general = (Element) list.item(j);											//creo un nuovo elemento per ogni figlio indipendentemente da che tipo sia, verrà analizzato con gli "if" dopo
							
							if (list.item(j).getNodeName().equalsIgnoreCase("elementImport")) {			//gestisco un element import
								continue;
							}
							if (list.item(j).getNodeName().equalsIgnoreCase("ownedAttribute")) {			//gestisco un element ownedAttribute
								if(sequence==null) {														//se non esiste ancora la sequence la creo altrimenti devo aggiungerlo alla stessa sequence
									sequence = gen.newElement("sequence");
								}
								internalElement = gen.newElement("element");
								gen.addAttribute(internalElement, "name", general.getAttribute("name"));
								
								//aggiungo il tipo se esiste
								if(general.getAttribute("type") != "") {																				//cerco se esiste l'attributo type, vuol dire che non è di tipo standard
									//System.out.println(general.getAttribute("type"));
									String s = findNameOfPackagedElementByIdandType(packagedElement,general.getAttribute("type"),"uml:Enumeration");	//controllo tra le enumerazioni se esiste questo tipo
									if(s==null) {
										s = findNameOfPackagedElementByIdandType(packagedElement,general.getAttribute("type"),"uml:Class");				//controllo tra le classi se esiste questo tipo
										if(s==null) {
											System.out.println("no type definition found..... "+general.getAttribute("type"));							//se arrivo qui non ho trovato il tipo.. e non è gestito uscita di emergenza
										}
									}
									gen.addAttribute(internalElement, "type", s);
								}
								
										
								NodeList value = general.getChildNodes();											//prendo tutti i figli di general
								
								Element nodo = null;
								if(value.getLength()!=0) {
									boolean exist = false;
									//ho dei nodi interni
									for(int k = 0; k < value.getLength(); k++) {
										if(!(value.item(k) instanceof DeferredElementImpl)) {				 //fattibile solo includendo la libreria relativa 
											continue;
										}
										nodo = (Element) value.item(k);
										String value1 = null;
										//System.out.println(nodo.getNodeName());
										//System.out.println(value.item(k).getNodeName()); 												
										if(value.item(k).getNodeName().equalsIgnoreCase("defaultValue")) {								//gestisco il default value 
											Element defaultValue = (Element) value.item(k);
											if(defaultValue.getAttribute("value")=="") {
												continue;
											}
											String s = defaultValue.getAttribute("value");												//bisogna modificare le quot
											//System.out.println(s);
											if(s.contains("\"")) {
												s = s.replace("\"","");
												//System.out.println("replacessato "+s);
											}
											gen.addAttribute(internalElement, "default", s);
										}
										else if (value.item(k).getNodeName().equalsIgnoreCase("lowerValue")) {							//gestisco il lowerValue
											if(nodo.getAttribute("value")=="") {
												value1 = "0"; 
											}
											else {
												value1 = nodo.getAttribute("value");
											}

											gen.addAttribute(internalElement, "minOccurs", value1);
										}
										else if (value.item(k).getNodeName().equalsIgnoreCase("type")) {								//gestisco il type
											if(nodo.getAttribute("xmi:type").equalsIgnoreCase("uml:PrimitiveType")) {
												String href = nodo.getAttribute("href");
												String[] split = href.split("#");
												value1 = split[1]; 
												if(value1.toLowerCase().contains("float")) {
													value1="float";
												}
												value1 = "xs:"+value1.toLowerCase();
											}
											gen.addAttribute(internalElement, "type", value1);
											
										}
										else if (value.item(k).getNodeName().equalsIgnoreCase("upperValue")) {							//gestisco il upperValue
											exist = true;
											if(nodo.getAttribute("value").equalsIgnoreCase("*")) {
												value1 = "unbounded"; 
											}
											else if(nodo.getAttribute("value")!="" ){
												value1 = nodo.getAttribute("value").toString();
											}
											else {
												value1 = "0"; 																//se viene indicato un uppervalue senza valore vuol dire che è 0.
											}
											//System.out.println(value1);
											gen.addAttribute(internalElement, "maxOccurs", value1);
										}
										
									}
									if(!exist) {
										gen.addAttribute(internalElement, "maxOccurs", "1");			//se non esiste la clausola upperValue mette di default 1 
									}
									
								}
								sequence.appendChild(internalElement);
								if(extension != null) {
									extension.appendChild(sequence);
								}
								else {
									complexType.appendChild(sequence);
								}
								
								continue;
							}
							
							if(list.item(j).getNodeName().equalsIgnoreCase("generalization")) {										//gestisco il generalization
								
								complexContent = gen.newElement("complexContent");
								complexType.appendChild(complexContent);
								extension = gen.newElement("extension");
								
								String s = findNameOfPackagedElementByIdandType(packagedElement,general.getAttribute("general"),"uml:Class");
								if(s==null) {
									return;
								}
								
								gen.addAttribute(extension, "base", s);
								
								
								complexContent.appendChild(extension);
								
								continue;
							}
						}
					}
					
					else if (element.getAttribute("xmi:type").equalsIgnoreCase("uml:Enumeration")) {	//controllo se il packagedElement rappresenta una enumerazione
						Element simpleType = gen.newElement("simpleType");								//ne creo l'elemento
						gen.addAttribute(simpleType, "name", element.getAttribute("name"));
						gen.addNewElement(simpleType);
						
						NodeList list = element.getChildNodes();										//prendo tutti i nodi figlio dell'elemento
						
						Element general = null;
						Element restriction = null;
						
						for(int j = 0; j < list.getLength(); j++) {										//scorro tutti i figli per trovare le caratteristiche che interessano
							if(!(list.item(j) instanceof DeferredElementImpl)) { 						//fattibile solo includendo la libreria relativa 
								continue;
							}
							general = (Element) list.item(j);											//creo un nuovo elemento per ogni figlio indipendentemente da che tipo sia, verrà analizzato con gli "if" dopo
							
							if (list.item(j).getNodeName().equalsIgnoreCase("ownedLiteral")) {			//quando incontro un figlio di questo tipo allora ho trovato un valore di questa enumerazione
								if(restriction==null) {
									restriction = gen.newElement("restriction");
									gen.addAttribute(restriction, "base", "xs:string");
									simpleType.appendChild(restriction);
								}
								Element enumeration = gen.newElement("enumeration");
								gen.addAttribute(enumeration, "value", general.getAttribute("name"));
								restriction.appendChild(enumeration);
								
							}
						}
					}
					
					
					//else if uml:Association; uml:AssociationClass
				}
			}
			
			
		} catch (ParserConfigurationException e) {
            System.out.println("Error: " + e.getMessage());
			return;
		} catch (SAXException e) {
            System.out.println("Error: " + e.getMessage());
			return;
		} catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
			return;
		}
		
		Element root = gen.newElement("element");				//genero l'elemento root, nsf
		root.setAttribute("name", "nsf");
		root.setAttribute("type", "NSF");
		gen.addNewElement(root);
		
		if(gen.transform(otuputPath)) {
			System.out.println("capability data model converted to XSD.");
		}
		
	}
	
	
	public static String findNameOfPackagedElementByIdandType(NodeList nl, String id, String classe) {
		for(int i = 0 ; i < nl.getLength(); i++) { 
			Element element = (Element) nl.item(i);
			if(element.getAttribute("xmi:type").equalsIgnoreCase(classe) && element.getAttribute("xmi:id").equals(id)) {
				//System.out.println(id+" "+element.getAttribute("name"));
				return element.getAttribute("name");
			}
		}
		return null;
		
	}
	
	
}
