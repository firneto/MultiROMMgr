package eu.tassadar.mrommgrgrouper;

//import eu.tassadar.mrommgrgrouper.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import eu.tassadar.mrommgrgrouper.ROMListItem.ROMItemClicked;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity implements ROMItemClicked
{
    private static final int MSG_CFG_LOADED = -1;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        m_shell_handler = new ShellHandler(this);
        requestRootCheck();
        
        Switch sw = (Switch)findViewById(R.id.enable_auto_boot);
        sw.setOnCheckedChangeListener(new OnAutoBootChecked());

         loadConfig();
    }
    
    @Override
    public void onStop()
    {
        super.onStop();
        
        if(m_config == null || m_roms_list == null)
            return;

        Switch sw = (Switch)findViewById(R.id.enable_auto_boot);
        if(!sw.isChecked())        
            m_config.auto_boot_seconds = -1;
        
        Spinner s = (Spinner)findViewById(R.id.auto_rom_spinner);
        m_config.auto_boot_rom = m_roms_list.get(s.getSelectedItemPosition());
        MultiROM.saveConfig(m_config);
    }
    
    private void requestRootCheck() {
        
        ProgressDialog d = new ProgressDialog(this);
        d.setMessage(getResources().getString(R.string.checking_root));
        d.show();
        
        m_shell_handler.setProgressDialog(d);
        Shell.requestRootCheck(m_shell_handler);
    }

    public void exitNoRoot()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.root_failed)
               .setTitle(R.string.error)
               .setNeutralButton(R.string.exit, new ExitClickedListener());

        AlertDialog d = builder.create();
        d.show();
    }
    
    private class ExitClickedListener implements OnClickListener  {
        @Override
        public void onClick(DialogInterface in, int arg1) {
            finish();
        }
    }
    
    public void loadRoms() {
        File folder = MultiROM.getRomsFolder();
        if(folder == null)
            return;

        LinearLayout l = (LinearLayout)findViewById(R.id.rom_list);
        l.removeAllViews();

        List<String> roms = new ArrayList<String>();
        File[] list = folder.listFiles();
        for(File f : list) {
            if(!f.isDirectory() || f.getName().equals(MultiROM.INTERNAL_NAME))
                continue;
            roms.add(f.getName());
        }
        Collections.sort(roms);
        for(int i = 0; i < roms.size(); ++i) {
             ROMListItem listItem = new ROMListItem(this, null, roms.get(i));
             l.addView(listItem.getView());
        }

        roms.add(0, MultiROM.INTERNAL_NAME);
        m_roms_list = roms;

	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
		android.R.layout.simple_spinner_item, roms);
	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	
        Spinner s = (Spinner)findViewById(R.id.auto_rom_spinner);
        s.setAdapter(adapter);
        selectAutoBootROM(s);
    }
    
    private void selectAutoBootROM(Spinner s) {
        if(m_config == null || m_roms_list == null)
            return;
        
        int idx = m_roms_list.indexOf(m_config.auto_boot_rom);
        if(idx == -1)
            idx = 0;
        s.setSelection(idx);
    }
    
    @Override
    public void onRomEditClicked(String rom_name)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_rename, null))
               .setTitle(R.string.rename_rom)
               .setPositiveButton(R.string.rename, null)
               .setNegativeButton(R.string.cancel, null);
        AlertDialog d = builder.create();
        d.show();
        
        Button b = d.getButton(DialogInterface.BUTTON_POSITIVE);

        RenameClickedListener l = new RenameClickedListener(rom_name, d);
        b.setOnClickListener(l);
        
        EditText t = (EditText)d.findViewById(R.id.rom_name_edit);
        t.setText(rom_name);
    }
    
    @Override
    public void onRomEraseClicked(String rom_name)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        String text = getResources().getText(R.string.delete_ask).toString();
        builder.setTitle(R.string.delete_rom)
               .setMessage(String.format(text, rom_name))
               .setPositiveButton(R.string.delete, new DeleteClickedListener(rom_name))
               .setNegativeButton(R.string.cancel, null);
        AlertDialog d = builder.create();
        d.show();
    }
    
    private class RenameClickedListener implements android.view.View.OnClickListener  {
        private String m_orig_name;
        private AlertDialog m_dialog;
        public RenameClickedListener(String orig_name, AlertDialog d) {
            m_orig_name = orig_name;
            m_dialog = d;
        }

        @Override
        public void onClick(View v) {
            EditText t = (EditText)m_dialog.findViewById(R.id.rom_name_edit);
            
            if(!MultiROM.isROMNameOkay(t.getText().toString()))
            {
                TextView err = (TextView)m_dialog.findViewById(R.id.error_text);
                err.setText(R.string.invalid_name);
                err.setVisibility(View.VISIBLE);
                return;
            }
            
            m_dialog.dismiss();
            
            try {
                MultiROM.renameROM(m_orig_name, t.getText().toString(), m_shell_handler);
            }
            catch(Exception ex) {
                String text = getResources().getString(R.string.rename_fail);
                text = String.format(text, ex.getMessage());
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private class DeleteClickedListener implements OnClickListener {
        private String m_rom_name;
        public DeleteClickedListener(String rom_name) {
            m_rom_name = rom_name;
        }
        @Override
        public void onClick(DialogInterface i, int arg) {
            try {
                MultiROM.deleteROM(m_rom_name, m_shell_handler);
            }
            catch(Exception ex) {
                String text = getResources().getString(R.string.delete_fail);
                text = String.format(text, ex.getMessage());
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        }
    }

    static private class ShellHandler extends Handler {
        private volatile WeakReference<MainActivity> m_activity;
        private ProgressDialog m_progress_dialog;

        public ShellHandler(MainActivity activity) {
            m_activity = new WeakReference<MainActivity>(activity);
        }

        public void setProgressDialog(ProgressDialog d) {
            m_progress_dialog = d;
        }
        
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = m_activity.get();
            if(activity == null)
                return;
            
            switch(msg.what){
                case Shell.SHELL_MSG_ROOT_STATUS:
                {
                    if(m_progress_dialog != null)
                    {
                        m_progress_dialog.dismiss();
                        m_progress_dialog = null;
                    }
                 
                    if(msg.arg1 == 0)
                        activity.exitNoRoot();
                    else
                        activity.loadRoms();
                    break;
                }
                case Shell.SHELL_MSG_CMD_FINISHED:
                {
                    switch(msg.arg1) {
                        case MultiROM.CMD_RENAME_ROM:
                            Toast.makeText(activity, R.string.rename_ok, Toast.LENGTH_SHORT).show();
                            activity.loadRoms();
                            break;
                        case MultiROM.CMD_DELETE_ROM:
                            Toast.makeText(activity, R.string.delete_ok, Toast.LENGTH_SHORT).show();
                            activity.loadRoms();
                            break;
                    }
                    break;
                }
                case MSG_CFG_LOADED:
                {
                    MultiROMCfg cfg = (MultiROMCfg)msg.obj;
                    activity.setCfg(cfg);
                    
                    Switch sw = (Switch)activity.findViewById(R.id.enable_auto_boot);
                    sw.setChecked(cfg.auto_boot_seconds > 0);

                    TextView t = (TextView)activity.findViewById(R.id.seconds);
                    t.setText(cfg.auto_boot_seconds + "s");

                    Spinner s = (Spinner)activity.findViewById(R.id.auto_rom_spinner);
                    activity.selectAutoBootROM(s);
                    break;
                }
            }
        }
    }
    
    private void loadConfig() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MultiROMCfg cfg = MultiROM.loadCfg();
                if(cfg == null)
                    return;
                
                m_shell_handler.obtainMessage(MSG_CFG_LOADED, cfg).sendToTarget();
            }
        }).start();
    }
    
    public void setCfg(MultiROMCfg cfg) {
        m_config = cfg;
    }
    
    private class OnAutoBootChecked implements OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton c, boolean checked) {
            Button b = (Button)findViewById(R.id.seconds_add);
            b.setEnabled(checked);
            b = (Button)findViewById(R.id.seconds_rm);
            b.setEnabled(checked && m_config.auto_boot_seconds > 1);
            
            TextView t = (TextView)findViewById(R.id.seconds);
            t.setEnabled(checked);
            
            Spinner s = (Spinner)findViewById(R.id.auto_rom_spinner);
            s.setEnabled(checked);
        }
    }
    
    public void on_addSeconds_clicked(View v) {
        ++m_config.auto_boot_seconds;
        TextView t = (TextView)findViewById(R.id.seconds);
        t.setText(m_config.auto_boot_seconds + "s");  
        
        Button b = (Button)findViewById(R.id.seconds_rm);
        b.setEnabled(m_config.auto_boot_seconds > 1);
    }
    
    public void on_rmSeconds_clicked(View v) {
        --m_config.auto_boot_seconds;
        TextView t = (TextView)findViewById(R.id.seconds);
        t.setText(m_config.auto_boot_seconds + "s");  
        
        v.setEnabled(m_config.auto_boot_seconds > 1);
    }
    
    private ShellHandler m_shell_handler;
    private MultiROMCfg m_config;
    private List<String> m_roms_list;
}
