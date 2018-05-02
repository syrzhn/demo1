package ru.demo1.multythread;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * This example represents a data source. There are many competing threads wishing to
 * read(3 threads) and write(2 threads). It is acceptable to have multiple processes 
 * reading at the same time, but if one thread is writing then no other process may 
 * either read or write.
 */
public class ReadersWritersMultiThreading {
	private final DataSource mSourcer = new DataSource();
	/** Represents data source */
	class DataSource {
		/** main resource for reading and writing */
		private String resource = "\"Start value\""; 
		/**
		 * Reads the resource
		 * @param process that attempts to read
		 * @return state or resource after reading
		 */
		public  String read(Task process) {
			String name = process.getTaskName();
			Integer key = process.getKey();
			try {
				synchronized(this) {
					print(name + " is beginning to read the " + resource);
				}
				for (int i = 1; i <= N; i++) {
					Thread.sleep(randomDelay());
					process.setProgressView(i * (100 / N));
				}
			}
			catch (InterruptedException e) {
				print(name + " is interrupted");
			}
			finally {
				synchronized(this) {
					mReaders.remove(key);
					process.setButtonView("Start \"Reader" + key +"\""); 
					print(name + " was ended to read the " + resource + " and unloaded");
					if (mReaders.size() == 0)
						this.notifyAll();
				}
			}
			return resource;
		}
		/**
		 * Writes the resource
		 * @param process that attempts to write
		 * @return state or resource after writing
		 */
		public synchronized String write(Task process){
			String name = process.getTaskName();
			Integer key = process.getKey();
			try {
				while (!mReaders.isEmpty()) 
					this.wait();
				print(name + " is beginning to write the " + resource);
				resource = "value by " + name;
				for (int i = 1; i <= N; i++) {
					Thread.sleep(randomDelay());
					process.setProgressView(i * (100 / N));
				}
			}
			catch (InterruptedException e) {
				print(name + " is interrupted");
			}
			finally {
				mWriters.remove(key);
				process.setButtonView("Start \"Writer" + key +"\"");
				print(name + " was ended to read the " + resource + " and unloaded");
				this.notifyAll();
			}
			return resource;
		}
		/** rounds of read or write cycles*/
		private final Integer N = 5;          
		// Maps for threads managing
		private final Map<Integer, Thread> mReaders = new HashMap<Integer, Thread>(), 
                mWriters = new HashMap<Integer, Thread>();
		/** Sleep time in milliseconds for the threads, it's for hard work emulating */
		private final Integer DELAY = 1000;
		/** Generates any random integer value. */
		private int randomDelay() { return (int) (Math.random() * DELAY); }
	}
	
	/** Base functionality of all readers and writers */
	abstract class Task extends Thread {
		protected String  mName; // name for printing 
		protected Integer mNumber; // integer key for by map managing
		/** Base constructor */
		public Task(Integer number) { mNumber = number; };
		/** Paints progress of the task on a his own progress bar */
		abstract void  setProgressView(Integer prog);
		/** Sets caption on the button starts/stops the task */
		abstract void  setButtonView(String caption);
		/** Gets name of the task */
		public String  getTaskName() { return mName; }
		/** Gets thread of the task */
		public Thread  getThread() { return this; }
		/** Gets integer key is the task */
		public Integer getKey() { return mNumber; }
	}
	
	/** Reader */
	class Reader extends Task {
		public  Reader(Integer number) { 
			super(number);
			mName = "Reader" + mNumber; 
		}
		public void run() {
			print(mName + " attempts to start...");
			mSourcer.read(this);
		}
		@Override
		public void setProgressView(Integer prog) {
			setProgress(prog, progBarsRead[mNumber]);
		}
		@Override
		void setButtonView(String caption) {
			setButton(caption, btnsRead[mNumber]);
		}
	}
	/** Writer */
	class Writer extends Task {
		public  Writer(Integer number) { 
			super(number);
			mName = "Writer" + mNumber; 
		}
		public  void run() {
			print(mName + " attempts to start...");
			mSourcer.write(this);
		}
		@Override
		public void setProgressView(Integer prog) {
			setProgress(prog, progBarsWrite[mNumber]);
		}
		@Override
		void setButtonView(String caption) {
			setButton(caption, btnsWrite[mNumber]);
		}
	}	
	/**
	 * Writes <b>str</> in text component of main form
	 * from any thread
	 * @param str - string need to write 
	 */
	public void print(String str) {
		display.asyncExec(() -> { text.append(str + "\r\n"); });			
	}
	/**
	 * Sets stage of task executing progress on a progress bar
	 * from any thread
	 * @param prog - stage of progress
	 * @param bar  - progress bar 
	 */
	public void setProgress(Integer prog, ProgressBar bar) {
		display.asyncExec(() -> { 
			bar.setSelection(prog);
			"".toCharArray();
			});			
	}
	/**
	 * Sets caption on the button
	 * from any thread
	 * @param caption - caption
	 * @param button  - button 
	 */
	public void setButton(String caption, Button button) {
		display.asyncExec(() -> { 
			button.setText(caption);
			"".toCharArray();
			});			
	}
	/** 
	 * Creates and starts reader task with <b>i</b> key 
	 * @param i - key number of task
	 */
	private void startReader(Integer i) {
		Thread t = new Reader(i);
		mSourcer.mReaders.put(i, t);
		t.start();
	}
	/** 
	 * Stops reader task with <b>i</b> key 
	 * @param i - key number of task
	 */
	private void interruptReader(Integer i) {
		if (mSourcer.mReaders.containsKey(i)) {
			mSourcer.mReaders.get(i).interrupt();
		}
	}
	/** 
	 * Creates and starts writer task with <b>i</b> key 
	 * @param i - key number of task
	 */
	private void startWriter(Integer i) {
		Thread t = new Writer(i);
		mSourcer.mWriters.put(i, t);
		t.start();
	}
	/** 
	 * Stops writer task with <b>i</b> key 
	 * @param i - key number of task
	 */
	private void interruptWriter(Integer i) {
		if (mSourcer.mWriters.containsKey(i)) {
			mSourcer.mWriters.get(i).interrupt();
		}
	}
	/**
	 * This class represents part of viewer functionality that serves
	 * for connection between any task and SWT main form  
	 */
	class Viewer {
		public TaskViewer getReaderButtonAction(Integer i) {
				TaskViewer s = new ReaderViewer(i);
				return s;
		}
		public TaskViewer getWriterButtonAction(Integer i) {
				TaskViewer s = new WriterViewer(i);
				return s;
		}
		abstract class TaskViewer implements SelectionListener {
			public Boolean isStartedNow;
			public Button button;
			public Integer number;
			public TaskViewer(Integer i) { number = i; }
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		}
		class ReaderViewer extends TaskViewer {
			public ReaderViewer(Integer i) { 
				super(i);
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				button = (Button)e.getSource();
				if (!isStartedNow) {
					startReader(number);
					button.setText("Stop \"Reader" + number +"\"");
				}
				else {
					interruptReader(number);
					button.setText("Start \"Reader" + number +"\"");
				}
				isStartedNow = !isStartedNow;
			}			
		}
		class WriterViewer extends TaskViewer {
			public WriterViewer(Integer i) { 
				super(i);
			}
			@Override
			public void widgetSelected(SelectionEvent e) {
				button = (Button)e.getSource();
				if (!isStartedNow) {
					startWriter(number);
					button.setText("Stop \"Writer" + number +"\"");
				}
				else {
					interruptWriter(number);
					button.setText("Start \"Writer" + number +"\"");
				}
				isStartedNow = !isStartedNow;
			}			
		}
	}
	private Viewer mViewer; 
	
