package com.dianping.cat.agent.monitor.executors.jvm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import com.dianping.cat.Cat;
import com.dianping.cat.agent.monitor.DataEntity;
import com.dianping.cat.agent.monitor.Utils;

public class JVMMemoryExecutor extends AbstractJVMExecutor implements Initializable {

	public static final String ID = "JVMMemoryExecutor";

	private List<DataEntity> buildJVMMemoryInfo() {
		List<DataEntity> entities = new ArrayList<DataEntity>();
		Set<String> pids = findPidOfTomcat();

		for (String pid : pids) {
			List<String> lines = null;

			try {
				lines = Utils.runShell("/usr/local/jdk/bin/jstat -gcutil " + pid);
			} catch (Exception e) {
				try {
					lines = Utils.runShell("jstat -gcutil " + pid);
				} catch (Exception cause) {
					Cat.logError("Maybe cat agent doesn't know path of jstat ", cause);
				}
			}
			if (lines.size() == 2) {
				Iterator<String> iterator = lines.iterator();
				iterator.next();
				String line = iterator.next();
				String[] metrics = line.split(" +");
				long current = System.currentTimeMillis();

				try {
					DataEntity eden = new DataEntity();
					eden.setId(buildJVMDataEntityId("edenUsage", pid)).setType(AVG_TYPE).setTime(current)
					      .setValue(Double.valueOf(metrics[2]) / 100);
					addGroupDomainInfo(eden);
					entities.add(eden);

					DataEntity old = new DataEntity();
					old.setId(buildJVMDataEntityId("oldUsage", pid)).setType(AVG_TYPE).setTime(current)
					      .setValue(Double.valueOf(metrics[3]) / 100);
					addGroupDomainInfo(old);
					entities.add(old);

					DataEntity perm = new DataEntity();
					perm.setId(buildJVMDataEntityId("permUsage", pid)).setType(AVG_TYPE).setTime(current)
					      .setValue(Double.valueOf(metrics[4]) / 100);
					addGroupDomainInfo(perm);
					entities.add(perm);
				} catch (Exception e) {
					Cat.logError(e);
				}
			} else {
				Cat.logError(new RuntimeException("No tomcat is running, [jstat -gcutil] result: " + lines));
			}
		}
		return entities;
	}

	@Override
	public List<DataEntity> execute() {
		List<DataEntity> entities = new ArrayList<DataEntity>();
		entities.addAll(buildJVMMemoryInfo());

		return entities;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void initialize() throws InitializationException {
		if (m_pidsOfTomcat.isEmpty()) {
			m_pidsOfTomcat.addAll(findPidOfTomcat());
		}
	}
}