/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package domaine;

import gui.InterfacePrincipale;
import gui.OptionsGUI;
import gui.ProprietesSon;
import gui.ProprietesTouche;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.io.*;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import javax.sound.midi.InvalidMidiDataException;
import java.util.Timer;
import java.util.Vector;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Sequencer;

/**

 */
public class Controleur implements Observable {

    private Instrument instrument;
    private List<Observateur> observateurs;
    private Touche derniereToucheJouee;
    private Mode modeEnCours;
    private Boolean affichageNom;
    private Boolean affichagePersistance;
    private Boolean affichageTimbre;
    private StrategieGabarit gabarit;
    private Metronome metronome;
    private StrategieRecherche strategieRecherche;
    private Vector<Triplet> listeBoucle = new Vector<Triplet>();
    private ArrayList<Timer> listTimer = new ArrayList<Timer>(9);
    private boolean boucleEstActive = false;
    private long startTime;
    private Composition composition;

    public Controleur() {
        instrument = new Instrument(this);
        observateurs = new LinkedList<Observateur>();
        modeEnCours = Mode.SELECTION;
        affichageNom = false;
        affichagePersistance = false;
        affichageTimbre = false;
        metronome = new Metronome();
        boucleEstActive = false;
        for (int i = 0; i < 10; i++) {
            Timer timer = new Timer(true);
            listTimer.add(timer);
        }
    }

//    public void setVolume(int volume){
//        MidiChannel[] channels = instrument.getChannels();
//        for (int i = 0; i < channels.length; ++i){
//            channels[i].controlChange(7, volume);
//        }
//    }
    public void setMode(Mode nouveauMode) {
        this.modeEnCours = nouveauMode;
    }

    public Mode getMode() {
        return this.modeEnCours;
    }

    public void creerTouche(Point point, OptionsGUI options, ProprietesSon propsSon, ProprietesTouche propsTouche, Dimension dimensionZoneJeu) {
        Coordonnee coordonneeCentreTouche = new Coordonnee(point.x, point.y, dimensionZoneJeu);
        double dimensionMin = (double) Integer.min(dimensionZoneJeu.height, dimensionZoneJeu.width);
        Son sonTouche = null;

        if (options.optionNote) {
            sonTouche = new Note(propsSon.nomNote, propsSon.octave, propsSon.persistance, propsSon.nomTimbre);
        } else if (options.optionFichierAudio) {
            sonTouche = new FichierAudio(propsSon.fichierAudio, this);
        }

        Forme formeTouche = null;
        if ("Rectangle".equals(propsTouche.formeTouche)) {
            formeTouche = genererRectangle(propsTouche, options, dimensionMin);
        } else if ("Cercle".equals(propsTouche.formeTouche)) {
            formeTouche = genererCercle(propsTouche, options, dimensionMin);
        }

        if (sonTouche != null && formeTouche != null) {
            Touche nouvelleTouche = new Touche(sonTouche, formeTouche, coordonneeCentreTouche);
            instrument.ajouterTouche(nouvelleTouche);
            informerObservateurChangementJeu();
        }
    }

    private Rectangle genererRectangle(ProprietesTouche propsTouche, OptionsGUI options, double dimensionMin) {
        double bordureCentre = propsTouche.bordureCentre / dimensionMin;
        double hauteurTouche = propsTouche.hauteurTouche / dimensionMin;
        double largeurTouche = propsTouche.largeurTouche / dimensionMin;
        double bordureHaut = propsTouche.bordureHaut / dimensionMin;
        double bordureBas = propsTouche.bordureBas / dimensionMin;
        double bordureGauche = propsTouche.bordureGauche / dimensionMin;
        double bordureDroite = propsTouche.bordureDroite / dimensionMin;
        Rectangle rectangleTouche = null;
        if (options.optionCouleurTouche) {
            rectangleTouche = new Rectangle(propsTouche.couleurFondTouche, hauteurTouche,
                    largeurTouche, bordureDroite, bordureHaut, bordureBas, bordureGauche, bordureCentre, propsTouche.couleurBordureHaut,
                    propsTouche.couleurBordureBas, propsTouche.couleurBordureDroite, propsTouche.couleurBordureGauche, propsTouche.couleurBordureCentre);
        } else if (options.optionImageTouche) {
            rectangleTouche = new Rectangle(propsTouche.imageFondTouche, hauteurTouche,
                    largeurTouche, bordureDroite, bordureHaut, bordureBas, bordureGauche, bordureCentre, propsTouche.couleurBordureHaut,
                    propsTouche.couleurBordureBas, propsTouche.couleurBordureDroite, propsTouche.couleurBordureGauche, propsTouche.couleurBordureCentre);
        }
        return rectangleTouche;
    }

