//  Copyright 2011 David Ekholm, Jalbum AB
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
// limitations under the License.package net.jalbum.editor;
package net.jalbum.cameraimporter;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import javax.swing.Action;
import javax.swing.event.HyperlinkEvent;
import se.datadosen.jalbum.JAlbumFrame;
import se.datadosen.jalbum.JAlbumPlugin;
import se.datadosen.jalbum.PluginContext;
import se.datadosen.util.Platform;
import edu.stanford.ejalbert.BrowserLauncher;
import java.awt.Color;
import javax.swing.ImageIcon;
import se.datadosen.component.JNotification;
import se.datadosen.jalbum.JHtmlBox;
import se.datadosen.jalbum.Msg;
import se.datadosen.util.MultilingualAction;

/**
 *
 * @author david
 */
public class CameraImporterPlugin implements JAlbumPlugin {

    private static final ImageIcon memoryCardIcon = new ImageIcon(CameraImporterPlugin.class.getResource("res/sd_card_32.png"));
    private PluginContext context;
    JAlbumFrame window;
    private Set<File> lastRoots = new HashSet<File>();
    private File firstCard;
    MultilingualAction importFromCamera = new MultilingualAction(Msg.getString(this, "ui.importFromCamera") + "...") {
        {
            putValue(Action.SMALL_ICON, new ImageIcon(this.getClass().getResource("res/camera.png")));
        }

        public void actionPerformed(ActionEvent e) {
            showMemoryCard(firstCard);
        }

        public void languageUpdated() {
//            System.out.println("Switching to " + Locale.getDefault());
            importFromCamera.putValue(Action.NAME, Msg.getString(this, "ui.importFromCamera") + "...");
        }
    };

    public void init(PluginContext context) {
        this.context = context;
        this.window = context.getJAlbumContext().getFrame();
        Msg.maintainTranslated(this);

        context.addImportAction(importFromCamera);

        Thread monitor = new Thread("File system monitor") {

            @Override
            public void run() {
                try {
                    while (true) {
                        sleep(3000);
                        checkForMemoryCard();
                    }
                } catch (InterruptedException ex) {
                }
            }
        };
        monitor.setDaemon(true);
        monitor.start();
    }

    public boolean onExit() {
        return true;
    }

    private void showMemoryCard(File cardFolder) {
        if (cardFolder != null) {
            try {
                File firstFolder = null;
                boolean manyFolders = false;
                for (File f : cardFolder.listFiles()) {
                    if (f.isDirectory()) {
                        if (firstFolder == null) {
                            firstFolder = f;
                        } else {
                            manyFolders = true;
                        }
                    }
                }
                BrowserLauncher.openLocal(!manyFolders && firstFolder != null ? firstFolder : cardFolder);
            } catch (IOException ex) {
            }
        }
    }

    private static File[] stripInvalidRoots(File[] roots) {
        ArrayList<File> stripped = new ArrayList<File>();
        for (File root : roots) {
            String name = root.getAbsolutePath();
            if (!name.startsWith("C:") && !name.startsWith("A:") && !name.startsWith("B:")) {
                stripped.add(root);
            }
        }
        return stripped.toArray(new File[stripped.size()]);
    }

    private void checkForMemoryCard() {
        if (Platform.isWindows()) {
            checkForMemoryCard(stripInvalidRoots(File.listRoots()));
        } else if (Platform.isMac()) {
            checkForMemoryCard(new File("/Volumes").listFiles());
        } else if (Platform.isLinux()) {
            checkForMemoryCard(new File("/media").listFiles());
        }
    }

    private File firstMemoryCard(File[] roots) {
        for (File root : roots) {
            if (isMemoryCard(root)) {
                return new File(root, "DCIM");
            }
        }
        return null;
    }

    private synchronized void checkForMemoryCard(File[] roots) {
        if (roots == null) {
            return;
        }
        firstCard = firstMemoryCard(roots);
        importFromCamera.setEnabled(firstCard != null);
        for (File root : roots) {
            if (!lastRoots.contains(root)) {
                if (isMemoryCard(root)) {
                    importFromCamera.setEnabled(true);
                    memoryCardDetected(root);
                }
            }
        }
        lastRoots.clear();
        lastRoots.addAll(Arrays.asList(roots));
    }

    private boolean isMemoryCard(File root) {
        return new File(root, "DCIM").exists();
    }

    private void memoryCardDetected(final File root) {
        JHtmlBox box = new JHtmlBox(Msg.getString(this, "ui.memoryCardDetected", "http://www.dummy.com")) {

            @Override
            protected void linkActivated(HyperlinkEvent e) {
                showMemoryCard(new File(root, "DCIM"));
            }
        };
        JNotification n = new JNotification(box);
        n.setIcon(memoryCardIcon);
        n.setExpiration(8);
        n.setBackground(new Color(200, 200, 255));
        n.setBackgroundGradient(true);
        window.showNotification(n);
    }

    static class FileSystemMonitor extends Thread {

        interface FileSystemListener {

            void rootAdded(File root);
        }

        public FileSystemMonitor() {
            super("File system monitor");
            setDaemon(true);
        }
    }
}
