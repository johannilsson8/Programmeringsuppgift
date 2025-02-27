
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;


public class ConvertToXML {

    //Map för att ange vad som tillåts att komma efter Building, Owner och Company
    private static final Map<String, List<String>> allowedNext = Map.of(
        "B", Arrays.asList("O", "C", "A"),
        "O", Arrays.asList("T", "A"),
        "C", Arrays.asList("T", "A")
    );

    private String recentType = ""; //Håller reda på senaste typen
    private String currentIndent = ""; //Håller reda på nurvarande indention

    private final Stack<String> openTags = new Stack<>(); //Håller reda på öppna taggar
    private BufferedWriter xmlFileWriter; //Används för att skriva till output-filen


    public static void main(String[] args) throws Exception{
        //Default-vörden
        String inputFile = "data/indata.txt";
        String outputFile = "output.xml";

        //Om det finns argument, använd dem (Används för testen)
        if (args.length >= 1) {
            inputFile = args[0];
        }
        if (args.length >= 2) {
            outputFile = args[1];
        }

        try {
            ConvertToXML converter = new ConvertToXML();
            converter.convertLegacyToXML(inputFile, outputFile);
        } 
        catch (Exception e) {
            throw new Exception("Fel vid konverteringen: " + e.getMessage() + "\n");
        }
    }


    private void convertLegacyToXML(final String inputFile, final String outputFile) throws Exception{
        try (

            //Instansiera en reader och writer för att läsa från inputfilen och skriva till outputfilen
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            

            StringBuilder fullLine = new StringBuilder();
            String line;
            this.xmlFileWriter = writer;

            //Börja alltid med att skapa en buildings-tag
            try {
                openTag("", "buildings");
            } 
            catch (IOException e) {
                throw new Exception("Fel vid öppning av tag: " + e.getMessage());
            }

            //Processera alla rader i input-filen
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; //Hoppa över tomma rader

                //Om raden börjar med B, A, O, C eller T, se till att tidigare data processeras. 
                //Används för att hantera indata som stäcker sig över flera rader)
                if (line.matches("^[BAOCT]\\|.*") && fullLine.length() > 0) {
                    processLine(fullLine.toString());
                    fullLine.setLength(0);
                }

                //Lägg samman datan från de olika raderna om fullLine inte är tom"
                if (fullLine.length() > 0) fullLine.append(" ");
                fullLine.append(line);
            }

            //Processera sista raden
            if (fullLine.length() > 0) {
                processLine(fullLine.toString());
            }

            //Stäng alla öppna taggar när allting är processerat
            while (!openTags.isEmpty()) {
                closeTag(openTags.pop());
            }

            System.out.println("Konvertering klar och sparad till: " + outputFile);

   } 
   catch (IOException e) {
       throw new Exception(e.getMessage());
   }
}

    //Hantera en rad i inputdatan
    private void processLine(final String line) throws IOException{

        String[] parts = line.split("\\|"); //Splitta de olika komponenterna av indatan
        
        //Kolla så att parts[0] finns
        if (parts.length == 0) {
            throw new IllegalArgumentException("Fel format: Förväntade minst ett element i raden.");
        }
        String type = parts[0];
        
        //Kolla om föregående typ får följas av den nuvarande typen
        try {
            isAllowed(type);
        } catch (IllegalArgumentException e) {
            throw new IOException("Felaktig radsekvens: " + e.getMessage(), e);
        }

        switch(type){
            case "B":
                validateParts(parts, 2); 
                closeOpenCompanyAndOwner();
                //Stäng buildings om den är öppen
                if (openTags.contains("building")) {
                    closeTag("building");
                    openTags.remove("building");
                }
                openTag(type, "building");
                writeInXMLFormat("name", parts[1]);
                break;

            case "A":
                if (parts.length < 3 || parts.length > 4) {
                    throw new IllegalArgumentException("Addressen måste ha 3 eller 4 parts (Typ, Gatuaddress, Stad, Postkod (Valfri)");
                }
                openTag(type, "address");
                writeInXMLFormat("street", parts[1]);
                writeInXMLFormat("city", parts[2]);

                if (parts.length == 4 && !parts[3].isEmpty()) {
                    writeInXMLFormat("zipcode", parts[3]);
                }
                closeTag(openTags.pop());
                break;
            
            case "O":
                validateParts(parts, 2);
                closeOpenCompanyAndOwner(); //Stäng öppna company and owner innan en ny owner öppnas
                openTag(type, "owner");
                writeInXMLFormat("name", parts[1]);
                break;
            
            case "C":
                validateParts(parts, 3);
                closeOpenCompanyAndOwner(); //Stäng öppna company and owner innan en ny owner öppnas
                openTag(type, "company");
                writeInXMLFormat("name", parts[1]);
                writeInXMLFormat("type", parts[2]);
                break;
            
            case "T":
                validateParts(parts, 3);
                openTag(type, "phone");
                writeInXMLFormat("landline", parts[1]);
                writeInXMLFormat("fax", parts[2]);
                closeTag(openTags.pop());
                break;

            default:
                throw new IOException("Radtypen är inte B, A, O, C eller T: " + type);
        }
    }

    //Öppna en tag
    private void openTag(final String type, final String tag) throws IOException{
        xmlFileWriter.write(currentIndent + "<" + tag + ">\n");
        openTags.push(tag);
        currentIndent += "  ";
        recentType = type;
    }

    //Stäng en tag
    private void closeTag(final String tag) throws IOException{
        
        //Kolla så att currentIndent-längden är minst 2 för att undvika IndexOutOfBounds
        if (currentIndent.length() >= 2) {
            currentIndent = currentIndent.substring(0, currentIndent.length() - 2);
        }
        xmlFileWriter.write(currentIndent + "</" + tag + ">\n");
    }

    //Stäng öppna company och owner taggar
    private void closeOpenCompanyAndOwner() throws IOException {
        while (!openTags.isEmpty() && (openTags.peek().equals("company") 
                || openTags.peek().equals("owner"))) {
            closeTag(openTags.pop());
        }
    }
    
    //Skriv till outputfilen i XML format
    private void writeInXMLFormat(final String tag, final String content) throws IOException {
        xmlFileWriter.write(currentIndent + "<" + tag + ">" + content + "</" + tag + ">\n");
    }

    //Kolla om föregående typ får följas av den nurvarande typen
    private void isAllowed(final String type) {

        //Det är bara B, O och C som har "regler".
        if(!recentType.toUpperCase().equals("B") && !recentType.toUpperCase().equals("O") && !recentType.toUpperCase().equals("C")) {
            return;
        }

        if (!recentType.isEmpty() && !allowedNext.getOrDefault(recentType.toUpperCase(), List.of()).contains(type.toUpperCase())) {
            throw new IllegalArgumentException("Error: " + type + " is not allowed after " + recentType);
        }
    }

    //Validera att det finns rätt antal delar i indatan
    private void validateParts(final String[] parts, final int expectedLength) throws IOException {
        if (parts.length != expectedLength) {
            throw new IOException("Felaktigt antal delar för " + parts[0] + ": Förväntades " + expectedLength + ", men fick " + parts.length + ". Rad: " + Arrays.toString(parts));
        }
    } 
}
    