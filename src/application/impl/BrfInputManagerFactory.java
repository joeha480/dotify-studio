package application.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.daisy.streamline.api.tasks.TaskGroup;
import org.daisy.streamline.api.tasks.TaskGroupFactory;
import org.daisy.streamline.api.tasks.TaskGroupInformation;
import org.daisy.streamline.api.tasks.TaskGroupSpecification;
import org.osgi.service.component.annotations.Component;

/**
 * Provides a task group factory for brf input.
 * @author Joel Håkansson
 */
@Component
public class BrfInputManagerFactory implements TaskGroupFactory {
	private final Set<TaskGroupInformation> information;
	/**
	 * Creates a new text input manager factory.
	 */
	public BrfInputManagerFactory() {

		Set<TaskGroupInformation> tmp = new HashSet<>();
		tmp.add(TaskGroupInformation.newConvertBuilder("brf", "pef").build());
		information = Collections.unmodifiableSet(tmp);
	}
	
	@Override
	public boolean supportsSpecification(TaskGroupInformation spec) {
		return listAll().contains(spec);
	}

	@Override
	public TaskGroup newTaskGroup(TaskGroupSpecification spec) {
		return new BrfInputManager(spec.getLocale());
	}

	@Override
	public Set<TaskGroupInformation> listAll() {
		return information;
	}

}
