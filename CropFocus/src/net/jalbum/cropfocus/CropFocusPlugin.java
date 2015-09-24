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

import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import se.datadosen.jalbum.AlbumObject;
import se.datadosen.jalbum.JAlbumPlugin;
import se.datadosen.jalbum.Msg;
import se.datadosen.jalbum.PluginContext;

/**
 * Allows the user to view and set the crop position of the FixedShapeFilter (horizontal and vertial weight)
 *
 * @author david
 */
public class CropFocusPlugin implements JAlbumPlugin {

    private static final ImageIcon smallIcon = new ImageIcon(CropFocusPlugin.class.getResource("cropfocus.png"));
    private PluginContext context;

    private final Action openDialogAction = new AbstractAction(Msg.getString(this, "ui.setCropFocus") + "...", smallIcon) {
        {
//            putValue(Action.SMALL_ICON, smallIcon);
            putValue(Action.SHORT_DESCRIPTION, Msg.getString(this, "ui.setCropFocusToolTip"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AlbumObject[] all = context.getSelectedAlbumObjects();
            int offset = 0;
            for (AlbumObject ao : all) {
                if (ao.getRepresentingAlbumObject() != null) {
                    ao = ao.getRepresentingAlbumObject();
                }
                if (ao.isDisplayableImage()) {
                    JCropFocusDialog dialog = new JCropFocusDialog(context, ao);
                    dialog.setLocationRelativeTo(context.getJAlbumContext().getFrame());
                    dialog.setLocation(dialog.getLocation().x + offset, dialog.getLocation().y);
                    dialog.setVisible(true);
                    offset += 10;
                }
            }
        }
    };

    @Override
    public void init(PluginContext context) {
        this.context = context;
        context.addContextMenuItem(new JMenuItem(openDialogAction));
    }

    @Override
    public boolean onExit() {
        return true;
    }
}
