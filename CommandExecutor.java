import java.util.Arrays;

public class CommandExecutor {
    // 3-й вариант, комментарии себе для понимания (проверяющим тоже можно пользоваться <3)
    private static final int MEM_SIZE = 512 * 1024; // Размер всей памяти (в байтах)
    private static final int ADDR_LEN = 19; // Длина адреса: для кэша разбивается на tag + index + offset
    private static final int CACHE_WAY = 4; // Количество кэш-линий в SET'е
    private static final int CACHE_TAG_LEN = 10; // Уникальный идентификатор кэш-линии (его длина)
    private static final int CACHE_INDEX_LEN = 4; // Определяет количество SET'ов
    private static final int CACHE_OFFSET_LEN = 5; // Определяет размер кэш-линии
    private static final int CACHE_SIZE = 2 * 1024; // Общий размер кэша
    private static final int CACHE_LINE_COUNT = 64; // Количество кэш-линий
    private static final int CACHE_LINE_SIZE = 32; // Размер одной кэш линии (32 байта)
    private static final int CACHE_SETS = 16; // Количество SET'ов
    public int hitToLRU = 0; // Попадания в кэш LRU
    public int hitTopLRU = 0; // Попадания в кэш pLRU
    public int triesToHitCache = 0;
    public int hitCommandToLRU = 0; // Попадания команд в кэш LRU
    public int hitCommandTopLRU = 0; // Попадания команд в кэш pLRU
    public int triesCommandToHitCache = 0;
    public int pc = 0; // Счетчик команд
    public static final int BEGIN = 65536; // Адрес начала выполнения команд

    private String[][] commands; // Массив команд
    private int[] registers = new int[32]; // Всего 32 регистра (GPR, SR, FR, IP)
    private int[] memoryLRU; // Память LRU
    private int[] memorypLRU; // Память pLRU
    private int[][][] cacheLRU = new int[CACHE_SETS][CACHE_WAY][CACHE_LINE_SIZE / 4]; //Last Recently Used
    private int[][][] cachepLRU = new int[CACHE_SETS][CACHE_WAY][CACHE_LINE_SIZE / 4]; //bit-Pseudo LRU
    private int[][] countOfLRU = new int[16][4]; // Количество использований кэш-линии (для вытеснения)
    private int[][] bitOfpLRU = new int[16][4]; // Массив bit'ов в pLRU (для вытеснения)
    private int[][] tagLRU = new int[16][4]; // Массив tag'ов в LRU
    private int[][] tagpLRU = new int[16][4]; // Массив tag'ов в pLRU
    private boolean[][] likeInMemoryLRU = new boolean[16][4]; // для write-back проверка на память
    private boolean[][] likeInMemorypLRU = new boolean[16][4]; // аналогично

