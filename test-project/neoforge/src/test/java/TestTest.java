import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTest {
    @Test
    public void test() {
        net.minecraft.client.Minecraft.getInstance();
        assertEquals(1, 2, "This test is intentionally failing to demonstrate the test setup.");
    }
}
