import java.io.*;
import java.util.*;
public class Main {
	public static void main(String[] args) throws IOException {
		LZ77 lz77 = new LZ77();
		BitInput input = new BitInput(System.in);
		BitOutput output = new BitOutput(System.out);
		
		Scanner sc = new Scanner(System.in);
		String filename = sc.nextLine();
		
		
		File file = new File(filename);
		if (!file.exists()) {
			System.out.println("File does not exist");
			return;
		}
		File file_1 = new File("compressed_file.txt");
		if(!file.exists()) {
			file.createNewFile();
		}
		FileInputStream fileInput = new FileInputStream(file);
		FileOutputStream fileOutput = null;
		
		lz77.compress(fileInput, output);
		lz77.decompress(input, fileOutput);
		
		byte[] bytes = filename.getBytes();
		
		fileOutput = new FileOutputStream(file_1);
		
		fileOutput.write(bytes);
		
		fileOutput.flush();
		sc.close();
	}
}

class BitInput implements AutoCloseable {
	private int bits;
	private int bitCount = 0;
	private final InputStream stream;

	BitInput(InputStream stream) {
		this.stream = stream;
	}

	int readBit() throws IOException {
		assert 0 <= bitCount && bitCount <= 8 : "bit count out of range";
		if(bitCount == 0) {
			bits = stream.read();
			if(bits == -1)
				return -1;
			bitCount = 8;
		}
		int result = bits & 1;
		bits >>= 1;
		bitCount--;
		return result;
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}
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
	static final class Rational {
		long top, bottom;
		private void set(long nTop, long nBottom) {
			long a = nTop;
			long b = nBottom;
			while(b != 0)
				if(a > b)
					a -= b;
				else
					b -= a;
			top = nTop / a;
			bottom = nBottom / a;
		}
		Rational(long t, long b) {
			set(t, b);
		}
		void add(long t, long b) {
			set(top*b + t*bottom, bottom * b);
		}
		void sub(long t, long b) {
			set(top*b - t*bottom, bottom * b);
		}
		void mul(long t, long b) {
			set(top*t, bottom*b);
		}
		void div(long t, long b) {
			set(top*b, bottom*t);
		}
	}
	void compress(int symbol, FrequencyTable table, BitOutput output) throws IOException {
	}
	int decompress(BitInput input, FrequencyTable table) throws IOException {
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
	int firstSymbolBelow(int sum){
		int i = 0, j = tree.length;
		while(j != Integer.lowestOneBit(j))
			j -= Integer.lowestOneBit(j);
		for (; j > 0; j >>= 1){
			if ( i + j <= tree.length && tree[i + j - 1] <= sum){
				sum -= tree[i + j -1];
				i += j;
			}
		}
		return i;
	}
}

class LZ77 {
	// algoritma konstantes
	private static int WINDOW_SIZE = 1024 * 4;
	private static int MAX_LENGTH = 200;
	private static int MIN_LENGTH = 3;

	// speciālie simboli
	static int MATCH = 256;
	static int EOF = 257;

	private final FrequencyTable chars = new FrequencyTable(256 + 2);
	private final FrequencyTable lengths = new FrequencyTable(MAX_LENGTH);
	private final FrequencyTable distances = new FrequencyTable(WINDOW_SIZE);
	private final ArithmeticCoder arithmetic = new ArithmeticCoder();

	private final StringBuilder inputBuffer = new StringBuilder(MAX_LENGTH);
	private final StringBuilder window = new StringBuilder(WINDOW_SIZE);

	// testēšanai
	java.util.function.Consumer<Match> debugMatchAction = m -> {};
	java.util.function.IntConsumer debugCharAction = c -> {};
	java.util.function.IntSupplier debugReadAction = null;

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
				break;
			// inputBuffer ir vismaz viens baits
			Match match = longestPrefixInWindow();
			if(match == null) {
				char c = inputBuffer.charAt(0);
				debugCharAction.accept(c);
				arithmetic.compress(c, chars, output);
				trimWindow();
				window.append(c);
				inputBuffer.delete(0, 1);
			} else {
				arithmetic.compress(MATCH, chars, output);
				arithmetic.compress(match.distance(), distances, output);
				arithmetic.compress(match.length(), lengths, output);
				debugMatchAction.accept(match);
				int len = match.length();
				trimWindow();
				window.append(inputBuffer, 0, len);
				inputBuffer.delete(0, len);
			}
		}
		arithmetic.compress(EOF, chars, output);
	}
	private void paste(int distance, int length, OutputStream output) throws IOException {
		for(int i = window.length() - distance, end = window.length() - distance + length; i < end; i++)
			output.write(window.charAt(i));
	}

	void decompress(BitInput input, OutputStream output) throws IOException {
		int symbol;
		while((symbol = debugReadAction != null ? debugReadAction.getAsInt() : arithmetic.decompress(input, chars)) != EOF) {
			if(symbol == MATCH) {
				int distance = debugReadAction != null ? debugReadAction.getAsInt() : arithmetic.decompress(input, distances);
				int length = debugReadAction != null ? debugReadAction.getAsInt() : arithmetic.decompress(input, lengths);
				paste(distance, length, output);
			} else {
				output.write((char)symbol);
				window.append((char)symbol);
			}
			trimWindow();
		}
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
