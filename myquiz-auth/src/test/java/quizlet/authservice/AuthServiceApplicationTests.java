package quizlet.authservice;

import com.unitbv.myquiz.auth.AuthServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		classes = AuthServiceApplication.class,
		properties = {
				"MYQUIZ_IAM_URL=http://localhost:8888/api",
				"FRONTEND_URL=http://localhost:8080"
		}
)
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
