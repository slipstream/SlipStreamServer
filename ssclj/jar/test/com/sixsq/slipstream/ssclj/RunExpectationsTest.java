package com.sixsq.slipstream.ssclj;

import expectations.junit.ExpectationsTestRunner;
import org.junit.runner.RunWith;

@RunWith(expectations.junit.ExpectationsTestRunner.class)
public class RunExpectationsTest implements ExpectationsTestRunner.TestSource {

    public String testPath() {
        return "src/test/clojure/";
    }
}
