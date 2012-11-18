/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.tassadar.mrommgrgrouper;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ROMListItem {
    public interface ROMItemClicked {
        void onRomEditClicked(String rom_name);
        void onRomEraseClicked(String rom_name);
    }
    
    public ROMListItem(MainActivity activity, ViewGroup parent, String rom_name) {
        m_view = View.inflate(activity, R.layout.rom_item, parent);
        
        m_listener = activity;
        m_rom_name = rom_name;
        
        Button b = (Button)m_view.findViewById(R.id.rename_rom);
        b.setOnClickListener(new OnEditClicked());
        b = (Button)m_view.findViewById(R.id.delete_rom);
        b.setOnClickListener(new OnEraseClicked());
        
        TextView t = (TextView)m_view.findViewById(R.id.rom_name);
        t.setText(m_rom_name);
    }
    
    public View getView() {
        return m_view;
    }
    
    private class OnEditClicked implements OnClickListener {
        @Override
        public void onClick(View v) {
            m_listener.onRomEditClicked(m_rom_name);
        }
    }
    
    private class OnEraseClicked implements OnClickListener {
        @Override
        public void onClick(View v) {
            m_listener.onRomEraseClicked(m_rom_name);
        }
    }
    
    private View m_view;
    private String m_rom_name;
    private ROMItemClicked m_listener;
}
