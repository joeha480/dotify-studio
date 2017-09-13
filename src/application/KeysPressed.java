package application;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

class KeysPressed {
	private final Set<KeyCode> pressed;
	private final Timer t;
	private TimerTask tt;
	private final Consumer<Set<KeyCode>> c;
	private final static Map<KeyCode, Integer> values = new HashMap<>();
	static {
		values.put(KeyCode.F, 1);
		values.put(KeyCode.D, 2);
		values.put(KeyCode.S, 4);
		values.put(KeyCode.J, 8);
		values.put(KeyCode.K, 16);
		values.put(KeyCode.L, 32);
	}
	
	KeysPressed(Node node, Consumer<Set<KeyCode>> c) {
		this.c = c;
		this.t = new Timer(true);
		this.pressed = new HashSet<>();
		node.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
			pressed.add(ev.getCode());
			setTimerTask();
		});
		node.addEventHandler(KeyEvent.KEY_RELEASED, ev-> {
			pressed.remove(ev.getCode());
			setTimerTask();
		});
	}

	private synchronized void setTimerTask() {
		if (tt!=null) {
			tt.cancel();
			tt = null;
		}
		tt = new TimerTask() {
			@Override
			public void run() {
				Set<KeyCode> p = getPressed();
				if (!p.isEmpty()) {
					char ch = (char)(0x2800+p.stream().mapToInt(v -> values.getOrDefault(v, 0)).sum());
					System.out.println(ch);
					c.accept(p);
				}
			}
		};
		t.schedule(tt, 100);
	}

	Set<KeyCode> getPressed() {
		return Collections.unmodifiableSet(pressed);
	}

}