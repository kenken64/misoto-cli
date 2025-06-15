package sg.edu.nus.iss.misoto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
	"misoto.agent.mode.enabled=false"
})
class MisotoApplicationTests {

	@Test
	void contextLoads() {
	}

}
