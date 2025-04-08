import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        String fileIn = "";
        String fileOut = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--asm")) {
                fileIn = args[++i];
            } else if (args[i].equals("--bin")){
                fileOut = args[++i];
            }else{
                System.err.println("Wrong format of entering files");
                System.exit(1);}

        }
        CommandParser result = new CommandParser(fileIn, fileOut);
        result.execution();
    }
}
