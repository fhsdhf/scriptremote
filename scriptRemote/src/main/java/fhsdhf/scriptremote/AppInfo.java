package fhsdhf.scriptremote;

public class AppInfo {
		private String name, exec;
		public String getName() { return name; }
		public String getExec() { return exec; }
		public AppInfo(String name, String exec) {
			this.name = name;
			this.exec = exec;
		}
	}