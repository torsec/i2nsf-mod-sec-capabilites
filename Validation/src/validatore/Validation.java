package validatore;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;

public class Validation {
	
	public static void main(String[] args) {
		String xsd, xml;
		
		if (args.length == 2 ){
			xsd = args[0];
			xml = args[1];
		}
		else {
			System.out.println("bad arguments error");
			return;
		}
		
		
		Validation val = new Validation();
		System.out.println(val.validate(xsd, xml));
	}

	public Validation() {
	}
	
    public boolean validate(String xsd, String xml) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
        	Schema schema = schemaFactory.newSchema(new File(xsd));

            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new File(xml)));
            return true;
        } catch (SAXException e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        } catch (IOException e){
            System.out.println("Error: " + e.getMessage());
        	return false;
        }
    }
    
}
