import java.io.*;
public class Main {
	public static void main(String[] args) throws IOException {
		System.out.println("Main.main()");
		LZ77 lz77 = new LZ77();
		ArithmeticCoder arithmetic = new ArithmeticCoder();
		BitInput input = new BitInput(System.in);
		BitOutput output = new BitOutput(System.out);

		lz77.compress(System.in, output);
		arithmetic.compress(input, output);
		arithmetic.decompress(input, output);
		lz77.decompress(input, System.out);
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
	void compress(BitInput input, BitOutput output) throws IOException {
		System.out.println("ArithmeticCoder.compress("+input+", "+output+")");
		input.readBit();
		output.writeBit(0);
		FrequencyTable frequencyTable = new FrequencyTable();
		frequencyTable.get(-1);
		frequencyTable.set(-1, -1);
		frequencyTable.increment(-1);
		frequencyTable.numberOfSymbols();
		frequencyTable.frequencySumBelow(-1);
	}
	void decompress(BitInput input, BitOutput output) throws IOException {
		System.out.println("ArithmeticCoder.decompress("+input+", "+output+")");
		input.readBit();
		output.writeBit(0);
	}
}

class FrequencyTable {
	int get(int symbol) {
		System.out.println("FrequencyTable.get("+symbol+")");
		return -1;
	}
	void set(int symbol, int frequency) {
		System.out.println("FrequencyTable.set("+symbol+", "+frequency+")");
	}
	void increment(int symbol) {
		System.out.println("FrequencyTable.increment("+symbol+")");
		set(symbol, -1);
		get(symbol);
	}
	int numberOfSymbols() {
		System.out.println("FrequencyTable.numberOfSymbols()");
		return -1;
	}
	int frequencySumBelow(int symbol) {
		System.out.println("FrequencyTable.frequencySumBelow("+symbol+")");
		return -1;
	}
}

class LZ77 {
	void compress(InputStream input, BitOutput output) throws IOException {
		System.out.println("LZ77.compress("+input+", "+output+")");
		output.writeBit(0);
		Match match = new Match();
		match.length();
		match.distance();
	}
	void decompress(BitInput input, OutputStream output) throws IOException {
		System.out.println("LZ77.decompress("+input+", "+output+")");
		input.readBit();
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
