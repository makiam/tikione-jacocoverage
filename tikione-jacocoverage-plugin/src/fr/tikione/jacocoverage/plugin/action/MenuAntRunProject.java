package fr.tikione.jacocoverage.plugin.action;

import fr.tikione.jacocoverage.plugin.Utils;
import fr.tikione.jacocoverage.plugin.config.Globals;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.swing.Action;
import javax.swing.JMenuItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DynamicMenuContent;
import org.openide.filesystems.FileObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

/**
 * The "Run with JaCoCoverage" contextual action registration.
 * Start the "run" Ant task with the JaCoCo JavaAgent correctly configured, colorize Java source files and show a coverage report.
 *
 * @author Jonathan Lermitage
 * @author Jan Lahoda (patch https://github.com/jonathanlermitage/tikione-jacocoverage/pull/3)
 */
@ActionID(category = "Project",
          id = "fr.tikione.jacocoverage.plugin.action.MenuAntRunProject")
@ActionRegistration(displayName = "#CTL_MenuAntRunProject",
                    lazy = false,
                    asynchronous = true,
                    surviveFocusChange = true)
@ActionReference(path = "Projects/Actions",
                 position = 1984,
                 separatorBefore = 1983)
@NbBundle.Messages("CTL_MenuAntRunProject=Run with JaCoCoverage")
public class MenuAntRunProject
        extends JaCoCoActionOnAnt
        implements ContextAwareAction, LookupListener, Presenter.Popup {

    private static final long serialVersionUID = 1L;

    public MenuAntRunProject() {
        this(Utilities.actionsGlobalContext());
    }

    public MenuAntRunProject(Lookup context) {
        super("run");
        setEnabled(Utils.isProjectSupported(getProject()));
        putValue(DynamicMenuContent.HIDE_WHEN_DISABLED, true);
        putValue(Action.NAME, Bundle.CTL_MenuAntRunProject());
        putValue(Action.SMALL_ICON, ImageUtilities.loadImageIcon(Globals.RUN_ICON, false));
        if (isEnabled()) { // Don't try to enable if project's type is not supported.
            FileObject prjPropsFo = getProject().getProjectDirectory().getFileObject("nbproject/project.properties");
            final Properties prjProps = new Properties();
            InputStream ins = null;
            try {
                if (prjPropsFo != null) {
                    prjProps.load(ins = prjPropsFo.getInputStream());
                }
                if (prjProps.getProperty("main.class", "").isEmpty()) {
                    setEnabled(false);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    if (ins != null) {
                        ins.close();
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (isEnabled()) {
            super.actionPerformed(ev);
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new MenuAntRunProject();
    }

    @Override
    public void resultChanged(LookupEvent ev) {
    }

    @Override
    public JMenuItem getPopupPresenter() {
        return new JMenuItem(this);
    }
}
