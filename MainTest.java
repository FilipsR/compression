import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.junit.Test;
import static org.junit.Assert.*;

public class MainTest {
	@Test
	public void testBitOutput() throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try(BitOutput b = new BitOutput(stream)) {
			b.writeBit(1);
			b.writeBit(1);
			b.writeBit(1);
		}
		assertArrayEquals(new byte[]{(byte)0b11100000}, stream.toByteArray());
		stream.reset();

		try(BitOutput b = new BitOutput(stream)) {
			for(int i = 1; i <= 32; i++)
				b.writeBit(1);
		}
		assertArrayEquals(new byte[]{-1, -1, -1, -1}, stream.toByteArray());
		stream.reset();

		try(BitOutput b = new BitOutput(stream)) {
		}
		assertArrayEquals(new byte[0], stream.toByteArray());
		stream.reset();
	}

	@Test
	public void testBitInput() throws IOException {
		try(BitInput b = new BitInput(new ByteArrayInputStream(new byte[]{(byte)0b11100000}))) {
			assertEquals(1, b.readBit());
			assertEquals(1, b.readBit());
			assertEquals(1, b.readBit());
			assertEquals(0, b.readBit());
			assertEquals(0, b.readBit());
			assertEquals(0, b.readBit());
			assertEquals(0, b.readBit());
			assertEquals(0, b.readBit());
			assertEquals(-1, b.readBit());
		}

		try(BitInput b = new BitInput(new ByteArrayInputStream(new byte[]{-1, -1, -1, -1}))) {
			for(int i = 1; i <= 32; i++)
				assertEquals(1, b.readBit());
			assertEquals(-1, b.readBit());
		}

		try(BitInput b = new BitInput(new ByteArrayInputStream(new byte[0]))) {
			assertEquals(-1, b.readBit());
		}
	}

	private void testArithmeticCoderWith(int symbols, byte[] data) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try(BitOutput bitOut = new BitOutput(out)) {
			ArithmeticCoder enc = new ArithmeticCoder();
			FrequencyTable freq1 = new FrequencyTable(symbols);
			freq1.setAll(1);
			for(byte b : data) {
				enc.compress(b & 0xFF, freq1, bitOut);
			}
			enc.compress(0, freq1, bitOut);
			enc.finish();
		}
		FrequencyTable freq2 = new FrequencyTable(symbols);
		freq2.setAll(1);
		ArithmeticCoder dec = new ArithmeticCoder();
		BitInput bitIn = new BitInput(new ByteArrayInputStream(out.toByteArray()));
		for(byte b : data) {
			assertEquals(b & 0xFF, dec.decompress(bitIn, freq2));
		}
	}

	@Test
	public void testArithmeticCoder70() throws IOException {
		testArithmeticCoderWith(70, new byte[]{0, 33, 44, 44, 55});
	}

	@Test
	public void testArithmeticCoder7() throws IOException {
		testArithmeticCoderWith(7, new byte[]{0, 3, 4, 4, 5, 1, 6, 1, 1});
	}

	@Test
	public void testArithmeticCoderLoremIpsum() throws IOException {
		testArithmeticCoderWith(256, Files.readAllBytes(Paths.get("test/lorem-ipsum-7k")));
	}

	@Test
	public void testArithmeticCoderRandom() throws IOException {
		testArithmeticCoderWith(256, Files.readAllBytes(Paths.get("test/dev-random-1k")));
	}

	@Test
	public void testMatch() {
		Match m = new Match(100, 50);
		assertEquals(100, m.distance());
		assertEquals(50, m.length());
	}

	@Test
	public void testFrequencyTable() {
		FrequencyTable t = new FrequencyTable(1234);
		assertEquals(1234, t.numberOfSymbols());
		assertEquals(0, t.get(0));
		assertEquals(0, t.get(456));
		assertEquals(0, t.get(1233));
		assertEquals(0, t.frequencySumBelow(123));

		t.set(0, 4);
		assertEquals(4, t.get(0));
		assertEquals(1234, t.numberOfSymbols());
		assertEquals(4, t.frequencySumBelow(123));
		assertEquals(4, t.frequencySumBelow(1));

		t.set(0, 5);
		assertEquals(5, t.get(0));
		assertEquals(1234, t.numberOfSymbols());
		assertEquals(5, t.frequencySumBelow(1));
		assertEquals(0, t.frequencySumBelow(0));

		t.set(1, 5);
		assertEquals(5, t.get(0));
		assertEquals(5, t.get(1));
		assertEquals(0, t.get(2));
		assertEquals(0, t.frequencySumBelow(0));
		assertEquals(5, t.frequencySumBelow(1));
		assertEquals(10, t.frequencySumBelow(2));
		assertEquals(10, t.frequencySumBelow(3));

		t.set(1233, 5);
		assertEquals(5, t.get(1233));
		assertEquals(10, t.frequencySumBelow(1233));
		assertEquals(10, t.frequencySumBelow(2));
		assertEquals(15, t.frequencySumBelow(1234));

		t.add(1233, 5);
		assertEquals(10, t.frequencySumBelow(1233));
		assertEquals(20, t.frequencySumBelow(1234));

		t.add(0, 5);
		assertEquals(15, t.frequencySumBelow(1233));
		assertEquals(25, t.frequencySumBelow(1234));
	}

	@Test
	public void testLZ77() throws IOException {
		BitOutput nullOutput = new BitOutput(new OutputStream() {
			public void write(int b) {}
		});
		LZ77 lz = new LZ77();
		StringBuilder literal = new StringBuilder();
		List<Match> matches = new ArrayList<>();
		lz.debugMatchAction = matches::add;
		lz.debugCharAction = c -> literal.append((char)c);

		{
			InputStream inputStream = new ByteArrayInputStream("123word456word789".getBytes());
			lz.compress(inputStream, nullOutput);
			assertEquals("123word456789", literal.toString());
			assertEquals(1, matches.size());
			assertEquals(4, matches.get(0).length());
			assertEquals(7, matches.get(0).distance());
			matches.clear();
			literal.delete(0, literal.length());
		}

		{
			InputStream inputStream = new ByteArrayInputStream("blueblueblueblue".getBytes());
			lz.compress(inputStream, nullOutput);
			assertEquals("blue", literal.toString());
			assertEquals(2, matches.size());
			assertEquals(4, matches.get(0).length());
			assertEquals(4, matches.get(0).distance());
			assertEquals(8, matches.get(1).length());
			assertEquals(8, matches.get(1).distance());
			matches.clear();
			literal.delete(0, literal.length());
		}

		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			Iterator<Integer> input = Arrays.<Integer>asList((int)'a', (int)'b', (int)'c', LZ77.EOF).iterator();
			lz.debugReadAction = () -> input.next();
			lz.decompress(null, output);
			assertEquals("abc", new String(output.toByteArray()));
		}

		{
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			Iterator<Integer> input = Arrays.<Integer>asList((int)'a', (int)'b', (int)'c', LZ77.MATCH, 3, 3, LZ77.MATCH, 5, 4, LZ77.EOF).iterator();
			lz.debugReadAction = () -> input.next();
			lz.decompress(null, output);
			assertEquals("abcabc"+"bcab", new String(output.toByteArray()));
		}
	}

	private static void testWith(byte[] source) throws IOException {
		ByteArrayOutputStream tmp = new ByteArrayOutputStream();
		try(
			BitOutput output = new BitOutput(tmp)
		) {
			LZ77 lz = new LZ77();
			lz.compress(new ByteArrayInputStream(source), output);
		}

		try(
			BitInput input = new BitInput(new ByteArrayInputStream(tmp.toByteArray()))
		) {
			tmp.reset();
			LZ77 lz = new LZ77();
			lz.decompress(input, tmp);
		}

		assertArrayEquals(source, tmp.toByteArray());
	}

	@Test
	public void testIntegrationRandom() throws IOException {
		testWith(Files.readAllBytes(Paths.get("test/dev-random-1k")));
	}

	@Test
	public void testIntegrationLoremIpsum() throws IOException {
		testWith(Files.readAllBytes(Paths.get("test/lorem-ipsum-7k")));
	}
}
