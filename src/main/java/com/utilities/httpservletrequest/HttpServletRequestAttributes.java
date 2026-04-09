import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Enumeration;

public class HttpServletRequestAttributes {

	public static void getAttributes(HttpServletRequest request) {
		try {
			Enumeration<String> params = request.getParameterNames();
			while (params.hasMoreElements()) {
			    String key = params.nextElement();
			    String value = request.getParameter(key);
			}
			HttpSession session = request.getSession(false); // false = don't create if missing
			if (session != null) {

			    Enumeration<String> names = session.getAttributeNames();

			    while (names.hasMoreElements()) {
			        String name = names.nextElement();
			        Object value = session.getAttribute(name);
			    }
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
}
