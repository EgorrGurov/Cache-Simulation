import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
public class CommandParser {
    private static final int MEM_SIZE = 512 * 1024; // Размер всей памяти (в байтах)
    private static final int BEGIN = 65536 / 4; // Отсюда начинаются команды
    private static int programCounterr = 0; // Счётчик команд
    public static int[] memory = new int[MEM_SIZE / 4]; // Память
    public static String[][] commands = new String[MEM_SIZE / 4 - BEGIN][]; // Все команды
    public static List<byte[]> data = new ArrayList<>(); // Данные
    public static String[] newArgs;
    protected static HashMap<String, String> binaryRegisters = new HashMap<>(); // Бинарное представление регистров
    protected static HashMap<String, WordDatum> binaryCommand = new HashMap<>(); // Для парсера команд
    public static String fileIn; // Входной файл
    public static String fileOut; // Выходной файл

    public CommandParser(String fileIn, String fileOut) { // Конструктор нашего парсера
        this.fileIn = fileIn;
        this.fileOut = fileOut;
    }

    public static String zfill(String s, int count) { // Заполнение нулями (когда необходимо)
        StringBuilder zeros = new StringBuilder();
        while (count != 0) {
            zeros.append("0");
            count -= 1;
        }
        if (s.startsWith("0x")) {
            s = s.substring(2);
            int num = Integer.parseInt(s, 16);
            s = Integer.toBinaryString(num);
            s = zeros.substring(s.length()) + s;
            return s;
        } else if (s.startsWith("-0x")) {
            s = s.substring(3);
            int num = Integer.parseInt("-" + s, 16);
            s = Integer.toBinaryString(num);
            s = zeros.substring(s.length()) + s;
            return s;
        }
        int num = Integer.parseInt(s, 10);
        s = Integer.toBinaryString(num);
        s = zeros.substring(s.length()) + s;
        return s;
    }

    public static void loadToData(String s) { // Загрузка байтов в массив данных
        byte[] result = new byte[4];
        for (int i = 3; i >= 0; i--) {
            String byt = "";
            if (i == 3) {
                byt = s.substring(i * 8);
            }
            else{
                byt = s.substring(i * 8, i * 8 + 8);
            }
            result[3 - i] = (byte) (Integer.parseInt(byt, 2));
        }
        data.add(result);
    }

