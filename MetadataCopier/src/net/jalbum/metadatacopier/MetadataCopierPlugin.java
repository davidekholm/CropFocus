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
// limitations under the License.package net.jalbum.editor;
package net.jalbum.metadatacopier;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import se.datadosen.jalbum.AlbumObject;
import se.datadosen.jalbum.Config;
import se.datadosen.jalbum.Config.LabelType;
import se.datadosen.jalbum.JAlbumPlugin;
import se.datadosen.jalbum.Msg;
import se.datadosen.jalbum.PluginContext;
import se.datadosen.util.StringUtil;

/**
 * Plugin that can copy and paste captions, titles and keywords to and from the clipboard By selecting several album
 * objects, metadata can be pasted to multiple album objects
 *
 * @author david
 */
public class MetadataCopierPlugin implements JAlbumPlugin, FlavorListener, ClipboardOwner {

    private PluginContext context;
    private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    private JMenu menu = new JMenu();

    Action copyAction = new AbstractAction(Msg.get("edit.copy")) {
        public void actionPerformed(ActionEvent e) {
            AlbumObject ao = context.getSelectedAlbumObjects()[0];
            String data;
            switch (Config.getConfig().getLabelType()) {
                case title:
                    data = ao.getTitle();
                    break;
                case keywords:
                    data = ao.getKeywords();
                    break;
                default:
                    data = ao.getComment();
            }
            clipboard.setContents(new StringSelection(data), MetadataCopierPlugin.this);
        }
    };

    Action pasteAction = new AbstractAction(Msg.get("edit.paste")) {
        public void actionPerformed(ActionEvent e) {
            try {
                String data = (String) clipboard.getData(DataFlavor.stringFlavor);
                for (AlbumObject ao : context.getSelectedAlbumObjects()) {
                    switch (Config.getConfig().getLabelType()) {
                        case title:
                            ao.setTitle(data);
                            break;
                        case keywords:
                            Set<String> keywords = StringUtil.stringToSet(ao.getKeywords());
                            Set<String> newKeywords = StringUtil.stringToSet(data);
                            keywords.addAll(newKeywords);
                            ao.setKeywords(StringUtil.setToString(keywords));
                            break;
                        default:
                            ao.setComment(data);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
    };
    
    Action clearAction = new AbstractAction(Msg.get("effects.clear")) {
        public void actionPerformed(ActionEvent e) {
            try {
                String data = "";
                for (AlbumObject ao : context.getSelectedAlbumObjects()) {
                    switch (Config.getConfig().getLabelType()) {
                        case title:
                            ao.setTitle(data);
                            break;
                        case keywords:
                            ao.setKeywords(data);
                            break;
                        default:
                            ao.setComment(data);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }
    };
    
    public void init(PluginContext context) {
        this.context = context;
        clipboard.addFlavorListener(this);
        menu.add(new JMenuItem(clearAction));
        menu.add(new JMenuItem(copyAction));
        menu.add(new JMenuItem(pasteAction));
        context.addContextMenuItem(menu);
        updateActionStates();

        Config.getConfig().addPropertyChangeListener("labelType", new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                setLabelType((LabelType) evt.getNewValue());
            }
        });

        setLabelType(Config.getConfig().getLabelType());
    }

    private void setLabelType(LabelType lt) {
        if (lt == LabelType.fileName) {
            lt = LabelType.caption;
        }
        menu.setText(lt.toString());
//        copyAction.putValue(Action.NAME, Msg.get("edit.copy") + " " + lt);
//        pasteAction.putValue(Action.NAME, Msg.get("edit.paste") + " " + lt);
    }

    public boolean onExit() {
        return true;
    }

    private void updateActionStates() {
        pasteAction.setEnabled(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor));
    }

    public void flavorsChanged(FlavorEvent e) {
//        System.out.println("Flavors changed");
        updateActionStates();
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
//        System.out.println("Lost ownership");
        updateActionStates();
    }

}
