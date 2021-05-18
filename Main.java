// 201RDB160, Filips Jānis Romāns, 10. grupa
// 201RDB207, Kārlis Baumanis, 10. grupa

import java.io.*;
import java.util.*;
import java.nio.file.*;

public class Main {
	private static Scanner scanner = new Scanner(System.in);
	public static void main(String[] args) throws Exception {
		loop:
		while (true) {
			System.out.print("command: ");
			final String command;
			if(scanner.hasNextLine())
				command = scanner.nextLine();
			else
				break;
			switch(command) {
				case "comp":
					comp();
					break;
				case "decomp":
					decomp();
					break;
				case "size":
					size();
					break;
				case "equal":
					equal();
					break;
				case "about":
					about();
					break;
				case "exit":
					break loop;
				default:
					System.out.println("wrong command");
			}
		}
		scanner.close();
	}
	public static void comp() throws Exception{
		LZ77 lz77 = new LZ77();

		System.out.print("source file name: ");
		File sourceFile = new File(scanner.nextLine());
		if (!sourceFile.exists()) {
			System.out.println("File does not exist");
			return;
		}

		System.out.print("archive name: ");
		File archiveFile = new File(scanner.nextLine());

		try(
			InputStream input = new FileInputStream(sourceFile);
			BitOutput output = new BitOutput(new FileOutputStream(archiveFile))
		) {
			lz77.compress(input, output);
		}
	}
	public static void decomp() throws Exception{
		LZ77 lz77 = new LZ77();

		System.out.print("archive name: ");
		File archiveFile = new File(scanner.nextLine());
		if (!archiveFile.exists()) {
			System.out.println("File does not exist");
			return;
		}

		System.out.print("file name: ");
		File destFile = new File(scanner.nextLine());

		try(
			InputStream input = new FileInputStream(archiveFile);
			OutputStream output = new FileOutputStream(destFile)
		) {
			lz77.decompress(new BitInput(input), output);
		}
	}
	public static void size() throws Exception{
		System.out.println("file name: ");
		Path path = Paths.get(scanner.nextLine());

		try {
			long bytes = Files.size(path);
			System.out.printf("size: %d\n", bytes);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void equal() throws Exception{
		Scanner sc = new Scanner(System.in);
		System.out.println("first file name: ");
		Path path1 = Paths.get(sc.nextLine());
		System.out.println("second file name: ");
		Path path2 = Paths.get(sc.nextLine());

		byte[] f1 = Files.readAllBytes(path1);
		byte[] f2 = Files.readAllBytes(path2);
		if (Arrays.equals(f1,f2))
			System.out.println("true");
		else
			System.out.println("false");
	}
	public static void about() {
		System.out.println("Informācija par grupu:");
		System.out.println("201RDB160 Filips Jānis Romāns 10.grupa");
		System.out.println("201RDB207 Kārlis Baumanis 10. grupa");
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
		bits = (bits << 1) | bit;
		bitCount++;
		if(bitCount == 8) {
			stream.write(bits);
			bits = bitCount = 0;
		}
	}

	@Override
	public void close() throws IOException {
		while(bitCount != 0)
			writeBit(0);
		stream.close();
	}
}

class ArithmeticCoder {
	private static final long ST_BITS = 32;
	private static final long FULL = (1L << ST_BITS);
	private static final long HALF = (FULL >>> 1);
	private static final long QUARTER = (HALF >>> 1);
	private static final long MIN_RANGE = (QUARTER + 2);
	private static final long MAX_SUM = Math.min(Long.MAX_VALUE / FULL, MIN_RANGE);
	private static final long MASK = (FULL - 1);

	private long low = 0;
	private long high = MASK;
	private long code = 0;
	private long under = 0;
	private boolean decompress = false;
	private boolean init = false;

	private BitInput input;
	private BitOutput output;

	void finish() throws IOException {
		output.writeBit(1);
	}

	void shift() throws IOException {
		if(!decompress) {
			int bit = (int)(low >> (ST_BITS - 1));
			output.writeBit(bit);
			for(; under > 0; under--)
				output.writeBit(bit ^ 1);
		} else {
			code = ((code << 1) & MASK) | get();
		}
	}

	void underflow() throws IOException {
		if(!decompress) {
			assert under < Integer.MAX_VALUE;
			under++;
		} else {
			code = (code & HALF) | ((code << 1) & (MASK >> 1)) | get();
		}
	}


	void update(int sym, FrequencyTable freq) throws IOException {
		assert low < high && (low & MASK) == low && (high & MASK) == high;
		long range = high - low + 1;
		assert range >= MIN_RANGE && range <= FULL;
		long total = freq.frequencySumBelow(freq.numberOfSymbols());
		long symLow = freq.frequencySumBelow(sym);
		long symHigh = freq.frequencySumBelow(sym+1);
		assert symLow != symHigh;
		assert total <= MAX_SUM;
		long newLow = low + symLow * range / total;
		long newHigh = low + symHigh * range / total - 1;
		low = newLow;
		high = newHigh;
		while(((low ^ high) & HALF) == 0) {
			shift();
			low  = ((low  << 1) & MASK);
			high = ((high << 1) & MASK) | 1;
		}
		while((low &~ high & QUARTER) != 0) {
			underflow();
			low = (low << 1) ^ HALF;
			high = ((high ^ HALF) << 1) | HALF | 1;
		}
	}

	private int get() throws IOException {
		int bit = input.readBit();
		return bit == -1 ? 0 : bit;
	}

	int decode(FrequencyTable table) throws IOException {
		long total = table.frequencySumBelow(table.numberOfSymbols());
		assert total <= MAX_SUM;
		long range = high - low + 1;
		long offset = code - low;
		long value = ((offset + 1) * total - 1) / range;
		assert value * range / total <= offset;
		assert value >= 0 && value < total;
		int start = 0;
		int end = table.numberOfSymbols();
		while(end - start > 1) {
			int mid = (start + end) / 2;
			if(table.frequencySumBelow(mid) > value)
				end = mid;
			else
				start = mid;
		}
		assert start + 1 == end;
		int sym = start;
		assert offset >= table.frequencySumBelow(sym) * range / total;
		assert offset < table.frequencySumBelow(sym+1) * range / total;
		update(sym, table);
		assert code >= low && code <= high;
		return sym;
	}

	void compress(int symbol, FrequencyTable table, BitOutput output) throws IOException {
		decompress = false;
		this.output = output;
		update(symbol, table);
		table.add(symbol, 1);
	}

	int decompress(BitInput input, FrequencyTable table) throws IOException {
		decompress = true;
		this.input = input;
		if(!init) {
			init = true;
			for(int i = 0; i < ST_BITS; i++)
				code = (code << 1) | get();
		}
		int symbol = decode(table);
		table.add(symbol, 1);
		return symbol;
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
	private final FrequencyTable distances = new FrequencyTable(WINDOW_SIZE + MAX_LENGTH);
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
				window.append(c);
				trimWindow();
				inputBuffer.delete(0, 1);
			} else {
				arithmetic.compress(MATCH, chars, output);
				arithmetic.compress(match.distance(), distances, output);
				arithmetic.compress(match.length(), lengths, output);
				debugMatchAction.accept(match);
				int len = match.length();
				window.append(inputBuffer, 0, len);
				trimWindow();
				inputBuffer.delete(0, len);
			}
		}
		arithmetic.compress(EOF, chars, output);
		arithmetic.finish();
	}
	private void paste(int distance, int length, OutputStream output) throws IOException {
		int start = window.length() - distance;
		int end = start + length;
		for(int i = start; i < end; i++)
			output.write(window.charAt(i));
		window.append(window, start, end);
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
//		assert d < LZ77.WINDOW_SIZE && l < LZ77.MAX_LENGTH;
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
