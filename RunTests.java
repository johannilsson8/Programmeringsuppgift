import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RunTests {

    public static void main(String[] args) {
        System.out.println("Startar testningen...\n");
        Helhetstest();
        felRegler();
        felAntalParts();
        System.out.println("Testning över\n");
    }

    private static void Helhetstest(){
        //Test 1: Helhetstest (Jämför outputen med en korrekt output)
        try {
            ConvertToXML.main(new String[]{"tests/input/HelhetsTestInput.txt"});

            if (compareFiles("output.xml", "tests/output/HelhetsTestOutput.xml")) {
                System.out.println("Test 1: Helhetstest - RÄTT");
            } else {
                System.out.println("Test 1: Helhetstest - FEL");
            }
        } 
        catch (Exception e) {
            System.err.println("Fel när testet kördes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void felRegler(){
        //Test 2: Fel Regler (ska kasta exception)
        try {
            ConvertToXML.main(new String[]{"tests/input/FelRegler.txt"});
            System.out.println("Test 2: Fel Regler - FEL (Ingen exception kastades)");
        } 
        catch (Exception e) {
            System.out.println("Test 2: Fel Regler - RÄTT (Fick exception: " + e.getMessage() + ")");
        }
    }

    private static void felAntalParts(){
        // Test 3: Fel Antal Parts (ska kasta exception)
        try {
            ConvertToXML.main(new String[]{"tests/input/FelAntalPartsInput.txt"});
            System.out.println("Test 3: Fel Antal Parts - FEL (Ingen exception kastades)");
        } 
        catch (Exception e) {
            System.out.println("Test 3: Fel Antal Parts - RÄTT (Fick exception: " + e.getMessage() + ")");
        }
    }
        
    //Jämför två filer och returnera true om de är identiska
    private static boolean compareFiles(String file1, String file2) throws IOException {
        var lines1 = Files.readAllLines(Paths.get(file1));
        var lines2 = Files.readAllLines(Paths.get(file2));
    
        //Kollar rad-antal
        if (lines1.size() != lines2.size()) {
            System.out.println("Filerna har olika antal rader.");
            return false;
        }
    
        //Normaliserar rad för rad och jämför
        for (int i = 0; i < lines1.size(); i++) {
            String line1 = lines1.get(i).trim().replaceAll("\\r\\n?", "\n");
            String line2 = lines2.get(i).trim().replaceAll("\\r\\n?", "\n");
            if (!line1.equals(line2)) {
                System.out.printf("Skillnad på rad %d:\n'%s'\n'%s'\n", i + 1, line1, line2);
                return false;
            }
        }
        return true;
    } 
}

