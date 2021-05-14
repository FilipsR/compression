import java.io.*;
import java.util.*;
public class Main {
	public static void main(String[] args) throws IOException {
		System.out.println("Main.main()");
		LZ77 lz77 = new LZ77();
		BitInput input = new BitInput(System.in);
		BitOutput output = new BitOutput(System.out);
	}
}

class BitInput implements AutoCloseable {
	BitInput(InputStream stream) {
	}

	int readBit() {
		System.out.println("BitInput.readBit");
		return -1;
	}

	@Override
	public void close() {}
}

class BitOutput implements AutoCloseable {
	private int bits;
	private int bitCount = 0;
	private final OutputStream stream;

	BitOutput(OutputStream stream) {
		this.stream = stream;
	}

	void writeBit(int bit) throws IOException {
		assert 0 <= bitCount && bitCount <= 8 : "bit count out of range";
		assert bit == 0 || bit == 1 : "bit must be 0 or 1";
		if(bitCount == 8) {
			stream.write(bits);
			bits = bitCount = 0;
		}
		bits = (bits << 1) | bit;
		bitCount++;
	}

	@Override
	public void close() throws IOException {
		if(bitCount != 0)
			stream.write(bits);
		stream.close();
	}
}

class ArithmeticCoder {
	private final FrequencyTable table;
	ArithmeticCoder(FrequencyTable table) {
		this.table = table;
	}
	void compress(int symbol, BitOutput output) throws IOException {
	}
	int decompress(BitInput input) throws IOException {
		return -1;
	}
}

class FrequencyTable {
	// https://en.wikipedia.org/wiki/Fenwick_tree
	private final int[] tree;
	FrequencyTable(int symbolCount) {
		tree = new int[symbolCount];
	}
	private int rangeSum(int from, int to) {
		assert 0 <= from && from <= to;
		int sum = 0;
		for(; to > from; to -= Integer.lowestOneBit(to))
			sum += tree[to - 1];
		for(; from > to; from -= Integer.lowestOneBit(from))
			sum += tree[from - 1];
		return sum;
	}
	int get(int symbol) {
		assert symbol >= 0;
		return rangeSum(symbol, symbol + 1);
	}
	void set(int symbol, int frequency) {
		System.out.println("FrequencyTable.set("+symbol+", "+frequency+")");
	}
	void increment(int symbol, int amount) {
		assert symbol >= 0;
		for(; symbol < tree.length - 1; symbol += Integer.lowestOneBit(symbol))
			tree[symbol] += amount;
	}
	int numberOfSymbols() {
		return tree.length;
	}
	int frequencySumBelow(int symbol) {
		System.out.println("FrequencyTable.frequencySumBelow("+symbol+")");
		return -1;
	}
}

class LZ77 {
	// algoritma konstantes
	private static int WINDOW_SIZE = 1024 * 4;
	private static int MAX_LENGTH = 200;

	// speciālie simboli
	private static int MATCH = 256;
	private static int EOF = 257;

	private final ArithmeticCoder chars = new ArithmeticCoder(new FrequencyTable(256 + 2));
	private final ArithmeticCoder lengths = new ArithmeticCoder(new FrequencyTable(MAX_LENGTH));
	private final ArithmeticCoder distances = new ArithmeticCoder(new FrequencyTable(WINDOW_SIZE));
	void compress(InputStream input, BitOutput output) throws IOException {
		System.out.println("LZ77.compress("+input+", "+output+")");
		output.writeBit(0);
		Match match = new Match();
		match.length();
		match.distance();
	}
	void decompress(BitInput input, OutputStream output) throws IOException {
		System.out.println("LZ77.decompress("+input+", "+output+")");
	}
}

class Match {
	int length() {
		System.out.println("Match.length()");
		return -1;
	}
	int distance() {
		System.out.println("Match.distance()");
		return -1;
	}
}
