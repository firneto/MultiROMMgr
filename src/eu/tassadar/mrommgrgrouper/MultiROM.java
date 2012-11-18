package eu.tassadar.mrommgrgrouper;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import java.io.File;
import java.util.List;

public class MultiROM {
    
    public static final String INTERNAL_NAME = "Internal";
    
    public static final int CMD_RENAME_ROM = 0;
    public static final int CMD_DELETE_ROM = 1;
    
    static public File getMRomFolder() {
        if(m_mrom_folder != null)
            return m_mrom_folder;
        
        File ext = Environment.getExternalStorageDirectory();
        if(ext == null)
            return null;
        
        File mrom = new File(ext, "multirom");
        if(!mrom.exists() || !mrom.isDirectory())
            return null;
        m_mrom_folder = mrom;
        return m_mrom_folder;
    }
    
    static public File getRomsFolder() {
        File f = getMRomFolder();
        if(f == null)
            return null;

        File roms = new File(f, "roms");
        if(!roms.exists() || !roms.isDirectory())
            return null;
        return roms;
    }
    
    static public String getBusybox() {
        File f = getMRomFolder();
        if(f == null)
            return null;

        File box = new File(f, "busybox");
        box.setExecutable(true);

        if(!box.exists() || !box.isFile() || !box.canExecute())
            return null;
        return box.getAbsolutePath();
    }
    
    static public boolean isROMNameOkay(String name) {
        if(name == null || name.isEmpty())
            return false;

        if(name.equals(INTERNAL_NAME))
            return false;
        
        File dir = getRomsFolder();
        if(dir == null)
            return false;
        
        File[] files = dir.listFiles();
        for(File f : files) {
            if(f.getName().compareToIgnoreCase(name) == 0)
                return false;
        }
        return true;
    }
    
    static public void renameROM(String from, String to, Handler handler) throws Exception {
        String cmd = getBusybox();
        if(cmd == null)
            throw new Exception("Could not find busybox");
        
        File folder = getRomsFolder();
        if(folder == null)
            throw new Exception("Could not get roms folder");
        
        cmd += " mv \"" + folder.getAbsolutePath() + "/" + from + "\"";
        cmd += " \"" + folder.getAbsolutePath() + "/" + to + "\"";
        
        Shell.runAsync(cmd, CMD_RENAME_ROM, handler);
    }
    
    static public void deleteROM(String rom, Handler handler) throws Exception {
        if(rom == null || rom.isEmpty())
            throw new Exception("Rom name must not be empty");

        String cmd = getBusybox();
        if(cmd == null)
            throw new Exception("Could not find busybox");
        
        File folder = getRomsFolder();
        if(folder == null)
            throw new Exception("Could not get roms folder");
        
        cmd += " rm -rf \"" + folder.getAbsolutePath() + "/" + rom + "\"";
        
        Shell.runAsync(cmd, CMD_DELETE_ROM, handler);
    }
    
    static public MultiROMCfg loadCfg() {
        MultiROMCfg cfg = new MultiROMCfg();
        
        File folder = getMRomFolder();
        if(folder == null)
            return null;
        
        File file = new File(folder, "multirom.ini");
        if(file == null || !file.isFile() || !file.canRead())
            return null;
        
        try {
            String[] cmd = { "cat " + file.getAbsolutePath() };
            List<String> res = Shell.run(cmd, false);
            
            for(String line : res)
            {
                String[] tokens = line.split("=");
                if(tokens.length != 2)
                    continue;
               
                if(tokens[0].equals("is_second_boot"))
                    cfg.is_second_boot = Integer.parseInt(tokens[1]);
                else if(tokens[0].equals("current_rom"))
                    cfg.current_rom = tokens[1];
                else if(tokens[0].equals("auto_boot_seconds"))
                    cfg.auto_boot_seconds = Integer.parseInt(tokens[1]);
                else if(tokens[0].equals("auto_boot_rom"))
                    cfg.auto_boot_rom = tokens[1];
            }
        }
        catch(Exception e) {
            return null;
        }
        
        return cfg;
    }
    
    static public void saveConfig(MultiROMCfg cfg) {
        
        Log.e("aa", "bbbb");
        File folder = getMRomFolder();
        if(folder == null)
            return;
        
        Log.e("aa", "aa");
        File file = new File(folder, "multirom.ini");
        if(file == null || !file.isFile())
        {
            Log.e("aa", "Wtf");
            return;
        }
        String p = file.getAbsolutePath();
        
        try {
            String[] cmds = new String[4];
            cmds[0] = "echo is_second_boot=" + cfg.is_second_boot + " > " + p;
            cmds[1] = "echo \"current_rom=" + cfg.current_rom + "\" >> " + p;
            cmds[2] = "echo auto_boot_seconds=" + cfg.auto_boot_seconds + " >> " + p;
            cmds[3] = "echo \"auto_boot_rom=" + cfg.auto_boot_rom + "\" >> " + p;
            Shell.run(cmds, false);
        }
        catch(Exception ex)
        {
            Log.e("aa", ex.getMessage());
            int i;
            int s = 0;
        }
    }
    
    static File m_mrom_folder;
}
