package org.apache.aries.subsystem.scope.itests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.aries.subsystem.scope.ScopeUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

/**
 * Tests whether or not a bundle can be successfully uninstalled from a scope.
 * The root scope is used for this test.
 */
@RunWith(JUnit4TestRunner.class)
public class UninstallBundleTest extends AbstractTest {
	private Bundle bundle;
	private String location;
	
	@Test
	public void test() throws Exception {
		ScopeUpdate scopeUpdate = scope.newScopeUpdate();
		assertTrue("The bundle should have been removed", scopeUpdate.getBundles().remove(bundle));
		assertTrue("The commit should have been successful", scopeUpdate.commit());
		assertFalse("The bundle should have been removed from the scope", scope.getBundles().contains(bundle));
		assertFalse(Arrays.asList(bundleContext.getBundles()).contains(bundle));
		assertNull("The bundle should have been uninstalled", bundleContext.getBundle(location));
	}
	
	@Before
	public void before0() throws Exception {
		super.before();
		location = getBundleLocation("tb-2.jar");
		bundle = bundleContext.getBundle(location);
		assertNull("The bundle should not exist", bundle);
		installBundles(scope, new String[]{"tb-2.jar"});
		bundle = bundleContext.getBundle(location);
		assertNotNull("The bundle should exist", bundle);
		assertTrue("The bundle should part of the scope", scope.getBundles().contains(bundle));
	}
	
	@After
	public void after0() throws Exception {
		uninstallQuietly(bundle);
		super.after();
	}
}
