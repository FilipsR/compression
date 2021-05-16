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
			sum -= tree[from - 1];
		return sum;
	}
	void add(int symbol, int delta) {
		assert 0 <= symbol && symbol < tree.length;
		for (; symbol < tree.length; symbol += Integer.lowestOneBit(symbol + 1))
			tree[symbol] += delta;
	}
	int get(int symbol) {
		assert symbol >= 0;
		return rangeSum(symbol, symbol + 1);
	}
	void set(int symbol, int frequency) {
		add(symbol, frequency - get(symbol));
	}
	int numberOfSymbols() {
		return tree.length;
	}
	int frequencySumBelow(int symbol) {
		int sum = 0;
		assert 0 <= symbol && symbol <= tree.length;
		for (; symbol > 0; symbol -= Integer.lowestOneBit(symbol))
			sum += tree[symbol - 1];
		return sum;
	}
}

class LZ77 {
	// algoritma konstantes
	private static int WINDOW_SIZE = 1024 * 4;
	private static int MAX_LENGTH = 200;
	private static int MIN_LENGTH = 3;

	// speciālie simboli
	private static int MATCH = 256;
	private static int EOF = 257;

	private final ArithmeticCoder chars = new ArithmeticCoder(new FrequencyTable(256 + 2));
	private final ArithmeticCoder lengths = new ArithmeticCoder(new FrequencyTable(MAX_LENGTH));
	private final ArithmeticCoder distances = new ArithmeticCoder(new FrequencyTable(WINDOW_SIZE));

	private final StringBuilder inputBuffer = new StringBuilder(MAX_LENGTH);
	private final StringBuilder window = new StringBuilder(WINDOW_SIZE);

	// testēšanai
	java.util.function.Consumer<Match> debugMatchAction = m -> {};
	java.util.function.IntConsumer debugCharAction = c -> {};

	private Match longestPrefixInWindow() {
		int bestStart = -1;
		int bestLength = -1;
		for(int start = window.length() - MIN_LENGTH; start >= 0; start--) {
			int mismatch = 0;
			int limit = Math.min(window.length() - start, inputBuffer.length());
			while(mismatch < limit && window.charAt(start + mismatch) == inputBuffer.charAt(mismatch))
				mismatch++;
			if(mismatch >= MIN_LENGTH && mismatch > bestLength) {
				bestLength = mismatch;
				bestStart = start;
			}
		}
		return bestStart >= 0 ? new Match(window.length() - bestStart, bestLength) : null;
	}

	private void trimWindow() {
		int len = window.length();
		if(len > WINDOW_SIZE)
			window.delete(0, len - WINDOW_SIZE);
	}

	void compress(InputStream input, BitOutput output) throws IOException {
		while(true) {
			while(inputBuffer.length() < MAX_LENGTH) {
				// mēģina nolasīt vairāk datu
				int nextByte = input.read();
				if(nextByte == -1)
					break; // vairāk nav ko lasīt, bet iespējams, ka inputBuffer vēl nav tukšs
				inputBuffer.append((char)nextByte);
			}
			if(inputBuffer.length() == 0)
				return;
			// inputBuffer ir vismaz viens baits
			Match match = longestPrefixInWindow();
			if(match == null) {
				char c = inputBuffer.charAt(0);
				// TODO izvada c
				debugCharAction.accept(c);
				trimWindow();
				window.append(c);
				inputBuffer.delete(0, 1);
			} else {
				debugMatchAction.accept(match);
				int len = match.length();
				trimWindow();
				window.append(inputBuffer, 0, len);
				inputBuffer.delete(0, len);
				// TODO izvada match
			}
		}
	}
	void decompress(BitInput input, OutputStream output) throws IOException {
		System.out.println("LZ77.decompress("+input+", "+output+")");
	}
}

class Match {
	private final int distance;
	private final int length;
	Match(int d, int l) {
		assert d > 0 && l > 0;
		distance = d;
		length = l;
	}
	int distance() {
		return distance;
	}
	int length() {
		return length;
	}
}
