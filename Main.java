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
			if(bits == -1) {
				bitCount = 0;
				return -1;
			}
			bitCount = 8;
		}
//		int result = bits & 1;
//		bits >>>= 1;
//		bitCount--;
//		return result;
		bitCount--;
		return (bits >>> bitCount) & 1;
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
	private static final long NUM_BITS = 32;
	private static final long FULL_RANGE = 1L << NUM_BITS;
	private static final long HALF_RANGE = FULL_RANGE >>> 1;
	private static final long QUARTER_RANGE = HALF_RANGE >>> 1;
	private static final long MINIMUM_RANGE = QUARTER_RANGE + 2;
	private static final long MAXIMUM_TOTAL = Math.min(Long.MAX_VALUE / FULL_RANGE, MINIMUM_RANGE);
	private static final long STATE_MASK = FULL_RANGE - 1;

	private long low = 0;
	private long high = STATE_MASK;
	private long code;
	private long underflow;
	private boolean decompress = false;
	private boolean init = false;

	private BitInput input;
	private BitOutput output;

	void finish() throws IOException {
		output.writeBit(1);
		output.close();
	}

	private void update(int symbol, FrequencyTable table) throws IOException {
		long range = high - low + 1;
		assert MINIMUM_RANGE <= range && range <= FULL_RANGE;
		long total = table.frequencySumBelow(table.numberOfSymbols());
		long symLow = table.frequencySumBelow(symbol);
		long symHigh = table.frequencySumBelow(symbol + 1);
		assert symLow < symHigh;
		assert total < MAXIMUM_TOTAL;

		long newLow = low + symLow * range / total;
		long newHigh = low + symHigh * range / total - 1;
		low = newLow;
		high = newHigh;

		while(((low ^ high) & HALF_RANGE) == 0) {
			shift();
			low = (low << 1) & STATE_MASK;
			high = ((high << 1) & STATE_MASK) | 1;
		}
		while((low &~ high & QUARTER_RANGE) != 0) {
			underflow();
			low = (low << 1) ^ HALF_RANGE;
			high = ((high ^ HALF_RANGE) << 1) | HALF_RANGE | 1;
		}
	}

	private int codeBitOrZero() throws IOException {
		int bit = input.readBit();
		return bit == -1 ? 0 : bit;
	}

	private void shift() throws IOException {
		if(decompress) {
			code = ((code << 1) & STATE_MASK) | codeBitOrZero();
		} else {
			int bit = (int)(low >>> (NUM_BITS - 1));
			output.writeBit(bit);
			while(underflow-- > 0)
				output.writeBit(bit ^ 1);
		}
	}

	private void underflow() throws IOException {
		if(decompress) {
			code = (code & HALF_RANGE) | ((code << 1) & (STATE_MASK >>> 1)) | codeBitOrZero();
		} else {
			assert underflow < Integer.MAX_VALUE;
			underflow++;
		}
	}

	private int read(FrequencyTable table) throws IOException {
		long range = high - low + 1;
		assert MINIMUM_RANGE <= range && range <= FULL_RANGE : "out of range: "+range;
		long total = table.frequencySumBelow(table.numberOfSymbols());
		long offset = code - low;
		long value = ((offset + 1) * total - 1) / range;
		assert value * range / total <= offset;
		assert 0 <= value && value < total : "value out of range: "+value;
//		int symbol = table.numberOfSymbols();
//		while(--symbol <= 0)
//			if(table.frequencySumBelow(symbol) <= value)
//				break;
		int symbol = table.firstSymbolBelow((int)value);
		assert offset >= table.frequencySumBelow(symbol) * range / total : "offset too big: "+offset;
		assert offset < table.frequencySumBelow(symbol+1) * range / total : "offset too small: "+offset;
		update(symbol, table);
		assert code >= low && code <= high : "code out of range: "+code;
		return symbol;
	}

	void compress(int symbol, FrequencyTable table, BitOutput output) throws IOException {
		decompress = false;
		this.output = output;
		update(symbol, table);
//		for(int i = 0; i < 8; i++) {
//			output.writeBit((symbol >>> i) & 1);
//		}
	}
	int decompress(BitInput input, FrequencyTable table) throws IOException {
		decompress = true;
		this.input = input;
		if(!init) {
			for(int i = 0; i < NUM_BITS; i++)
				code = (code << 1) | codeBitOrZero();
			init = true;
		}
		return read(table);
//		int symbol = 0;
//		for(int i = 0; i < 8; i++) {
//			symbol = (symbol << 1) | codeBitOrZero();
//		}
//		return symbol;
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
	void setAll(int val) {
		for(int i = 0; i < tree.length; i++)
			set(i, val);
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
	int firstSymbolAbove(int sum) {
		int symbol = 0;
		while(symbol < tree.length && frequencySumBelow(symbol) < sum)
			symbol++;
		return symbol;
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
	static int WINDOW_SIZE = 4000;
	static int MAX_LENGTH = 200;
	static int MIN_LENGTH = 3;

	// speciālie simboli
	static int MATCH = 256;
	static int EOF = 257;

	private final FrequencyTable chars = new FrequencyTable(256 + 2);
	private final FrequencyTable lengths = new FrequencyTable(MAX_LENGTH + 1);
	private final FrequencyTable distances = new FrequencyTable(WINDOW_SIZE + 1);
	private final ArithmeticCoder arithmetic = new ArithmeticCoder();
	{
		chars.setAll(1);
		lengths.setAll(1);
		distances.setAll(1);
	}

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
		if(len >= WINDOW_SIZE)
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
		arithmetic.finish();
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
		assert d < LZ77.WINDOW_SIZE && l < LZ77.MAX_LENGTH;
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
