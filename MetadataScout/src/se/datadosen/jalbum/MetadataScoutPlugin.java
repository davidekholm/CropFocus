package se.datadosen.jalbum;

import com.drew.metadata.Metadata;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import se.datadosen.component.JSmartTextArea;
import se.datadosen.explorer.JAlbumObject;
import se.datadosen.explorer.JExplorerPanel;

/**
 *
 * @author david
 */
public class MetadataScoutPlugin implements JAlbumPlugin {

    final static KeyStroke plus = KeyStroke.getKeyStroke('+');
    final static KeyStroke minus = KeyStroke.getKeyStroke('-');
//    final static KeyStroke ctrlPlus = KeyStroke.getKeyStroke('+', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()); // Doesn't work
//    final static KeyStroke ctrlMinus = KeyStroke.getKeyStroke('-', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
    PluginContext context;
    Action listMetadataAction = new AbstractAction(Msg.get("ui.listMetadata")) {

        {
//            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_M, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.ALT_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JExplorerPanel explorer = context.getJAlbumContext().getExplorer().explorer;
            for (Component c : explorer.getComponents()) {
                JAlbumObject jao = (JAlbumObject) c;
                if (jao.isSelected() && !jao.ao.isFolder()) {
                    try {
                        listMetadata(jao);
                    } catch (IOException ex) {
                        System.err.println(ex);
                    }
                }
            }
        }

        private void listMetadata(JAlbumObject jao) throws IOException {
            final JDialog dialog = new JDialog(context.getJAlbumContext().getFrame(), Msg.get("ui.metadataFor", jao.ao.getName()));

            // Make sure ESC closes window
            KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            JComponent pane = dialog.getLayeredPane(); // So pressing escape won't conflict with menus
            Action closeAction = new AbstractAction("close") {

                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            };
            pane.registerKeyboardAction(closeAction, "close", ks, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

            AlbumObjectMetadata aoMeta = AlbumObjectMetadata.getInstance(jao.ao.getFile());
            String content = "";
            Metadata md = aoMeta.getMetadata();
            if (md != null) {
                Map meta = AlbumBean.getMetaObjectContent(md);
                StringBuilder buf = new StringBuilder();
                Map tm = new TreeMap(meta);
                for (Map.Entry e : (Set<Map.Entry>) tm.entrySet()) {
                    buf.append(e.getKey().toString());
                    buf.append("=");
                    buf.append((String) e.getValue());
                    buf.append('\n');
                }
                content = buf.toString();
            }
            final JTextArea textArea = new JSmartTextArea(content, 40, 40);
            JTextField dummyField = new JTextField();
            textArea.setFont(dummyField.getFont().deriveFont(dummyField.getFont().getSize2D() - 0));
            textArea.setEditable(false);
            dialog.add(new JScrollPane(textArea));
            dialog.pack();
            dialog.setLocationRelativeTo(jao);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

            Action largerFontAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Font f = textArea.getFont();
                    textArea.setFont(f.deriveFont(f.getSize2D() + 1));
                }
            };

            Action smallerFontAction = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Font f = textArea.getFont();
                    textArea.setFont(f.deriveFont(f.getSize2D() - 1));
                }
            };

            InputMap im = textArea.getInputMap(JComponent.WHEN_FOCUSED);
            im.put(minus, "smaller");
            im.put(plus, "larger");

            ActionMap am = textArea.getActionMap();
            am.put("smaller", smallerFontAction);
            am.put("larger", largerFontAction);

            dialog.setVisible(true);
        }
    };

    @Override
    public void init(PluginContext context) {
        this.context = context;
        context.addContextMenuItem(new JMenuItem(listMetadataAction));
    }

    @Override
    public boolean onExit() {
        return true;
    }
}
