package com.auction;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("com.auction")
@IncludeClassNamePatterns(".*Test")
class AllTestSuite {
}
