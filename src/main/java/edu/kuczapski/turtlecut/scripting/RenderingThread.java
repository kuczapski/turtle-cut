package edu.kuczapski.turtlecut.scripting;

public class RenderingThread extends Thread{


	public static class Job{
		public final Cutter cutter;
		public final String program;

		public final double speedMMPS;
		public final int currentLine;

		public Job(Cutter cutter, String program, double speedMMPS, int currentLine) {
			super();
			this.cutter = cutter;
			this.program = program;
			this.speedMMPS = speedMMPS;
			this.currentLine = currentLine;
		}
	}

	private final Object monitor = new Object();

	private Job currentJobPending;
	private Job currentJobInExecution;

	public RenderingThread() {
		setDaemon(true);
		start();
	}
	
	public void requestJob(Cutter cutter, String program, double speedMMPS, int currentLine) {

		synchronized (monitor) {
			Job newJob = new Job(cutter, program, speedMMPS, currentLine);
			currentJobPending = newJob;
			if(currentJobInExecution!=null) {
				currentJobInExecution.cutter.setStopExecution();
			}

			monitor.notifyAll();
		}
	}

	@Override
	public void run() {

		try {
			while(!isInterrupted()){

				synchronized(monitor) {
					while(currentJobPending==null) {
						monitor.wait(100);
					}

					currentJobInExecution = currentJobPending;
					currentJobPending = null;
				}
				
				try{
					Job job = currentJobInExecution;
					job.cutter.execute(job.program, job.speedMMPS, job.currentLine);
				}catch (Exception e) {
					e.printStackTrace();
				}
				
				synchronized(monitor) {
					currentJobInExecution = null;
				}
				

			}
		}catch (InterruptedException e) {

		}

	}

}
