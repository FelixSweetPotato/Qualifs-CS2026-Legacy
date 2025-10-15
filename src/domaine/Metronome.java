/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package domaine;

import java.awt.event.ActionEvent;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import javax.swing.Timer;

/**
 *
 * @author caron
 */
public class Metronome {

    private int bpm;
    private boolean estEnCours;
    transient private Synthesizer midiSynth;
    transient private javax.sound.midi.Instrument[] instr;
    transient private MidiChannel[] mChannels;
    private Timer timer;

    public Metronome() {
        bpm = 80;
        estEnCours = false;
        timer = new Timer((60000/bpm), (ActionEvent e) -> {
                mChannels[10].noteOn(60, 50);
                mChannels[10].noteOff(60);
            });
            timer.setRepeats(true);
            timer.start();
        try {

            midiSynth = MidiSystem.getSynthesizer();
            midiSynth.open();

            //get and load default instrument and channel lists
            instr = midiSynth.getDefaultSoundbank().getInstruments();
            mChannels = midiSynth.getChannels();

            mChannels[10].programChange(instr[116].getPatch().getProgram());

        } catch (MidiUnavailableException e) {
        }
    }

    public void toggle() {
        estEnCours = !estEnCours;
        if (estEnCours) {
            timer.start();
        }
        else{
            timer.stop();
        }
        
    }

    void setBpm(int nouveauBpm) {
        this.bpm = nouveauBpm;
        timer.setDelay(60000/bpm);
    }

}
