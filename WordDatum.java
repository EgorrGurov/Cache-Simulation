public class WordDatum {
    private final String binary; // Название команды
    private final int countArgs; // Количество аргументов
    private final int block; // Номер "блока" (для простоты написания кода)
    private final int[] typeOfArguments; // Типы аргументов (также для простоты, регистры / imm, shamt etc)

    public WordDatum(String binary, int countArgs, int block, int[] typeOfArguments) {
        this.binary = binary;
        this.countArgs = countArgs;
        this.block = block;
        this.typeOfArguments = typeOfArguments;
    }

    public String getBinaryCommand() {
        return this.binary;
    }

    public int getCountArgs() {
        return this.countArgs;
    }

    public int getBlock() {return this.block; }

    public int[] getArgs(){return this.typeOfArguments; }
}