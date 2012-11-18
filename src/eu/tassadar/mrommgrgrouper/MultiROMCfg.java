/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.tassadar.mrommgrgrouper;


public class MultiROMCfg {
    public MultiROMCfg() {
        is_second_boot = 0;
        current_rom = MultiROM.INTERNAL_NAME;
        auto_boot_seconds = 5;
        auto_boot_rom = MultiROM.INTERNAL_NAME;
    }

    public int is_second_boot;
    public String current_rom;
    public int auto_boot_seconds;
    public String auto_boot_rom;
}
