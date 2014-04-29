import expectations.junit.ExpectationsTestRunner;
import org.junit.runner.RunWith;

@RunWith(expectations.junit.ExpectationsTestRunner.class)
public class ClojureTests implements ExpectationsTestRunner.TestSource {

    public String testPath() {
        // return the path to your root test dir here
        return "src/test/clojure";
    }
}
