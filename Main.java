// 201RDB160, Filips Romāns, 10. grupa
public class Main {
	public static void main(String[] args) {
	}
}

class BitInput implements AutoCloseable {
	BitInput(InputStream stream);
	int nextBit();
	public void close();
}

class BitOutput implements AutoCloseable {
	BitInput(OutputStream stream);
	int writeBit();
	public void close();
}

class ArithmeticCoder {
	void compress(BitInput input; BitOutput output);
	void decompress(BitInput input; BitOutput output);
}

class FrequencyTable {
	int get(int symbol);
	int set(int symbol, int frequency);
	void increment(int symbol);
	int numberOfSymbols();
	int frequencySumBelow(int symbol);
	int frequencySumBelowInclusive(int symbol);
}

class LZ77 {
	void compress(InputStream input; BitOutput output);
	void decompress(BitInput input; OutputStream output);
}

class Match {
	Match(int length, int distance);
	int length();
	int distance();
}
