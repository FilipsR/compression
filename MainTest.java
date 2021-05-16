import java.io.*;
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
		assertArrayEquals(new byte[]{0b111}, stream.toByteArray());
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
		try(BitInput b = new BitInput(new ByteArrayInputStream(new byte[]{0b111}))) {
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

	@Test
	public void testMatch() {
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
		LZ77 lz = new LZ77();
		StringBuilder literal = new StringBuilder();
		List<Match> matches = new ArrayList<>();
		lz.debugMatchAction = matches::add;
		lz.debugCharAction = c -> literal.append((char)c);

		{
			InputStream inputStream = new ByteArrayInputStream("123word456word789".getBytes());
			lz.compress(inputStream, null);
			assertEquals("123word456789", literal.toString());
			assertEquals(1, matches.size());
			assertEquals(4, matches.get(0).length());
			assertEquals(7, matches.get(0).distance());
			matches.clear();
			literal.delete(0, literal.length());
		}

		{
			InputStream inputStream = new ByteArrayInputStream("blueblueblueblue".getBytes());
			lz.compress(inputStream, null);
			assertEquals("blue", literal.toString());
			assertEquals(2, matches.size());
			assertEquals(4, matches.get(0).length());
			assertEquals(4, matches.get(0).distance());
			assertEquals(8, matches.get(1).length());
			assertEquals(8, matches.get(1).distance());
			matches.clear();
			literal.delete(0, literal.length());
		}
	}
}
