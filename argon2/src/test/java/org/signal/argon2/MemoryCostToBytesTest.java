package org.signal.argon2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public final class MemoryCostToBytesTest {

  private final MemoryCost memoryCost;
  private final int        expectedBytes;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{

      { MemoryCost.KiB(8), 8 * 1024 },
      { MemoryCost.KiB(16), 16 * 1024 },
      { MemoryCost.KiB(32), 32 * 1024 },
      { MemoryCost.KiB(64), 64 * 1024 },
      { MemoryCost.KiB(128), 128 * 1024 },
      { MemoryCost.KiB(256), 256 * 1024 },
      { MemoryCost.KiB(512), 512 * 1024 },

      { MemoryCost.MiB(1), 1024 * 1024 },
      { MemoryCost.MiB(2), 2 * 1024 * 1024 },
      { MemoryCost.MiB(4), 4 * 1024 * 1024 },
      { MemoryCost.MiB(8), 8 * 1024 * 1024 },
      { MemoryCost.MiB(16), 16 * 1024 * 1024 },
      { MemoryCost.MiB(32), 32 * 1024 * 1024 },
      { MemoryCost.MiB(64), 64 * 1024 * 1024 },
      { MemoryCost.MiB(128), 128 * 1024 * 1024 }
    });
  }

  public MemoryCostToBytesTest(MemoryCost memoryCost, int expectedBytes) {
    this.memoryCost    = memoryCost;
    this.expectedBytes = expectedBytes;
  }

  @Test
  public void toBytes() {
    assertEquals(expectedBytes, memoryCost.toBytes());
  }
}
