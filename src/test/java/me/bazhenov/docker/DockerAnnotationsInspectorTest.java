package me.bazhenov.docker;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class DockerAnnotationsInspectorTest {

	private DockerAnnotationsInspector inspector;

	@BeforeMethod
	public void setUp() {
		inspector = new DockerAnnotationsInspector();
	}

	@Test
	public void shouldFindAnnotationsOnAType() {
		ContainerNamespace namespace = inspector.createNamespace(TestCase1.class);
		assertThat(namespace.size(), is(1));
		assertThat(namespace.getDefinition("foo").getImage(), is("image"));
	}

	@Test
	public void emptyObjectShouldReturnNoNamespaceAtAll() {
		assertThat(inspector.createNamespace(Object.class), nullValue());
	}

	@Test
	public void shouldSaveResolvedTypes() {
		ContainerNamespace namespace = inspector.createNamespace(TestCase1.class);

		NotificationMethod method = inspector.resolveNotificationMethod(TestCase1.class).orElse(null);
		assertThat(method, notNullValue());
		assertThat(method.getMethod().getName(), is("afterStart"));

		List<PortRef> portRefs = method.getArguments();
		assertThat(portRefs, hasSize(1));

		PortRef first = portRefs.get(0);
		assertThat(first.getContainerDefinition().getImage(), is("image"));
		assertThat(first.getContainerPort(), is(13));

		HashMap<Integer, Integer> ports = new HashMap<>();
		ports.put(13, 14);
		namespace.registerPublishedTcpPorts(first.getContainerDefinition(), ports);

		TestCase1 testCase = new TestCase1();
		method.call(testCase);

		assertThat(testCase.getPort(), is(14));
	}

	@Test
	public void shouldFindMultipleAnnotationsOnAType() {
		ContainerNamespace namespace = inspector.createNamespace(TestCase2.class);
		assertThat(namespace.size(), is(2));
		assertThat(namespace.getDefinition("foo").getImage(), is("image1"));
		assertThat(namespace.getDefinition("bar").getImage(), is("image2"));
	}

	@Test
	public void shouldImportSharedContainers() {
		ContainerNamespace case3 = inspector.createNamespace(TestCase3.class);
		ContainerNamespace case4 = inspector.createNamespace(TestCase4.class);

		assertThat(case3.size(), is(1));
		assertThat(case4.size(), is(1));
	}
}

@Container(name = "foo", image = "image") class TestCase1 {

	private int port;

	@AfterContainerStart
	public void afterStart(@ContainerPort(name = "foo", port = 13) int port) {
		this.port = port;
	}

	int getPort() {
		return port;
	}
}

@Container(name = "foo", image = "image1")
@Container(name = "bar", image = "image2") class TestCase2 {

}

@Container(name = "shared", image = "mysql") class LocalSharedContainers {

}

@ContainersFrom(LocalSharedContainers.class) class TestCase3 {

}

@ContainersFrom(LocalSharedContainers.class) class TestCase4 {

}