    public void execution() throws IOException {
        binaryRegisters.put("zero", "00000");
        binaryRegisters.put("ra", "00001");
        binaryRegisters.put("sp", "00010");
        binaryRegisters.put("gp", "00011");
        binaryRegisters.put("gp", "00011");
        binaryRegisters.put("tp", "00100");
        binaryRegisters.put("t0", "00101");
        binaryRegisters.put("t1", "00110");
        binaryRegisters.put("t2", "00111");
        binaryRegisters.put("s0", "01000");
        binaryRegisters.put("fp", "01000");
        binaryRegisters.put("s1", "01001");
        binaryRegisters.put("a0", "01010");
        binaryRegisters.put("a1", "01011");
        binaryRegisters.put("a2", "01100");
        binaryRegisters.put("a3", "01101");
        binaryRegisters.put("a4", "01110");
        binaryRegisters.put("a5", "01111");
        binaryRegisters.put("a6", "10000");
        binaryRegisters.put("a7", "10001");
        binaryRegisters.put("s2", "10010");
        binaryRegisters.put("s3", "10011");
        binaryRegisters.put("s4", "10100");
        binaryRegisters.put("s5", "10101");
        binaryRegisters.put("s6", "10110");
        binaryRegisters.put("s7", "10111");
        binaryRegisters.put("s8", "11000");
        binaryRegisters.put("s9", "11001");
        binaryRegisters.put("s10", "11010");
        binaryRegisters.put("s11", "11011");
        binaryRegisters.put("t3", "11100");
        binaryRegisters.put("t4", "11101");
        binaryRegisters.put("t5", "11110");
        binaryRegisters.put("t6", "11111");
        binaryCommand.put("lui", new WordDatum("0110111", 3, 1, new int[]{1, 2}));
        binaryCommand.put("auipc", new WordDatum("0010111", 3, 1, new int[]{1, 2}));
        binaryCommand.put("jal", new WordDatum("1101111", 3, 2, new int[]{1, 2}));
        binaryCommand.put("jalr", new WordDatum("1100111", 4, 3, new int[]{1, 1, 2}));
        binaryCommand.put("beq", new WordDatum("1100011", 4, 4, new int[]{1, 1, 2}));
        binaryCommand.put("bne", new WordDatum("1100011", 4, 4, new int[]{1, 1, 2}));
        binaryCommand.put("blt", new WordDatum("1100011", 4, 4, new int[]{1, 1, 2}));
        binaryCommand.put("bge", new WordDatum("1100011", 4, 4, new int[]{1, 1, 2}));
        binaryCommand.put("bltu", new WordDatum("1100011", 4, 4, new int[]{1, 1, 2}));
        binaryCommand.put("bgeu", new WordDatum("1100011", 4, 4, new int[]{1, 1, 2}));
        binaryCommand.put("lb", new WordDatum("0000011", 4, 5, new int[]{1, 2, 1}));
        binaryCommand.put("lh", new WordDatum("0000011", 4, 5, new int[]{1, 2, 1}));
        binaryCommand.put("lw", new WordDatum("0000011", 4, 5, new int[]{1, 2, 1}));
        binaryCommand.put("lbu", new WordDatum("0000011", 4, 5, new int[]{1, 2, 1}));
        binaryCommand.put("lhu", new WordDatum("0000011", 4, 5, new int[]{1, 2, 1}));
        binaryCommand.put("sb", new WordDatum("0100011", 4, 6, new int[]{1, 2, 1}));
        binaryCommand.put("sh", new WordDatum("0100011", 4, 6, new int[]{1, 2, 1}));
        binaryCommand.put("sw", new WordDatum("0100011", 4, 6, new int[]{1, 2, 1}));
        binaryCommand.put("addi", new WordDatum("0010011", 4, 7, new int[]{1, 1, 2}));
        binaryCommand.put("slti", new WordDatum("0010011", 4, 7, new int[]{1, 1, 2}));
        binaryCommand.put("sltiu", new WordDatum("0010011", 4, 7, new int[]{1, 1, 2}));
        binaryCommand.put("xori", new WordDatum("0010011", 4, 7, new int[]{1, 1, 2}));
        binaryCommand.put("ori", new WordDatum("0010011", 4, 7, new int[]{1, 1, 2}));
        binaryCommand.put("andi", new WordDatum("0010011", 4, 7, new int[]{1, 1, 2}));
        binaryCommand.put("slli", new WordDatum("0010011", 4, 8, new int[]{1, 1, 2}));
        binaryCommand.put("srli", new WordDatum("0010011", 4, 8, new int[]{1, 1, 2}));
        binaryCommand.put("srai", new WordDatum("0010011", 4, 9, new int[]{1, 1, 2}));
        binaryCommand.put("add", new WordDatum("0110011", 4, 8, new int[]{1, 1, 1}));
        binaryCommand.put("sub", new WordDatum("0110011", 4, 9, new int[]{1, 1, 1}));
        binaryCommand.put("sll", new WordDatum("0110011", 4, 8, new int[]{1, 1, 1}));
        binaryCommand.put("slt", new WordDatum("0110011", 4, 8, new int[]{1, 1, 1}));
        binaryCommand.put("sltu", new WordDatum("0110011", 4, 8, new int[]{1, 1, 1}));
        binaryCommand.put("xor", new WordDatum("0110011", 4, 8, new int[]{1, 1, 1}));
        binaryCommand.put("srl", new WordDatum("0110011", 4, 8, new int[]{1, 1, 1}));
        binaryCommand.put("sra", new WordDatum("0110011", 4, 9, new int[]{1, 1, 1}));
        binaryCommand.put("or", new WordDatum("0110011", 4, 8, new int[]{1, 1, 1}));
        binaryCommand.put("and", new WordDatum("0110011", 4, 8, new int[]{1, 1, 1}));
        binaryCommand.put("mul", new WordDatum("0110011", 4, 10, new int[]{1, 1, 1}));
        binaryCommand.put("mulh", new WordDatum("0110011", 4, 10, new int[]{1, 1, 1}));
        binaryCommand.put("mulhsu", new WordDatum("0110011", 4, 10, new int[]{1, 1, 1}));
        binaryCommand.put("mulhu", new WordDatum("0110011", 4, 10, new int[]{1, 1, 1}));
        binaryCommand.put("div", new WordDatum("0110011", 4, 10, new int[]{1, 1, 1}));
        binaryCommand.put("divu", new WordDatum("0110011", 4, 10, new int[]{1, 1, 1}));
        binaryCommand.put("rem", new WordDatum("0110011", 4, 10, new int[]{1, 1, 1}));
        binaryCommand.put("remu", new WordDatum("0110011", 4, 10, new int[]{1, 1, 1}));
        binaryCommand.put("fence", new WordDatum("0110011", 3, 12, new int[]{2, 2}));
        binaryCommand.put("fence.tso", new WordDatum("0001111", 1, 11, new int[]{}));
        binaryCommand.put("pause", new WordDatum("0001111", 1, 11, new int[]{}));
        binaryCommand.put("ecall", new WordDatum("1110011", 1, 11, new int[]{}));
        binaryCommand.put("ebreak", new WordDatum("1110011", 1, 11, new int[]{}));
        try {
            Scanner in = new Scanner(new File(fileIn));
            List<String[]> arguments = new ArrayList<>();
            while (in.hasNext()) {
                String nowCommand = in.next().trim();
                if (!binaryCommand.containsKey(nowCommand)) {
                    throw new IllegalArgumentException("Wrong name of command: " + nowCommand);
                } else {
                    WordDatum info = binaryCommand.get(nowCommand);
                    String[] resultArgs = new String[info.getCountArgs()];
                    resultArgs[0] = nowCommand;
                    int cntArgs = 1;
                    while (info.getCountArgs() > cntArgs && in.hasNext()) {
                        String nextElem = in.next().trim();
                        String[] elem = nextElem.split(",");
                        for (String str : elem) {
                            if (cntArgs >= info.getCountArgs()) {
                                throw new NumberFormatException("Wrong count of args");
                            }
                            resultArgs[cntArgs] = str.trim();
                            cntArgs++;
                        }
                    }
                    if (cntArgs != info.getCountArgs()) {
                        throw new NumberFormatException("Wrong count of args");
                    }
                    arguments.add(resultArgs);
                }
            }
            int number = 0;
            for (String[] currArgs : arguments) {
                newArgs = new String[currArgs.length];
                newArgs[0] = currArgs[0];
                //System.err.println(currArgs[0]);
                String ans = switch (binaryCommand.get(currArgs[0]).getBlock()) {
                    case 1 -> firstBlockParser(currArgs);
                    case 2 -> secondBlockParser(currArgs);
                    case 3 -> thirdBlockParser(currArgs);
                    case 4 -> fourthBlockParser(currArgs);
                    case 5 -> fifthBlockParser(currArgs);
                    case 6 -> sixthBlockParser(currArgs);
                    case 7 -> seventhBlockParser(currArgs);
                    case 8 -> eighthBlockParser(currArgs);
                    case 9 -> ninthBlockParser(currArgs);
                    case 10 -> tenthBlockParser(currArgs);
                    case 11 -> eleventhBlockParser(currArgs[0]);
                    case 12 -> onlyFenceParser(currArgs);
                    default -> throw new IllegalStateException("Unexpected value: " + binaryCommand.get(currArgs[0]).getBlock());
                };
                memory[programCounterr] = CommandExecutor.twoComplement(ans);
                programCounterr++;
                loadToData(ans);
                commands[number] = newArgs;
//                for (String str: commands[number]){
//                    System.out.print(str + " ");
//                }
//                System.out.println();
                number++;
            }
            in.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        CommandExecutor executor = new CommandExecutor(memory, memory, commands);
        executor.generalExecute();
        String formattedCommand1 = (executor.triesCommandToHitCache == 0) ? "nan%" : String.format("%3.5f%%", (double) executor.hitCommandToLRU / executor.triesCommandToHitCache * 100);
        String formattedCommand2 = (executor.triesCommandToHitCache == 0) ? "nan%" : String.format("%3.5f%%", (double) executor.hitCommandTopLRU / executor.triesCommandToHitCache * 100);
        String formatted1 = (executor.triesToHitCache == 0 && executor.triesCommandToHitCache == 0) ? "nan%" : String.format("%3.5f%%", (double) (executor.hitToLRU + executor.hitCommandToLRU) / (executor.triesToHitCache + executor.triesCommandToHitCache) * 100);
        String formatted2 = (executor.triesToHitCache == 0 && executor.triesCommandToHitCache == 0) ? "nan%" : String.format("%3.5f%%", (double) (executor.hitTopLRU + executor.hitCommandTopLRU) / (executor.triesToHitCache + executor.triesCommandToHitCache) * 100);
        String formattedData1 = (executor.triesToHitCache == 0) ? "nan%" : String.format("%3.5f%%", (double) executor.hitToLRU / executor.triesToHitCache * 100);
        String formattedData2 = (executor.triesToHitCache == 0) ? "nan%" : String.format("%3.5f%%", (double) executor.hitTopLRU / executor.triesToHitCache * 100);
        System.out.printf("replacement\thit rate\thit rate (inst)\thit rate (data)\n" +
                "        LRU\t%s%%\t%s%%\t%s%%\n", formatted1, formattedCommand1, formattedData1);
        System.out.printf(
                "       pLRU\t%s%%\t%s%%\t%s%%\n", formatted2, formattedCommand2, formattedData2);
        try {
            FileOutputStream out = new FileOutputStream(fileOut);
            for (byte[] myData : data) {
                out.write(myData);
            }
            out.close();
        } catch (IOException e) {
            System.exit(0);
        }
    }

    public static String firstBlockParser(String[] args) {
        if (args.length != 3) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String register = binaryRegisters.get(args[1]);
        String imm = zfill(args[2], 32);
        newArgs[1] = register;
        newArgs[2] = imm;
        return imm.substring(12, 32) + register + binCommand;
    }

    public static String secondBlockParser(String[] args) {
        if (args.length != 3) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String registerD = binaryRegisters.get(args[1]);
        String imm = zfill(args[2], 32);
        newArgs[1] = registerD;
        newArgs[2] = imm;
        imm = imm.charAt(11) + imm.substring(21, 31) + imm.charAt(20) + imm.substring(12, 20);
        return imm + registerD + binCommand;
    }

    public static String thirdBlockParser(String[] args) {
        if (args.length != 4) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String registerD = binaryRegisters.get(args[1]);
        String register1 = binaryRegisters.get(args[2]);
        String imm = zfill(args[3], 32);
        newArgs[1] = registerD;
        newArgs[2] = register1;
        newArgs[3] = imm;
        return imm.substring(20, 32) + register1 + "000" + registerD + binCommand;
    }

    public static String fourthBlockParser(String[] args) {
        if (args.length != 4) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String number = "";
        if (args[0].equals("beq")) {
            number = "000";
        }
        if (args[0].equals("bne")) {
            number = "001";
        }
        if (args[0].equals("blt")) {
            number = "100";
        }
        if (args[0].equals("bge")) {
            number = "101";
        }
        if (args[0].equals("bltu")) {
            number = "110";
        }
        if (args[0].equals("bgeu")) {
            number = "111";
        }
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String registerD = binaryRegisters.get(args[1]);
        String register1 = binaryRegisters.get(args[2]);
        String imm = zfill(args[3], 32);
        newArgs[1] = registerD;
        newArgs[2] = register1;
        newArgs[3] = imm;
        return imm.charAt(19) + imm.substring(21, 27) + register1 + registerD + number + imm.substring(27, 31) + imm.charAt(20) + binCommand;
    }

    public static String fifthBlockParser(String[] args) {
        if (args.length != 4) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String number = "";
        if (args[0].equals("lb")) {
            number = "000";
        }
        if (args[0].equals("lh")) {
            number = "001";
        }
        if (args[0].equals("lw")) {
            number = "010";
        }
        if (args[0].equals("lbu")) {
            number = "100";
        }
        if (args[0].equals("lhu")) {
            number = "101";
        }
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String registerD = binaryRegisters.get(args[1]);
        String imm = zfill(args[2], 32);
        String register1 = binaryRegisters.get(args[3]);
        newArgs[1] = registerD;
        newArgs[2] = imm;
        newArgs[3] = register1;
        return imm.substring(20, 32) + register1 + number + registerD + binCommand;
    }

    public static String sixthBlockParser(String[] args) {
        if (args.length != 4) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String number = "";
        if (args[0].equals("sb")) {
            number = "000";
        }
        if (args[0].equals("sh")) {
            number = "001";
        }
        if (args[0].equals("sw")) {
            number = "010";
        }
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String registerD = binaryRegisters.get(args[1]);
        String imm = zfill(args[2], 32);
        String register1 = binaryRegisters.get(args[3]);
        newArgs[1] = registerD;
        newArgs[2] = imm;
        newArgs[3] = register1;
        return imm.substring(20, 27) + registerD + register1 + number + imm.substring(27, 32) + binCommand;
    }

    public static String seventhBlockParser(String[] args) {
        if (args.length != 4) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String number = "";
        if (args[0].equals("addi")) {
            number = "000";
        }
        if (args[0].equals("slti")) {
            number = "001";
        }
        if (args[0].equals("sltiu")) {
            number = "010";
        }
        if (args[0].equals("xori")) {
            number = "100";
        }
        if (args[0].equals("ori")) {
            number = "101";
        }
        if (args[0].equals("andi")) {
            number = "111";
        }
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String registerD = binaryRegisters.get(args[1]);
        String register1 = binaryRegisters.get(args[2]);
        String imm = zfill(args[3], 32);
        newArgs[1] = registerD;
        newArgs[2] = register1;
        newArgs[3] = imm;
        return imm.substring(20, 32) + register1 + number + registerD + binCommand;
    }

    public static String eighthBlockParser(String[] args) {
        if (args.length != 4) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String number = "";
        if (args[0].equals("srli") || args[0].equals("srl")) {
            number = "101";
        }
        if (args[0].equals("slli") || args[0].equals("sll")) {
            number = "001";
        }
        if (args[0].equals("add")) {
            number = "000";
        }
        if (args[0].equals("sltu")) {
            number = "011";
        }
        if (args[0].equals("slt")) {
            number = "010";
        }
        if (args[0].equals("xor")) {
            number = "100";
        }
        if (args[0].equals("or")) {
            number = "110";
        }
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String registerD = binaryRegisters.get(args[1]);
        String register1 = binaryRegisters.get(args[2]);
        String register2;
        if (args[0].equals("srli") || args[0].equals("slli")) {
            register2 = zfill(args[3], 5);
        } else {
            register2 = binaryRegisters.get(args[3]);
        }
        newArgs[1] = registerD;
        newArgs[2] = register1;
        newArgs[3] = register2;
        return "0000000" + register2 + register1 + number + registerD + binCommand;
    }

    public static String ninthBlockParser(String[] args) {
        if (args.length != 4) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String number = "";
        if (args[0].equals("srai") || args[0].equals("sra")) {
            number = "101";
        }
        if (args[0].equals("sub")) {
            number = "000";
        }

        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String registerD = binaryRegisters.get(args[1]);
        String register1 = binaryRegisters.get(args[2]);
        String register2;
        if (args[0].equals("srai")) {
            newArgs[3] = zfill(args[3], 32);
            register2 = zfill(args[3], 5);
        } else {
            register2 = binaryRegisters.get(args[3]);
            newArgs[3] = register2;
        }
        newArgs[1] = registerD;
        newArgs[2] = register1;
        return "0100000" + register2 + register1 + number + registerD + binCommand;
    }

    public static String tenthBlockParser(String[] args) {
        if (args.length != 4) {
            throw new NumberFormatException("Wrong number of arguments");
        }
        String number = "";
        if (args[0].equals("mul")) {
            number = "000";
        }
        if (args[0].equals("mulh")) {
            number = "001";
        }
        if (args[0].equals("mulhsu")) {
            number = "010";
        }
        if (args[0].equals("mulhu")) {
            number = "011";
        }
        if (args[0].equals("div")) {
            number = "100";
        }
        if (args[0].equals("divu")) {
            number = "101";
        }
        if (args[0].equals("rem")) {
            number = "110";
        }
        if (args[0].equals("remu")) {
            number = "111";
        }
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String registerD = binaryRegisters.get(args[1]);
        String register1 = binaryRegisters.get(args[2]);
        String register2 = binaryRegisters.get(args[3]);
        newArgs[1] = registerD;
        newArgs[2] = register1;
        newArgs[3] = register2;
        return "0000001" + register2 + register1 + number + registerD + binCommand;
    }

    public static String eleventhBlockParser(String command) { //fence.tso и т.д.
        String binCommand = "0001111";
        String fnPredSucc = switch (command) {
            case "fence.tso" -> "1000" + "0011" + "0011";
            case "pause" -> "0000" + "0001" + "0000";
            case "ecall" -> "000000000000";
            case "ebreak" -> "000000000001";
            default -> throw new IllegalArgumentException("Wrong name of command: " + command);
        };
        if (command.equals("ecall") || command.equals("ebreak")) {
            binCommand = "1110011";
        }
        return fnPredSucc + "0000000000000" + binCommand;
    }

    public static String onlyFenceParser(String[] args) {
        String binCommand = binaryCommand.get(args[0]).getBinaryCommand();
        String pred = args[1];
        String succ = args[2];
        int sumPred = 0;
        int sumSucc = 0;
        for (int i = 0; i < pred.length(); i++){
            switch (pred.charAt(i)){
                case 'i' -> sumPred += 1;
                case 'o' -> sumPred += 2;
                case 'r' -> sumPred += 4;
                case 'w' -> sumPred += 8;
            }
        }
        for (int i = 0; i < succ.length(); i++){
            switch (succ.charAt(i)){
                case 'i' -> sumSucc += 1;
                case 'o' -> sumSucc += 2;
                case 'r' -> sumSucc += 4;
                case 'w' -> sumSucc += 8;
            }
        }
        if (sumPred > 0){
            pred = Integer.toBinaryString(sumPred);
        }
        if (sumSucc > 0){
            succ = Integer.toBinaryString(sumSucc);
        }
        pred = CommandExecutor.zfillBinary(pred, 4);
        succ = CommandExecutor.zfillBinary(succ, 4);
        if (pred.length() > 4){
            pred = pred.substring(pred.length() - 4);
        }
        if (succ.length() > 4){
            succ = succ.substring(succ.length() - 4);
        }
        newArgs[1] = pred;
        newArgs[2] = succ;
        return pred + succ + "00000000000000000" + binCommand;
    }
}
