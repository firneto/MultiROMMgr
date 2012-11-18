package eu.tassadar.mrommgrgrouper;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Shell {
    static final public String TAG = "MultiROMMgr";
    static final private String EXIT_NOTE = "exited4815162342";
    
    static final public int SHELL_MSG_ROOT_STATUS = 0;
    static final public int SHELL_MSG_CMD_FINISHED = 1;

        
    static public synchronized List<String> run(String[] commands, boolean keepOpen) {
        List<String> res = new ArrayList<String>();
        try
        {
            if(m_process == null)
                openShell();

            for (String write : commands) {
                Log.e(TAG, write);

		m_stdin.writeBytes(write + "\n");
	        m_stdin.flush();
	    }
            // FIXME: this just is wrong
            m_stdin.writeBytes("echo " + EXIT_NOTE + "\n");

            while (true) {
                while (!m_stdout.ready())
                {
                    Thread.sleep(100);
                } 

                String read = m_stdout.readLine();
                if(read.equals(EXIT_NOTE))
                    break;
                
                Log.e(TAG, read);
                res.add(read);
	    }

	    while (m_stderr.ready()) {
                String read = m_stderr.readLine();
		Log.e(TAG, read);
                res.add(read);
            }
                
            if(!keepOpen)
                closeShell();
            
        } catch(IOException ex) {
            return null;
        }
        catch(InterruptedException ex) {
            return null;
        }
        return res;
    }
    
    static public synchronized void openShell() throws IOException
    {
        Log.e(TAG, "Opening shell");

        m_process = Runtime.getRuntime().exec("su");
        m_stdin = new DataOutputStream(m_process.getOutputStream());
        m_stdout = new BufferedReader(new InputStreamReader(m_process.getInputStream()));
	m_stderr = new BufferedReader(new InputStreamReader(m_process.getErrorStream()));
    }
    
    static public synchronized void closeShell() throws IOException, InterruptedException
    {
        Log.e(TAG, "Closing shell");

        m_stdin.writeBytes("exit\n");
        m_stdin.flush();

        m_process.waitFor();
        
        m_stdin.close();
        m_stdout.close();
        m_stderr.close();

        m_process.destroy();
        m_process = null;
        m_stdin = null;
        m_stdout = null;
        m_stderr = null;
    }
    
    static public void requestRootCheck(Handler handler) {
        RootCheckThread t = new RootCheckThread(handler);
        t.start();
    }
    
    static private class RootCheckThread extends Thread {
        private Handler m_handler;
        
        public RootCheckThread(Handler handler) {
            m_handler = handler;
        }

        @Override
        public void run() {
            int res = 1;
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream stdin = new DataOutputStream(process.getOutputStream());
                stdin.writeBytes("exit\n");
                stdin.flush();
                process.waitFor();
                stdin.close();
                
                if(process.exitValue() != 0)
                    res = 0;
                process.destroy();
            } catch(IOException ex) {
                res = 0;
            } catch(InterruptedException ex){
                res = 0;
            }

            if(res == 1)
            {
                List<String> out = Shell.run(new String[] { "echo \"testing root\""}, false);
                res = (out.size() == 1 && out.get(0).equals("testing root")) ? 1 : 0;
            }

            Message msg = m_handler.obtainMessage(SHELL_MSG_ROOT_STATUS);
            msg.arg1 = res;
            msg.sendToTarget();
        }
    }
    
    static public void runAsync(String cmd, int id, Handler handler) {
        RunThread t = new RunThread(new String[] { cmd }, id, handler);
        t.start();
    }
    
    static private class RunThread extends Thread {
        private String[] m_cmds;
        private int m_id;
        private Handler m_handler;
        
        public RunThread(String[] cmds, int id, Handler handler) {
            m_cmds = cmds;
            m_id = id;
            m_handler = handler;
        }
        
        @Override
        public void run() {
            List<String> res = Shell.run(m_cmds, false);
            Message msg = m_handler.obtainMessage(SHELL_MSG_CMD_FINISHED);
            msg.arg1 = m_id;
            msg.obj = res;
            msg.sendToTarget();
        }
    }
    
    static Process m_process;
    static DataOutputStream m_stdin;
    static BufferedReader m_stdout;
    static BufferedReader m_stderr;
}
