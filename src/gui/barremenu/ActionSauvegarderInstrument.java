/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.barremenu;

import domaine.Controleur;
import domaine.Touche;
import gui.InterfacePrincipale;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

/**
 *
 */
public class ActionSauvegarderInstrument extends AbstractAction {

    private Controleur controleur;
    private InterfacePrincipale interfacePrincipale;

    public ActionSauvegarderInstrument(InterfacePrincipale interfacePrinc) {
        this.controleur = interfacePrinc.controleur;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser choixFichier = new JFileChooser();
        choixFichier.setCurrentDirectory(null);
        int actionUtilisateur = choixFichier.showOpenDialog(interfacePrincipale);
        if (actionUtilisateur == JFileChooser.APPROVE_OPTION) {
            File fichier = choixFichier.getSelectedFile();
            this.controleur.sauvegarderInstrument(fichier);
        }
    }

}
