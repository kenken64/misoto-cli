package sg.edu.nus.iss.misoto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
	"misoto.agent.mode.enabled=false",
	"misoto.mcp.auto-initialize=false",
	"spring.main.web-application-type=none",
	"spring.shell.interactive.enabled=false",
	"spring.shell.noninteractive.enabled=false"
})
class MisotoApplicationTests {

	@Test
	void contextLoads() {
	}

}