	/** Open the window. 
	 * @throws InterruptedException */
	public void open() throws InterruptedException {
		display = Display.getDefault();
		mViewer = new Viewer();
		createContents();		
		progBarsRead = new ProgressBar[] { progressBar3, progressBar4, progressBar5 }; 
		progBarsWrite = new ProgressBar[] { progressBar1, progressBar2 };
		btnsRead = new Button[] { btnStartReader0, btnStartReader1, btnStartReader2 };
		btnsWrite = new Button[] { btnStartWriter0, btnStartReader1 };
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	protected Shell shell;
	protected Display display;
	private Text text;
	private ProgressBar progressBar1;
	private ProgressBar progressBar2;
	private ProgressBar progressBar3;
	private ProgressBar progressBar4;
	private ProgressBar progressBar5;
	private ProgressBar progBarsRead[]; // This arrays provides access for any task 
	private ProgressBar progBarsWrite[]; // to any progress bar by number (from 0...) 
	private Button btnStartWriter0;
	private Button btnStartWriter1;
	private Button btnStartReader0;
	private Button btnStartReader1;
	private Button btnStartReader2;
	private Button btnsRead[];  // and any buttons to manage the threads
	private Button btnsWrite[];
	/**Launch the application.
	 * @param args*/
	public static void main(String[] args) {
		try {
			ReadersWritersMultiThreading window = new ReadersWritersMultiThreading();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/** Create contents of the window. */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(450, 450);
		shell.setText("SWT Application");
		shell.setLayout(new GridLayout(2, false));
		
		text = new Text(shell, SWT.BORDER | SWT.MULTI);
		text.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.NORMAL));
		GridData gd_text = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd_text.heightHint = 88;
		text.setLayoutData(gd_text);
		
		btnStartWriter0 = new Button(shell, SWT.NONE);
		btnStartWriter0.addSelectionListener(mViewer.getWriterButtonAction(0));
		btnStartWriter0.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 1, 1));
		btnStartWriter0.setText("Start \"Writer0\"");
		
		progressBar1 = new ProgressBar(shell, SWT.NONE);
		progressBar1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		btnStartWriter1 = new Button(shell, SWT.NONE);
		btnStartWriter1.addSelectionListener(mViewer.getWriterButtonAction(1));
		btnStartWriter1.setText("Start \"Writer1\" ");
		
		progressBar2 = new ProgressBar(shell, SWT.NONE);
		progressBar2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		btnStartReader0 = new Button(shell, SWT.NONE);
		btnStartReader0.addSelectionListener(mViewer.getReaderButtonAction(0));
		btnStartReader0.setText("Start \"Reader0\"");
		
		progressBar3 = new ProgressBar(shell, SWT.NONE);
		progressBar3.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		btnStartReader1 = new Button(shell, SWT.NONE);
		btnStartReader1.addSelectionListener(mViewer.getReaderButtonAction(1));
		btnStartReader1.setText("Start \"Reader1\"");
		
		progressBar4 = new ProgressBar(shell, SWT.NONE);
		progressBar4.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		btnStartReader2 = new Button(shell, SWT.NONE);
		btnStartReader2.addSelectionListener(mViewer.getReaderButtonAction(2));
		btnStartReader2.setText("Start \"Reader2\"");
		
		progressBar5 = new ProgressBar(shell, SWT.NONE);
		progressBar5.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	}
}
