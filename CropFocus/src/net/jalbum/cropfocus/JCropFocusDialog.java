//  Copyright 2009 David Ekholm, Jalbum AB
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.package net.jalbum.jprint;
package net.jalbum.cropfocus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import se.datadosen.jalbum.AlbumBean;
import se.datadosen.jalbum.AlbumObject;
import se.datadosen.jalbum.PluginContext;
import javax.swing.JRadioButton;
import javax.swing.SwingWorker;
import se.datadosen.component.CenterLayout;
import se.datadosen.component.ControlPanel;
import se.datadosen.component.JProgressSpinner;
import se.datadosen.component.JSmartDialog;
import se.datadosen.component.StateMonitor;
import se.datadosen.jalbum.AlbumObjectProperties;
import se.datadosen.jalbum.Msg;
import se.datadosen.util.Debug;

/**
 * Prints selected Jalbum images onto a local printer
 *
 * @author david
 */
public class JCropFocusDialog extends JSmartDialog {

    public static final int IMAGE_SIZE = 800;
//    public static final int BORDER_SIZE = 10;
    private static final Dimension imageBounds = new Dimension(IMAGE_SIZE, IMAGE_SIZE);

    private final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    private final PluginContext context;
    private AlbumObject ao;

    JCropFocusSelector display = new JCropFocusSelector();
    JProgressSpinner spinner = new JProgressSpinner(32);
    JRadioButton themeImage = new JRadioButton(Msg.getString(this, "ui.themeImage"), prefs.getBoolean("themeImage", true));
    JRadioButton square = new JRadioButton(Msg.getString(this, "ui.square"), prefs.getBoolean("square", false));
    JRadioButton thumbnailBounds = new JRadioButton(Msg.getString(this, "ui.thumbs"), prefs.getBoolean("thumbnailBounds", false));

    AlbumBean engine;

    private Action cancelAction = new AbstractAction("Cancel") {

        @Override
        public void actionPerformed(ActionEvent e) {
//            System.out.println("Closing");
            setVisible(false);
        }
    };

    private Action okAction = new AbstractAction("Ok") {

        @Override
        public void actionPerformed(ActionEvent e) {
            prefs.putBoolean("themeImage", themeImage.isSelected());
            prefs.putBoolean("square", square.isSelected());
            prefs.putBoolean("thumbnailBounds", thumbnailBounds.isSelected());
            prefs.putInt("displayWidth", display.getWidth());
            prefs.putInt("displayHeight", display.getHeight());

            // write the region as user variables
            AlbumObjectProperties aop = ao.getProperties();
            Map userVars = (Map) aop.get("userVariables", new LinkedHashMap());
            userVars.put("xWeight", "" + display.getxWeight());
            userVars.put("yWeight", "" + display.getyWeight());
            aop.put("userVariables", userVars);
            aop.save(false);
            setVisible(false);
        }
    };

    public JCropFocusDialog(PluginContext context, AlbumObject ao) {
        super(context.getJAlbumContext().getFrame(), Msg.getString(new Dummy(), "ui.setCropFocus"), false);
        this.context = context;
        this.ao = ao;
        init();
    }

    private void init() {
        engine = context.getJAlbumContext().getEngine();
        try {
            engine.init();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        registerActions(okAction, cancelAction);

        display.setLayout(new CenterLayout());
        display.add(spinner);
        display.setBackground(SystemColor.text);
        display.setImageBorder(BorderFactory.createLineBorder(new Color(96, 96, 96)));
        display.setPreferredSize(new Dimension(prefs.getInt("displayWidth", 500), prefs.getInt("displayHeight", 500)));
        spinner.workStarted();
        new SwingWorker<Image, Object>() {

            @Override
            protected Image doInBackground() throws Exception {
                return ao.loadImage(true, imageBounds);
            }

            @Override
            protected void done() {
                try {
                    spinner.workDone();
//                    display.remove(spinner); // Perhaps not needed, but nice
                    display.validate();
                    display.setBackgroundImage(get());
                    display.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } catch (InterruptedException ex) {
                } catch (ExecutionException ex) {
                    Debug.showErrorDialog(JCropFocusDialog.this, ex);
                }
            }
        }.execute();

        AlbumObjectProperties aop = ao.getProperties();
        Map userVars = aop.get("userVariables", new LinkedHashMap());
        try {
            display.setxWeight(Float.parseFloat((String) userVars.get("xWeight")));
        } catch (Exception ex) {
//                ex.printStackTrace(System.err);
        }
        try {
            display.setyWeight(Float.parseFloat((String) userVars.get("yWeight")));
        } catch (Exception ex) {
//                ex.printStackTrace(System.err);
        }

        ButtonGroup bg = new ButtonGroup();
        bg.add(themeImage);
        bg.add(square);
        bg.add(thumbnailBounds);

        new StateMonitor() {

            @Override
            public void onChange() {
                if (themeImage.isSelected()) {
                    engine.getSkinVariables().get("folderImageSize");
                    context.getJAlbumContext().getFrame().ui2Engine();
                    String folderImageSize = (String) engine.getSkinVariables().get("folderImageSize");
                    if (folderImageSize != null) {
                        Dimension d = parseSize(folderImageSize);
                        display.setRatio((float) d.width / d.height);
                    } else {
                        display.setRatio(3.75f);
                    }
                } else if (square.isSelected()) {
                    display.setRatio(1);
                } else {
                    context.getJAlbumContext().getFrame().ui2Engine();
                    Dimension d = parseSize(engine.getThumbSize());
                    display.setRatio((float) d.width / d.height);
                }
            }

        }.add(themeImage).add(square).add(thumbnailBounds).done();

        Container content = getContentPane();
        content.setLayout(new BorderLayout());

        // UI layout
        content.add(display, BorderLayout.CENTER);

        ControlPanel controls = new ControlPanel();
        controls.add(new JLabel(Msg.getString(this, "ui.sampleShape")));
        controls.add("tab", themeImage);
        controls.add("br", new JLabel(""));
        controls.add("tab", square);
        controls.add("br", new JLabel(""));
        controls.add("tab", thumbnailBounds);

        content.add(controls, BorderLayout.SOUTH);

//        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAction.actionPerformed(null);
            }
        });
        pack();
    }

    private static Dimension parseSize(String sizeString)
            throws IllegalArgumentException {
        String size = sizeString.toLowerCase();
        StringTokenizer tokens = new StringTokenizer(size, "x ");
        int w = 0;
        int h;

        try {
            w = Integer.parseInt(tokens.nextToken());
            h = Integer.parseInt(tokens.nextToken());

            if ((w < 0) || (h < 0)) {
                throw new Exception();
            }

            return new Dimension(w, h);
        } catch (Exception ex) {
            if (w != 0) {
                return new Dimension(w, w);
            }
            throw new IllegalArgumentException(Msg.get("engine.invalidSizeError", sizeString));
        }
    }
}
