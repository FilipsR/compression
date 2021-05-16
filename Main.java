import java.io.*;
import java.util.*;
public class Main {
	public static void main(String[] args) throws Exception {

		
		Scanner sc = new Scanner(System.in);

		String choiseStr = sc.nextLine();
		
		System.out.println("function: ");
		loop: while (true) {
		switch(choiseStr) {
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
			System.out.println("no match");
		}
		}
		
		sc.close();
	}
	public static void comp() throws Exception{
		LZ77 lz77 = new LZ77();
		BitInput input = new BitInput(System.in);
		BitOutput output = new BitOutput(System.out);		
		
		Scanner sc = new Scanner(System.in);
		System.out.print("Source file name: ");
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
		lz77.compress(fileInput, output);
		FileOutputStream fileOutput = null;	
		
		lz77.decompress(input, fileOutput);
		
		byte[] bytes = filename.getBytes();
		
		fileOutput = new FileOutputStream(file_1);
		
		fileOutput.write(bytes);
		
		sc.close();		
	}
	public static void decomp() throws Exception{
		LZ77 lz77 = new LZ77();
		BitInput input = new BitInput(System.in);
		BitOutput output = new BitOutput(System.out);	
		Scanner sc = new Scanner(System.in);
		
		System.out.print("archive name: ");
		String filename = sc.nextLine();
		File file = new File(filename);
		
		if (!file.exists()) {
			System.out.println("File does not exist");
			return;
		}
		File file_1 = new File("decompressed_file.txt");
		if(!file.exists()) {
			file.createNewFile();
		}
		FileOutputStream fileOutput = null;	
		
		lz77.decompress(input, fileOutput);
		
		byte[] bytes = filename.getBytes();
		
		fileOutput = new FileOutputStream(file_1);
		
		fileOutput.write(bytes);
		
		sc.close();
		output.close();	
		fileOutput.close();
	}
	public static void size() throws Exception{
		System.out.println("file name: ");
		Scanner sc = new Scanner(System.in);
		String filename;
		filename = sc.nextLine();
		Path path = Paths.get(filename);

		try {
			long bytes = Files.size(path);
			System.out.println(String.format("%,d bytes", bytes));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		sc.close();
	}
	public static void equal() throws Exception{
		Scanner sc = new Scanner(System.in);
		System.out.println("first file name: ");
		String filename_1 = sc.nextLine();
		System.out.println("second file name: ");
		String filename_2 = sc.nextLine();
		Path path1 = Paths.get(filename_1);
		Path path2 = Paths.get(filename_2);
		
		byte[] f1 = Files.readAllBytes(path1);
		byte[] f2 = Files.readAllBytes(path2);
		if (Arrays.equals(f1,f2))
			System.out.println("true");
		else
			System.out.println("false");
		sc.close();
	}
	public static void about() {
		System.out.println("Informācija par grupu:");
		System.out.println("000RDB000 Filips Jānis Romāns 10.grupa");
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