    private Cercle genererCercle(ProprietesTouche propsTouche, OptionsGUI options, double dimensionMin) {
        Cercle cercleTouche = null;
        double rayonTouche = propsTouche.rayonTouche / dimensionMin;
        double bordureCercle = propsTouche.bordureCercle / dimensionMin;
        double bordureCentre = propsTouche.bordureCentre / dimensionMin;
        if (options.optionCouleurTouche) {
            cercleTouche = new Cercle(propsTouche.couleurFondTouche, propsTouche.couleurBordureCercle, rayonTouche, bordureCercle, bordureCentre, propsTouche.couleurBordureCentre);
        } else if (options.optionImageTouche) {
            cercleTouche = new Cercle(propsTouche.imageFondTouche, propsTouche.couleurBordureCercle, rayonTouche, bordureCercle, bordureCentre, propsTouche.couleurBordureCentre);
        }
        return cercleTouche;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    @Override
    public void informerObservateurChangementJeu() {
        for (Observateur observateur : observateurs) {
            observateur.changementJeu();
        }
    }

    @Override
    public void ajouterObservateur(Observateur observateur) {
        observateurs.add(observateur);
    }

    @Override
    public void enleverObservateur(Observateur observateur) {
        observateurs.remove(observateur);
    }

    private Touche toucheClicked(Point pointSouris, Dimension dimensionZoneJeu) {
        double x = pointSouris.getX();
        double y = pointSouris.getY();
        double dimMin = (double) Integer.min(dimensionZoneJeu.width, dimensionZoneJeu.height);

        ArrayList<Touche> arrayTouche = instrument.getListeTouches();
        for (int i = arrayTouche.size() - 1; i >= 0; i--) {
            Touche touche = arrayTouche.get(i);
            Forme forme = touche.getForme();
            double xTouche = touche.getCoordonnee().getRelativeX() * dimensionZoneJeu.width;
            double yTouche = touche.getCoordonnee().getRelativeY() * dimensionZoneJeu.height;
            if (forme instanceof Cercle) {
                Cercle cercleTouche = (Cercle) forme;

                double rayon = cercleTouche.getRayon() * dimMin;
                double rayonBordure = (cercleTouche.getBordure() * dimMin) + rayon;

                double pythagoreX = x - xTouche;
                double pythagoreY = y - yTouche;
                double distanceCentre = sqrt(pow(pythagoreX, 2) + pow(pythagoreY, 2));

                if (distanceCentre <= rayonBordure) {
                    return touche;
                }
            } else if (forme instanceof Rectangle) {
                Rectangle rectangleTouche = (Rectangle) forme;
                double hauteur = rectangleTouche.getHauteur();
                double largeur = rectangleTouche.getLargeur();

                double hauteurBordure = (hauteur + rectangleTouche.getBordureBas() + rectangleTouche.getBordureHaut()) * dimMin;
                double largeurBordure = (largeur + rectangleTouche.getBordureDroite() + rectangleTouche.getBordureGauche()) * dimMin;

                int x1 = (int) (xTouche - largeurBordure / 2);
                int x2 = (int) (xTouche + largeurBordure / 2);
                int y1 = (int) (yTouche - hauteurBordure / 2);
                int y2 = (int) (yTouche + hauteurBordure / 2);

                if (x >= x1 && x <= x2) {
                    if (y >= y1 && y <= y2) {
                        return touche;
                    }
                }
            }
        }
        return null;
    }

    //enregistrement des notes à boucler
    public void enregistrementBouclage() {
        this.startTime = System.currentTimeMillis();
        this.boucleEstActive = true;

    }

    //Débute le bouclage son
    public void startBouclage(int bouton) {
        this.boucleEstActive = false;
        if (listeBoucle != null) {
            for (Triplet it : listeBoucle) {
                TimerTaskBoucle timerTask = new TimerTaskBoucle();
                timerTask.setTriplet(it, instrument);
                Triplet period = listeBoucle.lastElement();
                listTimer.get(bouton).scheduleAtFixedRate(timerTask, it.getSecond(), period.getSecond());
            }
            listeBoucle.clear();
        }
    }

    //Stop le boulacge + supprime la listeBouclage
    public void stopBouclage(int bouton) {

        listTimer.get(bouton).cancel();
        listTimer.set(bouton, new Timer());

    }

    public void jouerTouche(Point pointSouris, Dimension dimensionZoneJeu) {
        derniereToucheJouee = toucheClicked(pointSouris, dimensionZoneJeu);
        if (derniereToucheJouee != null) {
            Son son = derniereToucheJouee.getSon();
            if (son.getEnCours()) {
                addListeBoucleStop();
                instrument.stopSon(son); //si elle est deja en cours...arreter la touche ou le timer (est-ce que plusieurs notes peuvent jouer en meme temps)?
            }
            addListeBoucleJouer();
            instrument.jouerSon(son);
        }
    }

    public void arretTouche() {
        if (derniereToucheJouee != null) {
            Son son = derniereToucheJouee.getSon();
            if (son instanceof Note) {
                Note note = (Note) son;
                if (note.getEnCours()) {
                    addListeBoucleStop();
                    instrument.stopApresPersistance(note);
                }
            }
        }
    }

    public void supprimerTouche() {
        Touche toucheSelectionnee = getToucheSelectionne();
        if (toucheSelectionnee != null) {
            instrument.deleteTouche(toucheSelectionnee);
            informerObservateurChangementJeu();
        }
    }

    public void toucheSelectionnee(Point pointSouris, Dimension dimensionZoneJeu) {
        Touche touche = toucheClicked(pointSouris, dimensionZoneJeu);
        if (touche != null) {
            touche.switchEstSelection();
        }
        informerObservateurChangementJeu();
    }

    public Touche getToucheSelectionne() {
        for (Touche touche : instrument.getListeTouches()) {
            if (touche.getEstSélectionnee()) {
                return touche;
            }
        }
        return null;
    }

    public void majPositionTouche(Point pointSouris, Dimension dimensionZoneJeu) {
        instrument.majToucheSelectionnee(pointSouris, dimensionZoneJeu);
        informerObservateurChangementJeu();
    }

    public void switchEstSelectionneeTouches() {
        instrument.switchEstSelectionneeListTouche();
        informerObservateurChangementJeu();
    }

    public void modifierToucheSelectionnee(OptionsGUI options, ProprietesSon propsSon, ProprietesTouche propsTouche, Dimension dimensionZoneJeu) {
        Touche toucheSelectionnee = getToucheSelectionne();
        double dimensionMin = (double) Integer.min(dimensionZoneJeu.height, dimensionZoneJeu.width);

        if (toucheSelectionnee != null) {

            Son sonTouche = null;

            if (options.optionNote) {
                sonTouche = new Note(propsSon.nomNote, propsSon.octave, propsSon.persistance, propsSon.nomTimbre);
            } else if (options.optionFichierAudio) {
                sonTouche = new FichierAudio(propsSon.fichierAudio, this);
            }

            Forme formeTouche = null;
            if ("Rectangle".equals(propsTouche.formeTouche)) {
                formeTouche = genererRectangle(propsTouche, options, dimensionMin);

            } else if ("Cercle".equals(propsTouche.formeTouche)) {
                formeTouche = genererCercle(propsTouche, options, dimensionMin);
            }
            if (sonTouche != null && formeTouche != null) {
                toucheSelectionnee.setSon(sonTouche);
                toucheSelectionnee.setForme(formeTouche);
                informerObservateurChangementJeu();
            }
        }
    }

    public void sauvegarderInstrument(File fichier) {
        // todo

        try (PrintWriter writer = new PrintWriter(new FileWriter(fichier.getPath()))) {
            //on sauvegarder l'important
            //Soyez gentil

            writer.write(instrument.getNomInstrument());

            String touches ="";

            for (Touche touche : instrument.getListeTouches()){
                touches+=touche.getSon().getNom()+ ",";
            }
            writer.write(touches);
        }catch(Exception ex){

        }
    }
//⡴⠒⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣼⠉⠳⡆⠀
//        ⣇⠰⠉⢙⡄⠀⠀⣴⠖⢦⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⣆⠁⠙⡆
//        ⠘⡇⢠⠞⠉⠙⣾⠃⢀⡼⠀⠀⠀⠀⠀⠀⠀⢀⣼⡀⠄⢷⣄⣀⠀⠀⠀⠀⠀⠀⠀⠰⠒⠲⡄⠀⣏⣆⣀⡍
//        ⠀⢠⡏⠀⡤⠒⠃⠀⡜⠀⠀⠀⠀⠀⢀⣴⠾⠛⡁⠀⠀⢀⣈⡉⠙⠳⣤⡀⠀⠀⠀⠘⣆⠀⣇⡼⢋⠀⠀⢱
//        ⠀⠘⣇⠀⠀⠀⠀⠀⡇⠀⠀⠀⠀⡴⢋⡣⠊⡩⠋⠀⠀⠀⠣⡉⠲⣄⠀⠙⢆⠀⠀⠀⣸⠀⢉⠀⢀⠿⠀⢸
//        ⠀⠀⠸⡄⠀⠈⢳⣄⡇⠀⠀⢀⡞⠀⠈⠀⢀⣴⣾⣿⣿⣿⣿⣦⡀⠀⠀⠀⠈⢧⠀⠀⢳⣰⠁⠀⠀⠀⣠⠃
//        ⠀⠀⠀⠘⢄⣀⣸⠃⠀⠀⠀⡸⠀⠀⠀⢠⣿⣿⣿⣿⣿⣿⣿⣿⣿⣆⠀⠀⠀⠈⣇⠀⠀⠙⢄⣀⠤⠚⠁⠀
//        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡇⠀⠀⢠⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡄⠀⠀⠀⢹⠀⠀⠀⠀⠀⠀⠀⠀⠀
//        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡀⠀⠀⢘⠀⠀⠀⠀⠀⠀⠀⠀⠀
//        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡇⠀⢰⣿⣿⣿⡿⠛⠁⠀⠉⠛⢿⣿⣿⣿⣧⠀⠀⣼⠀⠀⠀⠀⠀⠀⠀⠀⠀
//        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⡀⣸⣿⣿⠟⠀⠀⠀⠀⠀⠀⠀⢻⣿⣿⣿⡀⢀⠇⠀⠀⠀⠀⠀⠀⠀⠀⠀
//        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⡇⠹⠿⠋⠀⠀⠀⠀⠀⠀⠀⠀⠀⠙⢿⡿⠁⡏⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
//        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠻⣤⣞⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢢⣀⣠⠇⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
//        ⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠙⠲⢤⣀⣀⠀⢀⣀⣀⠤⠒⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀

    public void chargerInstrument(File fichier) {
        // todo
        if(fichier.canRead()){
            try {
                BufferedReader bufferreader = new BufferedReader(new FileReader(fichier.getPath()));
                String instrumentName= bufferreader.readLine();
                String notesNom = String.format(bufferreader.readLine(),",");

                //pas eu le temps de finir en faite ....
                /*
                * ⡴⠒⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣼⠉⠳⡆⠀
⣇⠰⠉⢙⡄⠀⠀⣴⠖⢦⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⣆⠁⠙⡆
⠘⡇⢠⠞⠉⠙⣾⠃⢀⡼⠀⠀⠀⠀⠀⠀⠀⢀⣼⡀⠄⢷⣄⣀⠀⠀⠀⠀⠀⠀⠀⠰⠒⠲⡄⠀⣏⣆⣀⡍
⠀⢠⡏⠀⡤⠒⠃⠀⡜⠀⠀⠀⠀⠀⢀⣴⠾⠛⡁⠀⠀⢀⣈⡉⠙⠳⣤⡀⠀⠀⠀⠘⣆⠀⣇⡼⢋⠀⠀⢱
⠀⠘⣇⠀⠀⠀⠀⠀⡇⠀⠀⠀⠀⡴⢋⡣⠊⡩⠋⠀⠀⠀⠣⡉⠲⣄⠀⠙⢆⠀⠀⠀⣸⠀⢉⠀⢀⠿⠀⢸
⠀⠀⠸⡄⠀⠈⢳⣄⡇⠀⠀⢀⡞⠀⠈⠀⢀⣴⣾⣿⣿⣿⣿⣦⡀⠀⠀⠀⠈⢧⠀⠀⢳⣰⠁⠀⠀⠀⣠⠃
⠀⠀⠀⠘⢄⣀⣸⠃⠀⠀⠀⡸⠀⠀⠀⢠⣿⣿⣿⣿⣿⣿⣿⣿⣿⣆⠀⠀⠀⠈⣇⠀⠀⠙⢄⣀⠤⠚⠁⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡇⠀⠀⢠⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡄⠀⠀⠀⢹⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡀⠀⠀⢘⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡇⠀⢰⣿⣿⣿⡿⠛⠁⠀⠉⠛⢿⣿⣿⣿⣧⠀⠀⣼⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⡀⣸⣿⣿⠟⠀⠀⠀⠀⠀⠀⠀⢻⣿⣿⣿⡀⢀⠇⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⡇⠹⠿⠋⠀⠀⠀⠀⠀⠀⠀⠀⠀⠙⢿⡿⠁⡏⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠻⣤⣞⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢢⣀⣠⠇⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠙⠲⢤⣀⣀⠀⢀⣀⣀⠤⠒⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀
                * */



            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void renommerInstrument(String nouveauNom) {
        instrument.setNomInstrument(nouveauNom);
    }

    public String getNomInstrument() {
        return instrument.getNomInstrument();
    }

    public void setImageFondInstrument(Image imageFond) {
        instrument.setImageFond(imageFond);
        informerObservateurChangementJeu();
    }

    public void switchAffichageNom() {
        affichageNom = !affichageNom;
        informerObservateurChangementJeu();
    }

    public void switchAffichagePersistance() {
        affichagePersistance = !affichagePersistance;
        informerObservateurChangementJeu();
    }

    public Boolean getAffichageNom() {
        return affichageNom;
    }

    public Boolean getAffichagePersistance() {
        return affichagePersistance;
    }

    public Boolean getAffichageTimbre() {
        return affichageTimbre;
    }

    public void genererGuitare() throws InvalidMidiDataException {
        this.gabarit = new GabaritGuitare();
        stopPieceMusicale();
        this.instrument = gabarit.genererInstrument(this);
        resetSequencePiece();
        informerObservateurChangementJeu();
    }

    public void toggleMetronome() {
        metronome.toggle();
    }

    public void setBpm(int nouveauBpm) {
        metronome.setBpm(nouveauBpm);
    }

    public void reinitialiserInstrument() {
        this.instrument = new Instrument(this);
        informerObservateurChangementJeu();

    }

    public void recherche(String rechercheChaine) {
        String[] mots = rechercheChaine.trim().split("\\s+");
        ArrayList<Touche> touchesInstrument = instrument.getListeTouches();
        for (Touche touche : touchesInstrument) {
            touche.setEstRecherchee(false);
        }
        ArrayList<Touche> touchesTrouvees = new ArrayList<Touche>();
        for (String mot : mots) {
            int longueurMot = mot.length();
            if (longueurMot <= 2) {
                strategieRecherche = new RechercheNote();
                touchesTrouvees.addAll(strategieRecherche.rechercherTouches(mot, touchesInstrument));

                if (longueurMot > 0 && Character.isDigit(mot.charAt(0))) {
                    if (longueurMot == 2 && Character.isDigit(mot.charAt(1))) {
                        strategieRecherche = new RechercheOctave();
                        touchesTrouvees.addAll(strategieRecherche.rechercherTouches(mot, touchesInstrument));
                    } else if (longueurMot == 1) {
                        strategieRecherche = new RechercheOctave();
                        touchesTrouvees.addAll(strategieRecherche.rechercherTouches(mot, touchesInstrument));
                    }
                }
            }

            if (longueurMot >= 2 || longueurMot <= 3) {
                if (longueurMot > 0 && Character.isLetter(mot.charAt(0))) {
                    if (longueurMot == 3 && Character.isDigit(mot.charAt(2))) {
                        strategieRecherche = new RechercheNom();
                        touchesTrouvees.addAll(strategieRecherche.rechercherTouches(mot, touchesInstrument));
                    } else if (longueurMot == 2 && Character.isDigit(mot.charAt(1))) {
                        strategieRecherche = new RechercheNom();
                        touchesTrouvees.addAll(strategieRecherche.rechercherTouches(mot, touchesInstrument));
                    }
                }
            }

            if (longueurMot >= 5) {
                strategieRecherche = new RechercheTimbre();
                touchesTrouvees.addAll(strategieRecherche.rechercherTouches(mot, touchesInstrument));
                strategieRecherche = new RechercheForme();
                touchesTrouvees.addAll(strategieRecherche.rechercherTouches(mot, touchesInstrument));
            }

            //Ajouter d'autres algos de recherche ici : ajouter les touches retournees de la strategie a toucheTrouvees
        }
        for (Touche touche : touchesTrouvees) {
            touche.setEstRecherchee(true);
        }
        informerObservateurChangementJeu();
    }

    public void parsePieceMusicale(File fichier, InterfacePrincipale interfacePrincipale) throws FileNotFoundException, InvalidMidiDataException {
        composition = new Composition(fichier);
        if (composition.getBpm() != 0) { // composition valide
            instrument.creerSequenceMidi(composition);

            Scanner scanner = new Scanner(new BufferedReader(new FileReader(fichier))); // Afficher le texte de la chanson dans le UI
            while (scanner.hasNext()) {
                String ligne = scanner.nextLine();
                if (ligne.contains(Integer.toString(composition.getBpm()))) {
                    break; // Se rendre dans le fichier apres les BPM
                }
            }
            int numeroLigne = 1;
            boolean lignesTrouvees = false;
            ArrayList<String> lignes = new ArrayList<String>();
            lignes.add(0, "");
            while (scanner.hasNext()) {
                String ligne = scanner.nextLine();
                if (!ligne.isEmpty()) {
                    if (ligne.startsWith("//")) {
                        lignes.set(0, lignes.get(0) + ligne);
                    } else {
                        if (!lignesTrouvees) {
                            lignes.add(numeroLigne, "");
                            lignes.set(numeroLigne, lignes.get(numeroLigne) + ligne);
                        } else {
//                        if (numeroLigne == 0) {
//                            int espaces = lignes.get(lignes.size() - 1).length() - lignes.get(numeroLigne).length();
//                            for (int i = 0; i != espaces; i++) {
//                                lignes.set(numeroLigne, lignes.get(numeroLigne) + " ");
//                            }
//                        } 
//pour ajouter des espaces pour alligner les paroles..

                            lignes.set(numeroLigne, lignes.get(numeroLigne) + ligne);
                        }
                        ++numeroLigne;
                    }
                } else {
                    if (numeroLigne != 1) {
                        lignesTrouvees = true;
                    }
                    numeroLigne = 1;
                }

                String textePiece = "";
                for (int i = 0; i != lignes.size(); i++) {
                    textePiece += lignes.get(i) + "\n";
                }
                interfacePrincipale.setTextePiece(textePiece);

            }
        }
    }

    public void genererPiano() throws InvalidMidiDataException {
        this.gabarit = new GabaritPiano();
        stopPieceMusicale();
        this.instrument = gabarit.genererInstrument(this);
        resetSequencePiece();
        informerObservateurChangementJeu();
    }

    public void addListeBoucleJouer() {
        if (boucleEstActive) {
            long delay = System.currentTimeMillis() - startTime;
            Triplet triplet = new Triplet(derniereToucheJouee, delay, false);
            this.listeBoucle.add(triplet);
        }
    }

    public void addListeBoucleStop() {
        if (boucleEstActive) {
            long delay = System.currentTimeMillis() - startTime;
            Triplet triplet = new Triplet(derniereToucheJouee, delay, true);
            this.listeBoucle.add(triplet);
        }
    }

    public void startPieceMusicale() {
        Sequencer sequenceur = instrument.getSequenceur();
        if (sequenceur.getSequence() != null) {
            sequenceur.start();
            if (sequenceur.getMicrosecondPosition() == sequenceur.getSequence().getMicrosecondLength()) {
                sequenceur.setMicrosecondPosition(0);
            }
            sequenceur.setTempoInBPM(composition.getBpm());
        }
    }

    public void stopPieceMusicale() {
        if (instrument.getSequenceur().isRunning()) {
            instrument.getSequenceur().stop();
        }
    }

    public String getPositionPieceEnMinutes() {
        Sequencer seq = instrument.getSequenceur();
        seq.setTempoInBPM(composition.getBpm());

        // int tempsPieceSecondes = (int)(seq.getTickLength() * (60000/(seq.getTempoInBPM() * 24)));
        int progresSecondes = (int) (seq.getTickPosition() * (60000 / (seq.getTempoInBPM() * 24000)));
        int progresMinutes = progresSecondes / 60;
        return String.format("%d:%02d", progresMinutes, progresSecondes % 60);
    }

    public int getDureePiece() {
        Sequencer seq = instrument.getSequenceur();
        int tempsPieceSecondes = (int) (seq.getTickLength() * (60000 / (seq.getTempoInBPM() * 24)));
        return tempsPieceSecondes;
    }

    public int getPositionPiece() {
        Sequencer seq = instrument.getSequenceur();
        int progresSecondes = (int) (seq.getTickPosition() * (60000 / (seq.getTempoInBPM() * 24000)));
        return progresSecondes;
    }

    public void resetSequencePiece() throws InvalidMidiDataException {
        if (!(composition == null) && composition.getBpm() != 0) {
            instrument.creerSequenceMidi(composition);
        }
    }
}