    public CommandExecutor(int[] memoryLRU, int[] memorypLRU, String[][] commands){ // Передаем в конструктор память и набор команд
        this.memoryLRU = memoryLRU;
        this.memorypLRU = memorypLRU;
        this.commands = commands;
    }
    public void generalExecute(){ // Главное выполнение всех команд
        for (int i = 0; i < 16; i++){ // Ставим -1 tag'и, как неиспользованные
            Arrays.fill(tagLRU[i], -1);
            Arrays.fill(tagpLRU[i], -1);
        }
        while (pc < commands.length && commands[pc] != null){
            String[] args = commands[pc];
            triesCommandToHitCache++;
            checkCommandInCacheLRU(BEGIN + pc * 4);
            checkCommandInCachepLRU(BEGIN + pc * 4);
            String name = args[0].toLowerCase();
//            System.out.println(name);
//            System.out.println(pc);
//            System.out.println(triesCommandToHitCache);
//            System.out.println(hitCommandToLRU);
//            System.out.println(hitCommandTopLRU);
//            System.out.println();
            switch (name){
                case "lb" -> LB(args);
                case "lh" -> LH(args);
                case "lw" -> LW(args);
                case "lbu" -> LBU(args);
                case "lhu" -> LHU(args);
                case "sb" -> SB(args);
                case "sh" -> SH(args);
                case "sw" -> SW(args);
                case "lui" -> LUI(args);
                case "auipc" -> AUIPC(args);
                case "jal" -> JAL(args);
                case "jalr" -> JALR(args);
                case "beq" -> BEQ(args);
                case "bne" -> BNE(args);
                case "blt" -> BLT(args);
                case "bge" -> BGE(args);
                case "bltu" -> BLTU(args);
                case "bgeu" -> BGEU(args);
                case "addi" -> ADDI(args);
                case "slti" -> SLTI(args);
                case "sltiu" -> SLTIU(args);
                case "xori" -> XORI(args);
                case "ori" -> ORI(args);
                case "andi" -> ANDI(args);
                case "slli" -> SLLI(args);
                case "srli" -> SRLI(args);
                case "srai" -> SRAI(args);
                case "add" -> ADD(args);
                case "sub" -> SUB(args);
                case "sll" -> SLL(args);
                case "slt" -> SLT(args);
                case "sltu" -> SLTU(args);
                case "xor" -> XOR(args);
                case "srl" -> SRL(args);
                case "sra" -> SRA(args);
                case "or" -> OR(args);
                case "and" -> AND(args);
                case "mul" -> MUL(args);
                case "mulhu" -> MULHU(args);
                case "mulh" -> MULH(args);
                case "mulhsu" -> MULHSU(args);
                case "div" -> DIV(args);
                case "divu" -> DIVU(args);
                case "rem" -> REM(args);
                case "remu" -> REMU(args);
                case "fence", "ecall", "ebreak", "pause", "fence.tso" -> {}
                default -> throw new IllegalArgumentException("Wrong name of command: " + name);
            }
            if (!isBranchOrJumpInstructions(name)){ // Счетчик команд +1 для всех команд, которые не меняют его
                pc++;
            }
        }
    }
    public static String zfillBinary(String s, int count){
        if (s.length() >= count){
            return s;
        }
        else{
            return "0".repeat(count - s.length()) + s;
        }
    }
    private int getTag(int address){
        return address >> 9;
    }
    private int getSet(int address){
        return (address >> 5) % 16;
    }
    private int getOffset(int address){
        return address % 32;
    }
    private int getAddress (int tag, int set){ //В кэш линии 32-байта, начинаются с адреса, определяемого tag'ом и set'ом
        //System.out.println(tag + " " + set);
        return (tag * 16 * 32 + set * 32) / 4; // Делим на 4, поскольку одна команда состоит из 4 байт
    }
    private boolean isBranchOrJumpInstructions(String name){
        return (name.equals("beq") || name.equals("bne") || name.equals("blt") || name.equals("bge")
                || name.equals("bltu") || name.equals("bgeu") || name.equals("jal") || name.equals("jalr"));
    }
    private void executeBranchInstructions(String name, int offset, int rs1, int rs2){
        boolean flag = switch (name) {
            case "beq" -> registers[rs1] == registers[rs2];
            case "bne" -> registers[rs1] != registers[rs2];
            case "blt" -> registers[rs1] < registers[rs2];
            case "bge" -> registers[rs1] >= registers[rs2];
            default -> throw new RuntimeException("Wrong name of command");
        };
        if (flag) {
            pc = pc + offset / 4; // Опять же делим на 4, поскольку одна команда - 4 байта
        }
        else{
            pc++; // Иначе просто +1 счетчик команд
        }
    }
    private void changeCacheLRU(int set, int i, int offset, int loadingByte){
        String nowData = zfillBinary(Integer.toBinaryString(cacheLRU[set][i][offset / 4]), 32);
        //System.out.println(nowData);
        if (offset % 4 == 3){
            cacheLRU[set][i][offset / 4] = twoComplement(nowData.substring(0, 24) + zfillBinary(Integer.toBinaryString(loadingByte), 8));
        }
        else {
            cacheLRU[set][i][offset / 4] = twoComplement(nowData.substring(0, (offset % 4) * 8) + zfillBinary(Integer.toBinaryString(loadingByte), 8) + nowData.substring((offset % 4 + 1) * 8));
        }
    }
    private void changeCachepLRU(int set, int i, int offset, int loadingByte){
        String nowData = zfillBinary(Integer.toBinaryString(cachepLRU[set][i][offset / 4]), 32);
        if (offset % 4 == 3){
            cachepLRU[set][i][offset / 4] = twoComplement(nowData.substring(0, 24) + zfillBinary(Integer.toBinaryString(loadingByte), 8));
        }
        else {
            cachepLRU[set][i][offset / 4] = twoComplement(nowData.substring(0, (offset % 4) * 8) + zfillBinary(Integer.toBinaryString(loadingByte), 8) + nowData.substring((offset % 4 + 1) * 8));
        }
    }
    private void analyzeMissLRU(boolean hit, int set, int address, int offset, int tag, int isLoad, int bytes){ // Один и тот же код, для получения/загрузки байта при промахе в кэш
        if (bytes != -1) { // bytes = -1 при load, иначе >= 0
            String nowData = zfillBinary(Integer.toBinaryString(memoryLRU[address / 4]), 32);
            bytes = Integer.parseInt(nowData.substring((offset % 4) * 8, (offset % 4 + 1) * 8), 2);
        }
        for (int i = 0; i < 4; i++){
            if (tagLRU[set][i] == -1){
                hit = true;
                tagLRU[set][i] = tag;
                countOfLRU[set][i]++;
                likeInMemoryLRU[set][i] = isLoad != -1;
                for (int j = 0; j < 8; j++) {
                    cacheLRU[set][i][j] = memoryLRU[(address-offset) / 4 + j];
                }
                if (isLoad != -1) { // Если load, то меняем CacheLRU
                    changeCacheLRU(set, i, offset, isLoad);
                }
                break;
            }
        }
        if (!hit){ // tag нет в кэше и в нем нет места
            int number = 0; // В кэш-линию с этим номером поставим новый адрес
            int minCount = countOfLRU[set][0];
            for (int i = 1; i < 4; i++){
                if (countOfLRU[set][1] < minCount){
                    minCount = countOfLRU[set][1];
                    number = i;
                }
            }
            tagLRU[set][number] = tag;
            countOfLRU[set][number]++;
            likeInMemoryLRU[set][number] = isLoad != -1; // При загрузке - 1, иначе - 0
            int beginAddress = getAddress(tag, set);
            for (int j = 0; j < 8; j++) {
                memoryLRU[beginAddress + j] = cacheLRU[set][number][j];
                cacheLRU[set][number][j] = memoryLRU[(address-offset) / 4 + j];
            }
            if (isLoad != -1) {
                changeCacheLRU(set, number, offset, isLoad);
            }
        }
    }
    private void checkBitsInpLRU(int set, int i){ // Вытеснение с помощью dirty бита
        int nowBits = 0;
        for (int j = 0; j < 4; j++){
            if (bitOfpLRU[set][j] == 0){
                break;
            }
            nowBits++;
        }
        if (nowBits == 4){
            Arrays.fill(bitOfpLRU[set], 0);
        }
        bitOfpLRU[set][i] = 1;
    }
    private int withoutRemainder(int number, int divider){
        return number / divider * divider; // equal number - number % divider
    }
    public static int twoComplement(String s){ // Функция дополнения до двух
        if (s.length() < 32){
            return Integer.parseInt(s, 2);
        }
        else{
            int firstBit = s.charAt(0) - '0';
            return Integer.parseInt(s.substring(1), 2) + firstBit * (1 << 31); // "+" - потому что 1 << 31 уже отрицательный
        }
    }
    private void loadBytesLRU(int loadingByte, int address){ // Загрузка байта в LRU
        int tag = getTag(address);
        int set = getSet(address);
        int offset = getOffset(address);
        boolean hit = false;
        for (int i = 0; i < 4; i++){
            if (tag == tagLRU[set][i]){
                hit = true;
                hitToLRU++;
                countOfLRU[set][i]++;
                likeInMemoryLRU[set][i] = true;
                changeCacheLRU(set, i, offset, loadingByte);
                break;
            }
        }
        if (!hit){ // Если данного tag нет в кэше
            analyzeMissLRU(hit, set, address, offset, tag, 1, -1);
        }
    }
    private int getBytesLRU (int address){ // Получаем данные из кэша LRU
        int tag = getTag(address);
        int set = getSet(address);
        int offset = getOffset(address);
        boolean hit = false;
        int bytes = 0;
        for (int i = 0; i < 4; i++){
            if (tag == tagLRU[set][i]){
                hit = true;
                hitToLRU++;
                countOfLRU[set][i]++;
                likeInMemoryLRU[set][i] = false;
                String nowData = zfillBinary(Integer.toBinaryString(cacheLRU[set][i][offset / 4]), 32);
                bytes = Integer.parseInt(nowData.substring((offset % 4) * 8, (offset % 4 + 1) * 8), 2);
                break;
            }
        }
        if (!hit){
            analyzeMissLRU(hit, set, address, offset, tag, -1, bytes);
        }
        return bytes;
    }
    private void loadBytespLRU(int loadingByte, int address) { // Загружаем байт в pLRU
        int tag = getTag(address);
        int set = getSet(address);
        int offset = getOffset(address);
        boolean hit = false;
        for (int i = 0; i < 4; i++) {
            if (tag == tagpLRU[set][i]) {
                hit = true;
                hitTopLRU++;
                bitOfpLRU[set][i] = 1;
                likeInMemorypLRU[set][i] = true;
                checkBitsInpLRU(set, i);
                changeCachepLRU(set, i, offset, loadingByte);
                break;
            }
        }
        if (!hit) { // Если данного tag нет в кэше
            for (int i = 0; i < 4; i++) {
                if (tagpLRU[set][i] == -1) {
                    hit = true;
                    tagpLRU[set][i] = tag;
                    bitOfpLRU[set][i] = 1;
                    likeInMemorypLRU[set][i] = true;
                    checkBitsInpLRU(set, i);
                    for (int j = 0; j < 8; j++) {
                        cachepLRU[set][i][j] = memorypLRU[(address-offset) / 4 + j];
                    }
                    changeCachepLRU(set, i, offset, loadingByte);
                    break;
                }
            }
            if (!hit) { // tag нет в кэше и в нем нет места
                for (int i = 0; i < 4; i++) {
                    if (bitOfpLRU[set][i] == 0) { // Нашли первый нулевой бит
                        for (int j = 0; j < 8; j++) {
                            memorypLRU[getAddress(tag, set) + j] = cachepLRU[set][i][j];
                            cachepLRU[set][i][j] = memorypLRU[(address-offset) / 4 + j];
                        }
                        tagpLRU[set][i] = tag;
                        likeInMemorypLRU[set][i] = true;
                        checkBitsInpLRU(set, i);
                        changeCachepLRU(set, i, offset, loadingByte);
                        break;
                    }
                }
            }
        }
    }
    private int getBytespLRU(int address){ // Получаем данные из кэша pLRU
        int tag = getTag(address);
        int set = getSet(address);
        int offset = getOffset(address);
        boolean hit = false;
        int bytes = 0;
        for (int i = 0; i < 4; i++) {
            if (tag == tagpLRU[set][i]) {
                hit = true;
                hitTopLRU++;
                bitOfpLRU[set][i] = 1;
                likeInMemorypLRU[set][i] = false;
                checkBitsInpLRU(set, i);
                String nowData = zfillBinary(Integer.toBinaryString(cachepLRU[set][i][offset / 4]), 32);
                bytes = Integer.parseInt(nowData.substring((offset % 4) * 8, (offset % 4 + 1) * 8), 2);
                break;
            }
        }
        if (!hit){
            String nowData = zfillBinary(Integer.toBinaryString(memorypLRU[address / 4]), 32);
            bytes = Integer.parseInt(nowData.substring((offset % 4) * 8, (offset % 4 + 1) * 8), 2);
            for (int i = 0; i < 4; i++) {
                if (tagpLRU[set][i] == -1) {
                    hit = true;
                    tagpLRU[set][i] = tag;
                    bitOfpLRU[set][i] = 1;
                    likeInMemorypLRU[set][i] = false;
                    checkBitsInpLRU(set, i);
                    for (int j = 0; j < 8; j++) {
                        cachepLRU[set][i][j] = memorypLRU[(address-offset) / 4 + j];
                    }
                    break;
                }
            }
            if (!hit) { // tag нет в кэше и в нем нет места
                for (int i = 0; i < 4; i++) {
                    if (bitOfpLRU[set][i] == 0) { // Нашли первый нулевой бит
                        for (int j = 0; j < 8; j++) {
                            memorypLRU[getAddress(tag, set) + j] = cachepLRU[set][i][j];
                            cachepLRU[set][i][j] = memorypLRU[(address-offset) / 4 + j];
                        }
                        tagpLRU[set][i] = tag;
                        bitOfpLRU[set][i] = 1;
                        likeInMemorypLRU[set][i] = false;
                        break;
                    }
                }
            }
        }
        return bytes;
    }
    private void loadOneByte(int loadingBytes, int address) { // Загружаем один байт в кэш-линию
        triesToHitCache++;
        loadBytesLRU(loadingBytes, address);
        loadBytespLRU(loadingBytes, address);
    }
    private void loadTwoBytes(int loadingBytes, int address) { // Загружаем два байта в кэш-линию
        triesToHitCache++;
        hitToLRU -= 2;
        hitTopLRU -= 2;
        int beginAddress = withoutRemainder(address, 2);
        loadBytesLRU(loadingBytes, beginAddress);
        loadBytesLRU(loadingBytes, beginAddress + 1);
        loadBytespLRU(loadingBytes, beginAddress);
        loadBytespLRU(loadingBytes, beginAddress + 1);
    }
    private void loadFourBytes(int loadingBytes, int address) { // Загружаем 4 байта в кэш-линию
        triesToHitCache++;
        hitToLRU -= 3;    // Мы 4 раза посчитаем одно и то же попадание, если оно будет (потому что set и кэш-линия не поменяются)
        hitTopLRU -= 3;   // Аналогично, для двух функций выше
        int beginAddress = withoutRemainder(address, 4);
        loadBytesLRU(loadingBytes, beginAddress);
        loadBytesLRU(loadingBytes, beginAddress + 1);
        loadBytesLRU(loadingBytes, beginAddress + 2);
        loadBytesLRU(loadingBytes, beginAddress + 3);
        loadBytespLRU(loadingBytes, beginAddress);
        loadBytespLRU(loadingBytes, beginAddress + 1);
        loadBytespLRU(loadingBytes, beginAddress + 2);
        loadBytespLRU(loadingBytes, beginAddress + 3);
    }
    private int getOneByte(int address) { // Достаем один байт из кэш-линии
        triesToHitCache++;
        int byteLRU = getBytesLRU(address);
        int bytepLRU = getBytespLRU(address);
        return byteLRU;
    }
    private int getTwoBytes(int address) { // Достаем два байта в кэш-линии
        triesToHitCache++;
        hitToLRU -= 2;
        hitTopLRU -= 2;
        int beginAddress = withoutRemainder(address, 2);
        int byteLRU1 = getBytesLRU(beginAddress);
        int byteLRU2 = getBytesLRU(beginAddress + 1);
        int bytepLRU1 = getBytespLRU(beginAddress);
        int bytepLRU2 = getBytespLRU(beginAddress + 1);
        return twoComplement(Integer.toBinaryString(byteLRU1) + Integer.toBinaryString(byteLRU2));
    }
    private int getFourBytes(int address) { // Достаем 4 байта из кэш-линии
        triesToHitCache++;
        hitToLRU -= 3;    // Мы 4 раза посчитаем одно и то же попадание, если оно будет (потому что set и кэш-линия не поменяются)
        hitTopLRU -= 3;   // Аналогично, для двух функций выше
        int beginAddress = withoutRemainder(address, 4);
        int byteLRU1 = getBytesLRU(beginAddress);
        int byteLRU2 = getBytesLRU(beginAddress + 1);
        int byteLRU3 = getBytesLRU(beginAddress + 2);
        int byteLRU4 = getBytesLRU(beginAddress + 3);
        int bytepLRU1 = getBytespLRU(beginAddress);
        int bytepLRU2 = getBytespLRU(beginAddress + 1);
        int bytepLRU3 = getBytespLRU(beginAddress + 2);
        int bytepLRU4 = getBytespLRU(beginAddress + 3);
        return twoComplement(Integer.toBinaryString(byteLRU1) + Integer.toBinaryString(byteLRU2) + Integer.toBinaryString(byteLRU3) + Integer.toBinaryString(byteLRU4));
    }
    private void checkCommandInCacheLRU(int address){ // Загрузка байта в LRU
        int tag = getTag(address);
        int set = getSet(address);
        int offset = getOffset(address);
        boolean hit = false;
        for (int i = 0; i < 4; i++){
            if (tag == tagLRU[set][i]){
                hit = true;
                hitCommandToLRU++;
                countOfLRU[set][i]++;
                likeInMemoryLRU[set][i] = true; // ?
                break;
            }
        }
        if (!hit){ // Если данного tag нет в кэше
            for (int i = 0; i < 4; i++) {
                if (tagLRU[set][i] == -1) {
                    hit = true;
                    tagLRU[set][i] = tag;
                    countOfLRU[set][i]++;
                    likeInMemoryLRU[set][i] = false;
                    for (int j = 0; j < 8; j++) {
                        cacheLRU[set][i][j] = memoryLRU[(address - offset) / 4 + j];
                    }
                    break;
                }
            }
            if (!hit){
                int number = 0; // В кэш-линию с этим номером поставим новый адрес
                int minCount = countOfLRU[set][0];
                for (int i = 1; i < 4; i++){
                    if (countOfLRU[set][1] < minCount){
                        minCount = countOfLRU[set][1];
                        number = i;
                    }
                }
                tagLRU[set][number] = tag;
                countOfLRU[set][number]++;
                likeInMemoryLRU[set][number] = false;
                int beginAddress = getAddress(tag, set);
                for (int j = 0; j < 8; j++) {
                    memoryLRU[beginAddress + j] = cacheLRU[set][number][j];
                    cacheLRU[set][number][j] = memoryLRU[(address-offset) / 4 + j];
                }
            }
        }
    }
    private void checkCommandInCachepLRU(int address){ // Загрузка байта в LRU
        int tag = getTag(address);
        int set = getSet(address);
        int offset = getOffset(address);
        boolean hit = false;
        for (int i = 0; i < 4; i++){
            if (tag == tagpLRU[set][i]){
                hit = true;
                hitCommandTopLRU++;
                bitOfpLRU[set][i] = 1;
                likeInMemorypLRU[set][i] = true; // ?
                checkBitsInpLRU(set, i);
                break;
            }
        }
        if (!hit){ // Если данного tag нет в кэше
            for (int i = 0; i < 4; i++) {
                if (tagpLRU[set][i] == -1) {
                    hit = true;
                    tagpLRU[set][i] = tag;
                    bitOfpLRU[set][i] = 1;
                    likeInMemorypLRU[set][i] = false;
                    checkBitsInpLRU(set, i);
                    for (int j = 0; j < 8; j++) {
                        cachepLRU[set][i][j] = memorypLRU[(address - offset) / 4 + j];
                    }
                    break;
                }
            }
            if (!hit){
                for (int i = 0; i < 4; i++) {
                    if (bitOfpLRU[set][i] == 0) { // Нашли первый нулевой бит
                        for (int j = 0; j < 8; j++) {
                            memorypLRU[getAddress(tag, set) + j] = cachepLRU[set][i][j];
                            cachepLRU[set][i][j] = memorypLRU[(address-offset) / 4 + j];
                        }
                        tagpLRU[set][i] = tag;
                        bitOfpLRU[set][i] = 1;
                        likeInMemorypLRU[set][i] = false;
                        checkBitsInpLRU(set, i);
                        break;
                    }
                }
            }
        }
    }
    private void LB(String[] args) {
        int rd = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        int rs1 = twoComplement(args[3]);
        int address = registers[rs1] + offset;
        int data = getOneByte(address);
        registers[rd] = data;
        registers[0] = 0;
    }
    private void LH(String[] args) {
        int rd = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        int rs1 = twoComplement(args[3]);
        int address = registers[rs1] + offset;
        int data = getTwoBytes(address);
        registers[rd] = data;
        registers[0] = 0;
    }
    private void LW(String[] args) {
        int rd = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        int rs1 = twoComplement(args[3]);
        int address = registers[rs1] + offset;
        int data = getFourBytes(address);
        registers[rd] = data;
        registers[0] = 0;
    }
    private void LBU(String[] args) {
        int rd = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        int rs1 = twoComplement(args[3]);
        int address = registers[rs1] + offset;
        int data = getOneByte(address);
        registers[rd] = data;
        registers[0] = 0;
    }
    private void LHU(String[] args) {
        int rd = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        int rs1 = twoComplement(args[3]);
        int address = registers[rs1] + offset;
        int data = getTwoBytes(address);
        registers[rd] = data;
        registers[0] = 0;
    }
    private void SB(String[] args) {
        int rs2 = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        int rs1 = twoComplement(args[3]);
        int address = registers[rs1] + offset;
        int data = twoComplement(zfillBinary(Integer.toBinaryString(registers[rs2]), 32).substring(24));
        loadOneByte(data, address);
        registers[0] = 0;
    }
    private void SH(String[] args) {
        int rs2 = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        int rs1 = twoComplement(args[3]);
        int address = registers[rs1] + offset;
        int data = twoComplement(zfillBinary(Integer.toBinaryString(registers[rs2]), 32).substring(16));
        loadTwoBytes(data, address);
        registers[0] = 0;
    }
    private void SW(String[] args) {
        int rs2 = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        int rs1 = twoComplement(args[3]);
        int address = registers[rs1] + offset;
        int data = twoComplement(zfillBinary(Integer.toBinaryString(registers[rs2]), 32));
        loadFourBytes(data, address);
        registers[0] = 0;
    }
    private void ADDI(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int imm = twoComplement(args[3]);
        registers[rd] = registers[rs1] + imm;
        registers[0] = 0;
    }
    private void SLTI(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int imm = twoComplement(args[3]);
        registers[rd] = (registers[rs1] < imm) ? 1 : 0;
        registers[0] = 0;
    }
    private void SLTIU(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int imm = twoComplement(args[3]);
        long unsigned1 = Integer.toUnsignedLong(registers[rs1]);
        long unsigned2 = Integer.toUnsignedLong(imm);
        registers[rd] = (unsigned1 < unsigned2) ? 1 : 0;
        registers[0] = 0;
    }
    private void XORI(String[] args) {
        int rd  = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int imm = twoComplement(args[3]);
        registers[rd] = registers[rs1] ^ imm;
        registers[0] = 0;
    }
    private void ORI(String[] args) {
        int rd  = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int imm = twoComplement(args[3]);
        registers[rd] = registers[rs1] | imm;
        registers[0] = 0;
    }
    private void ANDI(String[] args) {
        int rd  = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int imm = twoComplement(args[3]);
        registers[rd] = registers[rs1] & imm;
        registers[0] = 0;
    }
    private void SLLI(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int shamt = twoComplement(args[3]);
        registers[rd] = registers[rs1] << shamt;
        registers[0] = 0;
    }
    private void SRLI(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int shamt  = twoComplement(args[3]);
        registers[rd] = registers[rs1] >>> shamt;
        registers[0] = 0;
    }
    private void SRAI(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int shamt = twoComplement(args[3]);
        registers[rd] = registers[rs1] >> shamt;
        registers[0] = 0;
    }
    private void ADD(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        registers[rd] = registers[rs1] + registers[rs2];
        registers[0] = 0;
    }
    private void SUB(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        registers[rd] = registers[rs1] - registers[rs2];
        registers[0] = 0;
    }
    private void SLL(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        registers[rd] = registers[rs1] << registers[rs2];
        registers[0] = 0;
    }
    private void SLT(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        registers[rd] = (registers[rs1] < registers[rs2]) ? 1 : 0;
        registers[0] = 0;
    }
    private void SLTU(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        long unsigned1 = Integer.toUnsignedLong(registers[rs1]);
        long unsigned2 = Integer.toUnsignedLong(registers[rs2]);
        registers[rd] = (unsigned1 < unsigned2) ? 1 : 0;
        registers[0] = 0;
    }
    private void XOR(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        registers[rd] = registers[rs1] ^ registers[rs2];
        registers[0] = 0;
    }
    private void SRL(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        registers[rd] = registers[rs1] >>> registers[rs2];
        registers[0] = 0;
    }
    private void SRA(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        registers[rd] = registers[rs1] >> registers[rs2];
        registers[0] = 0;
    }
    private void OR(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        registers[rd] = registers[rs1] | registers[rs2];
        registers[0] = 0;
    }
    private void AND(String[] args) {
        int rd  = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        registers[rd] = registers[rs1] & registers[rs2];
        registers[0] = 0;
    }
    private void BEQ(String[] args) {
        int rs1 = twoComplement(args[1]);
        int rs2 = twoComplement(args[2]);
        int offset = twoComplement(args[3]);
        executeBranchInstructions("beq", offset, rs1, rs2);
        registers[0] = 0;
    }
    private void BNE(String[] args) {
        int rs1 = twoComplement(args[1]);
        int rs2 = twoComplement(args[2]);
        int offset = twoComplement(args[3]);
        executeBranchInstructions("bne", offset, rs1, rs2);
        registers[0] = 0;
    }
    private void BLT(String[] args) {
        int rs1 = twoComplement(args[1]);
        int rs2 = twoComplement(args[2]);
        int offset = twoComplement(args[3]);
        executeBranchInstructions("blt", offset, rs1, rs2);
        registers[0] = 0;
    }
    private void BGE(String[] args) {
        int rs1 = twoComplement(args[1]);
        int rs2 = twoComplement(args[2]);
        int offset = twoComplement(args[3]);
        executeBranchInstructions("bge", offset, rs1, rs2);
        registers[0] = 0;
    }
    private void BLTU(String[] args) {
        int rs1 = twoComplement(args[1]);
        int rs2 = twoComplement(args[2]);
        int offset = twoComplement(args[3]);
        long unsigned1 = Integer.toUnsignedLong(registers[rs1]);
        long unsigned2 = Integer.toUnsignedLong(registers[rs2]);
        if (unsigned1 >= unsigned2) {
            pc = pc + offset / 4;
        } else {
            pc++;
        }
        registers[0] = 0;
    }
    private void BGEU(String[] args) {
        int rs1 = twoComplement(args[1]);
        int rs2 = twoComplement(args[2]);
        int offset = twoComplement(args[3]);
        long unsigned1 = Integer.toUnsignedLong(registers[rs1]);
        long unsigned2 = Integer.toUnsignedLong(registers[rs2]);
        if (unsigned1 >= unsigned2) {
            pc = pc + offset / 4;
        } else {
            pc++;
        }
        registers[0] = 0;
    }
    private void JAL(String[] args) {
        int rd = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        registers[rd] = (pc * 4 + BEGIN) + 4;
        pc = pc + offset / 4;
        registers[0] = 0;
    }
    private void JALR(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int offset = twoComplement(args[3]);
        registers[rd] = (pc * 4 + BEGIN) + 4;
        pc = ((rs1 + offset - BEGIN) / 4);
        registers[0] = 0;
    }
    private void LUI(String[] args) {
        int rd = twoComplement(args[1]);
        int imm = twoComplement(args[2]);
        registers[rd] = imm;
        registers[0] = 0;
    }
    private void AUIPC(String[] args) {
        int rd = twoComplement(args[1]);
        int offset = twoComplement(args[2]);
        registers[rd] = pc * 4 + BEGIN + offset;
        registers[0] = 0;
    }
    private void MUL(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        long a = registers[rs1];
        long b = registers[rs2];
        long result = a * b;
        registers[rd] = (int)result;
        registers[0] = 0;
    }
    private void MULH(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        long a = registers[rs1];
        long b = registers[rs2];
        long result = a * b;
        registers[rd] = (int)(result >> 32);
        registers[0] = 0;
    }
    private void MULHSU(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        long a = registers[rs1];
        long b = Integer.toUnsignedLong(registers[rs2]);
        long result = a * b;
        registers[rd] = (int)(result >> 32);
        registers[0] = 0;
    }
    private void MULHU(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        long a = Integer.toUnsignedLong(registers[rs1]);
        long b = Integer.toUnsignedLong(registers[rs2]);
        long result = a * b;
        registers[rd] = (int)(result >> 32);
        registers[0] = 0;
    }
    private void DIV(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        int divisor = registers[rs2];
        int dividend = registers[rs1];
        if (divisor == 0) {
            registers[rd] = -1;
        } else {
            registers[rd] = dividend / divisor;
        }
        registers[0] = 0;
    }
    private void DIVU(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        long divisor = Integer.toUnsignedLong(registers[rs2]);
        long dividend = Integer.toUnsignedLong(registers[rs1]);
        if (divisor == 0) {
            registers[rd] = 0xFFFFFFFF;
        } else {
            registers[rd] = (int)(dividend / divisor);
        }
        registers[0] = 0;
    }
    private void REM(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        int divisor = registers[rs2];
        int dividend = registers[rs1];
        if (divisor == 0) {
            registers[rd] = dividend;
        } else {
            registers[rd] = dividend % divisor;
        }
        registers[0] = 0;
    }
    private void REMU(String[] args) {
        int rd = twoComplement(args[1]);
        int rs1 = twoComplement(args[2]);
        int rs2 = twoComplement(args[3]);
        long divisor = Integer.toUnsignedLong(registers[rs2]);
        long dividend = Integer.toUnsignedLong(registers[rs1]);
        if (divisor == 0) {
            registers[rd] = registers[rs1];
        } else {
            registers[rd] = (int)(dividend % divisor);
        }
        registers[0] = 0;
    }
}
