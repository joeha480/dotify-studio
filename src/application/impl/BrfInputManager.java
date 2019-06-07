package application.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.daisy.streamline.api.tasks.InternalTask;
import org.daisy.streamline.api.tasks.TaskGroup;
import org.daisy.streamline.api.tasks.TaskSystemException;

class BrfInputManager implements TaskGroup {
	private final String rootLang;

	BrfInputManager(String rootLang) {
		this.rootLang = rootLang;
	}

	@Override
	public String getName() {
		return "BrfInputManager";
	}

	@Override
	public List<InternalTask> compile(Map<String, Object> parameters) throws TaskSystemException {
		List<InternalTask> ret = new ArrayList<>();
		ret.add(new Brf2PefTask("BRF to PEF converter", rootLang, parameters));
		return ret;
	}

}