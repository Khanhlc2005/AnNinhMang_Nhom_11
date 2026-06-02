package des;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaddingUtilsTest {
    @Test
    void appliesPkcs5PaddingToPartialBlock() {
        byte[] padded = PaddingUtils.applyPkcs5Padding(new byte[]{1, 2, 3});

        assertArrayEquals(new byte[]{1, 2, 3, 5, 5, 5, 5, 5}, padded);
    }

    @Test
    void appliesFullPaddingBlockWhenInputIsAlreadyAligned() {
        byte[] padded = PaddingUtils.applyPkcs5Padding(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});

        assertEquals(16, padded.length);
        assertEquals(8, padded[15]);
    }

    @Test
    void removesValidPkcs5Padding() {
        byte[] unpadded = PaddingUtils.removePkcs5Padding(new byte[]{1, 2, 3, 5, 5, 5, 5, 5});

        assertArrayEquals(new byte[]{1, 2, 3}, unpadded);
    }

    @Test
    void rejectsInvalidPkcs5Padding() {
        assertThrows(IllegalArgumentException.class,
                () -> PaddingUtils.removePkcs5Padding(new byte[]{1, 2, 3, 4, 4, 4, 4, 3}));
    }
}
